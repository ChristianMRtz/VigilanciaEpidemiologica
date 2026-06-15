package com.chrismr.vigilancia.data.local.dao

import androidx.room.*
import com.chrismr.vigilancia.data.local.entity.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Query("SELECT * FROM patients WHERE isDeleted = 0 ORDER BY nombreCompleto")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Long): Patient?

    @Query("SELECT * FROM patients WHERE dni = :dni AND isDeleted = 0 LIMIT 1")
    suspend fun getPatientByDni(dni: String): Patient?

    @Query("""
        SELECT * FROM patients
        WHERE isDeleted = 0 AND (
            dni           LIKE '%' || :query || '%'
            OR nombreCompleto LIKE '%' || :query || '%'
        )
        ORDER BY nombreCompleto
    """)
    fun searchPatients(query: String): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedPatients(): Flow<List<Patient>>

    /** Snapshot (suspend) para usarse en exportaciones Excel. */
    @Query("SELECT * FROM patients WHERE isDeleted = 0 ORDER BY nombreCompleto")
    suspend fun getAllPatientsSnapshot(): List<Patient>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)

    /** Borrado lógico: lo mueve a la papelera. */
    @Query("UPDATE patients SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :patientId")
    suspend fun moveToTrash(patientId: Long, deletedAt: Long = System.currentTimeMillis())

    /** Restaura un paciente de la papelera. */
    @Query("UPDATE patients SET isDeleted = 0, deletedAt = NULL WHERE id = :patientId")
    suspend fun restoreFromTrash(patientId: Long)

    /** Borrado físico: elimina permanentemente de la BD. */
    @Delete
    suspend fun deletePatientPermanently(patient: Patient)
}
