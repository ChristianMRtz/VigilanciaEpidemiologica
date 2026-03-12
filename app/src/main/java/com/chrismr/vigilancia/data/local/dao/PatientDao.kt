package com.chrismr.vigilancia.data.local.dao

import androidx.room.*
import com.chrismr.vigilancia.data.local.entity.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Query("SELECT * FROM patients ORDER BY nombreCompleto")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Long): Patient?

    @Query("SELECT * FROM patients WHERE dni = :dni LIMIT 1")
    suspend fun getPatientByDni(dni: String): Patient?

    @Query("""
        SELECT * FROM patients
        WHERE dni           LIKE '%' || :query || '%'
           OR nombreCompleto LIKE '%' || :query || '%'
        ORDER BY nombreCompleto
    """)
    fun searchPatients(query: String): Flow<List<Patient>>

    /** Snapshot (suspend) para usarse en exportaciones Excel. */
    @Query("SELECT * FROM patients ORDER BY nombreCompleto")
    suspend fun getAllPatientsSnapshot(): List<Patient>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)
}
