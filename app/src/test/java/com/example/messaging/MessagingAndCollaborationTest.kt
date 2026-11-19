package com.example.messaging

import com.example.data.model.CloudNotification
import com.example.data.model.CollaborationTask
import com.example.data.model.CollaborationProgressCalculator
import com.example.data.model.Conversation
import com.example.data.model.ConversationType
import com.example.data.model.NotificationAlert
import com.example.data.model.NotificationType
import com.example.data.model.TaskStatus
import com.example.data.repository.MessagingRepository
import com.example.ui.viewmodel.AppViewModel
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MessagingAndCollaborationTest {

    @Test
    fun buildDeterministicDirectConversationId_isSymmetricAndSafe() {
        val ab = MessagingRepository.buildDeterministicDirectConversationId("user_b", "user_a")
        val ba = MessagingRepository.buildDeterministicDirectConversationId("user_a", "user_b")

        assertThat(ab).isEqualTo(ba)
        assertThat(ab).isEqualTo("dm_user_a_user_b")
        assertThat(MessagingRepository.isSafeFirestoreId(ab)).isTrue()
    }

    @Test
    fun sanitizeIdSegment_stripsDisallowedCharsAndHandlesEmpty() {
        assertThat(MessagingRepository.sanitizeIdSegment("hello@world/test!")).isEqualTo("hello_world_test")
        assertThat(MessagingRepository.sanitizeIdSegment("   ")).isEqualTo("peer")
        assertThat(MessagingRepository.buildShowcaseDirectConversationId("uid1", "t_dr_meera"))
            .isEqualTo("dm_uid1_p_t_dr_meera")
    }

    @Test
    fun conversationHelpers_resolvePeerDisplayUnreadTypingAndBlocks() {
        val nowMs = 1_750_000_000_000L
        val conv = Conversation(
            id = "dm_u1_u2",
            type = ConversationType.DIRECT.name,
            memberUids = listOf("u1", "u2"),
            memberSummaries = mapOf(
                "u1" to mapOf("name" to "Aarav", "role" to "Creator"),
                "u2" to mapOf("name" to "Meera", "role" to "Investor", "avatarUrl" to "https://example.com/m.png")
            ),
            unreadCounts = mapOf("u1" to 3L, "u2" to 0L),
            typingByUid = mapOf(
                "u1" to Timestamp(Date(nowMs - 1000L)),
                "u2" to Timestamp(Date(nowMs - 2000L))
            ),
            blockedByUids = listOf("u2")
        )

        assertThat(conv.peerUidFor("u1")).isEqualTo("u2")
        assertThat(conv.displayTitleFor("u1")).isEqualTo("Meera")
        assertThat(conv.displayRoleFor("u1")).isEqualTo("Investor")
        assertThat(conv.displayAvatarFor("u1")).isEqualTo("https://example.com/m.png")
        assertThat(conv.unreadFor("u1")).isEqualTo(3)
        assertThat(conv.unreadFor("u2")).isEqualTo(0)
        assertThat(conv.activeTypingNames("u1", nowMs)).containsExactly("Meera")
        assertThat(conv.isBlockedByAny()).isTrue()
        assertThat(conv.isBlockedByMe("u1")).isFalse()
        assertThat(conv.isBlockedByMe("u2")).isTrue()
    }

    @Test
    fun collaborationProgressCalculator_computesAccurateCompletionPercent() {
        assertThat(CollaborationProgressCalculator.computePercent(emptyList())).isEqualTo(0)

        val partial = listOf(
            CollaborationTask(id = "t1", status = TaskStatus.COMPLETED.name),
            CollaborationTask(id = "t2", status = TaskStatus.IN_PROGRESS.name),
            CollaborationTask(id = "t3", status = TaskStatus.TODO.name),
            CollaborationTask(id = "t4", status = TaskStatus.COMPLETED.name)
        )
        assertThat(CollaborationProgressCalculator.computePercent(partial)).isEqualTo(50)

        val done = partial.map { it.copy(status = TaskStatus.COMPLETED.name) }
        assertThat(CollaborationProgressCalculator.computePercent(done)).isEqualTo(100)
    }

    @Test
    fun mergeNotificationAlerts_mapsRoutesDeduplicatesAndSortsDescending() {
        val local = listOf(
            NotificationAlert(
                id = "local_1",
                type = NotificationType.PLATFORM_UPDATE,
                title = "Pitch Deck Ready",
                message = "Your AI deck is ready.",
                createdAtEpochMs = 2000L,
                isRead = true
            )
        )
        val cloud = listOf(
            CloudNotification(
                id = "c_msg",
                notificationType = "MESSAGE",
                title = "Meera sent a message",
                body = "Let's discuss licensing.",
                targetType = "CONVERSATION",
                targetId = "dm_u1_u2",
                read = false,
                createdAt = Timestamp(Date(5000L))
            ),
            CloudNotification(
                id = "c_inv",
                notificationType = "COLLAB_INVITE",
                title = "Project Invitation",
                body = "Join Solar Grid Hardware",
                targetType = "COLLABORATION",
                targetId = "proj_1",
                read = false,
                createdAt = Timestamp(Date(3000L))
            ),
            CloudNotification(
                id = "c_task",
                notificationType = "TASK_ASSIGNED",
                title = "Task Assigned",
                body = "Complete CAD enclosure",
                targetType = "COLLABORATION",
                targetId = "proj_1",
                read = true,
                createdAt = Timestamp(Date(4000L))
            )
        )

        val merged = AppViewModel.mergeNotificationAlerts(local, cloud)
        assertThat(merged).hasSize(4)
        assertThat(merged.map { it.id }).containsExactly("cloud_c_msg", "cloud_c_task", "cloud_c_inv", "local_1").inOrder()

        assertThat(merged[0].type).isEqualTo(NotificationType.MESSAGE)
        assertThat(merged[0].targetRoute).isEqualTo("messages")
        assertThat(merged[0].targetId).isEqualTo("dm_u1_u2")

        assertThat(merged[1].type).isEqualTo(NotificationType.TASK)
        assertThat(merged[1].targetRoute).isEqualTo("collaborations")
        assertThat(merged[1].targetId).isEqualTo("proj_1")

        assertThat(merged[2].type).isEqualTo(NotificationType.COLLABORATION)
        assertThat(merged[2].targetRoute).isEqualTo("collaborations")
    }
}
