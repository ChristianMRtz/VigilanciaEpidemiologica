package com.chrismr.vigilancia.data.local.dao

import androidx.room.*
import com.chrismr.vigilancia.data.local.entity.DailyMonitoring
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMonitoringDao {

    @Query("""
        SELECT * FROM daily_monitorings
        WHERE patientId = :patientId AND year = :year AND month = :month
        ORDER BY day
    """)
    fun getMonitoringForPatientMonth(
        patientId: Long, year: Int, month: Int
    ): Flow<List<DailyMonitoring>>

    @Query("""
        SELECT * FROM daily_monitorings
        WHERE patientId = :patientId AND year = :year AND month = :month AND day = :day
        LIMIT 1
    """)
    suspend fun getMonitoringForDay(
        patientId: Long, year: Int, month: Int, day: Int
    ): DailyMonitoring?

    /** Todos los registros de un paciente en todos los meses. */
    @Query("SELECT * FROM daily_monitorings WHERE patientId = :patientId ORDER BY year, month, day")
    fun getAllForPatient(patientId: Long): Flow<List<DailyMonitoring>>

    /** Todos los registros de un mes como Flow (para filtros reactivos). */
    @Query("SELECT * FROM daily_monitorings WHERE year = :year AND month = :month")
    fun getAllMonitoringForMonthFlow(year: Int, month: Int): Flow<List<DailyMonitoring>>

    /** Todos los registros de un mes (para exportación Excel). */
    @Query("SELECT * FROM daily_monitorings WHERE year = :year AND month = :month")
    suspend fun getAllMonitoringForMonth(year: Int, month: Int): List<DailyMonitoring>

    /** Todos los registros de un año completo (para exportación anual). */
    @Query("SELECT * FROM daily_monitorings WHERE year = :year ORDER BY month, day")
    suspend fun getAllMonitoringForYear(year: Int): List<DailyMonitoring>

    /** INSERT o UPDATE: la clave de unicidad es (patientId, year, month, day). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(monitoring: DailyMonitoring): Long

    @Query("""
        DELETE FROM daily_monitorings
        WHERE patientId = :patientId AND year = :year AND month = :month AND day = :day
    """)
    suspend fun deleteMonitoringForDay(patientId: Long, year: Int, month: Int, day: Int)

    /** Borra todos los registros del paciente en el mes indicado. */
    @Query("""
        DELETE FROM daily_monitorings
        WHERE patientId = :patientId AND year = :year AND month = :month
    """)
    suspend fun deleteAllForPatientMonth(patientId: Long, year: Int, month: Int)
}

