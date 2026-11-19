package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CollaborationStatus(val label: String) {
    PLANNING("Planning"),
    ACTIVE("Active"),
    REVIEW("In Review"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold");

    companion object {
        fun fromString(raw: String?): CollaborationStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    companion object {
        fun fromString(raw: String?): TaskPriority =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    COMPLETED("Completed");

    companion object {
        fun fromString(raw: String?): TaskStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TODO
    }
}

enum class InvitationStatus(val label: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    DECLINED("Declined"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(raw: String?): InvitationStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class CollaborationProject(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Software",
    val relatedInnovationId: String? = null,
    val relatedInnovationTitle: String? = null,
    val requiredSkills: List<String> = emptyList(),
    val objectives: String = "",
    val expectedDeliverables: String = "",
    val proposedTimeline: String = "",
    val referenceLinks: List<String> = emptyList(),
    val status: String = CollaborationStatus.ACTIVE.name,
    val ownerId: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val memberRoles: Map<String, String> = emptyMap(),
    val memberNames: Map<String, String> = emptyMap(),
    val conversationId: String? = null,
    val progressPercent: Long = 0L,
    val totalTasks: Long = 0L,
    val completedTasks: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): CollaborationStatus = CollaborationStatus.fromString(status)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "title" to title.take(140),
            "description" to description.take(4000),
            "category" to category.take(80),
            "relatedInnovationId" to relatedInnovationId?.takeIf { it.isNotBlank() }?.take(128),
            "relatedInnovationTitle" to relatedInnovationTitle?.takeIf { it.isNotBlank() }?.take(160),
            "requiredSkills" to requiredSkills.take(20).map { it.take(64) },
            "objectives" to objectives.take(2000),
            "expectedDeliverables" to expectedDeliverables.take(2000),
            "proposedTimeline" to proposedTimeline.take(300),
            "referenceLinks" to referenceLinks.take(10).map { it.take(300) },
            "status" to status,
            "ownerId" to ownerId,
            "adminUids" to adminUids.distinct().take(50),
            "memberUids" to memberUids.distinct().take(50),
            "memberRoles" to memberRoles,
            "memberNames" to memberNames,
            "conversationId" to conversationId?.takeIf { it.isNotBlank() }?.take(128),
            "progressPercent" to progressPercent.coerceIn(0L, 100L),
            "totalTasks" to totalTasks.coerceAtLeast(0L),
            "completedTasks" to completedTasks.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class CollaborationInvitation(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val projectCategory: String = "Software",
    val inviterId: String = "",
    val inviterName: String = "",
    val inviteeId: String = "",
    val inviteeName: String = "",
    val proposedRole: String = "Collaborator",
    val message: String = "",
    val conversationId: String? = null,
    val status: String = InvitationStatus.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): InvitationStatus = InvitationStatus.fromString(status)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "projectId" to projectId,
            "projectTitle" to projectTitle.take(140),
            "projectCategory" to projectCategory.take(80),
            "inviterId" to inviterId,
            "inviterName" to inviterName.ifBlank { "Innovator" }.take(120),
            "inviteeId" to inviteeId,
            "inviteeName" to inviteeName.ifBlank { "Collaborator" }.take(120),
            "proposedRole" to proposedRole.ifBlank { "Collaborator" }.take(80),
            "message" to message.take(1000),
            "conversationId" to conversationId?.takeIf { it.isNotBlank() }?.take(128),
            "status" to InvitationStatus.PENDING.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class CollaborationTask(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val assigneeUid: String? = null,
    val assigneeName: String? = null,
    val createdBy: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.TODO.name,
    val dueAtEpochMs: Long? = null,
    val attachmentName: String? = null,
    val commentCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedPriority(): TaskPriority = TaskPriority.fromString(priority)
    fun parsedStatus(): TaskStatus = TaskStatus.fromString(status)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "projectId" to projectId,
            "memberUids" to memberUids.distinct().take(50),
            "title" to title.take(160),
            "description" to description.take(2000),
            "assigneeUid" to assigneeUid?.takeIf { it.isNotBlank() }?.take(128),
            "assigneeName" to assigneeName?.takeIf { it.isNotBlank() }?.take(120),
            "createdBy" to createdBy,
            "priority" to priority,
            "status" to status,
            "dueAtEpochMs" to dueAtEpochMs?.coerceAtLeast(0L),
            "attachmentName" to attachmentName?.takeIf { it.isNotBlank() }?.take(200),
            "commentCount" to commentCount.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class TaskComment(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val taskId: String = "",
    val memberUids: List<String> = emptyList(),
    val authorId: String = "",
    val authorName: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "projectId" to projectId,
        "taskId" to taskId,
        "memberUids" to memberUids.distinct().take(50),
        "authorId" to authorId,
        "authorName" to authorName.ifBlank { "Member" }.take(120),
        "body" to body.take(1500),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CollaborationFile(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val memberUids: List<String> = emptyList(),
    val uploadedBy: String = "",
    val uploadedByName: String = "",
    val fileName: String = "",
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0L,
    val referenceUrl: String? = null,
    val previewBase64: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "projectId" to projectId,
            "memberUids" to memberUids.distinct().take(50),
            "uploadedBy" to uploadedBy,
            "uploadedByName" to uploadedByName.ifBlank { "Member" }.take(120),
            "fileName" to fileName.take(200),
            "mimeType" to mimeType.take(120),
            "sizeBytes" to sizeBytes.coerceIn(0L, 104_857_600L),
            "referenceUrl" to referenceUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "previewBase64" to previewBase64?.takeIf { it.isNotBlank() }?.take(220_000),
            "notes" to notes?.takeIf { it.isNotBlank() }?.take(500),
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class CollaborationActivity(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val memberUids: List<String> = emptyList(),
    val actorId: String = "",
    val actorName: String = "",
    val actionType: String = "UPDATE",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "projectId" to projectId,
        "memberUids" to memberUids.distinct().take(50),
        "actorId" to actorId,
        "actorName" to actorName.ifBlank { "Member" }.take(120),
        "actionType" to actionType.take(60),
        "summary" to summary.take(500),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

data class InviteCollaboratorTarget(
    val userId: String,
    val name: String,
    val proposedRole: String = "Collaborator"
)

data class StartCollaborationDraft(
    val sourceConversationId: String? = null,
    val title: String = "",
    val description: String = "",
    val relatedInnovationId: String? = null,
    val relatedInnovationTitle: String? = null,
    val category: String = "Software",
    val requiredSkills: List<String> = emptyList(),
    val objectives: String = "",
    val expectedDeliverables: String = "",
    val proposedTimeline: String = "4 Weeks",
    val referenceLinks: List<String> = emptyList(),
    val invitedPeers: List<InviteCollaboratorTarget> = emptyList(),
    val invitationMessage: String = "I would love to turn our discussion into an active IDEA2IMPACT collaboration!"
)

object CollaborationProgressCalculator {
    fun computePercent(tasks: List<CollaborationTask>): Int {
        if (tasks.isEmpty()) return 0
        val completed = tasks.count { it.parsedStatus() == TaskStatus.COMPLETED }
        return ((completed * 100.0) / tasks.size).toInt().coerceIn(0, 100)
    }
}
