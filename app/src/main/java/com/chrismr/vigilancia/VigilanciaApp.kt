package com.chrismr.vigilancia

import android.app.Application
import com.chrismr.vigilancia.data.local.database.AppDatabase
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
// import com.chrismr.vigilancia.util.DataSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application entry-point. Expone los repositorios sin Hilt para
 * que los ViewModelFactory los puedan obtener sin inyección.
 */
class VigilanciaApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val patientRepository by lazy { PatientRepository(database.patientDao()) }
    val monitoringRepository by lazy { MonitoringRepository(database.dailyMonitoringDao()) }

    override fun onCreate() {
        super.onCreate()
        // Poblar datos de prueba si la BD está vacía (desactivado para escenario real)
        // CoroutineScope(Dispatchers.IO).launch {
        //     DataSeeder.seedIfEmpty(patientRepository, monitoringRepository)
        // }
    }
}
