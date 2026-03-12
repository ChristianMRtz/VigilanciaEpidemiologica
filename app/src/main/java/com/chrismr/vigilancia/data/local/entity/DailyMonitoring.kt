package com.chrismr.vigilancia.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room para el seguimiento diario de un paciente.
 * Relación 1-a-muchos con [Patient] (CASCADE on delete).
 */
@Entity(
    tableName = "daily_monitorings",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId")]
)
data class DailyMonitoring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val year: Int,
    val month: Int,
    val day: Int,
    /** Almacena el nombre del enum [com.chrismr.vigilancia.domain.enums.MonitoringStatus] */
    val status: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

