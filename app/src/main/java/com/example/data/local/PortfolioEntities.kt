package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import java.util.UUID

@Entity(
    tableName = "idea_portfolio",
    indices = [
        Index(value = ["category"]),
        Index(value = ["savedAt"])
    ]
)
data class IdeaPortfolioEntity(
    @PrimaryKey val id: String,
    val innovationId: String? = null,
    val title: String,
    val summary: String,
    val category: String,
    val stage: String,
    val creatorName: String,
    val fundingGoal: Double,
    val valuation: Double,
    val ndaRequired: Boolean,
    val priorityTag: String = "High Priority",
    val validationNotes: String = "",
    val savedAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "investor_portfolio",
    indices = [
        Index(value = ["diligenceStatus"]),
        Index(value = ["category"]),
        Index(value = ["savedAt"])
    ]
)
data class InvestorPortfolioEntity(
    @PrimaryKey val id: String,
    val innovationId: String? = null,
    val title: String,
    val summary: String,
    val category: String,
    val stage: String,
    val founderName: String,
    val fundingGoal: Double,
    val valuation: Double,
    val ndaSigned: Boolean = false,
    val targetTicketSize: String = "",
    val diligenceStatus: String = "Watching",
    val investmentThesisNotes: String = "",
    val savedAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "saved_profiles",
    indices = [
        Index(value = ["roleCategory"]),
        Index(value = ["savedAt"])
    ]
)
data class SavedProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    val roleCategory: String,
    val headline: String,
    val skillsEncoded: String,
    val location: String,
    val avatarUrl: String? = null,
    val verified: Boolean = true,
    val savedAt: Long,
    val updatedAt: Long
)

val SavedProfileEntity.skills: List<String>
    get() = skillsEncoded
        .split('\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

fun BackendInnovation.toIdeaPortfolioEntity(
    priorityTag: String = "High Priority",
    validationNotes: String = "",
    nowMs: Long = System.currentTimeMillis()
): IdeaPortfolioEntity = IdeaPortfolioEntity(
    id = id,
    innovationId = id,
    title = title,
    summary = description,
    category = category,
    stage = stage,
    creatorName = authorName,
    fundingGoal = fundingGoal,
    valuation = valuation,
    ndaRequired = ndaRequired,
    priorityTag = priorityTag,
    validationNotes = validationNotes,
    savedAt = nowMs,
    updatedAt = nowMs
)

fun BackendInnovation.toInvestorPortfolioEntity(
    diligenceStatus: String = "Watching",
    targetTicketSize: String = "",
    investmentThesisNotes: String = "",
    ndaSigned: Boolean = false,
    nowMs: Long = System.currentTimeMillis()
): InvestorPortfolioEntity = InvestorPortfolioEntity(
    id = id,
    innovationId = id,
    title = title,
    summary = description,
    category = category,
    stage = stage,
    founderName = authorName,
    fundingGoal = fundingGoal,
    valuation = valuation,
    ndaSigned = ndaSigned,
    targetTicketSize = targetTicketSize,
    diligenceStatus = diligenceStatus,
    investmentThesisNotes = investmentThesisNotes,
    savedAt = nowMs,
    updatedAt = nowMs
)

fun IdeaPortfolioEntity.toBackendInnovation(): BackendInnovation = BackendInnovation(
    id = innovationId ?: id,
    title = title,
    description = summary,
    category = category,
    stage = stage,
    fundingGoal = fundingGoal,
    valuation = valuation,
    imageUrl = null,
    authorId = "local_portfolio",
    authorName = creatorName,
    authorTitle = priorityTag,
    authorAvatar = null,
    isVerifiedCreator = false,
    likeCount = 0,
    commentCount = 0,
    isLiked = false,
    isBookmarked = true,
    ndaRequired = ndaRequired,
    createdAt = ""
)

fun InvestorPortfolioEntity.toBackendInnovation(): BackendInnovation = BackendInnovation(
    id = innovationId ?: id,
    title = title,
    description = summary,
    category = category,
    stage = stage,
    fundingGoal = fundingGoal,
    valuation = valuation,
    imageUrl = null,
    authorId = "local_investor_portfolio",
    authorName = founderName,
    authorTitle = diligenceStatus,
    authorAvatar = null,
    isVerifiedCreator = false,
    likeCount = 0,
    commentCount = 0,
    isLiked = false,
    isBookmarked = true,
    ndaRequired = !ndaSigned,
    createdAt = ""
)

fun BackendTalent.toSavedProfileEntity(
    nowMs: Long = System.currentTimeMillis()
): SavedProfileEntity = SavedProfileEntity(
    id = id,
    name = name,
    role = role,
    roleCategory = roleCategory,
    headline = headline,
    skillsEncoded = skills.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n"),
    location = location,
    avatarUrl = avatarUrl,
    verified = verified,
    savedAt = nowMs,
    updatedAt = nowMs
)

fun SavedProfileEntity.toBackendTalent(): BackendTalent = BackendTalent(
    id = id,
    name = name,
    role = role,
    roleCategory = roleCategory,
    headline = headline,
    skills = skills,
    location = location,
    avatarUrl = avatarUrl,
    verified = verified
)

fun newIdeaPortfolioId(): String = "idea-${UUID.randomUUID()}"

fun newInvestorPortfolioId(): String = "inv-${UUID.randomUUID()}"
