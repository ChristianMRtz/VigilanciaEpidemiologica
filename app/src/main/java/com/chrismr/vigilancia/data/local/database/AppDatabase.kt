package com.chrismr.vigilancia.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chrismr.vigilancia.data.local.dao.DailyMonitoringDao
import com.chrismr.vigilancia.data.local.dao.PatientDao
import com.chrismr.vigilancia.data.local.entity.DailyMonitoring
import com.chrismr.vigilancia.data.local.entity.Patient

@Database(
    entities = [Patient::class, DailyMonitoring::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun dailyMonitoringDao(): DailyMonitoringDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vigilancia_epidemiologica.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

