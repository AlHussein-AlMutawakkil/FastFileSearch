package com.example.fastfilesearch.licenseadmin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PendingUploadEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LicenseAdminDatabase : RoomDatabase() {

    abstract fun pendingUploadDao(): PendingUploadDao

    companion object {
        private const val DATABASE_NAME = "license_admin_db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_uploads` (
                        `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `issued_at` TEXT NOT NULL,
                        `issuer_id` TEXT NOT NULL,
                        `issuer_device` TEXT NOT NULL,
                        `customer_name` TEXT,
                        `installation_id` TEXT NOT NULL,
                        `client_device_id` TEXT,
                        `client_manufacturer` TEXT,
                        `client_model` TEXT,
                        `client_os` TEXT,
                        `client_abi` TEXT,
                        `client_app_version` TEXT,
                        `plan_type` TEXT NOT NULL,
                        `duration_days` INTEGER NOT NULL,
                        `expires_at` TEXT,
                        `max_folders` INTEGER NOT NULL,
                        `allow_excel` INTEGER NOT NULL,
                        `allow_app_lock` INTEGER NOT NULL,
                        `license_code` TEXT NOT NULL,
                        `attempts` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: LicenseAdminDatabase? = null

        fun getInstance(context: Context): LicenseAdminDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LicenseAdminDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
