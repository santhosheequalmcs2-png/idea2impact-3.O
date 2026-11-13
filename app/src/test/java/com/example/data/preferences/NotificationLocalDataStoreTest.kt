package com.example.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendProposal
import com.example.data.model.DashboardStats
import com.example.data.model.NotificationType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NotificationLocalDataStoreTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private fun withStore(
        clock: () -> Long = { 1_700_000_000_000L },
        block: suspend (NotificationLocalDataStore) -> Unit
    ) = runBlocking<Unit> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = tempFolder.newFile("notifications_${System.nanoTime()}.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file }
            )
            val store = NotificationLocalDataStore(
                dataStore = dataStore,
                timeProvider = clock
            )
            block(store)
        } finally {
            scope.cancel()
        }
    }

    private fun sampleInnovation(
        id: String = "inn-1",
        title: String = "Solar Grid Micro-Inverter",
        isLiked: Boolean = true,
        likeCount: Int = 14
    ) = BackendInnovation(
        id = id,
        title = title,
        description = "High-efficiency modular inverter",
        category = "Green Tech",
        stage = "Prototype",
        fundingGoal = 120_000.0,
        valuation = 800_000.0,
        imageUrl = null,
        authorId = "user-1",
        authorName = "Maya Lin",
        authorTitle = "Founder",
        authorAvatar = null,
        isVerifiedCreator = true,
        likeCount = likeCount,
        commentCount = 2,
        isLiked = isLiked,
        isBookmarked = false
    )

    @Test
    fun ensureSeeded_seedsPlatformUpdatesIdempotently() = withStore { store ->
        store.ensureSeeded()
        val firstList = store.alerts.first()
        val firstUnread = store.unreadCount.first()

        assertThat(firstList).hasSize(3)
        assertThat(firstList.all { it.type == NotificationType.PLATFORM_UPDATE }).isTrue()
        assertThat(firstUnread).isEqualTo(2)

        // Calling ensureSeeded again should not duplicate platform updates
        store.ensureSeeded()
        val secondList = store.alerts.first()
        assertThat(secondList).hasSize(3)
    }

    @Test
    fun recordProposalSubmittedAndLikeAction_persistUnreadAlertsSortedNewestFirst() {
        var now = 1_000_000L
        withStore(clock = { now }) { store ->
            val inn = sampleInnovation()
            store.recordProposalSubmitted(
                innovation = inn,
                innovationId = inn.id,
                proposalType = "Investment",
                amount = "$50,000"
            )

            now = 2_000_000L
            store.recordLikeAction(inn)

            // Unliked state should not emit a like alert
            now = 3_000_000L
            store.recordLikeAction(inn.copy(isLiked = false))

            val alerts = store.alerts.first()
            assertThat(alerts).hasSize(2)
            assertThat(alerts[0].type).isEqualTo(NotificationType.LIKE)
            assertThat(alerts[0].innovationId).isEqualTo("inn-1")
            assertThat(alerts[1].type).isEqualTo(NotificationType.PROPOSAL)
            assertThat(alerts[1].message).contains("$50,000")
            assertThat(store.unreadCount.first()).isEqualTo(2)
        }
    }

    @Test
    fun syncDashboardActivity_deduplicatesInboundProposalsAndTracksUpvoteDeltas() {
        var now = 5_000_000L
        withStore(clock = { now }) { store ->
            val proposal1 = BackendProposal(
                id = "prop-1",
                innovationId = "inn-1",
                innovationTitle = "Solar Grid Micro-Inverter",
                senderName = "Apex Ventures",
                proposalType = "Investment",
                amount = "$100k",
                message = "Let's discuss Series Seed.",
                contactInfo = "apex@vc.io",
                status = "Pending",
                createdAt = "2026-01-01"
            )

            // First sync establishes upvote baseline (10) and captures proposal1
            store.syncDashboardActivity(
                DashboardStats(
                    totalUpvotes = 10,
                    myInnovations = listOf(sampleInnovation()),
                    inboundProposals = listOf(proposal1)
                )
            )
            assertThat(store.alerts.first().map { it.id }).containsExactly("proposal_in_prop-1")

            // Repeat sync with identical stats creates no duplicates
            now += 10_000L
            store.syncDashboardActivity(
                DashboardStats(
                    totalUpvotes = 10,
                    myInnovations = listOf(sampleInnovation()),
                    inboundProposals = listOf(proposal1)
                )
            )
            assertThat(store.alerts.first()).hasSize(1)

            // Subsequent sync with higher upvotes (+3) and a second proposal creates both new alerts
            now += 10_000L
            val proposal2 = proposal1.copy(id = "prop-2", senderName = "Helena Labs", proposalType = "Co-Founder", amount = "")
            store.syncDashboardActivity(
                DashboardStats(
                    totalUpvotes = 13,
                    myInnovations = listOf(sampleInnovation()),
                    inboundProposals = listOf(proposal1, proposal2)
                )
            )

            val alerts = store.alerts.first()
            assertThat(alerts).hasSize(3)
            assertThat(alerts.any { it.type == NotificationType.LIKE && it.title.contains("+3") }).isTrue()
            assertThat(alerts.any { it.id == "proposal_in_prop-2" }).isTrue()
        }
    }

    @Test
    fun readDeleteAndClearActions_updateUnreadCountsAndPersistence() {
        var now = 9_000_000L
        withStore(clock = { now }) { store ->
            store.recordPlatformUpdate(
                title = "Update A",
                message = "First update",
                dedupeKey = "upd_a"
            )
            now += 1_000L
            store.recordPlatformUpdate(
                title = "Update B",
                message = "Second update",
                dedupeKey = "upd_b"
            )
            // Duplicate dedupeKey should be ignored
            store.recordPlatformUpdate(
                title = "Update B Duplicate",
                message = "Should ignore",
                dedupeKey = "upd_b"
            )

            assertThat(store.alerts.first()).hasSize(2)
            assertThat(store.unreadCount.first()).isEqualTo(2)

            store.markAsRead("upd_a")
            assertThat(store.unreadCount.first()).isEqualTo(1)

            store.markAllRead()
            assertThat(store.unreadCount.first()).isEqualTo(0)

            store.deleteAlert("upd_a")
            assertThat(store.alerts.first().map { it.id }).containsExactly("upd_b")

            store.clearAll()
            assertThat(store.alerts.first()).isEmpty()
            assertThat(store.unreadCount.first()).isEqualTo(0)
        }
    }
}
