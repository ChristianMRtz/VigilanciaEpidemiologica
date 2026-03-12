package com.chrismr.vigilancia.domain.model

import com.chrismr.vigilancia.data.local.entity.DailyMonitoring
import com.chrismr.vigilancia.domain.enums.MonitoringStatus

data class DailyMonitoringModel(
    val id: Long = 0,
    val patientId: Long = 0,
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val status: MonitoringStatus = MonitoringStatus.VACIO,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ── Mappers ──────────────────────────────────────────────────────────────────

fun DailyMonitoring.toModel() = DailyMonitoringModel(
    id = id,
    patientId = patientId,
    year = year,
    month = month,
    day = day,
    status = runCatching { MonitoringStatus.valueOf(status) }.getOrDefault(MonitoringStatus.VACIO),
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DailyMonitoringModel.toEntity() = DailyMonitoring(
    id = id,
    patientId = patientId,
    year = year,
    month = month,
    day = day,
    status = status.name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

