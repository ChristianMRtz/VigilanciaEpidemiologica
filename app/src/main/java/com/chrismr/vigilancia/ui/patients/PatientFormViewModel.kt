package com.chrismr.vigilancia.ui.patients

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.model.DailyMonitoringModel
import com.chrismr.vigilancia.domain.model.PatientModel
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import kotlinx.coroutines.launch

sealed class FormResult {
    object Idle : FormResult()
    object Success : FormResult()
    data class Error(val message: String) : FormResult()
}

class PatientFormViewModel(
    private val repository: PatientRepository,
    private val monitoringRepository: MonitoringRepository,
    private val patientId: Long?
) : ViewModel() {

    var nombreCompleto by mutableStateOf("")
    var edadNumero by mutableStateOf("")        // parte numérica: "10"
    var edadUnidad by mutableStateOf("a")       // sufijo: "a", "m", "d"
    var sexo by mutableStateOf("Masculino")
    private var _dni by mutableStateOf("")
    var dni: String
        get() = _dni
        set(value) {
            _dni = value
            dniError = when {
                value.isBlank() -> ""
                value.length != 8 -> "El DNI debe tener mínimo 8 dígitos"
                else -> ""
            }
        }
    var dniError by mutableStateOf("")
    var intervencionQuirurgica by mutableStateOf("")
    var fechaNacimiento by mutableStateOf("")
    var fechaIngreso by mutableStateOf("")
    var diagnostico by mutableStateOf("")
    var numeroCama by mutableStateOf("")
    var result by mutableStateOf<FormResult>(FormResult.Idle)
    var isLoading by mutableStateOf(false)

    init {
        if (patientId != null) {
            viewModelScope.launch {
                isLoading = true
                repository.getPatientById(patientId)?.let { p ->
                    nombreCompleto = p.nombreCompleto
                    // Parsear "10a" → edadNumero="10", edadUnidad="a"
                    val raw = p.edad
                    if (raw.isNotBlank() && raw.last() in listOf('a', 'm', 'd')) {
                        edadNumero = raw.dropLast(1)
                        edadUnidad = raw.last().toString()
                    } else {
                        edadNumero = raw
                        edadUnidad = "a"
                    }
                    sexo = p.sexo
                    dni = p.dni
                    intervencionQuirurgica = p.intervencionQuirurgica
                    fechaNacimiento = p.fechaNacimiento
                    fechaIngreso = p.fechaIngreso
                    diagnostico = p.diagnostico
                    numeroCama = p.numeroCama
                }
                isLoading = false
            }
        }
    }

    fun save() {
        if (nombreCompleto.isBlank() || dni.isBlank()) {
            result = FormResult.Error("Nombre completo y DNI son obligatorios.")
            return
        }
        if (dni.length != 8) {
            dniError = "El DNI debe tener exactamente 8 dígitos"
            result = FormResult.Error("El DNI debe tener exactamente 8 dígitos.")
            return
        }
        // Combinar: "10" + "a" = "10a"
        val edadStr = if (edadNumero.isNotBlank()) "${edadNumero.trim()}${edadUnidad}" else ""
        viewModelScope.launch {
            try {
                // ── Validar DNI único ────────────────────────────────────
                val existing = repository.getPatientByDni(dni.trim())
                if (existing != null && existing.id != patientId) {
                    dniError = "Este DNI ya está registrado"
                    result = FormResult.Error("El DNI ${dni.trim()} ya pertenece a ${existing.nombreCompleto}.")
                    return@launch
                }

                if (patientId == null) {
                    val newId = repository.insertPatient(
                        PatientModel(
                            nombreCompleto = nombreCompleto.trim(),
                            edad = edadStr,
                            sexo = sexo,
                            dni = dni.trim(),
                            intervencionQuirurgica = intervencionQuirurgica.trim(),
                            fechaNacimiento = fechaNacimiento.trim(),
                            fechaIngreso = fechaIngreso.trim(),
                            diagnostico = diagnostico.trim(),
                            numeroCama = numeroCama.trim()
                        )
                    )
                    // Registrar INICIO automático en el día de fechaIngreso
                    parseFechaIngreso(fechaIngreso.trim())?.let { (day, month, year) ->
                        monitoringRepository.insertOrUpdate(
                            DailyMonitoringModel(
                                patientId = newId,
                                year      = year,
                                month     = month,
                                day       = day,
                                status    = MonitoringStatus.INICIO
                            )
                        )
                    }
                } else {
                    val existing = repository.getPatientById(patientId) ?: return@launch
                    repository.updatePatient(
                        existing.copy(
                            nombreCompleto = nombreCompleto.trim(),
                            edad = edadStr,
                            sexo = sexo,
                            dni = dni.trim(),
                            intervencionQuirurgica = intervencionQuirurgica.trim(),
                            fechaNacimiento = fechaNacimiento.trim(),
                            fechaIngreso = fechaIngreso.trim(),
                            diagnostico = diagnostico.trim(),
                            numeroCama = numeroCama.trim()
                        )
                    )
                }
                result = FormResult.Success
            } catch (e: Exception) {
                result = FormResult.Error(e.message ?: "Error al guardar el paciente.")
            }
        }
    }

    /** Parsea "DD/MM/YYYY" → Triple(día, mes, año). Devuelve null si el formato es inválido. */
    private fun parseFechaIngreso(fecha: String): Triple<Int, Int, Int>? = runCatching {
        val p = fecha.split("/")
        Triple(p[0].toInt(), p[1].toInt(), p[2].toInt())
    }.getOrNull()

    companion object {
        fun factory(
            repository: PatientRepository,
            monitoringRepository: MonitoringRepository,
            patientId: Long?
        ) = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PatientFormViewModel(repository, monitoringRepository, patientId) as T
            }
    }
}
