package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.repository.PortfolioRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PortfolioDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: Idea2ImpactDatabase
    private lateinit var repository: PortfolioRepository

    private val sampleInnovation = BackendInnovation(
        id = "inn-solar-01",
        title = "Solar Desalination Microgrid",
        description = "Off-grid brackish water desalination for coastal agricultural clusters.",
        category = "CleanTech",
        stage = "MVP",
        fundingGoal = 150000.0,
        valuation = 1200000.0,
        imageUrl = null,
        authorId = "founder-1",
        authorName = "Ananya Verma",
        authorTitle = "Hardware Lead",
        authorAvatar = null,
        isVerifiedCreator = true,
        likeCount = 12,
        commentCount = 3,
        isLiked = false,
        isBookmarked = false,
        ndaRequired = true,
        createdAt = "2026-10-12"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, Idea2ImpactDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PortfolioRepository(
            ideaPortfolioDao = db.ideaPortfolioDao(),
            investorPortfolioDao = db.investorPortfolioDao(),
            savedProfileDao = db.savedProfileDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun ideaPortfolio_saveSearchUpdateAndRemove_persistsCorrectly() {
        runBlocking {
        val toggledOn = repository.toggleInnovationInIdeaPortfolio(sampleInnovation)
        assertThat(toggledOn).isTrue()

        val custom = repository.saveCustomIdeaToPortfolio(
            title = "Cold-Chain Telemetry Node",
            summary = "LoRaWAN spoilage alert sensors for rural dairy.",
            category = "AgriTech",
            stage = "Concept",
            priorityTag = "Validating",
            notes = "Interview 10 dairy co-ops in Gujarat."
        )

        val allIdeas = repository.observeIdeaPortfolio().first()
        assertThat(allIdeas).hasSize(2)
        assertThat(allIdeas.map { it.title }).containsExactly(
            "Cold-Chain Telemetry Node",
            "Solar Desalination Microgrid"
        )

        val filtered = repository.observeIdeaPortfolio("desalination").first()
        assertThat(filtered.map { it.id }).containsExactly(sampleInnovation.id)

        repository.updateIdeaPortfolioNotes(
            id = custom.id,
            priorityTag = "MVP Ready",
            notes = "Validated with 6 cold storage units."
        )
        val updated = db.ideaPortfolioDao().getByIdOnce(custom.id)
        assertThat(updated?.priorityTag).isEqualTo("MVP Ready")
        assertThat(updated?.validationNotes).isEqualTo("Validated with 6 cold storage units.")

        val toggledOff = repository.toggleInnovationInIdeaPortfolio(sampleInnovation)
        assertThat(toggledOff).isFalse()
        assertThat(repository.observeIdeaPortfolioIds().first()).containsExactly(custom.id)
        }
    }

    @Test
    fun investorPortfolio_saveFilterUpdateDiligenceAndRemove_persistsCorrectly() {
        runBlocking {
        val savedDeal = repository.saveInnovationToInvestorPortfolio(
            innovation = sampleInnovation,
            diligenceStatus = "In Diligence",
            targetTicketSize = "$50K",
            notes = "Strong hardware unit economics; check membrane lifetime.",
            ndaSigned = true
        )

        val customDeal = repository.saveCustomProjectToInvestorPortfolio(
            title = "Solid-State Zinc Battery",
            summary = "Non-flammable telecom tower backup storage.",
            category = "Energy",
            stage = "Seed",
            founderName = "Rohan Kulkarni",
            fundingGoal = 500000.0,
            valuation = 4000000.0,
            diligenceStatus = "Watching",
            targetTicketSize = "$100K",
            notes = "Track pilot results in Pune."
        )

        val allDeals = repository.observeInvestorPortfolio().first()
        assertThat(allDeals).hasSize(2)

        val inDiligence = repository.observeInvestorPortfolio(statusFilter = "In Diligence").first()
        assertThat(inDiligence.map { it.id }).containsExactly(savedDeal.id)
        assertThat(inDiligence.first().ndaSigned).isTrue()

        val searchEnergy = repository.observeInvestorPortfolio(query = "Zinc").first()
        assertThat(searchEnergy.map { it.id }).containsExactly(customDeal.id)

        repository.updateInvestorDiligence(
            id = customDeal.id,
            diligenceStatus = "Term Sheet",
            targetTicketSize = "$150K",
            notes = "Lead co-investor confirmed."
        )
        val updatedDeal = db.investorPortfolioDao().getByIdOnce(customDeal.id)
        assertThat(updatedDeal?.diligenceStatus).isEqualTo("Term Sheet")
        assertThat(updatedDeal?.targetTicketSize).isEqualTo("$150K")
        assertThat(updatedDeal?.investmentThesisNotes).isEqualTo("Lead co-investor confirmed.")

        repository.removeFromInvestorPortfolio(savedDeal.id)
        assertThat(repository.observeInvestorPortfolioIds().first()).containsExactly(customDeal.id)
        }
    }

    @Test
    fun sameProject_canLiveInBothIdeaAndInvestorPortfoliosIndependently() {
        runBlocking {
        repository.saveInnovationToIdeaPortfolio(sampleInnovation, priorityTag = "MVP Ready", notes = "Build v2 membrane")
        repository.saveInnovationToInvestorPortfolio(sampleInnovation, diligenceStatus = "Committed", targetTicketSize = "$75K")

        assertThat(repository.observeIdeaPortfolioIds().first()).containsExactly(sampleInnovation.id)
        assertThat(repository.observeInvestorPortfolioIds().first()).containsExactly(sampleInnovation.id)

        repository.removeFromIdeaPortfolio(sampleInnovation.id)
        assertThat(repository.observeIdeaPortfolioIds().first()).isEmpty()
        assertThat(repository.observeInvestorPortfolioIds().first()).containsExactly(sampleInnovation.id)
        }
    }

    @Test
    fun migration2To3_preservesExistingV2DataAndCreatesPortfolioTables() {
        val dbName = "portfolio_migration_test_${System.nanoTime()}.db"
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `cached_innovations` (
                                `id` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `description` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `stage` TEXT NOT NULL,
                                `fundingGoal` REAL NOT NULL,
                                `valuation` REAL NOT NULL,
                                `imageUrl` TEXT,
                                `authorId` TEXT NOT NULL,
                                `authorName` TEXT NOT NULL,
                                `authorTitle` TEXT NOT NULL,
                                `authorAvatar` TEXT,
                                `isVerifiedCreator` INTEGER NOT NULL,
                                `likeCount` INTEGER NOT NULL,
                                `commentCount` INTEGER NOT NULL,
                                `isLiked` INTEGER NOT NULL,
                                `isBookmarked` INTEGER NOT NULL,
                                `ndaRequired` INTEGER NOT NULL,
                                `createdAt` TEXT NOT NULL,
                                `sortOrder` INTEGER NOT NULL,
                                `cachedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `cached_categories` (
                                `name` TEXT NOT NULL,
                                `description` TEXT NOT NULL,
                                `count` INTEGER NOT NULL,
                                `sortOrder` INTEGER NOT NULL,
                                `cachedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`name`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `search_history` (
                                `normalizedQuery` TEXT NOT NULL,
                                `query` TEXT NOT NULL,
                                `searchedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`normalizedQuery`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO cached_innovations (
                                id, title, description, category, stage, fundingGoal, valuation,
                                imageUrl, authorId, authorName, authorTitle, authorAvatar,
                                isVerifiedCreator, likeCount, commentCount, isLiked, isBookmarked,
                                ndaRequired, createdAt, sortOrder, cachedAt
                            ) VALUES (
                                'inn-v2', 'Preserved V2 Innovation', 'Desc', 'CleanTech', 'MVP',
                                90000.0, 450000.0, NULL, 'u-2', 'Founder', 'Lead', NULL,
                                1, 8, 2, 0, 1, 1, '2026-06-01', 0, 2000
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO search_history (normalizedQuery, query, searchedAt) VALUES ('solar', 'Solar', 2500)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        val migratedDb = Room.databaseBuilder(context, Idea2ImpactDatabase::class.java, dbName)
            .addMigrations(
                Idea2ImpactDatabase.MIGRATION_1_2,
                Idea2ImpactDatabase.MIGRATION_2_3,
                Idea2ImpactDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val preservedInn = migratedDb.innovationDao().getInnovationByIdOnce("inn-v2")
            assertThat(preservedInn?.title).isEqualTo("Preserved V2 Innovation")

            val preservedSearch = migratedDb.searchHistoryDao().observeRecentSearches(10).first()
            assertThat(preservedSearch.map { it.query }).containsExactly("Solar")

            val migratedRepo = PortfolioRepository(
                ideaPortfolioDao = migratedDb.ideaPortfolioDao(),
                investorPortfolioDao = migratedDb.investorPortfolioDao(),
                savedProfileDao = migratedDb.savedProfileDao()
            )
            migratedRepo.saveCustomIdeaToPortfolio(
                title = "Post-Migration Idea",
                summary = "Saved into v3 Idea Portfolio table.",
                category = "DeepTech",
                stage = "Prototype"
            )
            migratedRepo.saveCustomProjectToInvestorPortfolio(
                title = "Post-Migration Deal",
                summary = "Saved into v3 Investor Portfolio table.",
                category = "FinTech",
                stage = "Seed",
                targetTicketSize = "$40K"
            )

            assertThat(migratedRepo.observeIdeaPortfolio().first().map { it.title })
                .containsExactly("Post-Migration Idea")
            assertThat(migratedRepo.observeInvestorPortfolio().first().map { it.title })
                .containsExactly("Post-Migration Deal")
        }

        migratedDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun savedProfiles_bookmarkSearchToggleAndRemove_persistsCorrectly() {
        val builder = BackendTalent(
            id = "talent-kavya-01",
            name = "Kavya Nair",
            role = "Embedded Firmware Architect",
            roleCategory = "Builders",
            headline = "Designs ultra-low-power LoRaWAN & BLE medical telemetry nodes.",
            skills = listOf("STM32", "Zephyr RTOS", "BLE, RF Design"),
            location = "Bengaluru",
            avatarUrl = null,
            verified = true
        )
        val investor = BackendTalent(
            id = "talent-arjun-02",
            name = "Arjun Mehta",
            role = "Climate Seed Investor",
            roleCategory = "Investors",
            headline = "Backing pre-seed CleanTech hardware across India.",
            skills = listOf("Hardware Seed", "Supply Chain"),
            location = "Mumbai",
            avatarUrl = null,
            verified = false
        )

        runBlocking {
            val savedFirst = repository.toggleCollaboratorProfile(builder)
            assertThat(savedFirst).isTrue()

            val savedSecond = repository.toggleCollaboratorProfile(investor)
            assertThat(savedSecond).isTrue()

            val allSaved = repository.observeSavedProfiles().first()
            assertThat(allSaved).hasSize(2)
            assertThat(repository.observeSavedProfileIds().first())
                .containsExactly("talent-kavya-01", "talent-arjun-02")

            val roundTripBuilder = allSaved.first { it.id == "talent-kavya-01" }.toBackendTalent()
            assertThat(roundTripBuilder.skills).containsExactly("STM32", "Zephyr RTOS", "BLE, RF Design").inOrder()

            val searchZephyr = repository.observeSavedProfiles("Zephyr").first()
            assertThat(searchZephyr.map { it.id }).containsExactly("talent-kavya-01")

            val toggledOff = repository.toggleCollaboratorProfile(builder)
            assertThat(toggledOff).isFalse()
            assertThat(repository.observeSavedProfileIds().first()).containsExactly("talent-arjun-02")

            repository.removeSavedProfile("talent-arjun-02")
            assertThat(repository.observeSavedProfiles().first()).isEmpty()
        }
    }

    @Test
    fun migration3To4_preservesExistingV3PortfoliosAndCreatesSavedProfilesTable() {
        val dbName = "saved_profiles_migration_test_${System.nanoTime()}.db"
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `cached_innovations` (
                                `id` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `description` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `stage` TEXT NOT NULL,
                                `fundingGoal` REAL NOT NULL,
                                `valuation` REAL NOT NULL,
                                `imageUrl` TEXT,
                                `authorId` TEXT NOT NULL,
                                `authorName` TEXT NOT NULL,
                                `authorTitle` TEXT NOT NULL,
                                `authorAvatar` TEXT,
                                `isVerifiedCreator` INTEGER NOT NULL,
                                `likeCount` INTEGER NOT NULL,
                                `commentCount` INTEGER NOT NULL,
                                `isLiked` INTEGER NOT NULL,
                                `isBookmarked` INTEGER NOT NULL,
                                `ndaRequired` INTEGER NOT NULL,
                                `createdAt` TEXT NOT NULL,
                                `sortOrder` INTEGER NOT NULL,
                                `cachedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `cached_categories` (
                                `name` TEXT NOT NULL,
                                `description` TEXT NOT NULL,
                                `count` INTEGER NOT NULL,
                                `sortOrder` INTEGER NOT NULL,
                                `cachedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`name`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `search_history` (
                                `normalizedQuery` TEXT NOT NULL,
                                `query` TEXT NOT NULL,
                                `searchedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`normalizedQuery`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `idea_portfolio` (
                                `id` TEXT NOT NULL,
                                `innovationId` TEXT,
                                `title` TEXT NOT NULL,
                                `summary` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `stage` TEXT NOT NULL,
                                `creatorName` TEXT NOT NULL,
                                `fundingGoal` REAL NOT NULL,
                                `valuation` REAL NOT NULL,
                                `ndaRequired` INTEGER NOT NULL,
                                `priorityTag` TEXT NOT NULL,
                                `validationNotes` TEXT NOT NULL,
                                `savedAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_idea_portfolio_category` ON `idea_portfolio` (`category`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_idea_portfolio_savedAt` ON `idea_portfolio` (`savedAt`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `investor_portfolio` (
                                `id` TEXT NOT NULL,
                                `innovationId` TEXT,
                                `title` TEXT NOT NULL,
                                `summary` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `stage` TEXT NOT NULL,
                                `founderName` TEXT NOT NULL,
                                `fundingGoal` REAL NOT NULL,
                                `valuation` REAL NOT NULL,
                                `ndaSigned` INTEGER NOT NULL,
                                `targetTicketSize` TEXT NOT NULL,
                                `diligenceStatus` TEXT NOT NULL,
                                `investmentThesisNotes` TEXT NOT NULL,
                                `savedAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_investor_portfolio_diligenceStatus` ON `investor_portfolio` (`diligenceStatus`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_investor_portfolio_category` ON `investor_portfolio` (`category`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_investor_portfolio_savedAt` ON `investor_portfolio` (`savedAt`)")

                        db.execSQL(
                            """
                            INSERT INTO idea_portfolio (
                                id, innovationId, title, summary, category, stage, creatorName,
                                fundingGoal, valuation, ndaRequired, priorityTag, validationNotes,
                                savedAt, updatedAt
                            ) VALUES (
                                'idea-v3', NULL, 'V3 Preserved Idea', 'Saved prior to v4 upgrade',
                                'CleanTech', 'MVP', 'Founder', 0.0, 0.0, 0, 'High Priority', '', 1000, 1000
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        val migratedDb = Room.databaseBuilder(context, Idea2ImpactDatabase::class.java, dbName)
            .addMigrations(
                Idea2ImpactDatabase.MIGRATION_1_2,
                Idea2ImpactDatabase.MIGRATION_2_3,
                Idea2ImpactDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val migratedRepo = PortfolioRepository(
                ideaPortfolioDao = migratedDb.ideaPortfolioDao(),
                investorPortfolioDao = migratedDb.investorPortfolioDao(),
                savedProfileDao = migratedDb.savedProfileDao()
            )
            assertThat(migratedRepo.observeIdeaPortfolio().first().map { it.title })
                .containsExactly("V3 Preserved Idea")

            migratedRepo.saveCollaboratorProfile(
                BackendTalent(
                    id = "talent-migrated-1",
                    name = "Priya Sharma",
                    role = "Patent Attorney",
                    roleCategory = "Patent Professionals",
                    headline = "DeepTech IP strategy",
                    skills = listOf("PCT Filings", "Prior Art"),
                    location = "Delhi",
                    avatarUrl = null,
                    verified = true
                )
            )
            assertThat(migratedRepo.observeSavedProfiles().first().map { it.name })
                .containsExactly("Priya Sharma")
        }

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
