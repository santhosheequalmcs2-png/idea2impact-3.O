package com.example.data.repository

import com.example.data.local.IdeaPortfolioDao
import com.example.data.local.IdeaPortfolioEntity
import com.example.data.local.InvestorPortfolioDao
import com.example.data.local.InvestorPortfolioEntity
import com.example.data.local.SavedProfileDao
import com.example.data.local.SavedProfileEntity
import com.example.data.local.newIdeaPortfolioId
import com.example.data.local.newInvestorPortfolioId
import com.example.data.local.toIdeaPortfolioEntity
import com.example.data.local.toInvestorPortfolioEntity
import com.example.data.local.toSavedProfileEntity
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PortfolioRepository(
    private val ideaPortfolioDao: IdeaPortfolioDao,
    private val investorPortfolioDao: InvestorPortfolioDao,
    private val savedProfileDao: SavedProfileDao? = null
) {
    fun observeIdeaPortfolio(query: String = ""): Flow<List<IdeaPortfolioEntity>> {
        val cleaned = query.trim()
        return if (cleaned.isEmpty()) {
            ideaPortfolioDao.observeAll()
        } else {
            ideaPortfolioDao.searchIdeas(cleaned)
        }
    }

    fun observeIdeaPortfolioIds(): Flow<Set<String>> =
        ideaPortfolioDao.observeSavedIds().map { it.toSet() }

    fun observeInvestorPortfolio(query: String = "", statusFilter: String = "All"): Flow<List<InvestorPortfolioEntity>> {
        val cleaned = query.trim()
        val baseFlow = if (cleaned.isEmpty()) {
            investorPortfolioDao.observeAll()
        } else {
            investorPortfolioDao.searchDeals(cleaned)
        }
        return if (statusFilter == "All" || statusFilter.isBlank()) {
            baseFlow
        } else {
            baseFlow.map { items ->
                items.filter { it.diligenceStatus.equals(statusFilter, ignoreCase = true) }
            }
        }
    }

    fun observeInvestorPortfolioIds(): Flow<Set<String>> =
        investorPortfolioDao.observeSavedIds().map { it.toSet() }

    suspend fun saveInnovationToIdeaPortfolio(
        innovation: BackendInnovation,
        priorityTag: String = "High Priority",
        notes: String = ""
    ): IdeaPortfolioEntity = withContext(Dispatchers.IO) {
        val existing = ideaPortfolioDao.getByIdOnce(innovation.id)
        val now = System.currentTimeMillis()
        val entity = innovation.toIdeaPortfolioEntity(
            priorityTag = existing?.priorityTag ?: priorityTag,
            validationNotes = existing?.validationNotes?.takeIf { notes.isBlank() } ?: notes,
            nowMs = now
        ).copy(savedAt = existing?.savedAt ?: now)
        ideaPortfolioDao.upsert(entity)
        entity
    }

    suspend fun saveCustomIdeaToPortfolio(
        title: String,
        summary: String,
        category: String,
        stage: String,
        creatorName: String = "Founder",
        fundingGoal: Double = 0.0,
        valuation: Double = 0.0,
        priorityTag: String = "High Priority",
        notes: String = ""
    ): IdeaPortfolioEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entity = IdeaPortfolioEntity(
            id = newIdeaPortfolioId(),
            innovationId = null,
            title = title.trim(),
            summary = summary.trim(),
            category = category.trim().ifBlank { "General" },
            stage = stage.trim().ifBlank { "Concept" },
            creatorName = creatorName.trim().ifBlank { "Founder" },
            fundingGoal = fundingGoal.coerceAtLeast(0.0),
            valuation = valuation.coerceAtLeast(0.0),
            ndaRequired = false,
            priorityTag = priorityTag.trim().ifBlank { "High Priority" },
            validationNotes = notes.trim(),
            savedAt = now,
            updatedAt = now
        )
        ideaPortfolioDao.upsert(entity)
        entity
    }

    suspend fun toggleInnovationInIdeaPortfolio(innovation: BackendInnovation): Boolean = withContext(Dispatchers.IO) {
        val existing = ideaPortfolioDao.getByIdOnce(innovation.id)
        if (existing != null) {
            ideaPortfolioDao.deleteById(existing.id)
            false
        } else {
            saveInnovationToIdeaPortfolio(innovation)
            true
        }
    }

    suspend fun updateIdeaPortfolioNotes(
        id: String,
        priorityTag: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        ideaPortfolioDao.updateNotes(
            id = id,
            priorityTag = priorityTag.trim().ifBlank { "High Priority" },
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun removeFromIdeaPortfolio(id: String) = withContext(Dispatchers.IO) {
        ideaPortfolioDao.deleteById(id)
    }

    suspend fun saveInnovationToInvestorPortfolio(
        innovation: BackendInnovation,
        diligenceStatus: String = "Watching",
        targetTicketSize: String = "",
        notes: String = "",
        ndaSigned: Boolean = false
    ): InvestorPortfolioEntity = withContext(Dispatchers.IO) {
        val existing = investorPortfolioDao.getByIdOnce(innovation.id)
        val now = System.currentTimeMillis()
        val entity = innovation.toInvestorPortfolioEntity(
            diligenceStatus = existing?.diligenceStatus ?: diligenceStatus,
            targetTicketSize = existing?.targetTicketSize?.takeIf { targetTicketSize.isBlank() } ?: targetTicketSize,
            investmentThesisNotes = existing?.investmentThesisNotes?.takeIf { notes.isBlank() } ?: notes,
            ndaSigned = existing?.ndaSigned ?: ndaSigned,
            nowMs = now
        ).copy(savedAt = existing?.savedAt ?: now)
        investorPortfolioDao.upsert(entity)
        entity
    }

    suspend fun saveCustomProjectToInvestorPortfolio(
        title: String,
        summary: String,
        category: String,
        stage: String,
        founderName: String = "Founder",
        fundingGoal: Double = 0.0,
        valuation: Double = 0.0,
        diligenceStatus: String = "Watching",
        targetTicketSize: String = "",
        notes: String = ""
    ): InvestorPortfolioEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entity = InvestorPortfolioEntity(
            id = newInvestorPortfolioId(),
            innovationId = null,
            title = title.trim(),
            summary = summary.trim(),
            category = category.trim().ifBlank { "General" },
            stage = stage.trim().ifBlank { "Seed" },
            founderName = founderName.trim().ifBlank { "Founder" },
            fundingGoal = fundingGoal.coerceAtLeast(0.0),
            valuation = valuation.coerceAtLeast(0.0),
            ndaSigned = false,
            targetTicketSize = targetTicketSize.trim(),
            diligenceStatus = diligenceStatus.trim().ifBlank { "Watching" },
            investmentThesisNotes = notes.trim(),
            savedAt = now,
            updatedAt = now
        )
        investorPortfolioDao.upsert(entity)
        entity
    }

    suspend fun toggleInnovationInInvestorPortfolio(
        innovation: BackendInnovation,
        ndaSigned: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = investorPortfolioDao.getByIdOnce(innovation.id)
        if (existing != null) {
            investorPortfolioDao.deleteById(existing.id)
            false
        } else {
            saveInnovationToInvestorPortfolio(innovation, ndaSigned = ndaSigned)
            true
        }
    }

    suspend fun updateInvestorDiligence(
        id: String,
        diligenceStatus: String,
        targetTicketSize: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        investorPortfolioDao.updateDiligence(
            id = id,
            diligenceStatus = diligenceStatus.trim().ifBlank { "Watching" },
            targetTicketSize = targetTicketSize.trim(),
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun removeFromInvestorPortfolio(id: String) = withContext(Dispatchers.IO) {
        investorPortfolioDao.deleteById(id)
    }

    fun observeSavedProfiles(query: String = ""): Flow<List<SavedProfileEntity>> {
        val dao = savedProfileDao ?: return flowOf(emptyList())
        val cleaned = query.trim()
        return if (cleaned.isEmpty()) {
            dao.observeAll()
        } else {
            dao.searchSavedProfiles(cleaned)
        }
    }

    fun observeSavedProfileIds(): Flow<Set<String>> =
        (savedProfileDao?.observeSavedIds() ?: flowOf(emptyList())).map { it.toSet() }

    suspend fun saveCollaboratorProfile(talent: BackendTalent): SavedProfileEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dao = savedProfileDao
        val existing = dao?.getByIdOnce(talent.id)
        val entity = talent.toSavedProfileEntity(nowMs = now)
            .copy(savedAt = existing?.savedAt ?: now)
        dao?.upsert(entity)
        entity
    }

    suspend fun toggleCollaboratorProfile(talent: BackendTalent): Boolean = withContext(Dispatchers.IO) {
        val dao = savedProfileDao ?: return@withContext false
        val existing = dao.getByIdOnce(talent.id)
        if (existing != null) {
            dao.deleteById(existing.id)
            false
        } else {
            saveCollaboratorProfile(talent)
            true
        }
    }

    suspend fun removeSavedProfile(id: String) = withContext(Dispatchers.IO) {
        savedProfileDao?.deleteById(id)
    }
}
