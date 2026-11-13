package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object InnovationCatalog {
    val categories = listOf(
        "Ideas",
        "Inventions",
        "Physical Products",
        "Websites",
        "Mobile Apps",
        "Games",
        "Software",
        "Creative Designs"
    )

    val roles = listOf(
        "Creator",
        "Builder",
        "Investor",
        "Patent Professional",
        "Company",
        "Customer / Buyer"
    )

    val proposalTypes = listOf(
        "Investment",
        "Licensing",
        "Co-founder",
        "Acquisition",
        "Collaboration"
    )

    val lifecycleStages = listOf(
        "Concept",
        "Prototype",
        "IP / Patent",
        "Scale & Deal"
    )
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String? = null,
    val role: String = "Creator",
    val headline: String? = null,
    val bio: String? = null,
    val skills: List<String> = emptyList(),
    val location: String? = null,
    val avatarUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "name" to name.take(100),
            "email" to email?.take(160),
            "role" to role,
            "headline" to headline?.take(200),
            "bio" to bio?.take(2000),
            "skills" to skills.take(20).map { it.take(64) },
            "location" to location?.take(120),
            "avatarUrl" to avatarUrl?.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "name" to name.take(100),
            "email" to email?.take(160),
            "role" to role,
            "headline" to headline?.take(200),
            "bio" to bio?.take(2000),
            "skills" to skills.take(20).map { it.take(64) },
            "location" to location?.take(120),
            "avatarUrl" to avatarUrl?.take(1000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class Innovation(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val authorName: String = "",
    val authorHeadline: String? = null,
    val authorAvatar: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "Ideas",
    val price: Double = 0.0,
    val visibility: String = "PUBLIC",
    val licensing: Boolean = true,
    val funding: Boolean = true,
    val ownershipConfirmed: Boolean = true,
    val imageUrl: String? = null,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val stageIndex: Long = 0L,
    val isShowcaseSeed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "creatorId" to creatorId,
            "authorName" to authorName.take(120),
            "authorHeadline" to authorHeadline?.take(200),
            "authorAvatar" to authorAvatar?.take(1000),
            "title" to title.take(140),
            "description" to description.take(10000),
            "category" to category.take(60),
            "price" to price.coerceIn(0.0, 100_000_000.0),
            "visibility" to visibility,
            "licensing" to licensing,
            "funding" to funding,
            "ownershipConfirmed" to ownershipConfirmed,
            "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "likeCount" to likeCount.coerceAtLeast(0L),
            "commentCount" to commentCount.coerceAtLeast(0L),
            "stageIndex" to stageIndex.coerceIn(0L, 3L),
            "isShowcaseSeed" to isShowcaseSeed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toOwnerUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "title" to title.take(140),
            "description" to description.take(10000),
            "category" to category.take(60),
            "price" to price.coerceIn(0.0, 100_000_000.0),
            "visibility" to visibility,
            "licensing" to licensing,
            "funding" to funding,
            "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "stageIndex" to stageIndex.coerceIn(0L, 3L),
            "authorName" to authorName.take(120),
            "authorHeadline" to authorHeadline?.take(200),
            "authorAvatar" to authorAvatar?.take(1000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class InnovationComment(
    @DocumentId val id: String = "",
    val innovationId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val body: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "innovationId" to innovationId,
            "authorId" to authorId,
            "authorName" to authorName.take(120),
            "authorAvatar" to authorAvatar?.take(1000),
            "body" to body.take(2000),
            "visibility" to visibility,
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class TalentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "Creator",
    val headline: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val location: String = "",
    val avatarUrl: String? = null,
    val projectsCount: Long = 0L,
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "name" to name.take(120),
            "role" to role,
            "headline" to headline.take(200),
            "bio" to bio.take(2000),
            "skills" to skills.take(20).map { it.take(64) },
            "location" to location.take(120),
            "avatarUrl" to avatarUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "projectsCount" to projectsCount.coerceIn(0L, 1000L),
            "isPublic" to isPublic,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "name" to name.take(120),
            "role" to role,
            "headline" to headline.take(200),
            "bio" to bio.take(2000),
            "skills" to skills.take(20).map { it.take(64) },
            "location" to location.take(120),
            "avatarUrl" to avatarUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "projectsCount" to projectsCount.coerceIn(0L, 1000L),
            "isPublic" to isPublic,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class Proposal(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderHeadline: String? = null,
    val senderLocation: String? = null,
    val recipientId: String = "",
    val innovationId: String = "",
    val innovationTitle: String = "",
    val proposalType: String = "Collaboration",
    val amount: String? = null,
    val message: String = "",
    val contactInfo: String? = null,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "senderId" to senderId,
            "senderName" to senderName.take(120),
            "senderHeadline" to senderHeadline?.take(200),
            "senderLocation" to senderLocation?.take(120),
            "recipientId" to recipientId,
            "innovationId" to innovationId,
            "innovationTitle" to innovationTitle.take(160),
            "proposalType" to proposalType,
            "amount" to amount?.takeIf { it.isNotBlank() }?.take(120),
            "message" to message.take(2500),
            "contactInfo" to contactInfo?.takeIf { it.isNotBlank() }?.take(200),
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class SavedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val innovationId: String = "",
    val title: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val authorName: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "innovationId" to innovationId,
            "title" to title.take(160),
            "category" to category.take(80),
            "price" to price.coerceAtLeast(0.0),
            "authorName" to authorName.take(120),
            "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class SignedNda(
    @DocumentId val id: String = "",
    val userId: String = "",
    val innovationId: String = "",
    val innovationTitle: String = "",
    val signerName: String = "",
    val signerOrg: String? = null,
    val signatureId: String = "",
    @ServerTimestamp val signedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "innovationId" to innovationId,
            "innovationTitle" to innovationTitle.take(160),
            "signerName" to signerName.take(120),
            "signerOrg" to signerOrg?.takeIf { it.isNotBlank() }?.take(160),
            "signatureId" to signatureId.take(80),
            "signedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class Pledge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val innovationId: String = "",
    val innovationTitle: String = "",
    val supporterName: String = "",
    val amount: Double = 0.0,
    val message: String? = null,
    val receiptId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "innovationId" to innovationId,
            "innovationTitle" to innovationTitle.take(160),
            "supporterName" to supporterName.take(120),
            "amount" to amount.coerceIn(1.0, 100_000.0),
            "message" to message?.takeIf { it.isNotBlank() }?.take(500),
            "receiptId" to receiptId.take(80),
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

data class InvestorOnePager(
    val executiveSummary: String,
    val problemStatement: String,
    val solutionAdvantage: String,
    val marketOpportunity: String,
    val suggestedTranche: String,
    val milestones: List<String>
)

data class AiChatMessage(
    val id: String,
    val isAssistant: Boolean,
    val content: String,
    val timestampLabel: String,
    val engineLabel: String = "Gemini 3.8 Flash",
    val isStreaming: Boolean = false
)
