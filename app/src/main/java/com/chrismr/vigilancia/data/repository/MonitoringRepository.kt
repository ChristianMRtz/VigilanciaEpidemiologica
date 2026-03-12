package com.chrismr.vigilancia.data.repository

import com.chrismr.vigilancia.data.local.dao.DailyMonitoringDao
import com.chrismr.vigilancia.domain.model.DailyMonitoringModel
import com.chrismr.vigilancia.domain.model.toEntity
import com.chrismr.vigilancia.domain.model.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MonitoringRepository(private val dao: DailyMonitoringDao) {

    fun getMonitoringForPatientMonth(
        patientId: Long, year: Int, month: Int
    ): Flow<List<DailyMonitoringModel>> =
        dao.getMonitoringForPatientMonth(patientId, year, month)
            .map { list -> list.map { it.toModel() } }

    suspend fun getMonitoringForDay(
        patientId: Long, year: Int, month: Int, day: Int
    ): DailyMonitoringModel? =
        dao.getMonitoringForDay(patientId, year, month, day)?.toModel()

    fun getAllForPatient(patientId: Long): Flow<List<DailyMonitoringModel>> =
        dao.getAllForPatient(patientId).map { list -> list.map { it.toModel() } }

    fun getAllMonitoringForMonthFlow(
        year: Int, month: Int
    ): Flow<List<DailyMonitoringModel>> =
        dao.getAllMonitoringForMonthFlow(year, month).map { list -> list.map { it.toModel() } }

    suspend fun getAllMonitoringForMonth(
        year: Int, month: Int
    ): List<DailyMonitoringModel> =
        dao.getAllMonitoringForMonth(year, month).map { it.toModel() }

    suspend fun insertOrUpdate(model: DailyMonitoringModel): Long =
        dao.insertOrUpdate(model.toEntity())

    suspend fun deleteMonitoringForDay(
        patientId: Long, year: Int, month: Int, day: Int
    ) = dao.deleteMonitoringForDay(patientId, year, month, day)

    suspend fun deleteAllForPatientMonth(patientId: Long, year: Int, month: Int) =
        dao.deleteAllForPatientMonth(patientId, year, month)
}

