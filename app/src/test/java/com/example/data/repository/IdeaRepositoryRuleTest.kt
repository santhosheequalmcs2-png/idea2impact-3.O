package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class IdeaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserAndSeedCatalog_authenticatedOwner_createsProfileAndSeedsPublicMarketplace() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = IdeaRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserAndSeedCatalog("Alice Chen", ALICE_EMAIL, null)
        }

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertThat(profile?.userId).isEqualTo(uid)
        assertThat(profile?.name).isEqualTo("Alice Chen")

        val publicItems = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicInnovations().first { it.isNotEmpty() }
        }
        assertThat(publicItems.size).isAtLeast(10)

        val talents = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTalents().first { it.isNotEmpty() }
        }
        assertThat(talents.size).isAtLeast(5)
    }

    @Test
    fun createAndUpdateInnovation_authenticatedOwner_persistsAndStreamsRealtime() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = IdeaRepository(firestore, auth)
        val uniqueTitle = "Solar Harvester ${UUID.randomUUID().toString().take(8)}"

        val createdId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createInnovation(
                title = uniqueTitle,
                description = "High-efficiency thermal water condenser for off-grid clinics.",
                category = "Inventions",
                price = 250.0,
                visibility = "PUBLIC",
                licensing = true,
                funding = true,
                imageUrl = null,
                stageIndex = 1L,
                authorProfile = UserProfile(userId = uid, name = "Alice Chen", headline = "Hardware Founder")
            )
        }
        assertThat(createdId).isNotEmpty()

        val mine = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyInnovations(uid).first { list -> list.any { it.id == createdId } }
        }
        val created = mine.first { it.id == createdId }
        assertThat(created.title).isEqualTo(uniqueTitle)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateInnovation(created.copy(price = 500.0, stageIndex = 2L))
        }

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyInnovations(uid).first { list ->
                list.any { it.id == createdId && it.price == 500.0 && it.stageIndex == 2L }
            }
        }
        assertThat(updated.first { it.id == createdId }.stageIndex).isEqualTo(2L)
    }

    @Test
    fun likesCommentsProposalsNdaAndPledge_fullWorkflow_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = IdeaRepository(firestore, auth)
        val innovationId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createInnovation(
                title = "NeuroGrip ${UUID.randomUUID().toString().take(6)}",
                description = "Myoelectric bionic prosthesis prototype with tactile feedback.",
                category = "Physical Products",
                price = 790.0,
                visibility = "PUBLIC",
                licensing = true,
                funding = true,
                imageUrl = null,
                stageIndex = 2L,
                authorProfile = UserProfile(userId = aliceUid, name = "Alice Chen")
            )
        }
        val createdInnovation = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observePublicInnovations().first { list -> list.any { it.id == innovationId } }
                .first { it.id == innovationId }
        }

        // Bob likes, comments, signs NDA, backs, and submits a proposal
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = IdeaRepository(firestore, auth)
        val bobProfile = UserProfile(userId = bobUid, name = "Bob Vance", headline = "Seed Investor", location = "London")

        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleLike(createdInnovation, currentlyLiked = false)
            bobRepo.toggleSave(createdInnovation, currentlySaved = false)
            bobRepo.signDigitalNda(createdInnovation, "Bob Vance", "Apex Horizons")
            bobRepo.submitMicroPledge(createdInnovation, "Bob Vance", 25.0, "Keep shipping!", bobProfile)
            bobRepo.submitProposal(
                innovation = createdInnovation,
                proposalType = "Investment",
                amount = "$50,000 Seed",
                message = "Interested in leading a pre-seed hardware prototyping tranche.",
                contactInfo = "bob@apex.example",
                senderProfile = bobProfile
            )
        }

        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeComments(innovationId).first { it.isNotEmpty() }
        }
        assertThat(comments.first().body).contains("$25")

        val ndas = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeSignedNdas(bobUid).first { it.containsKey(innovationId) }
        }
        assertThat(ndas[innovationId]?.signerName).isEqualTo("Bob Vance")

        // Alice receives and accepts Bob's proposal
        signInTestUser(ALICE_EMAIL)
        val incoming = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeIncomingProposals(aliceUid).first { list ->
                list.any { it.innovationId == innovationId && it.status == "PENDING" }
            }
        }
        val proposal = incoming.first { it.innovationId == innovationId }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateProposalStatus(proposal.id, "ACCEPTED")
        }

        val accepted = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeIncomingProposals(aliceUid).first { list ->
                list.any { it.id == proposal.id && it.status == "ACCEPTED" }
            }
        }
        assertThat(accepted).isNotEmpty()
    }

    @Test
    fun crossUserMutationAndPrivateAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = IdeaRepository(firestore, auth)
        val privateId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createInnovation(
                title = "Stealth Patent ${UUID.randomUUID().toString().take(6)}",
                description = "Confidential solid-state electrolyte chemistry filing.",
                category = "Inventions",
                price = 0.0,
                visibility = "PRIVATE",
                licensing = false,
                funding = false,
                imageUrl = null,
                stageIndex = 0L,
                authorProfile = UserProfile(userId = aliceUid, name = "Alice Chen")
            )
        }

        // Switch to Bob and verify cross-user reads/deletes fail
        signInTestUser(BOB_EMAIL)
        val bobRepo = IdeaRepository(firestore, auth)

        val readException = assertFailsWith<FirebaseFirestoreException> {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
        }
        assertThat(readException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)

        val deleteException = assertFailsWith<FirebaseFirestoreException> {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.deleteInnovation(privateId)
            }
        }
        assertThat(deleteException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    @Test
    fun observePublicInnovations_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = IdeaRepository(firestore, auth)

        val exception = assertFailsWith<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublicInnovations().first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $exception"
        }
        assertThat(firestoreException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_i2i@test.com"
        const val BOB_EMAIL = "bob_i2i@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
