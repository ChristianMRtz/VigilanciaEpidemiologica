package com.chrismr.vigilancia.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chrismr.vigilancia.data.local.dao.DailyMonitoringDao
import com.chrismr.vigilancia.data.local.dao.PatientDao
import com.chrismr.vigilancia.data.local.entity.DailyMonitoring
import com.chrismr.vigilancia.data.local.entity.Patient

@Database(
    entities = [Patient::class, DailyMonitoring::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun dailyMonitoringDao(): DailyMonitoringDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Migración 5→6: añade la columna numeroCama sin borrar datos existentes. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE patients ADD COLUMN numeroCama TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vigilancia_epidemiologica.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
    }
}

