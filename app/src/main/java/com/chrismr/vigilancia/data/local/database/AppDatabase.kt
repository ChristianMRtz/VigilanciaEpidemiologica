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
    version = 7,
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

        /** Migración 6→7: Ajusta la estructura del backup antiguo a la actual. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // El backup antiguo no tiene las columnas isDeleted y deletedAt.
                db.execSQL("ALTER TABLE patients ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE patients ADD COLUMN deletedAt INTEGER DEFAULT NULL")

                // El backup tiene un DEFAULT '''' (con comillas extras) en numeroCama que causa error en Room.
                // Re-creamos la tabla para normalizarla al esquema esperado por Room.
                
                // 1. Crear tabla temporal con el esquema EXACTO que espera Room
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `patients_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `nombreCompleto` TEXT NOT NULL, 
                        `edad` TEXT NOT NULL, 
                        `sexo` TEXT NOT NULL, 
                        `dni` TEXT NOT NULL, 
                        `intervencionQuirurgica` TEXT NOT NULL, 
                        `fechaNacimiento` TEXT NOT NULL, 
                        `fechaIngreso` TEXT NOT NULL, 
                        `diagnostico` TEXT NOT NULL, 
                        `numeroCama` TEXT NOT NULL DEFAULT '', 
                        `createdAt` INTEGER NOT NULL, 
                        `isDeleted` INTEGER NOT NULL DEFAULT 0, 
                        `deletedAt` INTEGER
                    )
                """.trimIndent())

                // 2. Copiar datos de la tabla antigua a la nueva
                db.execSQL("""
                    INSERT INTO patients_new (id, nombreCompleto, edad, sexo, dni, intervencionQuirurgica, fechaNacimiento, fechaIngreso, diagnostico, numeroCama, createdAt, isDeleted, deletedAt)
                    SELECT id, nombreCompleto, edad, sexo, dni, intervencionQuirurgica, fechaNacimiento, fechaIngreso, diagnostico, numeroCama, createdAt, isDeleted, deletedAt FROM patients
                """.trimIndent())

                // 3. Eliminar la vieja y renombrar
                db.execSQL("DROP TABLE patients")
                db.execSQL("ALTER TABLE patients_new RENAME TO patients")
                
                // 4. Re-crear el índice único del DNI
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_patients_dni` ON `patients` (`dni`)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vigilancia_epidemiologica.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
    }
}

