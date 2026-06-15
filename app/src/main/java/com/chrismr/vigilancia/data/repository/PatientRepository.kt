package com.chrismr.vigilancia.data.repository

import com.chrismr.vigilancia.data.local.dao.PatientDao
import com.chrismr.vigilancia.domain.model.PatientModel
import com.chrismr.vigilancia.domain.model.toEntity
import com.chrismr.vigilancia.domain.model.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PatientRepository(private val dao: PatientDao) {

    fun getAllPatients(): Flow<List<PatientModel>> =
        dao.getAllPatients().map { list -> list.map { it.toModel() } }

    fun searchPatients(query: String): Flow<List<PatientModel>> =
        dao.searchPatients(query).map { list -> list.map { it.toModel() } }

    fun getDeletedPatients(): Flow<List<PatientModel>> =
        dao.getDeletedPatients().map { list -> list.map { it.toModel() } }

    suspend fun getPatientById(id: Long): PatientModel? =
        dao.getPatientById(id)?.toModel()

    suspend fun getPatientByDni(dni: String): PatientModel? =
        dao.getPatientByDni(dni)?.toModel()

    suspend fun getAllPatientsSnapshot(): List<PatientModel> =
        dao.getAllPatientsSnapshot().map { it.toModel() }

    suspend fun insertPatient(patient: PatientModel): Long =
        dao.insertPatient(patient.toEntity())

    suspend fun updatePatient(patient: PatientModel) =
        dao.updatePatient(patient.toEntity())

    /** Mueve el paciente a la papelera (borrado lógico). */
    suspend fun deletePatient(patient: PatientModel) =
        dao.moveToTrash(patient.id)

    /** Restaura al paciente de la papelera. */
    suspend fun restorePatient(id: Long) =
        dao.restoreFromTrash(id)

    /** Elimina permanentemente al paciente de la BD. */
    suspend fun deletePatientPermanently(patient: PatientModel) =
        dao.deletePatientPermanently(patient.toEntity())
}
