package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.BlockedUserRecord
import com.example.data.model.ChatMessage
import com.example.data.model.CloudNotification
import com.example.data.model.Conversation
import com.example.data.model.ConversationContextType
import com.example.data.model.ConversationType
import com.example.data.model.MessageType
import com.example.data.model.UserConversationPref
import com.example.data.model.UserPresence
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class ChatPeerDescriptor(
    val rawId: String,
    val name: String,
    val role: String = "Creator",
    val avatarUrl: String? = null,
    val isRealFirebaseUid: Boolean = true
)

class MessagingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google to use IDEA2IMPACT Messaging.")
    }

    private fun <T> Flow<T>.handleStreamError(
        op: OperationType,
        path: String,
        expectedUid: String? = null
    ): Flow<T> {
        return catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: (error as? Exception)
            if (root != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
                handleFirestoreError(root, op, path, auth)
            }
            throw error
        }
    }

    // --- Presence ---

    fun observeAllPresence(): Flow<Map<String, UserPresence>> {
        val path = "presence"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserPresence::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.associateBy { it.userId.ifBlank { it.id } }
            }
            .handleStreamError(OperationType.LIST, path)
    }

    suspend fun updateMyPresence(
        online: Boolean,
        displayName: String,
        role: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("presence").document(uid)
        val payload = UserPresence(
            userId = uid,
            displayName = displayName.ifBlank { "Innovator" },
            role = role.ifBlank { "Creator" }
        ).toWriteMap(onlineNow = online)
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
        }
    }

    // --- Conversation Prefs & Blocks ---

    fun observeConversationPrefs(uid: String): Flow<Map<String, UserConversationPref>> {
        val path = "users/$uid/conversation_prefs"
        return db.collection("users").document(uid).collection("conversation_prefs")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(UserConversationPref::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.associateBy { it.conversationId.ifBlank { it.id } }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeBlockedUsers(uid: String): Flow<List<BlockedUserRecord>> {
        val path = "users/$uid/blocked_users"
        return db.collection("users").document(uid).collection("blocked_users")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(BlockedUserRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    // --- Conversations ---

    fun observeConversations(uid: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection(path)
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeMessages(conversationId: String, uid: String): Flow<List<ChatMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId)
            .collection("messages")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.createdAt ?: Timestamp.now() }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    suspend fun openOrCreateDirectConversation(
        myUid: String,
        myName: String,
        myRole: String,
        myAvatar: String?,
        peer: ChatPeerDescriptor,
        contextType: ConversationContextType = ConversationContextType.GENERAL,
        innovationId: String? = null,
        innovationTitle: String? = null
    ): String {
        val uid = requireUserId()
        val peerKey = sanitizeIdSegment(peer.rawId.ifBlank { peer.name })
        val isDistinctRealPeer = peer.isRealFirebaseUid &&
            peer.rawId.isNotBlank() &&
            peer.rawId != uid &&
            isSafeFirestoreId(peer.rawId) &&
            !peer.rawId.startsWith("t_") &&
            !peer.rawId.startsWith("seed_")

        val convId = if (isDistinctRealPeer) {
            buildDeterministicDirectConversationId(uid, peer.rawId)
        } else {
            // Showcase or external profile thread anchored safely to myUid
            buildShowcaseDirectConversationId(uid, peerKey)
        }

        val convRef = db.collection("conversations").document(convId)
        val existing = try {
            convRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, convRef.path, auth)
            throw e
        }

        if (existing.exists()) {
            // Optionally refresh context link if opening from a specific innovation listing
            if (!innovationId.isNullOrBlank() && existing.getString("innovationId") != innovationId) {
                runCatching {
                    convRef.update(
                        mapOf(
                            "innovationId" to innovationId.take(128),
                            "innovationTitle" to innovationTitle?.take(160),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null }
                    ).await()
                }
            }
            return convId
        }

        val peerSummaryKey = if (isDistinctRealPeer) peer.rawId else "peer_$peerKey"
        val memberUids = if (isDistinctRealPeer) listOf(uid, peer.rawId).distinct() else listOf(uid)
        val unreadMap = memberUids.associateWith { 0L }
        val summaries = buildMap<String, Map<String, Any?>> {
            put(
                uid,
                mapOf(
                    "name" to myName.ifBlank { "Innovator" }.take(120),
                    "role" to myRole.ifBlank { "Creator" }.take(60),
                    "avatarUrl" to myAvatar?.take(1000)
                ).filterValues { it != null }
            )
            put(
                peerSummaryKey,
                mapOf(
                    "name" to peer.name.ifBlank { "Collaborator" }.take(120),
                    "role" to peer.role.ifBlank { "Innovator" }.take(60),
                    "avatarUrl" to peer.avatarUrl?.take(1000)
                ).filterValues { it != null }
            )
        }

        val newConv = Conversation(
            type = ConversationType.DIRECT.name,
            title = if (isDistinctRealPeer) "" else peer.name.ifBlank { "Direct Conversation" },
            createdBy = uid,
            adminUids = memberUids,
            memberUids = memberUids,
            memberSummaries = summaries,
            contextType = contextType.name,
            innovationId = innovationId,
            innovationTitle = innovationTitle,
            lastMessagePreview = if (!innovationTitle.isNullOrBlank()) {
                "Started conversation about \"$innovationTitle\""
            } else {
                "Conversation started"
            },
            lastSenderId = uid,
            lastSenderName = myName.ifBlank { "Innovator" },
            unreadCounts = unreadMap
        )

        try {
            convRef.set(newConv.toCreateMap()).await()
            return convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun createGroupConversation(
        myUid: String,
        myName: String,
        myRole: String,
        myAvatar: String?,
        title: String,
        peers: List<ChatPeerDescriptor>,
        contextType: ConversationContextType = ConversationContextType.GENERAL,
        innovationId: String? = null,
        innovationTitle: String? = null
    ): String {
        val uid = requireUserId()
        val convRef = db.collection("conversations").document()
        val convId = convRef.id
        val realPeers = peers.filter {
            it.isRealFirebaseUid && it.rawId.isNotBlank() && it.rawId != uid &&
                isSafeFirestoreId(it.rawId) && !it.rawId.startsWith("t_") && !it.rawId.startsWith("seed_")
        }
        val memberUids = (listOf(uid) + realPeers.map { it.rawId }).distinct().take(50)
        val summaries = buildMap<String, Map<String, Any?>> {
            put(
                uid,
                mapOf(
                    "name" to myName.ifBlank { "Innovator" }.take(120),
                    "role" to myRole.ifBlank { "Creator" }.take(60),
                    "avatarUrl" to myAvatar?.take(1000)
                ).filterValues { it != null }
            )
            peers.forEach { p ->
                val key = if (p in realPeers) p.rawId else "peer_${sanitizeIdSegment(p.rawId.ifBlank { p.name })}"
                put(
                    key,
                    mapOf(
                        "name" to p.name.ifBlank { "Collaborator" }.take(120),
                        "role" to p.role.ifBlank { "Collaborator" }.take(60),
                        "avatarUrl" to p.avatarUrl?.take(1000)
                    ).filterValues { it != null }
                )
            }
        }

        val groupConv = Conversation(
            type = ConversationType.GROUP.name,
            title = title.trim().ifBlank { "Collaboration Group" }.take(140),
            createdBy = uid,
            adminUids = listOf(uid),
            memberUids = memberUids,
            memberSummaries = summaries,
            contextType = contextType.name,
            innovationId = innovationId,
            innovationTitle = innovationTitle,
            lastMessagePreview = "${myName.ifBlank { "Innovator" }} created group \"${title.trim().take(60)}\"",
            lastSenderId = uid,
            lastSenderName = myName.ifBlank { "Innovator" },
            unreadCounts = memberUids.associateWith { if (it == uid) 0L else 1L }
        )

        val systemMsgRef = convRef.collection("messages").document()
        val systemMsg = ChatMessage(
            conversationId = convId,
            senderId = uid,
            senderName = myName.ifBlank { "Innovator" },
            senderRole = myRole,
            senderAvatar = myAvatar,
            memberUids = memberUids,
            messageType = MessageType.SYSTEM.name,
            body = "Created group \"${groupConv.title}\" with ${peers.size + 1} participant(s).",
            readByUids = listOf(uid),
            deliveredToUids = memberUids
        )

        val batch = db.batch()
        batch.set(convRef, groupConv.toCreateMap())
        batch.set(systemMsgRef, systemMsg.toCreateMap())
        realPeers.forEach { peer ->
            val notifRef = db.collection("app_notifications").document()
            val notif = CloudNotification(
                recipientId = peer.rawId,
                senderId = uid,
                senderName = myName.ifBlank { "Innovator" },
                notificationType = "MESSAGE",
                title = "Added to ${groupConv.title}",
                body = "${myName.ifBlank { "Someone" }} invited you to a group discussion on IDEA2IMPACT.",
                targetType = "CONVERSATION",
                targetId = convId,
                dedupeKey = "group_${convId}_${peer.rawId}"
            )
            batch.set(notifRef, notif.toCreateMap())
        }

        try {
            batch.commit().await()
            return convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convRef.path, auth)
            throw e
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderName: String,
        senderRole: String?,
        senderAvatar: String?,
        body: String,
        messageType: MessageType = MessageType.TEXT,
        replyTo: ChatMessage? = null,
        attachmentName: String? = null,
        attachmentMimeType: String? = null,
        attachmentSizeBytes: Long? = null,
        attachmentPreviewBase64: String? = null,
        attachmentUrl: String? = null,
        sharedInnovationId: String? = null,
        sharedInnovationTitle: String? = null,
        sharedCollaborationId: String? = null
    ) {
        val uid = requireUserId()
        if (conversation.blockedByUids.isNotEmpty()) {
            throw IllegalStateException("Messaging is blocked for this conversation.")
        }
        val cleanBody = body.trim()
        val hasPayload = cleanBody.isNotBlank() ||
            !attachmentName.isNullOrBlank() ||
            !sharedInnovationId.isNullOrBlank() ||
            !sharedCollaborationId.isNullOrBlank()
        if (!hasPayload) return

        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document()
        val msg = ChatMessage(
            conversationId = conversation.id,
            senderId = uid,
            senderName = senderName.ifBlank { "Innovator" },
            senderRole = senderRole,
            senderAvatar = senderAvatar,
            memberUids = conversation.memberUids,
            messageType = messageType.name,
            body = cleanBody,
            replyToMessageId = replyTo?.id,
            replyToSenderName = replyTo?.senderName,
            replyToSnippet = replyTo?.let {
                if (it.deleted) "Deleted message" else it.body.ifBlank { it.attachmentName ?: "Attachment" }
            },
            attachmentName = attachmentName,
            attachmentMimeType = attachmentMimeType,
            attachmentSizeBytes = attachmentSizeBytes,
            attachmentPreviewBase64 = attachmentPreviewBase64,
            attachmentUrl = attachmentUrl,
            sharedInnovationId = sharedInnovationId,
            sharedInnovationTitle = sharedInnovationTitle,
            sharedCollaborationId = sharedCollaborationId,
            readByUids = listOf(uid),
            deliveredToUids = conversation.memberUids
        )

        val preview = when (messageType) {
            MessageType.IMAGE -> cleanBody.ifBlank { "📷 Photo · ${attachmentName ?: "Image"}" }
            MessageType.DOCUMENT -> cleanBody.ifBlank { "📄 ${attachmentName ?: "Document"}" }
            MessageType.LINK -> "🔗 ${sharedInnovationTitle ?: cleanBody}"
            MessageType.COLLABORATION_INVITE -> "🤝 ${cleanBody.ifBlank { "Collaboration Invitation" }}"
            else -> cleanBody
        }.take(280)

        val updatedUnread = conversation.unreadCounts.toMutableMap().apply {
            conversation.memberUids.forEach { memberUid ->
                if (memberUid == uid) {
                    put(memberUid, 0L)
                } else {
                    val prev = this[memberUid] ?: 0L
                    put(memberUid, (prev + 1L).coerceAtMost(999L))
                }
            }
        }
        val updatedTyping = conversation.typingByUid.toMutableMap().apply { remove(uid) }

        val batch = db.batch()
        batch.set(msgRef, msg.toCreateMap())
        batch.update(
            convRef,
            mapOf(
                "lastMessagePreview" to preview,
                "lastSenderId" to uid,
                "lastSenderName" to senderName.ifBlank { "Innovator" }.take(120),
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "unreadCounts" to updatedUnread,
                "typingByUid" to updatedTyping,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        conversation.memberUids.filter { it != uid }.forEach { recipientUid ->
            val notifRef = db.collection("app_notifications").document()
            val notif = CloudNotification(
                recipientId = recipientUid,
                senderId = uid,
                senderName = senderName.ifBlank { "Innovator" },
                notificationType = "MESSAGE",
                title = if (conversation.parsedType() == ConversationType.DIRECT) {
                    "New message from ${senderName.ifBlank { "Collaborator" }}"
                } else {
                    "${conversation.displayTitleFor(recipientUid)}: ${senderName.ifBlank { "Member" }}"
                },
                body = preview,
                targetType = "CONVERSATION",
                targetId = conversation.id,
                dedupeKey = "msg_${conversation.id}_${msgRef.id}"
            )
            batch.set(notifRef, notif.toCreateMap())
        }

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun editMessage(conversationId: String, messageId: String, newBody: String) {
        val uid = requireUserId()
        val clean = newBody.trim()
        if (clean.isBlank()) return
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        try {
            msgRef.update(
                mapOf(
                    "body" to clean.take(4000),
                    "edited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String) {
        requireUserId()
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        try {
            msgRef.update(
                mapOf(
                    "body" to "This message was deleted",
                    "deleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun toggleMessageReaction(conversationId: String, message: ChatMessage, emoji: String) {
        val uid = requireUserId()
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(message.id)
        val nextReactions = message.reactions.toMutableMap()
        if (nextReactions[uid] == emoji) {
            nextReactions.remove(uid)
        } else {
            nextReactions[uid] = emoji.take(16)
        }
        try {
            msgRef.update(
                mapOf(
                    "reactions" to nextReactions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun markConversationRead(
        conversation: Conversation,
        messagesToMarkRead: List<ChatMessage>
    ) {
        val uid = auth.currentUser?.uid ?: return
        val needsConvClear = conversation.unreadFor(uid) > 0
        val unreadMsgs = messagesToMarkRead.filter { uid !in it.readByUids }.take(25)
        if (!needsConvClear && unreadMsgs.isEmpty()) return

        val batch = db.batch()
        if (needsConvClear) {
            val convRef = db.collection("conversations").document(conversation.id)
            batch.update(
                convRef,
                mapOf(
                    "unreadCounts.$uid" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        unreadMsgs.forEach { msg ->
            val msgRef = db.collection("conversations").document(conversation.id)
                .collection("messages").document(msg.id)
            batch.update(
                msgRef,
                mapOf(
                    "readByUids" to FieldValue.arrayUnion(uid),
                    "deliveredToUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "conversations/${conversation.id}", auth)
        }
    }

    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val convRef = db.collection("conversations").document(conversationId)
        try {
            val value: Any = if (isTyping) FieldValue.serverTimestamp() else FieldValue.delete()
            convRef.update(
                mapOf(
                    "typingByUid.$uid" to value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-critical typing indicator
        }
    }

    suspend fun updateConversationPref(
        conversationId: String,
        pinned: Boolean,
        muted: Boolean,
        archived: Boolean
    ) {
        val uid = requireUserId()
        val prefRef = db.collection("users").document(uid)
            .collection("conversation_prefs").document(conversationId)
        val payload = UserConversationPref(
            userId = uid,
            conversationId = conversationId,
            pinned = pinned,
            muted = muted,
            archived = archived
        ).toWriteMap()
        try {
            prefRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, prefRef.path, auth)
            throw e
        }
    }

    suspend fun addGroupMember(conversation: Conversation, peer: ChatPeerDescriptor) {
        val uid = requireUserId()
        if (uid !in conversation.adminUids) {
            throw IllegalStateException("Only group admins can add members.")
        }
        val convRef = db.collection("conversations").document(conversation.id)
        val isReal = peer.isRealFirebaseUid && peer.rawId.isNotBlank() && isSafeFirestoreId(peer.rawId) &&
            !peer.rawId.startsWith("t_") && !peer.rawId.startsWith("seed_")
        val summaryKey = if (isReal) peer.rawId else "peer_${sanitizeIdSegment(peer.rawId.ifBlank { peer.name })}"
        val nextSummaries = conversation.memberSummaries.toMutableMap().apply {
            put(
                summaryKey,
                mapOf(
                    "name" to peer.name.ifBlank { "Member" }.take(120),
                    "role" to peer.role.ifBlank { "Collaborator" }.take(60),
                    "avatarUrl" to peer.avatarUrl?.take(1000)
                ).filterValues { it != null }
            )
        }
        val nextMembers = if (isReal) (conversation.memberUids + peer.rawId).distinct().take(50) else conversation.memberUids
        try {
            convRef.update(
                mapOf(
                    "memberUids" to nextMembers,
                    "memberSummaries" to nextSummaries,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun removeGroupMember(conversation: Conversation, memberUid: String) {
        val uid = requireUserId()
        if (uid !in conversation.adminUids) {
            throw IllegalStateException("Only group admins can remove members.")
        }
        if (memberUid == conversation.createdBy) {
            throw IllegalStateException("The group creator cannot be removed.")
        }
        val convRef = db.collection("conversations").document(conversation.id)
        val nextMembers = conversation.memberUids.filter { it != memberUid }
        val nextAdmins = conversation.adminUids.filter { it != memberUid }
        val nextSummaries = conversation.memberSummaries.toMutableMap().apply { remove(memberUid) }
        try {
            convRef.update(
                mapOf(
                    "memberUids" to nextMembers,
                    "adminUids" to nextAdmins,
                    "memberSummaries" to nextSummaries,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun leaveGroupConversation(conversation: Conversation) {
        val uid = requireUserId()
        if (conversation.createdBy == uid) {
            throw IllegalStateException("Group creators cannot leave; archive or delete the group instead.")
        }
        val convRef = db.collection("conversations").document(conversation.id)
        val nextMembers = conversation.memberUids.filter { it != uid }
        val nextAdmins = conversation.adminUids.filter { it != uid }
        val nextUnread = conversation.unreadCounts.toMutableMap().apply { remove(uid) }
        try {
            convRef.update(
                mapOf(
                    "memberUids" to nextMembers,
                    "adminUids" to nextAdmins,
                    "unreadCounts" to nextUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun toggleBlockPeer(conversation: Conversation, blockedUid: String, blockedName: String, currentlyBlocked: Boolean) {
        val uid = requireUserId()
        val blockRef = db.collection("users").document(uid).collection("blocked_users").document(blockedUid)
        val convRef = db.collection("conversations").document(conversation.id)
        val nextBlockedList = if (currentlyBlocked) {
            conversation.blockedByUids.filter { it != uid }
        } else {
            (conversation.blockedByUids + uid).distinct()
        }

        val batch = db.batch()
        if (currentlyBlocked) {
            batch.delete(blockRef)
        } else {
            batch.set(
                blockRef,
                mapOf(
                    "userId" to uid,
                    "blockedUid" to blockedUid,
                    "blockedName" to blockedName.ifBlank { "User" }.take(120),
                    "reason" to "Blocked from messaging",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.update(
            convRef,
            mapOf(
                "blockedByUids" to nextBlockedList,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, blockRef.path, auth)
            throw e
        }
    }

    suspend fun submitModerationReport(
        targetUserId: String,
        conversationId: String?,
        reason: String,
        details: String
    ) {
        val uid = requireUserId()
        val reportRef = db.collection("reports").document(UUID.randomUUID().toString())
        val payload = mutableMapOf<String, Any?>(
            "reporterId" to uid,
            "targetUserId" to targetUserId.ifBlank { "unknown" },
            "conversationId" to conversationId?.takeIf { it.isNotBlank() },
            "reason" to reason.trim().ifBlank { "Inappropriate behavior" }.take(160),
            "details" to details.trim().takeIf { it.isNotBlank() }?.take(1500),
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            reportRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path, auth)
            throw e
        }
    }

    // --- Cloud Notifications ---

    fun observeCloudNotifications(uid: String): Flow<List<CloudNotification>> {
        val path = "app_notifications"
        return db.collection(path)
            .whereEqualTo("recipientId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(CloudNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    suspend fun markCloudNotificationRead(notificationId: String) {
        requireUserId()
        val ref = db.collection("app_notifications").document(notificationId)
        try {
            ref.update(
                mapOf(
                    "read" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun markAllCloudNotificationsRead(unreadIds: List<String>) {
        requireUserId()
        if (unreadIds.isEmpty()) return
        val batch = db.batch()
        unreadIds.take(40).forEach { id ->
            val ref = db.collection("app_notifications").document(id)
            batch.update(ref, mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun deleteCloudNotification(notificationId: String) {
        requireUserId()
        runCatching {
            db.collection("app_notifications").document(notificationId).delete().await()
        }
    }

    companion object {
        fun buildDeterministicDirectConversationId(uidA: String, uidB: String): String {
            val first = minOf(uidA, uidB)
            val second = maxOf(uidA, uidB)
            return "dm_${first}_${second}".take(128)
        }

        fun buildShowcaseDirectConversationId(myUid: String, peerSlug: String): String {
            return "dm_${myUid}_p_${sanitizeIdSegment(peerSlug)}".take(128)
        }

        fun sanitizeIdSegment(raw: String): String {
            val cleaned = raw.replace(Regex("[^A-Za-z0-9_-]"), "_").trim('_')
            return cleaned.ifBlank { "peer" }.take(40)
        }

        fun isSafeFirestoreId(raw: String): Boolean {
            return raw.length in 1..128 && raw.matches(Regex("^[A-Za-z0-9_-]+$"))
        }
    }
}
