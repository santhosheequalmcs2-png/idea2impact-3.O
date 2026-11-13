package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedInnovationEntity::class, CachedCategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class Idea2ImpactDatabase : RoomDatabase() {
    abstract fun innovationDao(): InnovationDao

    companion object {
        @Volatile
        private var INSTANCE: Idea2ImpactDatabase? = null

        fun getInstance(context: Context): Idea2ImpactDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    Idea2ImpactDatabase::class.java,
                    "idea2impact_cache.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
