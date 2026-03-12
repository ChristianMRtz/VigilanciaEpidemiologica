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
    val createdAt: Long = System.currentTimeMillis()
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
    createdAt = createdAt
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
    createdAt = createdAt
)
