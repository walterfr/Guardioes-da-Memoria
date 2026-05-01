package br.com.guardioesdamemoria.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MemoryEntity::class, UserProfile::class, MemoryReactionEntity::class, MemoryReportEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun userDao(): UserDao
    abstract fun reactionDao(): ReactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memories ADD COLUMN authorAge TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE memories ADD COLUMN triggerRadiusMeters REAL NOT NULL DEFAULT 50.0")
                db.execSQL("ALTER TABLE memories ADD COLUMN imageSource TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE memories ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `school` TEXT NOT NULL, `isTeacher` INTEGER NOT NULL, `pin` TEXT, `lastUsed` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `memory_reactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `memoryId` INTEGER NOT NULL, `userId` INTEGER NOT NULL, `reaction` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `memory_reports` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `memoryId` INTEGER NOT NULL, `userId` INTEGER NOT NULL, `reason` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guardioes_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration() // Backup if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
