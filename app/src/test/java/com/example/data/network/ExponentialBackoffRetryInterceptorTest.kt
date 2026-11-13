package com.example.data.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ExponentialBackoffRetryInterceptorTest {

    private class FakeBackendInterceptor(
        private val handler: (attempt: Int, path: String) -> Response
    ) : Interceptor {
        var attempts = 0
            private set

        override fun intercept(chain: Interceptor.Chain): Response {
            attempts += 1
            return handler(attempts, chain.request().url.encodedPath)
        }
    }

    private fun buildTestApi(
        config: RetryPolicyConfig = RetryPolicyConfig(),
        jitterProvider: () -> Double = { 0.5 },
        sleeps: MutableList<Long>,
        fakeBackend: FakeBackendInterceptor
    ): Idea2ImpactApiService {
        val retryInterceptor = ExponentialBackoffRetryInterceptor(
            config = config,
            sleeper = { ms -> sleeps += ms },
            jitterProvider = jitterProvider
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)
            .addInterceptor(fakeBackend)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://idea2impact.test/api/")
            .client(client)
            .build()
            .create(Idea2ImpactApiService::class.java)
    }

    private fun jsonResponse(
        chainRequest: okhttp3.Request,
        code: Int,
        body: String = "{}",
        retryAfter: String? = null
    ): Response {
        val builder = Response.Builder()
            .request(chainRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
        if (retryAfter != null) {
            builder.header("Retry-After", retryAfter)
        }
        return builder.build()
    }

    private fun responseForPath(
        path: String,
        method: String = "GET",
        code: Int,
        body: String = "{}",
        retryAfter: String? = null
    ): Response {
        val reqBuilder = okhttp3.Request.Builder()
            .url("https://idea2impact.test$path")
        if (method == "POST") {
            reqBuilder.post("{}".toRequestBody("application/json".toMediaType()))
        } else {
            reqBuilder.get()
        }
        return jsonResponse(reqBuilder.build(), code, body, retryAfter)
    }

    @Test
    fun getInnovations_recoversAfterTransientIOExceptions_withExponentialBackoff() = runBlocking<Unit> {
        val sleeps = mutableListOf<Long>()
        val backend = FakeBackendInterceptor { attempt, path ->
            when (attempt) {
                1 -> throw SocketTimeoutException("read timed out")
                2 -> throw UnknownHostException("DNS lookup failed")
                3 -> throw ConnectException("connection reset")
                else -> responseForPath(path, code = 200, body = """{"innovations":[]}""")
            }
        }
        val api = buildTestApi(sleeps = sleeps, fakeBackend = backend)

        val response = api.getInnovations()

        assertThat(response.isSuccessful).isTrue()
        assertThat(backend.attempts).isEqualTo(4)
        assertThat(sleeps).containsExactly(1_000L, 2_000L, 4_000L).inOrder()
    }

    @Test
    fun getTalents_recoversAfterRetryableServer5xxErrors() = runBlocking<Unit> {
        val sleeps = mutableListOf<Long>()
        val backend = FakeBackendInterceptor { attempt, path ->
            when (attempt) {
                1 -> responseForPath(path, code = 503)
                2 -> responseForPath(path, code = 502)
                3 -> responseForPath(path, code = 500)
                else -> responseForPath(path, code = 200, body = """{"talents":[]}""")
            }
        }
        val api = buildTestApi(sleeps = sleeps, fakeBackend = backend)

        val response = api.getTalents()

        assertThat(response.isSuccessful).isTrue()
        assertThat(backend.attempts).isEqualTo(4)
        assertThat(sleeps).containsExactly(1_000L, 2_000L, 4_000L).inOrder()
    }

    @Test
    fun getInnovations_exhaustsMaxRetriesAndThrowsLastIOException() = runBlocking<Unit> {
        val sleeps = mutableListOf<Long>()
        val backend = FakeBackendInterceptor { _, _ ->
            throw IOException("unstable network")
        }
        val api = buildTestApi(sleeps = sleeps, fakeBackend = backend)

        var caught: IOException? = null
        try {
            api.getInnovations()
        } catch (e: IOException) {
            caught = e
        }

        assertThat(caught).isNotNull()
        assertThat(backend.attempts).isEqualTo(4)
        assertThat(sleeps).containsExactly(1_000L, 2_000L, 4_000L).inOrder()
    }

    @Test
    fun http429_respectsRetryAfterHeaderAndCapsAtMaxDelay() = runBlocking<Unit> {
        val sleeps = mutableListOf<Long>()
        val backend = FakeBackendInterceptor { attempt, path ->
            when (attempt) {
                1 -> responseForPath(path, code = 429, retryAfter = "5")
                2 -> responseForPath(path, code = 429, retryAfter = "600")
                else -> responseForPath(path, code = 200, body = """{"categories":[]}""")
            }
        }
        val api = buildTestApi(
            config = RetryPolicyConfig(maxRetries = 3, initialDelayMs = 1_000L, maxDelayMs = 12_000L),
            sleeps = sleeps,
            fakeBackend = backend
        )

        val response = api.getCategories()

        assertThat(response.isSuccessful).isTrue()
        assertThat(backend.attempts).isEqualTo(3)
        assertThat(sleeps).containsExactly(5_000L, 12_000L).inOrder()
    }

    @Test
    fun clientErrors4xx_doNotRetry() = runBlocking<Unit> {
        val sleeps = mutableListOf<Long>()
        val backend = FakeBackendInterceptor { _, path ->
            responseForPath(path, code = 404)
        }
        val api = buildTestApi(sleeps = sleeps, fakeBackend = backend)

        val response = api.getInnovationDetail("missing-id")

        assertThat(response.code()).isEqualTo(404)
        assertThat(backend.attempts).isEqualTo(1)
        assertThat(sleeps).isEmpty()
    }

    @Test
    fun nonIdempotentPost_doesNotRetry_whileSafeLoginPostRetries() = runBlocking<Unit> {
        val commentSleeps = mutableListOf<Long>()
        val commentBackend = FakeBackendInterceptor { _, path ->
            responseForPath(path, method = "POST", code = 503)
        }
        val commentApi = buildTestApi(sleeps = commentSleeps, fakeBackend = commentBackend)

        val commentResponse = commentApi.postComment(
            id = "inn-1",
            body = """{"content":"Great"}""".toRequestBody("application/json".toMediaType())
        )
        assertThat(commentResponse.code()).isEqualTo(503)
        assertThat(commentBackend.attempts).isEqualTo(1)
        assertThat(commentSleeps).isEmpty()

        val loginSleeps = mutableListOf<Long>()
        val loginBackend = FakeBackendInterceptor { attempt, path ->
            if (attempt == 1) responseForPath(path, method = "POST", code = 503)
            else responseForPath(path, method = "POST", code = 200, body = """{"token":"jwt"}""")
        }
        val loginApi = buildTestApi(sleeps = loginSleeps, fakeBackend = loginBackend)

        val loginResponse = loginApi.login(
            body = """{"email":"a@b.com","password":"pw"}""".toRequestBody("application/json".toMediaType())
        )
        assertThat(loginResponse.isSuccessful).isTrue()
        assertThat(loginBackend.attempts).isEqualTo(2)
        assertThat(loginSleeps).containsExactly(1_000L)
    }

    @Test
    fun computeDelayMs_boundsJitterWithinConfiguredRatio() {
        val minJitterInterceptor = ExponentialBackoffRetryInterceptor(
            config = RetryPolicyConfig(initialDelayMs = 1_000L, backoffMultiplier = 2.0, jitterRatio = 0.25),
            jitterProvider = { 0.0 }
        )
        val maxJitterInterceptor = ExponentialBackoffRetryInterceptor(
            config = RetryPolicyConfig(initialDelayMs = 1_000L, backoffMultiplier = 2.0, jitterRatio = 0.25),
            jitterProvider = { 1.0 }
        )

        assertThat(minJitterInterceptor.computeDelayMs(retryAttempt = 1)).isEqualTo(750L)
        assertThat(maxJitterInterceptor.computeDelayMs(retryAttempt = 1)).isEqualTo(1_250L)
        assertThat(minJitterInterceptor.computeDelayMs(retryAttempt = 2)).isEqualTo(1_500L)
        assertThat(maxJitterInterceptor.computeDelayMs(retryAttempt = 2)).isEqualTo(2_500L)
    }
}
