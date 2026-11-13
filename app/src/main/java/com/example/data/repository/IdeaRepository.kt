package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.Innovation
import com.example.data.model.InnovationComment
import com.example.data.model.Pledge
import com.example.data.model.Proposal
import com.example.data.model.SavedItem
import com.example.data.model.SignedNda
import com.example.data.model.TalentProfile
import com.example.data.model.UserProfile
import com.example.data.seed.SeedCatalog
import com.example.util.SecurityCryptoUtils
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

class IdeaRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleStreamError(op: OperationType, path: String, expectedUid: String? = null): Flow<T> {
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

    // --- Real-time Streams ---

    fun observePublicInnovations(): Flow<List<Innovation>> {
        val path = "innovations"
        return db.collection(path)
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Innovation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path)
    }

    fun observeMyInnovations(userId: String): Flow<List<Innovation>> {
        val path = "innovations"
        return db.collection(path)
            .whereEqualTo("creatorId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Innovation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.filter { !it.isShowcaseSeed }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    fun observeComments(innovationId: String): Flow<List<InnovationComment>> {
        val path = "innovations/$innovationId/comments"
        return db.collection("innovations").document(innovationId)
            .collection("comments")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(InnovationComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path)
    }

    fun observeTalents(): Flow<List<TalentProfile>> {
        val path = "talents"
        return db.collection(path)
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TalentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.projectsCount }
            }
            .handleStreamError(OperationType.LIST, path)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .handleStreamError(OperationType.GET, path, userId)
    }

    fun observeSavedItems(userId: String): Flow<List<SavedItem>> {
        val path = "users/$userId/saved"
        return db.collection("users").document(userId).collection("saved")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SavedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    fun observeLikedIds(userId: String): Flow<Set<String>> {
        val path = "users/$userId/liked"
        return db.collection("users").document(userId).collection("liked")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    fun observeSignedNdas(userId: String): Flow<Map<String, SignedNda>> {
        val path = "users/$userId/signedNdas"
        return db.collection("users").document(userId).collection("signedNdas")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SignedNda::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.associateBy { it.innovationId }
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    fun observePledges(userId: String): Flow<List<Pledge>> {
        val path = "users/$userId/pledges"
        return db.collection("users").document(userId).collection("pledges")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pledge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    fun observeIncomingProposals(userId: String): Flow<List<Proposal>> {
        val path = "proposals"
        return db.collection(path)
            .whereEqualTo("recipientId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Proposal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    fun observeSentProposals(userId: String): Flow<List<Proposal>> {
        val path = "proposals"
        return db.collection(path)
            .whereEqualTo("senderId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Proposal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .handleStreamError(OperationType.LIST, path, userId)
    }

    // --- Initial Account & Showcase Seeding ---

    suspend fun ensureUserAndSeedCatalog(
        displayName: String?,
        email: String?,
        photoUrl: String?
    ) {
        val uid = requireUserId()
        val resolvedName = displayName?.takeIf { it.isNotBlank() } ?: "Innovator"

        val userDoc = db.collection("users").document(uid)
        try {
            val snap = userDoc.get().await()
            if (!snap.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    name = resolvedName,
                    email = email,
                    role = "Creator",
                    headline = "Independent Innovator & Builder",
                    bio = "Exploring high-impact concepts, prototypes, and licensing opportunities on IDEA2IMPACT.",
                    skills = listOf("Product Strategy", "Prototyping", "IP Discovery"),
                    location = "Global",
                    avatarUrl = photoUrl
                )
                userDoc.set(profile.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path, auth)
            throw e
        }

        val talentDoc = db.collection("talents").document(uid)
        try {
            val talentSnap = talentDoc.get().await()
            if (!talentSnap.exists()) {
                val talent = TalentProfile(
                    userId = uid,
                    name = resolvedName,
                    role = "Creator",
                    headline = "Independent Innovator & Builder",
                    bio = "Building and validating breakthrough ideas on IDEA2IMPACT.",
                    skills = listOf("Product Strategy", "Prototyping", "IP Discovery"),
                    location = "Global",
                    avatarUrl = photoUrl,
                    projectsCount = 0L,
                    isPublic = true
                )
                talentDoc.set(talent.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, talentDoc.path, auth)
            throw e
        }

        // Seed showcase catalog idempotently if empty or below starter threshold
        try {
            val existingInnovations = db.collection("innovations")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(5)
                .get()
                .await()

            if (existingInnovations.size() < 5) {
                val seedItems = SeedCatalog.buildSeedInnovations(uid)
                val batch = db.batch()
                for ((docId, item) in seedItems) {
                    val ref = db.collection("innovations").document(docId)
                    batch.set(ref, item.toCreateMap())
                }
                batch.commit().await()
            }

            val existingTalents = db.collection("talents")
                .whereEqualTo("isPublic", true)
                .limit(5)
                .get()
                .await()

            if (existingTalents.size() < 5) {
                val seedTalents = SeedCatalog.buildSeedTalents(uid)
                val batch = db.batch()
                for ((docId, talent) in seedTalents) {
                    val ref = db.collection("talents").document(docId)
                    batch.set(ref, talent.toCreateMap())
                }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed_catalog", auth)
            throw e
        }
    }

    // --- Innovations CRUD ---

    suspend fun createInnovation(
        title: String,
        description: String,
        category: String,
        price: Double,
        visibility: String,
        licensing: Boolean,
        funding: Boolean,
        imageUrl: String?,
        stageIndex: Long,
        authorProfile: UserProfile?
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("innovations").document()
        val innovation = Innovation(
            creatorId = uid,
            authorName = authorProfile?.name?.ifBlank { "Innovator" }
                ?: auth.currentUser?.displayName ?: "Innovator",
            authorHeadline = authorProfile?.headline,
            authorAvatar = authorProfile?.avatarUrl ?: auth.currentUser?.photoUrl?.toString(),
            title = title.trim(),
            description = description.trim(),
            category = category,
            price = price.coerceAtLeast(0.0),
            visibility = if (visibility == "PRIVATE") "PRIVATE" else "PUBLIC",
            licensing = licensing,
            funding = funding,
            ownershipConfirmed = true,
            imageUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() },
            likeCount = 0L,
            commentCount = 0L,
            stageIndex = stageIndex.coerceIn(0L, 3L),
            isShowcaseSeed = false
        )
        try {
            docRef.set(innovation.toCreateMap()).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateInnovation(innovation: Innovation) {
        requireUserId()
        require(innovation.id.isNotBlank()) { "Innovation ID is required for update." }
        val docRef = db.collection("innovations").document(innovation.id)
        try {
            docRef.update(innovation.toOwnerUpdateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteInnovation(innovationId: String) {
        requireUserId()
        val docRef = db.collection("innovations").document(innovationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- Likes, Saves & Comments ---

    suspend fun toggleLike(innovation: Innovation, currentlyLiked: Boolean) {
        val uid = requireUserId()
        val likedRef = db.collection("users").document(uid).collection("liked").document(innovation.id)
        val innovationRef = db.collection("innovations").document(innovation.id)
        try {
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likedRef)
                if (innovation.likeCount > 0L) {
                    batch.update(
                        innovationRef,
                        mapOf(
                            "likeCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                batch.set(
                    likedRef,
                    mapOf(
                        "userId" to uid,
                        "innovationId" to innovation.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    innovationRef,
                    mapOf(
                        "likeCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, innovationRef.path, auth)
            throw e
        }
    }

    suspend fun toggleSave(innovation: Innovation, currentlySaved: Boolean) {
        val uid = requireUserId()
        val savedRef = db.collection("users").document(uid).collection("saved").document(innovation.id)
        try {
            if (currentlySaved) {
                savedRef.delete().await()
            } else {
                val item = SavedItem(
                    userId = uid,
                    innovationId = innovation.id,
                    title = innovation.title,
                    category = innovation.category,
                    price = innovation.price,
                    authorName = innovation.authorName,
                    imageUrl = innovation.imageUrl
                )
                savedRef.set(item.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, savedRef.path, auth)
            throw e
        }
    }

    suspend fun addComment(innovationId: String, body: String, authorProfile: UserProfile?): String {
        val uid = requireUserId()
        val commentRef = db.collection("innovations").document(innovationId)
            .collection("comments").document()
        val innovationRef = db.collection("innovations").document(innovationId)
        val comment = InnovationComment(
            innovationId = innovationId,
            authorId = uid,
            authorName = authorProfile?.name?.ifBlank { "Innovator" }
                ?: auth.currentUser?.displayName ?: "Innovator",
            authorAvatar = authorProfile?.avatarUrl ?: auth.currentUser?.photoUrl?.toString(),
            body = body.trim(),
            visibility = "PUBLIC"
        )
        try {
            val batch = db.batch()
            batch.set(commentRef, comment.toCreateMap())
            batch.update(
                innovationRef,
                mapOf(
                    "commentCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            return commentRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path, auth)
            throw e
        }
    }

    suspend fun deleteComment(innovationId: String, commentId: String, currentCommentCount: Long) {
        requireUserId()
        val commentRef = db.collection("innovations").document(innovationId)
            .collection("comments").document(commentId)
        val innovationRef = db.collection("innovations").document(innovationId)
        try {
            val batch = db.batch()
            batch.delete(commentRef)
            if (currentCommentCount > 0L) {
                batch.update(
                    innovationRef,
                    mapOf(
                        "commentCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path, auth)
            throw e
        }
    }

    // --- Deal Room, NDA, Micro-Backing & Proposals ---

    suspend fun signDigitalNda(
        innovation: Innovation,
        signerName: String,
        signerOrg: String?
    ): SignedNda {
        val uid = requireUserId()
        val sigId = SecurityCryptoUtils.formatSignatureId(innovation.id)
        val ndaRef = db.collection("users").document(uid).collection("signedNdas").document(innovation.id)
        val nda = SignedNda(
            userId = uid,
            innovationId = innovation.id,
            innovationTitle = innovation.title,
            signerName = signerName.trim(),
            signerOrg = signerOrg?.trim()?.takeIf { it.isNotBlank() },
            signatureId = sigId
        )
        try {
            ndaRef.set(nda.toCreateMap()).await()
            return nda
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ndaRef.path, auth)
            throw e
        }
    }

    suspend fun submitMicroPledge(
        innovation: Innovation,
        supporterName: String,
        amount: Double,
        message: String?,
        authorProfile: UserProfile?
    ): Pledge {
        val uid = requireUserId()
        val receiptId = SecurityCryptoUtils.formatReceiptId()
        val pledgeRef = db.collection("users").document(uid).collection("pledges").document()
        val pledge = Pledge(
            userId = uid,
            innovationId = innovation.id,
            innovationTitle = innovation.title,
            supporterName = supporterName.trim(),
            amount = amount.coerceIn(1.0, 100_000.0),
            message = message?.trim()?.takeIf { it.isNotBlank() },
            receiptId = receiptId
        )
        try {
            pledgeRef.set(pledge.toCreateMap()).await()
            val noteText = message?.trim()?.takeIf { it.isNotBlank() } ?: "Keep building, amazing invention!"
            addComment(
                innovationId = innovation.id,
                body = "Pledged $${amount.toInt()} in micro-backing support! \"$noteText\"",
                authorProfile = authorProfile
            )
            return pledge
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, pledgeRef.path, auth)
            throw e
        }
    }

    suspend fun submitProposal(
        innovation: Innovation,
        proposalType: String,
        amount: String?,
        message: String,
        contactInfo: String?,
        senderProfile: UserProfile?
    ): String {
        val uid = requireUserId()
        val proposalRef = db.collection("proposals").document()
        val proposal = Proposal(
            senderId = uid,
            senderName = senderProfile?.name?.ifBlank { "Innovator" }
                ?: auth.currentUser?.displayName ?: "Innovator",
            senderHeadline = senderProfile?.headline,
            senderLocation = senderProfile?.location,
            recipientId = innovation.creatorId,
            innovationId = innovation.id,
            innovationTitle = innovation.title,
            proposalType = proposalType,
            amount = amount?.trim()?.takeIf { it.isNotBlank() },
            message = message.trim(),
            contactInfo = contactInfo?.trim()?.takeIf { it.isNotBlank() },
            status = "PENDING"
        )
        try {
            proposalRef.set(proposal.toCreateMap()).await()
            return proposalRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, proposalRef.path, auth)
            throw e
        }
    }

    suspend fun updateProposalStatus(proposalId: String, newStatus: String) {
        requireUserId()
        require(newStatus == "ACCEPTED" || newStatus == "DECLINED") { "Invalid proposal status: $newStatus" }
        val proposalRef = db.collection("proposals").document(proposalId)
        try {
            proposalRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, proposalRef.path, auth)
            throw e
        }
    }

    // --- Profile & Talent Directory Sync ---

    suspend fun updateUserProfile(
        name: String,
        role: String,
        headline: String,
        bio: String,
        skills: List<String>,
        location: String,
        existingProjectsCount: Long
    ) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val talentRef = db.collection("talents").document(uid)
        val email = auth.currentUser?.email
        val avatarUrl = auth.currentUser?.photoUrl?.toString()

        val updatedUser = UserProfile(
            userId = uid,
            name = name.trim().ifBlank { "Innovator" },
            email = email,
            role = role,
            headline = headline.trim(),
            bio = bio.trim(),
            skills = skills,
            location = location.trim(),
            avatarUrl = avatarUrl
        )
        val updatedTalent = TalentProfile(
            userId = uid,
            name = updatedUser.name,
            role = role,
            headline = headline.trim(),
            bio = bio.trim(),
            skills = skills,
            location = location.trim(),
            avatarUrl = avatarUrl,
            projectsCount = existingProjectsCount,
            isPublic = true
        )
        try {
            val batch = db.batch()
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                batch.update(userRef, updatedUser.toUpdateMap())
            } else {
                batch.set(userRef, updatedUser.toCreateMap())
            }
            val talentSnap = talentRef.get().await()
            if (talentSnap.exists()) {
                batch.update(talentRef, updatedTalent.toUpdateMap())
            } else {
                batch.set(talentRef, updatedTalent.toCreateMap())
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            throw e
        }
    }
}
