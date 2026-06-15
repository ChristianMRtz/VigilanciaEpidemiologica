package com.chrismr.vigilancia.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un paciente registrado en el sistema.
 * El DNI es único: la BD rechaza duplicados a nivel de constraint.
 */
@Entity(
    tableName = "patients",
    indices = [Index(value = ["dni"], unique = true)]
)
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombreCompleto: String,
    val edad: String,
    val sexo: String,
    val dni: String,
    val intervencionQuirurgica: String,
    val fechaNacimiento: String,
    val fechaIngreso: String,
    val diagnostico: String,
    val numeroCama: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
