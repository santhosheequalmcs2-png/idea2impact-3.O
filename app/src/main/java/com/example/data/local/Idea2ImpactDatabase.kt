package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedInnovationEntity::class,
        CachedCategoryEntity::class,
        SearchHistoryEntity::class,
        IdeaPortfolioEntity::class,
        InvestorPortfolioEntity::class,
        SavedProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class Idea2ImpactDatabase : RoomDatabase() {
    abstract fun innovationDao(): InnovationDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun ideaPortfolioDao(): IdeaPortfolioDao
    abstract fun investorPortfolioDao(): InvestorPortfolioDao
    abstract fun savedProfileDao(): SavedProfileDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_profiles` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `roleCategory` TEXT NOT NULL,
                        `headline` TEXT NOT NULL,
                        `skillsEncoded` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `avatarUrl` TEXT,
                        `verified` INTEGER NOT NULL,
                        `savedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_profiles_roleCategory` ON `saved_profiles` (`roleCategory`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_profiles_savedAt` ON `saved_profiles` (`savedAt`)")
            }
        }

        @Volatile
        private var INSTANCE: Idea2ImpactDatabase? = null

        fun getInstance(context: Context): Idea2ImpactDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    Idea2ImpactDatabase::class.java,
                    "idea2impact_cache.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
