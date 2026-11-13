package com.example.data.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.math.pow

/**
 * Configuration for OkHttp/Retrofit exponential backoff retry behavior on unstable connections.
 */
data class RetryPolicyConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 12_000L,
    val backoffMultiplier: Double = 2.0,
    val jitterRatio: Double = 0.25,
    val retryableStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504),
    val retryablePostPathSuffixes: Set<String> = setOf("/auth/login", "/pitch-deck", "/ai/chat")
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(initialDelayMs >= 0L) { "initialDelayMs must be non-negative" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be >= 1.0" }
        require(jitterRatio in 0.0..1.0) { "jitterRatio must be in 0.0..1.0" }
    }
}

/**
 * OkHttp application [Interceptor] implementing exponential backoff with bounded jitter and
 * `Retry-After` awareness for Retrofit API endpoints and innovation feeds.
 *
 * Retries transient [IOException] failures (timeouts, DNS drops, socket resets) and
 * retryable HTTP status codes (`408`, `429`, `5xx`) on idempotent HTTP methods (`GET`,
 * `HEAD`, `PUT`, `DELETE`, `OPTIONS`) plus explicitly safe `POST` endpoints.
 */
class ExponentialBackoffRetryInterceptor(
    private val config: RetryPolicyConfig = RetryPolicyConfig(),
    private val sleeper: (Long) -> Unit = { ms -> Thread.sleep(ms) },
    private val jitterProvider: () -> Double = { Math.random() },
    private val logger: (String) -> Unit = {}
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val rawRequest = chain.request()
        val retryable = isRequestRetryable(rawRequest)
        val request = stripControlHeaders(rawRequest)

        if (!retryable || config.maxRetries == 0) {
            return chain.proceed(request)
        }

        var attempt = 0
        var lastException: IOException? = null

        while (true) {
            if (chain.call().isCanceled()) {
                throw InterruptedIOException("Canceled")
            }

            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code !in config.retryableStatusCodes || attempt >= config.maxRetries) {
                    return response
                }

                val statusCode = response.code
                val retryAfterHeader = response.header("Retry-After")
                response.close()

                attempt += 1
                val delayMs = computeDelayMs(attempt, retryAfterHeader)
                logger(
                    "HTTP $statusCode on ${request.method} ${request.url.encodedPath}; " +
                        "retry $attempt/${config.maxRetries} in ${delayMs}ms"
                )
                sleepRespectingCancellation(chain, delayMs)
            } catch (e: IOException) {
                lastException = e
                if (chain.call().isCanceled() || e.isCancellationException() || attempt >= config.maxRetries) {
                    throw e
                }

                attempt += 1
                val delayMs = computeDelayMs(attempt, retryAfterHeader = null)
                logger(
                    "Network error (${e.javaClass.simpleName}) on ${request.method} ${request.url.encodedPath}; " +
                        "retry $attempt/${config.maxRetries} in ${delayMs}ms"
                )
                sleepRespectingCancellation(chain, delayMs)
            }
        }
    }

    internal fun computeDelayMs(retryAttempt: Int, retryAfterHeader: String? = null): Long {
        val exponent = (retryAttempt - 1).coerceAtLeast(0)
        val baseDelay = (config.initialDelayMs * config.backoffMultiplier.pow(exponent))
            .toLong()
            .coerceIn(0L, config.maxDelayMs)

        val unitRandom = jitterProvider().coerceIn(0.0, 1.0)
        val jitterMultiplier = 1.0 + ((unitRandom * 2.0 - 1.0) * config.jitterRatio)
        val jitteredDelay = (baseDelay * jitterMultiplier)
            .toLong()
            .coerceIn(0L, config.maxDelayMs)

        val retryAfterSeconds = retryAfterHeader?.trim()?.toLongOrNull()
        val retryAfterMs = if (retryAfterSeconds != null && retryAfterSeconds > 0L) {
            (retryAfterSeconds * 1_000L).coerceAtMost(config.maxDelayMs)
        } else {
            0L
        }
        return maxOf(jitteredDelay, retryAfterMs)
    }

    internal fun isRequestRetryable(request: Request): Boolean {
        if (request.header(HEADER_NO_RETRY).equals("true", ignoreCase = true)) {
            return false
        }
        if (request.header(HEADER_FORCE_RETRY).equals("true", ignoreCase = true)) {
            return true
        }
        val method = request.method.uppercase()
        if (method in IDEMPOTENT_METHODS) {
            return true
        }
        if (method == "POST") {
            val path = request.url.encodedPath
            return config.retryablePostPathSuffixes.any { suffix -> path.endsWith(suffix) }
        }
        return false
    }

    private fun stripControlHeaders(request: Request): Request {
        if (request.header(HEADER_NO_RETRY) == null && request.header(HEADER_FORCE_RETRY) == null) {
            return request
        }
        return request.newBuilder()
            .removeHeader(HEADER_NO_RETRY)
            .removeHeader(HEADER_FORCE_RETRY)
            .build()
    }

    private fun sleepRespectingCancellation(chain: Interceptor.Chain, delayMs: Long) {
        if (delayMs <= 0L) return
        if (chain.call().isCanceled()) {
            throw InterruptedIOException("Canceled")
        }
        try {
            sleeper(delayMs)
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Interrupted during retry backoff").apply { initCause(ie) }
        }
        if (chain.call().isCanceled()) {
            throw InterruptedIOException("Canceled")
        }
    }

    private fun IOException.isCancellationException(): Boolean {
        return this is InterruptedIOException && message.equals("Canceled", ignoreCase = true)
    }

    companion object {
        const val HEADER_NO_RETRY = "X-No-Retry"
        const val HEADER_FORCE_RETRY = "X-Force-Retry"
        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "PUT", "DELETE", "OPTIONS")
    }
}
