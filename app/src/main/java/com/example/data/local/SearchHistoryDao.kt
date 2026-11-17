package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecentSearches(limit: Int = 10): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 1")
    suspend fun getLatestSearchOnce(): SearchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearch(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE normalizedQuery = :normalizedQuery")
    suspend fun deleteByNormalizedQuery(normalizedQuery: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAllSearches()

    @Query(
        """
        DELETE FROM search_history
        WHERE normalizedQuery NOT IN (
            SELECT normalizedQuery FROM search_history ORDER BY searchedAt DESC LIMIT :maxEntries
        )
        """
    )
    suspend fun trimToLimit(maxEntries: Int)

    @Transaction
    suspend fun recordSearch(
        rawQuery: String,
        searchedAt: Long = System.currentTimeMillis(),
        maxEntries: Int = 10
    ) {
        val cleaned = rawQuery.trim()
        if (cleaned.isBlank()) return
        val normalized = cleaned.lowercase()
        val latest = getLatestSearchOnce()
        if (latest != null && latest.normalizedQuery == normalized && latest.query == cleaned) {
            return
        }
        upsertSearch(
            SearchHistoryEntity(
                normalizedQuery = normalized,
                query = cleaned,
                searchedAt = searchedAt
            )
        )
        trimToLimit(maxEntries.coerceAtLeast(1))
    }
}
