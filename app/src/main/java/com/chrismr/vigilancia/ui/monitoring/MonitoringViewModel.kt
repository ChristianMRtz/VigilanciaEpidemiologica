package com.chrismr.vigilancia.ui.monitoring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.domain.model.DailyMonitoringModel
import com.chrismr.vigilancia.domain.model.PatientModel
import com.chrismr.vigilancia.util.DateUtils
import com.chrismr.vigilancia.util.ExcelExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringViewModel(
    private val patientId: Long,
    private val patientRepository: PatientRepository,
    private val monitoringRepository: MonitoringRepository
) : ViewModel() {

    private val _year = MutableStateFlow(DateUtils.getCurrentYear())
    private val _month = MutableStateFlow(DateUtils.getCurrentMonth())
    val year: StateFlow<Int> = _year.asStateFlow()
    val month: StateFlow<Int> = _month.asStateFlow()

    init {
        // Al abrir, intentar ir al mes con la actividad más reciente, o a la fecha de ingreso
        viewModelScope.launch {
            val history = monitoringRepository.getAllForPatient(patientId).first()
            if (history.isNotEmpty()) {
                // Buscar el registro más reciente cronológicamente
                val latest = history.maxWith(
                    compareBy<DailyMonitoringModel> { it.year }
                        .thenBy { it.month }
                        .thenBy { it.day }
                )
                _year.value = latest.year
                _month.value = latest.month
            } else {
                // Si no hay registros, usar fecha de ingreso
                patientRepository.getPatientById(patientId)?.fechaIngreso?.let { parseYearMonth(it) }?.let { (y, m) ->
                    _year.value = y
                    _month.value = m
                }
            }
        }
    }

    val patient: StateFlow<PatientModel?> = flow {
        emit(patientRepository.getPatientById(patientId))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val monitoringMap: StateFlow<Map<Int, DailyMonitoringModel>> =
        combine(_year, _month) { y, m -> y to m }
            .flatMapLatest { (y, m) ->
                monitoringRepository
                    .getMonitoringForPatientMonth(patientId, y, m)
                    .map { list -> list.associateBy { it.day } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val daysInMonth: StateFlow<List<Int>> =
        combine(_year, _month) { y, m -> DateUtils.getDaysList(y, m) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val firstWeekdayOffset: StateFlow<Int> =
        combine(_year, _month) { y, m -> DateUtils.getFirstWeekdayOffset(y, m) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Historial completo del paciente en todos los meses. */
    private val allPatientMonitoring: StateFlow<List<DailyMonitoringModel>> =
        monitoringRepository.getAllForPatient(patientId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True si el paciente ya tiene un INICIO registrado en cualquier mes. */
    val hasInicioRecord: StateFlow<Boolean> = allPatientMonitoring
        .map { list -> list.any { it.status == MonitoringStatus.INICIO } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Registro de Alta (EGRESO) del paciente, si existe en cualquier mes. */
    val egresoRecord: StateFlow<DailyMonitoringModel?> = allPatientMonitoring
        .map { list -> list.firstOrNull { it.status == MonitoringStatus.EGRESO } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True si el mes que se está viendo es POSTERIOR al mes en que se dio el Alta. */
    val isEntireMonthLocked: StateFlow<Boolean> =
        combine(_year, _month, egresoRecord) { y, m, egreso ->
            if (egreso == null) false
            else (egreso.year < y) || (egreso.year == y && egreso.month < m)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Mensaje de error para mostrar en la UI (null = sin error). */
    val statusError = MutableStateFlow<String?>(null)
    fun clearError() { statusError.value = null }

    /** Días seleccionados en modo selección múltiple. */
    val selectedDays = MutableStateFlow<Set<Int>>(emptySet())
    val isSelectionMode: StateFlow<Boolean> = selectedDays
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleDaySelection(day: Int) {
        selectedDays.value = selectedDays.value.toMutableSet().apply {
            if (contains(day)) remove(day) else add(day)
        }
    }

    fun clearSelection() { selectedDays.value = emptySet() }

    /**
     * Aplica [status] (solo CONTINUA, SIN_DISPOSITIVO o VACIO) a todos los días seleccionados.
     * Valida las restricciones antes de aplicar.
     */
    fun applyStatusToSelected(status: MonitoringStatus) {
        viewModelScope.launch {
            val y = _year.value
            val m = _month.value
            val currentMap = monitoringMap.value
            val allHistory = allPatientMonitoring.value
            val days = selectedDays.value.sorted()

            if (days.isEmpty()) return@launch

            if (status == MonitoringStatus.VACIO) {
                days.forEach { monitoringRepository.deleteMonitoringForDay(patientId, y, m, it) }
                clearSelection()
                return@launch
            }

            val retiroDay = (allHistory + currentMap.values)
                .firstOrNull { it.status == MonitoringStatus.RETIRO }?.day

            if (status == MonitoringStatus.CONTINUA) {
                val hasInicio = allHistory.any { it.status == MonitoringStatus.INICIO } ||
                    currentMap.values.any { it.status == MonitoringStatus.INICIO }
                if (!hasInicio) {
                    statusError.value = "No puede colocar Continúa sin un Inicio previo."
                    return@launch
                }
                val afterRetiro = if (retiroDay != null) days.filter { it > retiroDay } else emptyList()
                if (afterRetiro.isNotEmpty()) {
                    statusError.value = "Los días ${afterRetiro.joinToString()} están después del Retiro. No se puede colocar Continúa."
                    return@launch
                }
            }

            if (status == MonitoringStatus.SIN_DISPOSITIVO) {
                if (retiroDay == null) {
                    statusError.value = "No puede colocar '-' sin un Retiro previo."
                    return@launch
                }
                val beforeRetiro = days.filter { it < retiroDay }
                if (beforeRetiro.isNotEmpty()) {
                    statusError.value = "Los días ${beforeRetiro.joinToString()} están antes del Retiro. No se puede colocar '-'."
                    return@launch
                }
            }

            // Aplicar a todos los días seleccionados
            days.forEach { d ->
                val existing = monitoringRepository.getMonitoringForDay(patientId, y, m, d)
                monitoringRepository.insertOrUpdate(
                    existing?.copy(status = status, updatedAt = System.currentTimeMillis())
                        ?: DailyMonitoringModel(patientId = patientId, year = y, month = m, day = d, status = status)
                )
            }
            clearSelection()
        }
    }

    fun previousMonth() {
        if (_month.value == 1) { _month.value = 12; _year.value-- }
        else _month.value--
    }

    fun nextMonth() {
        if (_month.value == 12) { _month.value = 1; _year.value++ }
        else _month.value++
    }

    fun setStatus(day: Int, status: MonitoringStatus) {
        viewModelScope.launch {
            val y = _year.value
            val m = _month.value
            val currentMap = monitoringMap.value
            val allHistory = allPatientMonitoring.value

            // Mes completo bloqueado por Alta en mes anterior
            if (isEntireMonthLocked.value) {
                val r = egresoRecord.value
                statusError.value = "Alta dada el ${r?.day}/${r?.month}/${r?.year}. No se puede modificar."
                return@launch
            }

            if (status == MonitoringStatus.VACIO) {
                monitoringRepository.deleteMonitoringForDay(patientId, y, m, day)
                return@launch
            }

            // Días posteriores al Alta dentro del mismo mes
            val egresoInMonth = currentMap.values.firstOrNull { it.status == MonitoringStatus.EGRESO }
            if (egresoInMonth != null && day > egresoInMonth.day) {
                statusError.value = "No se puede modificar un día posterior al Alta."
                return@launch
            }

            // Helper: estado existente fuera del día actual (cross-month)
            fun existsElsewhere(s: MonitoringStatus) = allHistory.firstOrNull {
                it.status == s && !(it.year == y && it.month == m && it.day == day)
            }

            // ── Validaciones para CONTINUA ────────────────────────────────
            if (status == MonitoringStatus.CONTINUA) {
                val hasInicio = allHistory.any { it.status == MonitoringStatus.INICIO } ||
                    currentMap.values.any { it.status == MonitoringStatus.INICIO }
                if (!hasInicio) {
                    statusError.value = "No puede colocar Continúa sin un Inicio previo."
                    return@launch
                }
            }

            // ── Validaciones para SIN_DISPOSITIVO ────────────────────────
            if (status == MonitoringStatus.SIN_DISPOSITIVO) {
                val hasRetiro = allHistory.any { it.status == MonitoringStatus.RETIRO } ||
                    currentMap.values.any { it.status == MonitoringStatus.RETIRO }
                if (!hasRetiro) {
                    statusError.value = "No puede colocar '-' sin un Retiro previo."
                    return@launch
                }
            }

            // ── Validaciones específicas para INICIO ──────────────────────
            if (status == MonitoringStatus.INICIO) {
                // No puede haber INICIO si ya existe Alta
                if (egresoRecord.value != null &&
                    !(egresoRecord.value!!.year == y && egresoRecord.value!!.month == m && egresoRecord.value!!.day == day)
                ) {
                    statusError.value = "El paciente ya tiene Alta. No se puede agregar un nuevo Inicio."
                    return@launch
                }
                // No puede ponerse INICIO después del RETIRO en el mismo mes
                val retiroInMonth = currentMap.values.firstOrNull { it.status == MonitoringStatus.RETIRO }
                if (retiroInMonth != null && day >= retiroInMonth.day) {
                    statusError.value = "No puede colocar Inicio después del Retiro. Elimine primero el Retiro."
                    return@launch
                }
                // Buscar INICIO existente para distinguir "mover" de "crear nuevo"
                val existingInicio = existsElsewhere(MonitoringStatus.INICIO)
                val isMovingInicio = existingInicio != null  // true si ya existe INICIO en cualquier mes

                // Solo bloquear por RETIRO si estamos creando un INICIO totalmente nuevo
                if (!isMovingInicio) {
                    existsElsewhere(MonitoringStatus.RETIRO)?.let {
                        statusError.value = "Ya hay un Retiro registrado. No se puede agregar un nuevo Inicio."
                        return@launch
                    }
                }
                // Unicidad de INICIO
                existingInicio?.let { old ->
                    if (old.year == y && old.month == m) {
                        if (old.day > day) {
                            // Mueve a día anterior (mismo mes): viejo pasa a CONTINUA, llenar intermedios
                            monitoringRepository.insertOrUpdate(
                                old.copy(status = MonitoringStatus.CONTINUA, updatedAt = System.currentTimeMillis())
                            )
                            for (d in (day + 1) until old.day) {
                                if (!currentMap.containsKey(d)) {
                                    monitoringRepository.insertOrUpdate(
                                        DailyMonitoringModel(patientId = patientId, year = y, month = m, day = d, status = MonitoringStatus.CONTINUA)
                                    )
                                }
                            }
                        } else {
                            // Mueve a día posterior (mismo mes): eliminar el viejo y días anteriores al nuevo
                            for (d in old.day until day) {
                                monitoringRepository.deleteMonitoringForDay(patientId, y, m, d)
                            }
                        }
                    } else {
                        // Mueve a otro mes: el viejo INICIO pasa a CONTINUA (sigue siendo un día con dispositivo)
                        monitoringRepository.insertOrUpdate(
                            old.copy(status = MonitoringStatus.CONTINUA, updatedAt = System.currentTimeMillis())
                        )
                    }
                }
            }

            // ── Validaciones para RETIRO ──────────────────────────────────
            if (status == MonitoringStatus.RETIRO) {
                val hasInicio = allHistory.any { it.status == MonitoringStatus.INICIO } ||
                        currentMap.values.any { it.status == MonitoringStatus.INICIO }
                if (!hasInicio) {
                    statusError.value = "No puede colocar Retiro sin un Inicio previo."
                    return@launch
                }

                existsElsewhere(MonitoringStatus.RETIRO)?.let { old ->
                    if (old.year == y && old.month == m) {
                        if (old.day > day) {
                            // Retiro se mueve a día anterior:
                            // el viejo y los días intermedios pasan a SIN_DISPOSITIVO
                            // (ahora están después del nuevo RETIRO)
                            monitoringRepository.insertOrUpdate(
                                old.copy(status = MonitoringStatus.SIN_DISPOSITIVO, updatedAt = System.currentTimeMillis())
                            )
                            for (d in (day + 1) until old.day) {
                                monitoringRepository.insertOrUpdate(
                                    currentMap[d]?.copy(status = MonitoringStatus.SIN_DISPOSITIVO, updatedAt = System.currentTimeMillis())
                                        ?: DailyMonitoringModel(patientId = patientId, year = y, month = m, day = d, status = MonitoringStatus.SIN_DISPOSITIVO)
                                )
                            }
                        } else if (old.day < day) {
                            // Retiro se mueve a día posterior:
                            // el viejo pasa a CONTINUA, los días SIN_DISPOSITIVO entre ambos también
                            monitoringRepository.insertOrUpdate(
                                old.copy(status = MonitoringStatus.CONTINUA, updatedAt = System.currentTimeMillis())
                            )
                            for (d in (old.day + 1) until day) {
                                currentMap[d]?.let { entry ->
                                    if (entry.status == MonitoringStatus.SIN_DISPOSITIVO) {
                                        monitoringRepository.insertOrUpdate(
                                            entry.copy(status = MonitoringStatus.CONTINUA, updatedAt = System.currentTimeMillis())
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        statusError.value = "Ya existe un Retiro en otro mes."
                        return@launch
                    }
                }
            }

            // ── Validaciones para EGRESO (Alta) ───────────────────────────
            if (status == MonitoringStatus.EGRESO) {
                val hasInicio = allHistory.any { it.status == MonitoringStatus.INICIO } ||
                        currentMap.values.any { it.status == MonitoringStatus.INICIO }
                if (!hasInicio) {
                    statusError.value = "No puede dar el Alta sin un Inicio previo."
                    return@launch
                }

                existsElsewhere(MonitoringStatus.EGRESO)?.let { old ->
                    if (old.year == y && old.month == m) {
                        monitoringRepository.deleteMonitoringForDay(patientId, y, m, old.day)
                        if (old.day > day) {
                            for (d in (day + 1) until old.day)
                                monitoringRepository.deleteMonitoringForDay(patientId, y, m, d)
                        }
                    } else {
                        statusError.value = "Ya existe un Alta en otro mes."
                        return@launch
                    }
                }
            }

            // ── Auto-relleno mejorado (Cross-month) ──────────────────────
            if (status != MonitoringStatus.INICIO) {
                // 1. Buscar último registro cronológico antes del día actual
                val allSorted = (allHistory + currentMap.values)
                    .filter { it.year < y || (it.year == y && it.month < m) || (it.year == y && it.month == m && it.day < day) }
                    .sortedWith(compareBy({ it.year }, { it.month }, { it.day }))

                val lastEntry = allSorted.lastOrNull()

                if (lastEntry != null && lastEntry.status != MonitoringStatus.EGRESO) {
                    // Encontrar si hay un RETIRO previo (global)
                    val currentRetiro = (allHistory + currentMap.values)
                        .firstOrNull { it.status == MonitoringStatus.RETIRO }

                    // Si estamos poniendo RETIRO ahora, ese es nuestro punto de referencia
                    val effectiveRetiro = if (status == MonitoringStatus.RETIRO) {
                        DailyMonitoringModel(patientId = patientId, year = y, month = m, day = day, status = MonitoringStatus.RETIRO)
                    } else currentRetiro

                    val curY = lastEntry.year
                    val curM = lastEntry.month
                    val curD = lastEntry.day

                    // Función para avanzar un día
                    fun Triple<Int, Int, Int>.next(): Triple<Int, Int, Int> {
                        val (cy, cm, cd) = this
                        val mDays = DateUtils.getDaysInMonth(cy, cm)
                        return if (cd < mDays) Triple(cy, cm, cd + 1)
                        else if (cm < 12) Triple(cy, cm + 1, 1)
                        else Triple(cy + 1, 1, 1)
                    }

                    var nextDate = Triple(curY, curM, curD).next()

                    // Rellenar huecos hasta el día actual
                    while (nextDate.first < y || (nextDate.first == y && nextDate.second < m) || (nextDate.first == y && nextDate.second == m && nextDate.third < day)) {
                        val (ny, nm, nd) = nextDate
                        val exists = if (ny == y && nm == m) currentMap.containsKey(nd)
                        else allHistory.any { it.year == ny && it.month == nm && it.day == nd }

                        if (!exists) {
                            val fillStatus = when {
                                effectiveRetiro == null -> MonitoringStatus.CONTINUA
                                ny < effectiveRetiro.year -> MonitoringStatus.CONTINUA
                                ny > effectiveRetiro.year -> MonitoringStatus.SIN_DISPOSITIVO
                                nm < effectiveRetiro.month -> MonitoringStatus.CONTINUA
                                nm > effectiveRetiro.month -> MonitoringStatus.SIN_DISPOSITIVO
                                else -> if (nd > effectiveRetiro.day) MonitoringStatus.SIN_DISPOSITIVO else MonitoringStatus.CONTINUA
                            }

                            monitoringRepository.insertOrUpdate(
                                DailyMonitoringModel(patientId = patientId, year = ny, month = nm, day = nd, status = fillStatus)
                            )
                        }
                        nextDate = nextDate.next()
                    }
                }
            }

            // ── Guardar estado elegido ────────────────────────────────────
            val existing = monitoringRepository.getMonitoringForDay(patientId, y, m, day)
            monitoringRepository.insertOrUpdate(
                existing?.copy(status = status, updatedAt = System.currentTimeMillis())
                    ?: DailyMonitoringModel(patientId = patientId, year = y, month = m, day = day, status = status)
            )

            // ── Si es INICIO, sincronizar fechaIngreso del paciente ───────
            if (status == MonitoringStatus.INICIO) {
                patient.value?.let { p ->
                    val nuevaFecha = "%02d/%02d/%04d".format(day, m, y)
                    patientRepository.updatePatient(p.copy(fechaIngreso = nuevaFecha))
                }
            }
        }
    }

    /** Borra todos los registros de vigilancia del mes visible para este paciente. */
    fun clearAllMonth() {
        viewModelScope.launch {
            monitoringRepository.deleteAllForPatientMonth(patientId, _year.value, _month.value)
        }
    }

    fun exportToExcel(context: Context, year: Int = _year.value, month: Int = _month.value) {
        viewModelScope.launch {
            val patients = patientRepository.getAllPatientsSnapshot()
            val monitorings = monitoringRepository.getAllMonitoringForMonth(year, month)
            ExcelExporter.export(context, patients, monitorings, year, month)
        }
    }

    /**
     * Parsea una fecha en formato "DD/MM/YYYY" y devuelve (año, mes).
     * Devuelve null si el formato es inválido.
     */
    private fun parseYearMonth(fecha: String): Pair<Int, Int>? = runCatching {
        val parts = fecha.split("/")
        parts[2].toInt() to parts[1].toInt()   // año to mes
    }.getOrNull()

    companion object {
        fun factory(patientId: Long, patientRepository: PatientRepository, monitoringRepository: MonitoringRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MonitoringViewModel(patientId, patientRepository, monitoringRepository) as T
            }
    }
}
