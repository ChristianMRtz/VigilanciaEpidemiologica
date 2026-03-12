package com.chrismr.vigilancia.util

import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.domain.model.DailyMonitoringModel
import com.chrismr.vigilancia.domain.model.PatientModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DataSeeder {

    /**
     * Inserta datos de prueba solo si la base de datos no tiene pacientes.
     */
    suspend fun seedIfEmpty(
        patientRepository: PatientRepository,
        monitoringRepository: MonitoringRepository
    ) = withContext(Dispatchers.IO) {
        val existing = patientRepository.getAllPatientsSnapshot()
        if (existing.isNotEmpty()) return@withContext

        // ── Pacientes de prueba ───────────────────────────────────────────
        val patients = listOf(
            PatientModel(
                nombreCompleto = "García López, Carlos",
                edad = "45a",
                sexo = "Masculino",
                dni = "12345678",
                diagnostico = "Neumonía",
                intervencionQuirurgica = "",
                fechaNacimiento = "15/06/1980",
                fechaIngreso = "01/03/2026"
            ),
            PatientModel(
                nombreCompleto = "Ríos Mamani, Ana María",
                edad = "32a",
                sexo = "Femenino",
                dni = "23456789",
                diagnostico = "Post-op. apendicectomía",
                intervencionQuirurgica = "Apendicectomía",
                fechaNacimiento = "22/09/1993",
                fechaIngreso = "03/03/2026"
            ),
            PatientModel(
                nombreCompleto = "Torres Vega, Miguel Ángel",
                edad = "67a",
                sexo = "Masculino",
                dni = "34567890",
                diagnostico = "Insuficiencia cardíaca",
                intervencionQuirurgica = "",
                fechaNacimiento = "10/01/1959",
                fechaIngreso = "28/02/2026"
            ),
            PatientModel(
                nombreCompleto = "Flores Huanca, Lucía",
                edad = "8m",
                sexo = "Femenino",
                dni = "45678901",
                diagnostico = "Bronquiolitis",
                intervencionQuirurgica = "",
                fechaNacimiento = "10/07/2025",
                fechaIngreso = "05/03/2026"
            ),
            PatientModel(
                nombreCompleto = "Condori Quispe, José Luis",
                edad = "54a",
                sexo = "Masculino",
                dni = "56789012",
                diagnostico = "Diabetes mellitus tipo 2",
                intervencionQuirurgica = "",
                fechaNacimiento = "03/11/1971",
                fechaIngreso = "02/03/2026"
            ),
            PatientModel(
                nombreCompleto = "Mamani Chura, Rosa",
                edad = "28a",
                sexo = "Femenino",
                dni = "67890123",
                diagnostico = "Post-parto cesárea",
                intervencionQuirurgica = "Cesárea",
                fechaNacimiento = "14/04/1997",
                fechaIngreso = "07/03/2026"
            ),
            PatientModel(
                nombreCompleto = "Vargas Pinto, Pedro",
                edad = "12a",
                sexo = "Masculino",
                dni = "78901234",
                diagnostico = "Fractura de tibia",
                intervencionQuirurgica = "Reducción quirúrgica",
                fechaNacimiento = "18/02/2014",
                fechaIngreso = "06/03/2026"
            ),
            PatientModel(
                nombreCompleto = "Quispe Lazo, Elena",
                edad = "15d",
                sexo = "Femenino",
                dni = "89012345",
                diagnostico = "Ictericia neonatal",
                intervencionQuirurgica = "",
                fechaNacimiento = "24/02/2026",
                fechaIngreso = "02/03/2026"
            )
        )

        // Insertar pacientes y capturar sus IDs asignados
        val ids = patients.map { patientRepository.insertPatient(it) }

        // ── Registros de seguimiento — Marzo 2026 (hoy: día 11) ─────────
        val year = 2026
        val month = 3

        // Patrones por paciente: (patientIndex, día → estado)
        // ─────────────────────────────────────────────────────────────────
        // CON ALTA (4 pacientes) → aparecen en filtro "Con Alta"
        // ─────────────────────────────────────────────────────────────────
        val monitoringData: List<Pair<Int, Map<Int, MonitoringStatus>>> = listOf(

            // 0 · García López, Carlos
            //   Ingresó día 1 · Retiro día 5 · Alta día 8
            0 to mapOf(
                1 to MonitoringStatus.INICIO,
                2 to MonitoringStatus.CONTINUA,
                3 to MonitoringStatus.CONTINUA,
                4 to MonitoringStatus.CONTINUA,
                5 to MonitoringStatus.RETIRO,
                6 to MonitoringStatus.SIN_DISPOSITIVO,
                7 to MonitoringStatus.SIN_DISPOSITIVO,
                8 to MonitoringStatus.EGRESO
            ),

            // 1 · Ríos Mamani, Ana María
            //   Ingresó día 3 · Retiro día 6 · Alta día 9
            1 to mapOf(
                3 to MonitoringStatus.INICIO,
                4 to MonitoringStatus.CONTINUA,
                5 to MonitoringStatus.CONTINUA,
                6 to MonitoringStatus.RETIRO,
                7 to MonitoringStatus.SIN_DISPOSITIVO,
                8 to MonitoringStatus.SIN_DISPOSITIVO,
                9 to MonitoringStatus.EGRESO
            ),

            // 3 · Flores Huanca, Lucía (bebé 8 meses)
            //   Ingresó día 5 · Retiro día 7 · Alta día 10
            3 to mapOf(
                5 to MonitoringStatus.INICIO,
                6 to MonitoringStatus.CONTINUA,
                7 to MonitoringStatus.RETIRO,
                8 to MonitoringStatus.SIN_DISPOSITIVO,
                9 to MonitoringStatus.SIN_DISPOSITIVO,
                10 to MonitoringStatus.EGRESO
            ),

            // 7 · Quispe Lazo, Elena (neonata 15 días)
            //   Ingresó día 2 · Retiro día 5 · Alta día 7
            7 to mapOf(
                2 to MonitoringStatus.INICIO,
                3 to MonitoringStatus.CONTINUA,
                4 to MonitoringStatus.CONTINUA,
                5 to MonitoringStatus.RETIRO,
                6 to MonitoringStatus.SIN_DISPOSITIVO,
                7 to MonitoringStatus.EGRESO
            ),

            // ─────────────────────────────────────────────────────────────
            // ACTIVOS (4 pacientes) → aparecen en filtro "Activos"
            // ─────────────────────────────────────────────────────────────

            // 2 · Torres Vega, Miguel Ángel
            //   Ingresó el 28/Feb → INICIO en febrero, en marzo sigue con dispositivo
            //   Retiro día 10 · Sin Alta aún (esperando al día 12+)
            2 to mapOf(
                1  to MonitoringStatus.CONTINUA,
                2  to MonitoringStatus.CONTINUA,
                3  to MonitoringStatus.CONTINUA,
                4  to MonitoringStatus.CONTINUA,
                5  to MonitoringStatus.CONTINUA,
                6  to MonitoringStatus.CONTINUA,
                7  to MonitoringStatus.CONTINUA,
                8  to MonitoringStatus.CONTINUA,
                9  to MonitoringStatus.CONTINUA,
                10 to MonitoringStatus.RETIRO,
                11 to MonitoringStatus.SIN_DISPOSITIVO
            ),

            // 4 · Condori Quispe, José Luis
            //   Ingresó día 2 · Aún con dispositivo hasta hoy (día 11)
            4 to mapOf(
                2  to MonitoringStatus.INICIO,
                3  to MonitoringStatus.CONTINUA,
                4  to MonitoringStatus.CONTINUA,
                5  to MonitoringStatus.CONTINUA,
                6  to MonitoringStatus.CONTINUA,
                7  to MonitoringStatus.CONTINUA,
                8  to MonitoringStatus.CONTINUA,
                9  to MonitoringStatus.CONTINUA,
                10 to MonitoringStatus.CONTINUA,
                11 to MonitoringStatus.CONTINUA
            ),

            // 5 · Mamani Chura, Rosa
            //   Ingresó día 7 · Retiro hoy día 11 · Sin Alta aún
            5 to mapOf(
                7  to MonitoringStatus.INICIO,
                8  to MonitoringStatus.CONTINUA,
                9  to MonitoringStatus.CONTINUA,
                10 to MonitoringStatus.CONTINUA,
                11 to MonitoringStatus.RETIRO
            ),

            // 6 · Vargas Pinto, Pedro
            //   Ingresó día 6 · Retiro día 9 · Sin Alta aún
            6 to mapOf(
                6  to MonitoringStatus.INICIO,
                7  to MonitoringStatus.CONTINUA,
                8  to MonitoringStatus.CONTINUA,
                9  to MonitoringStatus.RETIRO,
                10 to MonitoringStatus.SIN_DISPOSITIVO,
                11 to MonitoringStatus.SIN_DISPOSITIVO
            )
        )

        monitoringData.forEach { (patientIndex, dayMap) ->
            val patientId = ids[patientIndex]
            dayMap.forEach { (day, status) ->
                monitoringRepository.insertOrUpdate(
                    DailyMonitoringModel(
                        patientId = patientId,
                        year = year,
                        month = month,
                        day = day,
                        status = status
                    )
                )
            }
        }
    }
}
