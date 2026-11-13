package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BackendInnovation
import com.example.data.model.CategoryItem

@Entity(tableName = "cached_innovations")
data class CachedInnovationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val stage: String,
    val fundingGoal: Double,
    val valuation: Double,
    val imageUrl: String?,
    val authorId: String,
    val authorName: String,
    val authorTitle: String,
    val authorAvatar: String?,
    val isVerifiedCreator: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val ndaRequired: Boolean,
    val createdAt: String,
    val sortOrder: Int,
    val cachedAt: Long
)

@Entity(tableName = "cached_categories")
data class CachedCategoryEntity(
    @PrimaryKey val name: String,
    val description: String,
    val count: Int,
    val sortOrder: Int,
    val cachedAt: Long
)

fun CachedInnovationEntity.toDomain(): BackendInnovation = BackendInnovation(
    id = id,
    title = title,
    description = description,
    category = category,
    stage = stage,
    fundingGoal = fundingGoal,
    valuation = valuation,
    imageUrl = imageUrl,
    authorId = authorId,
    authorName = authorName,
    authorTitle = authorTitle,
    authorAvatar = authorAvatar,
    isVerifiedCreator = isVerifiedCreator,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
    ndaRequired = ndaRequired,
    createdAt = createdAt
)

fun BackendInnovation.toCachedEntity(
    sortOrder: Int = 0,
    cachedAt: Long = System.currentTimeMillis()
): CachedInnovationEntity = CachedInnovationEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    stage = stage,
    fundingGoal = fundingGoal,
    valuation = valuation,
    imageUrl = imageUrl,
    authorId = authorId,
    authorName = authorName,
    authorTitle = authorTitle,
    authorAvatar = authorAvatar,
    isVerifiedCreator = isVerifiedCreator,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
    ndaRequired = ndaRequired,
    createdAt = createdAt,
    sortOrder = sortOrder,
    cachedAt = cachedAt
)

fun CachedCategoryEntity.toDomain(): CategoryItem = CategoryItem(
    name = name,
    description = description,
    count = count
)

fun CategoryItem.toCachedEntity(
    sortOrder: Int,
    cachedAt: Long = System.currentTimeMillis()
): CachedCategoryEntity = CachedCategoryEntity(
    name = name,
    description = description,
    count = count,
    sortOrder = sortOrder,
    cachedAt = cachedAt
)
