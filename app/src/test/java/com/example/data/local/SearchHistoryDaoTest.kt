package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
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
class SearchHistoryDaoTest {

    private lateinit var context: Context
    private lateinit var db: Idea2ImpactDatabase
    private lateinit var dao: SearchHistoryDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, Idea2ImpactDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.searchHistoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun emptyInitialState_returnsEmptyList() = runBlocking {
        assertThat(dao.observeRecentSearches(10).first()).isEmpty()
    }

    @Test
    fun recordSearch_ignoresBlank_deduplicatesAndCapsEntries() = runBlocking {
        dao.recordSearch("   ", searchedAt = 100L, maxEntries = 3)
        dao.recordSearch("", searchedAt = 101L, maxEntries = 3)
        assertThat(dao.observeRecentSearches(10).first()).isEmpty()

        dao.recordSearch("Solar", searchedAt = 200L, maxEntries = 3)
        dao.recordSearch("Solar", searchedAt = 205L, maxEntries = 3) // duplicate consecutive
        dao.recordSearch("Robotics", searchedAt = 300L, maxEntries = 3)
        dao.recordSearch("Water Tech", searchedAt = 400L, maxEntries = 3)
        dao.recordSearch("solar", searchedAt = 500L, maxEntries = 3) // case-insensitive bump to top
        dao.recordSearch("AI Diagnostics", searchedAt = 600L, maxEntries = 3) // pushes oldest out

        val results = dao.observeRecentSearches(10).first().map { it.query }
        assertThat(results).containsExactly("AI Diagnostics", "solar", "Water Tech").inOrder()
    }

    @Test
    fun deleteAndClear_removeSelectedAndAllEntries() = runBlocking {
        dao.recordSearch("CleanTech", searchedAt = 100L, maxEntries = 10)
        dao.recordSearch("MedTech", searchedAt = 200L, maxEntries = 10)

        dao.deleteByNormalizedQuery("cleantech")
        assertThat(dao.observeRecentSearches(10).first().map { it.query }).containsExactly("MedTech")

        dao.clearAllSearches()
        assertThat(dao.observeRecentSearches(10).first()).isEmpty()
    }

    @Test
    fun migration1To2_preservesExistingCachedInnovationsAndAddsSearchHistoryTable() {
        val dbName = "migration_test_${System.nanoTime()}.db"
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
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
                            INSERT INTO cached_innovations (
                                id, title, description, category, stage, fundingGoal, valuation,
                                imageUrl, authorId, authorName, authorTitle, authorAvatar,
                                isVerifiedCreator, likeCount, commentCount, isLiked, isBookmarked,
                                ndaRequired, createdAt, sortOrder, cachedAt
                            ) VALUES (
                                'inn-v1', 'Preserved V1 Innovation', 'Desc', 'Green Tech', 'MVP',
                                50000.0, 250000.0, NULL, 'u-1', 'Founder', 'Lead', NULL,
                                1, 5, 1, 0, 1, 1, '2026-01-01', 0, 1000
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
            val cached = migratedDb.innovationDao().getInnovationByIdOnce("inn-v1")
            assertThat(cached).isNotNull()
            assertThat(cached?.title).isEqualTo("Preserved V1 Innovation")

            migratedDb.searchHistoryDao().recordSearch("Grid Microinverter", searchedAt = 500L)
            assertThat(migratedDb.searchHistoryDao().observeRecentSearches(10).first().map { it.query })
                .containsExactly("Grid Microinverter")
        }

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
