package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.CloudNotification
import com.example.data.model.CollaborationActivity
import com.example.data.model.CollaborationFile
import com.example.data.model.CollaborationInvitation
import com.example.data.model.CollaborationProgressCalculator
import com.example.data.model.CollaborationProject
import com.example.data.model.CollaborationStatus
import com.example.data.model.CollaborationTask
import com.example.data.model.Conversation
import com.example.data.model.ConversationContextType
import com.example.data.model.ConversationType
import com.example.data.model.MessageType
import com.example.data.model.StartCollaborationDraft
import com.example.data.model.TaskComment
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CollaborationRepository(
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
            ?: throw IllegalStateException("Sign in with Google to manage collaborations.")
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

    // --- Real-Time Streams ---

    fun observeMyProjects(uid: String): Flow<List<CollaborationProject>> {
        val path = "collaboration_projects"
        return db.collection(path)
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(CollaborationProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeIncomingInvitations(uid: String): Flow<List<CollaborationInvitation>> {
        val path = "collaboration_invitations"
        return db.collection(path)
            .whereEqualTo("inviteeId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(CollaborationInvitation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeSentInvitations(uid: String): Flow<List<CollaborationInvitation>> {
        val path = "collaboration_invitations"
        return db.collection(path)
            .whereEqualTo("inviterId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(CollaborationInvitation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeProjectTasks(projectId: String, uid: String): Flow<List<CollaborationTask>> {
        val path = "collaboration_projects/$projectId/tasks"
        return db.collection("collaboration_projects").document(projectId).collection("tasks")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(CollaborationTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedWith(
                    compareBy<CollaborationTask> { it.parsedStatus() == TaskStatus.COMPLETED }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeTaskComments(projectId: String, taskId: String, uid: String): Flow<List<TaskComment>> {
        val path = "collaboration_projects/$projectId/tasks/$taskId/comments"
        return db.collection("collaboration_projects").document(projectId)
            .collection("tasks").document(taskId)
            .collection("comments")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(TaskComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.createdAt ?: Timestamp.now() }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeProjectFiles(projectId: String, uid: String): Flow<List<CollaborationFile>> {
        val path = "collaboration_projects/$projectId/files"
        return db.collection("collaboration_projects").document(projectId).collection("files")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(CollaborationFile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    fun observeProjectActivity(projectId: String, uid: String): Flow<List<CollaborationActivity>> {
        val path = "collaboration_projects/$projectId/activity"
        return db.collection("collaboration_projects").document(projectId).collection("activity")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(CollaborationActivity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, uid)
    }

    // --- Step 1 & 2: Start Collaboration Workflow ---

    suspend fun createCollaborationWorkspace(
        draft: StartCollaborationDraft,
        myName: String,
        myRole: String,
        myAvatar: String?,
        sourceConversation: Conversation?
    ): Pair<String, String> {
        val uid = requireUserId()
        val projectRef = db.collection("collaboration_projects").document()
        val projectId = projectRef.id

        // Create or link a project conversation so Team Discussion & Invites work cleanly
        val convRef = if (sourceConversation != null && sourceConversation.collaborationId.isNullOrBlank()) {
            db.collection("conversations").document(sourceConversation.id)
        } else {
            db.collection("conversations").document()
        }
        val conversationId = convRef.id
        val isLinkingExistingConv = sourceConversation != null && sourceConversation.collaborationId.isNullOrBlank()

        val initialMemberUids = listOf(uid)
        val project = CollaborationProject(
            title = draft.title.trim().take(140).let { if (it.length < 3) "Collaboration Project" else it },
            description = draft.description.trim().take(4000).let { if (it.length < 5) "Structured collaboration workspace on IDEA2IMPACT." else it },
            category = draft.category.trim().ifBlank { "Software" }.take(80),
            relatedInnovationId = draft.relatedInnovationId?.takeIf { it.isNotBlank() },
            relatedInnovationTitle = draft.relatedInnovationTitle?.takeIf { it.isNotBlank() },
            requiredSkills = draft.requiredSkills.map { it.trim() }.filter { it.isNotBlank() }.take(25),
            objectives = draft.objectives.trim(),
            expectedDeliverables = draft.expectedDeliverables.trim(),
            proposedTimeline = draft.proposedTimeline.trim(),
            referenceLinks = draft.referenceLinks.map { it.trim() }.filter { it.isNotBlank() }.take(10),
            status = CollaborationStatus.ACTIVE.name,
            ownerId = uid,
            adminUids = initialMemberUids,
            memberUids = initialMemberUids,
            memberRoles = mapOf(uid to "Lead Owner"),
            memberNames = mapOf(uid to myName.ifBlank { "Innovator" }.take(120)),
            conversationId = conversationId,
            progressPercent = 0L,
            totalTasks = 0L,
            completedTasks = 0L
        )

        val realInviteeTargets = draft.invitedPeers.filter {
            it.userId.isNotBlank() &&
                it.userId != uid &&
                MessagingRepository.isSafeFirestoreId(it.userId) &&
                !it.userId.startsWith("t_") &&
                !it.userId.startsWith("seed_")
        }

        val activityRef = projectRef.collection("activity").document()
        val initialActivity = CollaborationActivity(
            projectId = projectId,
            memberUids = initialMemberUids,
            actorId = uid,
            actorName = myName.ifBlank { "Innovator" },
            actionType = "PROJECT_CREATED",
            summary = "Launched collaboration workspace \"${project.title}\"" +
                if (realInviteeTargets.isNotEmpty()) " and invited ${realInviteeTargets.size} collaborator(s)." else "."
        )

        val batch = db.batch()
        batch.set(projectRef, project.toCreateMap())
        batch.set(activityRef, initialActivity.toCreateMap())

        if (isLinkingExistingConv && sourceConversation != null) {
            val inviteMsgRef = convRef.collection("messages").document()
            val inviteMsg = ChatMessage(
                conversationId = conversationId,
                senderId = uid,
                senderName = myName.ifBlank { "Innovator" },
                senderRole = myRole,
                senderAvatar = myAvatar,
                memberUids = sourceConversation.memberUids,
                messageType = MessageType.COLLABORATION_INVITE.name,
                body = "Started Collaboration: ${project.title} (${project.proposedTimeline})",
                sharedInnovationId = project.relatedInnovationId,
                sharedInnovationTitle = project.relatedInnovationTitle,
                sharedCollaborationId = projectId,
                readByUids = listOf(uid),
                deliveredToUids = sourceConversation.memberUids
            )
            batch.set(inviteMsgRef, inviteMsg.toCreateMap())
            val unreadMap = sourceConversation.unreadCounts.toMutableMap().apply {
                sourceConversation.memberUids.forEach { memberUid ->
                    put(memberUid, if (memberUid == uid) 0L else ((this[memberUid] ?: 0L) + 1L))
                }
            }
            batch.update(
                convRef,
                mapOf(
                    "collaborationId" to projectId,
                    "lastMessagePreview" to "🤝 Started Collaboration: ${project.title}",
                    "lastSenderId" to uid,
                    "lastSenderName" to myName.ifBlank { "Innovator" }.take(120),
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "unreadCounts" to unreadMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            val newProjectConv = Conversation(
                type = ConversationType.PROJECT.name,
                title = project.title,
                createdBy = uid,
                adminUids = listOf(uid),
                memberUids = listOf(uid),
                memberSummaries = mapOf(
                    uid to mapOf(
                        "name" to myName.ifBlank { "Innovator" }.take(120),
                        "role" to myRole.ifBlank { "Lead Owner" }.take(60),
                        "avatarUrl" to myAvatar?.take(1000)
                    ).filterValues { it != null }
                ),
                contextType = ConversationContextType.COLLABORATION.name,
                innovationId = project.relatedInnovationId,
                innovationTitle = project.relatedInnovationTitle,
                collaborationId = projectId,
                lastMessagePreview = "Workspace chat created for ${project.title}",
                lastSenderId = uid,
                lastSenderName = myName.ifBlank { "Innovator" },
                unreadCounts = mapOf(uid to 0L)
            )
            val welcomeMsgRef = convRef.collection("messages").document()
            val welcomeMsg = ChatMessage(
                conversationId = conversationId,
                senderId = uid,
                senderName = myName.ifBlank { "Innovator" },
                senderRole = myRole,
                senderAvatar = myAvatar,
                memberUids = listOf(uid),
                messageType = MessageType.COLLABORATION_INVITE.name,
                body = "Welcome to ${project.title}! Timeline: ${project.proposedTimeline}.",
                sharedCollaborationId = projectId,
                readByUids = listOf(uid),
                deliveredToUids = listOf(uid)
            )
            batch.set(convRef, newProjectConv.toCreateMap())
            batch.set(welcomeMsgRef, welcomeMsg.toCreateMap())
        }

        // Send deterministic invitations + notifications
        realInviteeTargets.forEach { target ->
            val inviteDocId = "${projectId}_${target.userId}"
            val inviteRef = db.collection("collaboration_invitations").document(inviteDocId)
            val invite = CollaborationInvitation(
                projectId = projectId,
                projectTitle = project.title,
                projectCategory = project.category,
                inviterId = uid,
                inviterName = myName.ifBlank { "Innovator" },
                inviteeId = target.userId,
                inviteeName = target.name,
                proposedRole = target.proposedRole,
                message = draft.invitationMessage.trim(),
                conversationId = conversationId
            )
            batch.set(inviteRef, invite.toCreateMap())

            val notifRef = db.collection("app_notifications").document()
            val notif = CloudNotification(
                recipientId = target.userId,
                senderId = uid,
                senderName = myName.ifBlank { "Innovator" },
                notificationType = "COLLAB_INVITE",
                title = "Collaboration Invite: ${project.title}",
                body = "${myName.ifBlank { "An innovator" }} invited you as ${target.proposedRole}.",
                targetType = "COLLABORATION",
                targetId = projectId,
                dedupeKey = "invite_${projectId}_${target.userId}"
            )
            batch.set(notifRef, notif.toCreateMap())
        }

        try {
            batch.commit().await()
            return projectId to conversationId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, projectRef.path, auth)
            throw e
        }
    }

    suspend fun inviteCollaboratorToProject(
        project: CollaborationProject,
        inviteeId: String,
        inviteeName: String,
        proposedRole: String,
        message: String,
        myName: String
    ) {
        val uid = requireUserId()
        if (!MessagingRepository.isSafeFirestoreId(inviteeId) ||
            inviteeId == uid ||
            inviteeId.startsWith("t_") ||
            inviteeId.startsWith("seed_")
        ) {
            throw IllegalStateException("Select a signed-in collaborator profile to send a cloud invitation.")
        }
        val inviteId = "${project.id}_${inviteeId}"
        val inviteRef = db.collection("collaboration_invitations").document(inviteId)
        val invite = CollaborationInvitation(
            projectId = project.id,
            projectTitle = project.title,
            projectCategory = project.category,
            inviterId = uid,
            inviterName = myName.ifBlank { "Innovator" },
            inviteeId = inviteeId,
            inviteeName = inviteeName,
            proposedRole = proposedRole.ifBlank { "Collaborator" },
            message = message.trim(),
            conversationId = project.conversationId
        )
        val notifRef = db.collection("app_notifications").document()
        val notif = CloudNotification(
            recipientId = inviteeId,
            senderId = uid,
            senderName = myName.ifBlank { "Innovator" },
            notificationType = "COLLAB_INVITE",
            title = "Collaboration Invite: ${project.title}",
            body = "${myName.ifBlank { "An innovator" }} invited you as ${invite.proposedRole}.",
            targetType = "COLLABORATION",
            targetId = project.id,
            dedupeKey = "invite_${project.id}_${inviteeId}"
        )

        val batch = db.batch()
        batch.set(inviteRef, invite.toCreateMap())
        batch.set(notifRef, notif.toCreateMap())
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, inviteRef.path, auth)
            throw e
        }
    }

    suspend fun respondToInvitation(
        invitation: CollaborationInvitation,
        accept: Boolean,
        myName: String,
        myAvatar: String?
    ) {
        val uid = requireUserId()
        val inviteRef = db.collection("collaboration_invitations").document(invitation.id)
        val batch = db.batch()
        batch.update(
            inviteRef,
            mapOf(
                "status" to if (accept) "ACCEPTED" else "DECLINED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (accept) {
            val projectRef = db.collection("collaboration_projects").document(invitation.projectId)
            batch.update(
                projectRef,
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "memberRoles.$uid" to invitation.proposedRole.ifBlank { "Collaborator" }.take(80),
                    "memberNames.$uid" to myName.ifBlank { "Collaborator" }.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val convId = invitation.conversationId?.takeIf { it.isNotBlank() }
            if (convId != null) {
                val convRef = db.collection("conversations").document(convId)
                val convSnap = runCatching { convRef.get().await() }.getOrNull()
                val existingMembers = (convSnap?.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                if (convSnap != null && convSnap.exists() && uid !in existingMembers) {
                    val summaryMap = mapOf(
                        "name" to myName.ifBlank { "Collaborator" }.take(120),
                        "role" to invitation.proposedRole.ifBlank { "Collaborator" }.take(60),
                        "avatarUrl" to myAvatar?.take(1000)
                    ).filterValues { it != null }
                    batch.update(
                        convRef,
                        mapOf(
                            "memberUids" to FieldValue.arrayUnion(uid),
                            "memberSummaries.$uid" to summaryMap,
                            "unreadCounts.$uid" to 0L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        val notifRef = db.collection("app_notifications").document()
        val notif = CloudNotification(
            recipientId = invitation.inviterId,
            senderId = uid,
            senderName = myName.ifBlank { "Collaborator" },
            notificationType = "COLLAB_RESPONSE",
            title = if (accept) "${myName.ifBlank { "Collaborator" }} joined ${invitation.projectTitle}"
            else "Invitation declined · ${invitation.projectTitle}",
            body = if (accept) "${myName.ifBlank { "A collaborator" }} accepted your ${invitation.proposedRole} invitation."
            else "${myName.ifBlank { "A collaborator" }} declined the collaboration invitation.",
            targetType = "COLLABORATION",
            targetId = invitation.projectId,
            dedupeKey = "invite_resp_${invitation.id}_${if (accept) "acc" else "dec"}"
        )
        batch.set(notifRef, notif.toCreateMap())

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, inviteRef.path, auth)
            throw e
        }
    }

    // --- Step 3, 4 & 5: Tasks, Comments, Files, Progress & Members ---

    suspend fun createTask(
        project: CollaborationProject,
        existingTasks: List<CollaborationTask>,
        title: String,
        description: String,
        assigneeUid: String?,
        assigneeName: String?,
        priority: TaskPriority,
        dueAtEpochMs: Long?,
        attachmentName: String?,
        myName: String
    ) {
        val uid = requireUserId()
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val taskRef = projectRef.collection("tasks").document()
        val task = CollaborationTask(
            projectId = project.id,
            memberUids = project.memberUids,
            title = title.trim().take(160),
            description = description.trim().take(2000),
            assigneeUid = assigneeUid?.takeIf { it.isNotBlank() },
            assigneeName = assigneeName?.takeIf { it.isNotBlank() },
            createdBy = uid,
            priority = priority.name,
            status = TaskStatus.TODO.name,
            dueAtEpochMs = dueAtEpochMs,
            attachmentName = attachmentName?.takeIf { it.isNotBlank() }
        )

        val projected = existingTasks + task
        val nextTotal = projected.size.toLong()
        val nextCompleted = projected.count { it.parsedStatus() == TaskStatus.COMPLETED }.toLong()
        val nextProgress = CollaborationProgressCalculator.computePercent(projected).toLong()

        val activityRef = projectRef.collection("activity").document()
        val activity = CollaborationActivity(
            projectId = project.id,
            memberUids = project.memberUids,
            actorId = uid,
            actorName = myName.ifBlank { "Member" },
            actionType = "TASK_CREATED",
            summary = "Added task \"${task.title}\"" +
                if (!assigneeName.isNullOrBlank()) " (assigned to $assigneeName)." else "."
        )

        val batch = db.batch()
        batch.set(taskRef, task.toCreateMap())
        batch.set(activityRef, activity.toCreateMap())
        batch.update(
            projectRef,
            mapOf(
                "totalTasks" to nextTotal,
                "completedTasks" to nextCompleted,
                "progressPercent" to nextProgress,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (!assigneeUid.isNullOrBlank() && assigneeUid != uid && MessagingRepository.isSafeFirestoreId(assigneeUid)) {
            val notifRef = db.collection("app_notifications").document()
            val notif = CloudNotification(
                recipientId = assigneeUid,
                senderId = uid,
                senderName = myName.ifBlank { "Teammate" },
                notificationType = "TASK_ASSIGNED",
                title = "Task Assigned: ${task.title}",
                body = "${myName.ifBlank { "A teammate" }} assigned \"${task.title}\" in ${project.title}.",
                targetType = "COLLABORATION",
                targetId = project.id,
                dedupeKey = "task_${taskRef.id}_$assigneeUid"
            )
            batch.set(notifRef, notif.toCreateMap())
        }

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, taskRef.path, auth)
            throw e
        }
    }

    suspend fun updateTaskStatus(
        project: CollaborationProject,
        allTasks: List<CollaborationTask>,
        task: CollaborationTask,
        newStatus: TaskStatus,
        myName: String
    ) {
        val uid = requireUserId()
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val taskRef = projectRef.collection("tasks").document(task.id)

        val updatedList = allTasks.map { if (it.id == task.id) it.copy(status = newStatus.name) else it }
        val nextTotal = updatedList.size.toLong()
        val nextCompleted = updatedList.count { it.parsedStatus() == TaskStatus.COMPLETED }.toLong()
        val nextProgress = CollaborationProgressCalculator.computePercent(updatedList).toLong()

        val activityRef = projectRef.collection("activity").document()
        val activity = CollaborationActivity(
            projectId = project.id,
            memberUids = project.memberUids,
            actorId = uid,
            actorName = myName.ifBlank { "Member" },
            actionType = if (newStatus == TaskStatus.COMPLETED) "TASK_COMPLETED" else "TASK_UPDATED",
            summary = "Moved \"${task.title}\" to ${newStatus.label} (${nextProgress}% project progress)."
        )

        val batch = db.batch()
        batch.update(
            taskRef,
            mapOf(
                "status" to newStatus.name,
                "memberUids" to project.memberUids,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(activityRef, activity.toCreateMap())
        batch.update(
            projectRef,
            mapOf(
                "totalTasks" to nextTotal,
                "completedTasks" to nextCompleted,
                "progressPercent" to nextProgress,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (newStatus == TaskStatus.COMPLETED) {
            project.memberUids.filter { it != uid }.forEach { memberUid ->
                val notifRef = db.collection("app_notifications").document()
                val notif = CloudNotification(
                    recipientId = memberUid,
                    senderId = uid,
                    senderName = myName.ifBlank { "Member" },
                    notificationType = "TASK_COMPLETED",
                    title = "Task Completed · ${project.title}",
                    body = "${myName.ifBlank { "A member" }} completed \"${task.title}\".",
                    targetType = "COLLABORATION",
                    targetId = project.id,
                    dedupeKey = "task_done_${task.id}_$memberUid"
                )
                batch.set(notifRef, notif.toCreateMap())
            }
        }

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, taskRef.path, auth)
            throw e
        }
    }

    suspend fun deleteTask(
        project: CollaborationProject,
        allTasks: List<CollaborationTask>,
        task: CollaborationTask
    ) {
        requireUserId()
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val taskRef = projectRef.collection("tasks").document(task.id)
        val remaining = allTasks.filter { it.id != task.id }
        val nextTotal = remaining.size.toLong()
        val nextCompleted = remaining.count { it.parsedStatus() == TaskStatus.COMPLETED }.toLong()
        val nextProgress = CollaborationProgressCalculator.computePercent(remaining).toLong()

        val batch = db.batch()
        batch.delete(taskRef)
        batch.update(
            projectRef,
            mapOf(
                "totalTasks" to nextTotal,
                "completedTasks" to nextCompleted,
                "progressPercent" to nextProgress,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, taskRef.path, auth)
            throw e
        }
    }

    suspend fun addTaskComment(
        project: CollaborationProject,
        task: CollaborationTask,
        body: String,
        myName: String
    ) {
        val uid = requireUserId()
        val clean = body.trim()
        if (clean.isBlank()) return
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val taskRef = projectRef.collection("tasks").document(task.id)
        val commentRef = taskRef.collection("comments").document()
        val comment = TaskComment(
            projectId = project.id,
            taskId = task.id,
            memberUids = project.memberUids,
            authorId = uid,
            authorName = myName.ifBlank { "Member" },
            body = clean
        )
        val batch = db.batch()
        batch.set(commentRef, comment.toCreateMap())
        batch.update(
            taskRef,
            mapOf(
                "commentCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path, auth)
            throw e
        }
    }

    suspend fun uploadSharedFile(
        project: CollaborationProject,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        referenceUrl: String?,
        previewBase64: String?,
        notes: String?,
        myName: String
    ) {
        val uid = requireUserId()
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val fileRef = projectRef.collection("files").document()
        val file = CollaborationFile(
            projectId = project.id,
            memberUids = project.memberUids,
            uploadedBy = uid,
            uploadedByName = myName.ifBlank { "Member" },
            fileName = fileName.trim().ifBlank { "Shared Resource" }.take(200),
            mimeType = mimeType.ifBlank { "application/octet-stream" }.take(120),
            sizeBytes = sizeBytes.coerceAtLeast(0L),
            referenceUrl = referenceUrl?.trim()?.takeIf { it.isNotBlank() },
            previewBase64 = previewBase64?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() }
        )
        val activityRef = projectRef.collection("activity").document()
        val activity = CollaborationActivity(
            projectId = project.id,
            memberUids = project.memberUids,
            actorId = uid,
            actorName = myName.ifBlank { "Member" },
            actionType = "FILE_SHARED",
            summary = "Shared private project file \"${file.fileName}\"."
        )
        val batch = db.batch()
        batch.set(fileRef, file.toCreateMap())
        batch.set(activityRef, activity.toCreateMap())
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, fileRef.path, auth)
            throw e
        }
    }

    suspend fun deleteSharedFile(projectId: String, fileId: String) {
        requireUserId()
        val fileRef = db.collection("collaboration_projects").document(projectId)
            .collection("files").document(fileId)
        try {
            fileRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, fileRef.path, auth)
            throw e
        }
    }

    suspend fun postProjectUpdate(project: CollaborationProject, message: String, myName: String) {
        val uid = requireUserId()
        val clean = message.trim()
        if (clean.isBlank()) return
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val activityRef = projectRef.collection("activity").document()
        val activity = CollaborationActivity(
            projectId = project.id,
            memberUids = project.memberUids,
            actorId = uid,
            actorName = myName.ifBlank { "Member" },
            actionType = "PROJECT_UPDATE",
            summary = clean.take(500)
        )
        val batch = db.batch()
        batch.set(activityRef, activity.toCreateMap())
        project.memberUids.filter { it != uid }.forEach { memberUid ->
            val notifRef = db.collection("app_notifications").document()
            val notif = CloudNotification(
                recipientId = memberUid,
                senderId = uid,
                senderName = myName.ifBlank { "Teammate" },
                notificationType = "PROJECT_UPDATE",
                title = "Update · ${project.title}",
                body = clean.take(240),
                targetType = "COLLABORATION",
                targetId = project.id,
                dedupeKey = "upd_${activityRef.id}_$memberUid"
            )
            batch.set(notifRef, notif.toCreateMap())
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, activityRef.path, auth)
            throw e
        }
    }

    suspend fun updateProjectStatus(
        project: CollaborationProject,
        newStatus: CollaborationStatus,
        myName: String
    ) {
        val uid = requireUserId()
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val activityRef = projectRef.collection("activity").document()
        val activity = CollaborationActivity(
            projectId = project.id,
            memberUids = project.memberUids,
            actorId = uid,
            actorName = myName.ifBlank { "Member" },
            actionType = "STATUS_CHANGED",
            summary = "Changed project status to ${newStatus.label}."
        )
        val batch = db.batch()
        batch.update(
            projectRef,
            mapOf(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(activityRef, activity.toCreateMap())
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, projectRef.path, auth)
            throw e
        }
    }

    suspend fun updateMemberRole(project: CollaborationProject, memberUid: String, newRole: String) {
        val uid = requireUserId()
        if (uid !in project.adminUids) throw IllegalStateException("Only project admins can manage roles.")
        val projectRef = db.collection("collaboration_projects").document(project.id)
        try {
            projectRef.update(
                mapOf(
                    "memberRoles.$memberUid" to newRole.trim().ifBlank { "Collaborator" }.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, projectRef.path, auth)
            throw e
        }
    }

    suspend fun removeProjectMember(project: CollaborationProject, targetUid: String) {
        val uid = requireUserId()
        if (uid !in project.adminUids) throw IllegalStateException("Only project admins can remove members.")
        if (targetUid == project.ownerId) throw IllegalStateException("Project owner cannot be removed.")
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val nextMembers = project.memberUids.filter { it != targetUid }
        val nextAdmins = project.adminUids.filter { it != targetUid }
        val nextRoles = project.memberRoles.toMutableMap().apply { remove(targetUid) }
        val nextNames = project.memberNames.toMutableMap().apply { remove(targetUid) }
        try {
            projectRef.update(
                mapOf(
                    "memberUids" to nextMembers,
                    "adminUids" to nextAdmins,
                    "memberRoles" to nextRoles,
                    "memberNames" to nextNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, projectRef.path, auth)
            throw e
        }
    }

    suspend fun leaveCollaboration(project: CollaborationProject) {
        val uid = requireUserId()
        if (project.ownerId == uid) {
            throw IllegalStateException("Project owners cannot leave their own collaboration.")
        }
        val projectRef = db.collection("collaboration_projects").document(project.id)
        val nextMembers = project.memberUids.filter { it != uid }
        val nextAdmins = project.adminUids.filter { it != uid }
        val nextRoles = project.memberRoles.toMutableMap().apply { remove(uid) }
        val nextNames = project.memberNames.toMutableMap().apply { remove(uid) }
        try {
            projectRef.update(
                mapOf(
                    "memberUids" to nextMembers,
                    "adminUids" to nextAdmins,
                    "memberRoles" to nextRoles,
                    "memberNames" to nextNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, projectRef.path, auth)
            throw e
        }
    }
}
