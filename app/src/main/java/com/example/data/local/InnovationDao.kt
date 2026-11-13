package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InnovationDao {

    @Query("SELECT * FROM cached_innovations ORDER BY sortOrder ASC, cachedAt DESC")
    fun observeAllInnovations(): Flow<List<CachedInnovationEntity>>

    @Query(
        """
        SELECT * FROM cached_innovations
        WHERE (:category = '' OR :category = 'All' OR LOWER(category) = LOWER(:category))
          AND (:stage = '' OR :stage = 'All' OR LOWER(stage) = LOWER(:stage))
          AND (:search = '' OR LOWER(title) LIKE '%' || LOWER(:search) || '%'
               OR LOWER(description) LIKE '%' || LOWER(:search) || '%'
               OR LOWER(category) LIKE '%' || LOWER(:search) || '%'
               OR LOWER(authorName) LIKE '%' || LOWER(:search) || '%')
        ORDER BY sortOrder ASC, cachedAt DESC
        """
    )
    fun observeFilteredInnovations(
        search: String,
        category: String,
        stage: String
    ): Flow<List<CachedInnovationEntity>>

    @Query("SELECT * FROM cached_innovations WHERE id = :id LIMIT 1")
    fun observeInnovationById(id: String): Flow<CachedInnovationEntity?>

    @Query("SELECT * FROM cached_innovations WHERE id = :id LIMIT 1")
    suspend fun getInnovationByIdOnce(id: String): CachedInnovationEntity?

    @Query("SELECT COUNT(*) FROM cached_innovations")
    suspend fun getInnovationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInnovations(items: List<CachedInnovationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInnovation(item: CachedInnovationEntity)

    @Query(
        """
        UPDATE cached_innovations
        SET isLiked = :isLiked, likeCount = :likeCount
        WHERE id = :id
        """
    )
    suspend fun updateLikeState(id: String, isLiked: Boolean, likeCount: Int)

    @Query(
        """
        UPDATE cached_innovations
        SET isBookmarked = :isBookmarked
        WHERE id = :id
        """
    )
    suspend fun updateBookmarkState(id: String, isBookmarked: Boolean)

    @Query("DELETE FROM cached_innovations")
    suspend fun clearInnovations()

    @Transaction
    suspend fun replaceAllInnovations(items: List<CachedInnovationEntity>) {
        clearInnovations()
        if (items.isNotEmpty()) {
            upsertInnovations(items)
        }
    }

    @Query("SELECT * FROM cached_categories ORDER BY sortOrder ASC")
    fun observeCategories(): Flow<List<CachedCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CachedCategoryEntity>)

    @Query("DELETE FROM cached_categories")
    suspend fun clearCategories()

    @Transaction
    suspend fun replaceCategories(items: List<CachedCategoryEntity>) {
        clearCategories()
        if (items.isNotEmpty()) {
            upsertCategories(items)
        }
    }
}
