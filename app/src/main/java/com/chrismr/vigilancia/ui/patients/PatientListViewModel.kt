package com.chrismr.vigilancia.ui.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.domain.model.PatientModel
import com.chrismr.vigilancia.util.DateUtils
import com.chrismr.vigilancia.util.BackupManager
import com.chrismr.vigilancia.util.ExcelExporter
import android.widget.Toast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PatientFilter(val label: String) {
    ACTIVOS("Activos"),
    CON_ALTA("Con Alta"),
    TODOS("Todos")
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatientListViewModel(
    private val repository: PatientRepository,
    private val monitoringRepository: MonitoringRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val filter = MutableStateFlow(PatientFilter.ACTIVOS)

    // IDs de pacientes que tienen Alta en el mes actual
    private val egresoPatientIds: StateFlow<Set<Long>> =
        monitoringRepository.getAllMonitoringForMonthFlow(
            DateUtils.getCurrentYear(),
            DateUtils.getCurrentMonth()
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
            .let { monitoringFlow ->
                combine(monitoringFlow) { (list) ->
                    list.filter { it.status == MonitoringStatus.EGRESO }
                        .map { it.patientId }
                        .toSet()
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
            }

    private val allPatients: StateFlow<List<PatientModel>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllPatients()
            else repository.searchPatients(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val patients: StateFlow<List<PatientModel>> =
        combine(allPatients, filter, egresoPatientIds) { list, f, egreso ->
            when (f) {
                PatientFilter.ACTIVOS  -> list.filter { it.id !in egreso }
                PatientFilter.CON_ALTA -> list.filter { it.id in egreso }
                PatientFilter.TODOS    -> list
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearch(query: String) { searchQuery.value = query }
    fun setFilter(f: PatientFilter) { filter.value = f }

    fun deletePatient(patient: PatientModel) {
        viewModelScope.launch { repository.deletePatient(patient) }
    }

    fun exportConsolidado(context: Context, year: Int, month: Int) {
        viewModelScope.launch {
            val patients = repository.getAllPatientsSnapshot()
            val monitorings = monitoringRepository.getAllMonitoringForMonth(year, month)
            ExcelExporter.export(context, patients, monitorings, year, month)
        }
    }

    /** Exporta un año completo (una hoja por mes). Si es el año actual, solo hasta el mes actual. */
    fun exportAnual(context: Context, year: Int) {
        viewModelScope.launch {
            val patients  = repository.getAllPatientsSnapshot()
            val nowYear   = DateUtils.getCurrentYear()
            val nowMonth  = DateUtils.getCurrentMonth()
            val months    = if (year == nowYear) (1..nowMonth).toList() else (1..12).toList()
            val allMonitorings = monitoringRepository.getAllMonitoringForYear(year)
            val byMonth   = allMonitorings.groupBy { it.month }
            ExcelExporter.exportYear(context, patients, byMonth, year, months)
        }
    }

    /** Guarda un backup ZIP de la BD en la carpeta Descargas del dispositivo. */
    fun backupToDevice(context: Context) {
        viewModelScope.launch {
            val result = BackupManager.backupToDevice(context.applicationContext)
            result.fold(
                onSuccess = { (_, fileName) ->
                    Toast.makeText(
                        context,
                        "✓ Backup guardado en Descargas: $fileName",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Error al crear backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    /** Hace backup y abre el selector nativo para compartir (correo, Drive, WhatsApp…). */
    fun backupAndShare(context: Context) {
        viewModelScope.launch {
            BackupManager.backupAndShare(context.applicationContext)
        }
    }

    companion object {
        fun factory(repository: PatientRepository, monitoringRepository: MonitoringRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PatientListViewModel(repository, monitoringRepository) as T
            }
    }
}
