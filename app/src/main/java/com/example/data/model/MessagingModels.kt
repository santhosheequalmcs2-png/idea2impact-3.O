package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ConversationType(val label: String) {
    DIRECT("Direct"),
    GROUP("Group"),
    PROJECT("Project")
}

enum class ConversationContextType(val label: String) {
    GENERAL("General"),
    INNOVATION("Innovation"),
    PROPOSAL("Proposal"),
    COLLABORATION("Collaboration")
}

enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT,
    LINK,
    COLLABORATION_INVITE,
    SYSTEM
}

@Keep
@IgnoreExtraProperties
data class UserPresence(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = "Creator",
    val online: Boolean = false,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(onlineNow: Boolean): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.ifBlank { "Innovator" }.take(120),
        "role" to role.ifBlank { "Creator" }.take(60),
        "online" to onlineNow,
        "lastSeen" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = ConversationType.DIRECT.name,
    val title: String = "",
    val avatarUrl: String? = null,
    val createdBy: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val memberSummaries: Map<String, Map<String, Any?>> = emptyMap(),
    val contextType: String = ConversationContextType.GENERAL.name,
    val innovationId: String? = null,
    val innovationTitle: String? = null,
    val collaborationId: String? = null,
    val lastMessagePreview: String = "",
    val lastSenderId: String = "",
    val lastSenderName: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val typingByUid: Map<String, Timestamp> = emptyMap(),
    val blockedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedType(): ConversationType =
        runCatching { ConversationType.valueOf(type) }.getOrDefault(ConversationType.DIRECT)

    fun parsedContextType(): ConversationContextType =
        runCatching { ConversationContextType.valueOf(contextType) }.getOrDefault(ConversationContextType.GENERAL)

    fun peerUidFor(myUid: String): String? {
        if (parsedType() != ConversationType.DIRECT) return null
        val other = memberUids.firstOrNull { it != myUid }
        if (other != null) return other
        return memberSummaries.keys.firstOrNull { it != myUid }
    }

    fun summaryString(uid: String, key: String): String? =
        (memberSummaries[uid]?.get(key) as? String)?.takeIf { it.isNotBlank() }

    fun displayTitleFor(myUid: String): String {
        if (parsedType() != ConversationType.DIRECT && title.isNotBlank()) return title
        val peerUid = peerUidFor(myUid)
        if (peerUid != null) {
            summaryString(peerUid, "name")?.let { return it }
        }
        return title.ifBlank { innovationTitle?.takeIf { it.isNotBlank() } ?: "Direct Conversation" }
    }

    fun displayRoleFor(myUid: String): String {
        if (parsedType() == ConversationType.GROUP) return "${memberUids.size} members"
        if (parsedType() == ConversationType.PROJECT) return "Project Team · ${memberUids.size}"
        val peerUid = peerUidFor(myUid) ?: return "Collaborator"
        return summaryString(peerUid, "role") ?: "Innovator"
    }

    fun displayAvatarFor(myUid: String): String? {
        if (!avatarUrl.isNullOrBlank()) return avatarUrl
        val peerUid = peerUidFor(myUid) ?: return null
        return summaryString(peerUid, "avatarUrl")
    }

    fun unreadFor(myUid: String): Int {
        val raw = unreadCounts[myUid]
        return (raw ?: 0L).toInt().coerceAtLeast(0)
    }

    fun activeTypingNames(myUid: String, nowMs: Long = System.currentTimeMillis()): List<String> {
        return typingByUid.entries.mapNotNull { (uid, ts) ->
            if (uid == myUid) return@mapNotNull null
            val ageMs = nowMs - ts.toDate().time
            if (ageMs in -5000L..12000L) {
                summaryString(uid, "name") ?: "Someone"
            } else null
        }
    }

    fun isBlockedByAny(): Boolean = blockedByUids.isNotEmpty()
    fun isBlockedByMe(myUid: String): Boolean = blockedByUids.contains(myUid)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "type" to type,
            "title" to title.take(140),
            "avatarUrl" to avatarUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "createdBy" to createdBy,
            "adminUids" to adminUids.distinct().take(50),
            "memberUids" to memberUids.distinct().take(50),
            "memberSummaries" to memberSummaries,
            "contextType" to contextType,
            "innovationId" to innovationId?.takeIf { it.isNotBlank() }?.take(128),
            "innovationTitle" to innovationTitle?.takeIf { it.isNotBlank() }?.take(160),
            "collaborationId" to collaborationId?.takeIf { it.isNotBlank() }?.take(128),
            "lastMessagePreview" to lastMessagePreview.take(300),
            "lastSenderId" to lastSenderId.take(128),
            "lastSenderName" to lastSenderName.take(120),
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "unreadCounts" to unreadCounts,
            "typingByUid" to typingByUid,
            "blockedByUids" to blockedByUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String? = null,
    val senderAvatar: String? = null,
    val memberUids: List<String> = emptyList(),
    val messageType: String = MessageType.TEXT.name,
    val body: String = "",
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToSnippet: String? = null,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeBytes: Long? = null,
    val attachmentPreviewBase64: String? = null,
    val attachmentUrl: String? = null,
    val sharedInnovationId: String? = null,
    val sharedInnovationTitle: String? = null,
    val sharedCollaborationId: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val readByUids: List<String> = emptyList(),
    val deliveredToUids: List<String> = emptyList(),
    val edited: Boolean = false,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedType(): MessageType =
        runCatching { MessageType.valueOf(messageType) }.getOrDefault(MessageType.TEXT)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderName" to senderName.ifBlank { "Innovator" }.take(120),
            "senderRole" to senderRole?.takeIf { it.isNotBlank() }?.take(60),
            "senderAvatar" to senderAvatar?.takeIf { it.isNotBlank() }?.take(1000),
            "memberUids" to memberUids.distinct().take(50),
            "messageType" to messageType,
            "body" to body.take(4000),
            "replyToMessageId" to replyToMessageId?.takeIf { it.isNotBlank() }?.take(128),
            "replyToSenderName" to replyToSenderName?.takeIf { it.isNotBlank() }?.take(120),
            "replyToSnippet" to replyToSnippet?.takeIf { it.isNotBlank() }?.take(300),
            "attachmentName" to attachmentName?.takeIf { it.isNotBlank() }?.take(200),
            "attachmentMimeType" to attachmentMimeType?.takeIf { it.isNotBlank() }?.take(100),
            "attachmentSizeBytes" to attachmentSizeBytes?.coerceIn(0L, 104_857_600L),
            "attachmentPreviewBase64" to attachmentPreviewBase64?.takeIf { it.isNotBlank() }?.take(220_000),
            "attachmentUrl" to attachmentUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "sharedInnovationId" to sharedInnovationId?.takeIf { it.isNotBlank() }?.take(128),
            "sharedInnovationTitle" to sharedInnovationTitle?.takeIf { it.isNotBlank() }?.take(160),
            "sharedCollaborationId" to sharedCollaborationId?.takeIf { it.isNotBlank() }?.take(128),
            "reactions" to reactions,
            "readByUids" to readByUids.distinct().take(50),
            "deliveredToUids" to deliveredToUids.distinct().take(50),
            "edited" to false,
            "deleted" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class UserConversationPref(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val archived: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "conversationId" to conversationId,
        "pinned" to pinned,
        "muted" to muted,
        "archived" to archived,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class BlockedUserRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUid: String = "",
    val blockedName: String = "",
    val reason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val notificationType: String = "MESSAGE",
    val title: String = "",
    val body: String = "",
    val targetType: String = "CONVERSATION",
    val targetId: String = "",
    val dedupeKey: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "senderName" to senderName.ifBlank { "Innovator" }.take(120),
            "notificationType" to notificationType,
            "title" to title.take(160),
            "body" to body.take(600),
            "targetType" to targetType,
            "targetId" to targetId.take(128),
            "dedupeKey" to dedupeKey?.takeIf { it.isNotBlank() }?.take(160),
            "read" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

data class ConversationUiItem(
    val conversation: Conversation,
    val pref: UserConversationPref = UserConversationPref(),
    val peerPresence: UserPresence? = null,
    val isPeerBlocked: Boolean = false
)
