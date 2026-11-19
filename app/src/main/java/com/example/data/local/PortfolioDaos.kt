package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaPortfolioDao {

    @Query("SELECT * FROM idea_portfolio ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<IdeaPortfolioEntity>>

    @Query("SELECT * FROM idea_portfolio WHERE category = :category ORDER BY updatedAt DESC")
    fun observeByCategory(category: String): Flow<List<IdeaPortfolioEntity>>

    @Query(
        """
        SELECT * FROM idea_portfolio
        WHERE (:query = ''
            OR title LIKE '%' || :query || '%'
            OR summary LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
            OR validationNotes LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
        """
    )
    fun searchIdeas(query: String): Flow<List<IdeaPortfolioEntity>>

    @Query("SELECT id FROM idea_portfolio")
    fun observeSavedIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM idea_portfolio")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM idea_portfolio WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): IdeaPortfolioEntity?

    @Query("SELECT * FROM idea_portfolio WHERE innovationId = :innovationId LIMIT 1")
    suspend fun getByInnovationIdOnce(innovationId: String): IdeaPortfolioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: IdeaPortfolioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<IdeaPortfolioEntity>)

    @Query(
        """
        UPDATE idea_portfolio
        SET priorityTag = :priorityTag, validationNotes = :notes, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateNotes(id: String, priorityTag: String, notes: String, updatedAt: Long)

    @Query("DELETE FROM idea_portfolio WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM idea_portfolio")
    suspend fun clearAll()
}

@Dao
interface InvestorPortfolioDao {

    @Query("SELECT * FROM investor_portfolio ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<InvestorPortfolioEntity>>

    @Query("SELECT * FROM investor_portfolio WHERE diligenceStatus = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<InvestorPortfolioEntity>>

    @Query(
        """
        SELECT * FROM investor_portfolio
        WHERE (:query = ''
            OR title LIKE '%' || :query || '%'
            OR summary LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
            OR investmentThesisNotes LIKE '%' || :query || '%'
            OR diligenceStatus LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
        """
    )
    fun searchDeals(query: String): Flow<List<InvestorPortfolioEntity>>

    @Query("SELECT id FROM investor_portfolio")
    fun observeSavedIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM investor_portfolio")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM investor_portfolio WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): InvestorPortfolioEntity?

    @Query("SELECT * FROM investor_portfolio WHERE innovationId = :innovationId LIMIT 1")
    suspend fun getByInnovationIdOnce(innovationId: String): InvestorPortfolioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InvestorPortfolioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InvestorPortfolioEntity>)

    @Query(
        """
        UPDATE investor_portfolio
        SET diligenceStatus = :diligenceStatus,
            targetTicketSize = :targetTicketSize,
            investmentThesisNotes = :notes,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateDiligence(
        id: String,
        diligenceStatus: String,
        targetTicketSize: String,
        notes: String,
        updatedAt: Long
    )

    @Query("DELETE FROM investor_portfolio WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM investor_portfolio")
    suspend fun clearAll()
}

@Dao
interface SavedProfileDao {

    @Query("SELECT * FROM saved_profiles ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedProfileEntity>>

    @Query("SELECT * FROM saved_profiles WHERE roleCategory = :roleCategory ORDER BY savedAt DESC")
    fun observeByRoleCategory(roleCategory: String): Flow<List<SavedProfileEntity>>

    @Query(
        """
        SELECT * FROM saved_profiles
        WHERE (:query = ''
            OR name LIKE '%' || :query || '%'
            OR role LIKE '%' || :query || '%'
            OR roleCategory LIKE '%' || :query || '%'
            OR headline LIKE '%' || :query || '%'
            OR skillsEncoded LIKE '%' || :query || '%'
            OR location LIKE '%' || :query || '%')
        ORDER BY savedAt DESC
        """
    )
    fun searchSavedProfiles(query: String): Flow<List<SavedProfileEntity>>

    @Query("SELECT id FROM saved_profiles")
    fun observeSavedIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM saved_profiles")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM saved_profiles WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): SavedProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SavedProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SavedProfileEntity>)

    @Query("DELETE FROM saved_profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM saved_profiles")
    suspend fun clearAll()
}
