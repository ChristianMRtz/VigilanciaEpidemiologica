package com.chrismr.vigilancia.domain.model

import com.chrismr.vigilancia.data.local.entity.Patient

data class PatientModel(
    val id: Long = 0,
    val nombreCompleto: String = "",
    val edad: String = "",
    val sexo: String = "",
    val dni: String = "",
    val intervencionQuirurgica: String = "",
    val fechaNacimiento: String = "",
    val fechaIngreso: String = "",
    val diagnostico: String = "",
    val numeroCama: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

// ── Mappers ──────────────────────────────────────────────────────────────────

fun Patient.toModel() = PatientModel(
    id = id,
    nombreCompleto = nombreCompleto,
    edad = edad,
    sexo = sexo,
    dni = dni,
    intervencionQuirurgica = intervencionQuirurgica,
    fechaNacimiento = fechaNacimiento,
    fechaIngreso = fechaIngreso,
    diagnostico = diagnostico,
    numeroCama = numeroCama,
    createdAt = createdAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun PatientModel.toEntity() = Patient(
    id = id,
    nombreCompleto = nombreCompleto,
    edad = edad,
    sexo = sexo,
    dni = dni,
    intervencionQuirurgica = intervencionQuirurgica,
    fechaNacimiento = fechaNacimiento,
    fechaIngreso = fechaIngreso,
    diagnostico = diagnostico,
    numeroCama = numeroCama,
    createdAt = createdAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)
