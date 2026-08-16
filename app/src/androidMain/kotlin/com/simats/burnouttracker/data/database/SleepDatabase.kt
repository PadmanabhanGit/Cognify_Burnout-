package com.simats.burnouttracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SleepSession::class, WakeEvent::class, AppUsageLog::class],
    version = 4,
    exportSchema = false
)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: SleepDatabase? = null

        /**
         * v1 → v2: sleep_sessions gains [SleepSession.ownerUid].
         *
         * Existing rows are left unowned (""). Attributing them here — to
         * whichever account happened to be active when the database was first
         * opened after the upgrade — would hand one user's nights to another,
         * because the database can be opened for the first time long after the
         * upgrade, with a different account signed in. Ownership of pre-existing
         * rows is decided in exactly one place instead: AccountScope claims them
         * for the account that was already signed in when this build first ran,
         * and for no one else.
         *
         * No row is deleted and no detection value is altered.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v2 → v3: sleep_sessions gains [SleepSession.syncedAt].
         *
         * Existing rows default to 0, meaning "the backend has never confirmed
         * this night". That is the honest value: nothing was ever recorded about
         * whether those POSTs succeeded, so the app cannot claim they did. The
         * consequence is that nights already in Room are re-sent once, which
         * reconciles any night that silently failed to sync — the reason the web
         * app could disagree with the phone. Re-sending is idempotent server-side
         * (deterministic automatic document id + merge), so it corrects rather
         * than duplicates.
         *
         * No row is deleted and no detection value is altered.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN syncedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v3 → v4: sleep_sessions gains the sync state model — [SleepSession.syncState],
         * [SleepSession.lastSyncError] and [SleepSession.lastSyncAttemptAt].
         *
         * State is DERIVED from the existing `syncedAt`, not defaulted. A blanket
         * default of PENDING would mark every night the backend has already
         * accepted as owing an upload, and the next refresh would re-POST the lot.
         * `syncedAt > 0` is the app's existing record of "the server confirmed
         * this", so it is exactly the right evidence: those rows become SYNCED and
         * are never re-sent, and rows at 0 become PENDING, which is what the old
         * two-state model already treated them as. Sync behaviour is therefore
         * unchanged by this migration.
         *
         * No row is deleted, no detection value is altered, and no ownership is
         * touched.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN syncState TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN lastSyncError TEXT")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN lastSyncAttemptAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE sleep_sessions SET syncState = 'SYNCED' WHERE syncedAt > 0")
            }
        }

        fun getDatabase(context: Context): SleepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "sleep_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
