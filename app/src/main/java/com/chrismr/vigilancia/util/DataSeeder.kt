package com.chrismr.vigilancia.util

import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.domain.model.DailyMonitoringModel
import com.chrismr.vigilancia.domain.model.PatientModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object DataSeeder {

    // ── Vocabulario ──────────────────────────────────────────────────────

    private val AP_PATERNO = listOf(
        "García", "Ríos", "Torres", "Flores", "Condori", "Mamani", "Vargas",
        "Quispe", "López", "Martínez", "Rodríguez", "Mendoza", "Cruz", "Huanca",
        "Pinto", "Lazo", "Chura", "Vega", "Lima", "Ramos", "Cusi", "Apaza",
        "Ccama", "Tito", "Anca", "Soto", "Vera", "Paz", "Rojas", "Medina"
    )

    private val AP_MATERNO = listOf(
        "Mamani", "Quispe", "Huanca", "Chura", "Lazo", "Pinto", "Vega",
        "López", "García", "Torres", "Flores", "Cruz", "Mendoza", "Cusi",
        "Apaza", "Ccama", "Tito", "Soto", "Vera", "Paz", "Rojas", "Lima"
    )

    private val NOMBRES_M = listOf(
        "Carlos", "Miguel", "José", "Pedro", "Juan", "Luis", "Antonio",
        "Rodrigo", "Fernando", "Eduardo", "Marco", "Raúl", "David",
        "Álvaro", "Diego", "Óscar", "Javier", "Ricardo", "Sergio", "Martín"
    )

    private val NOMBRES_F = listOf(
        "Ana", "Rosa", "Lucía", "Elena", "Carmen", "María", "Isabel",
        "Patricia", "Sofía", "Claudia", "Mónica", "Sandra", "Laura",
        "Valeria", "Daniela", "Alejandra", "Gabriela", "Paola", "Natalia", "Luz"
    )

    // (diagnóstico, intervención quirúrgica — vacío = ninguna)
    private val DX_ADULTO = listOf(
        "Neumonía" to "",
        "Insuficiencia cardíaca" to "",
        "Diabetes mellitus tipo 2" to "",
        "Hipertensión arterial severa" to "",
        "Infección del tracto urinario" to "",
        "EPOC exacerbado" to "",
        "Gastroenteritis aguda" to "",
        "Anemia severa" to "",
        "Accidente cerebrovascular" to "",
        "Sepsis" to "",
        "Insuficiencia renal aguda" to "",
        "Pancreatitis aguda" to "",
        "Post-op. apendicectomía" to "Apendicectomía",
        "Post-op. colecistectomía" to "Colecistectomía",
        "Post-parto cesárea" to "Cesárea",
        "Post-parto vaginal" to "",
        "Fractura de fémur" to "Reducción quirúrgica",
        "Hernia inguinal" to "Hernioplastia",
        "Colitis ulcerativa" to "",
        "Trombosis venosa profunda" to ""
    )

    private val DX_PEDIATRICO = listOf(
        "Bronquiolitis" to "",
        "Neumonía" to "",
        "Gastroenteritis aguda" to "",
        "Broncoespasmo" to "",
        "Ictericia neonatal" to "",
        "Convulsión febril" to "",
        "Fractura de tibia" to "Reducción quirúrgica",
        "Apendicitis aguda" to "Apendicectomía",
        "Infección urinaria" to "",
        "Síndrome de dificultad respiratoria" to "",
        "Meningitis bacteriana" to "",
        "Otitis media aguda" to ""
    )

    // ── Punto de entrada ─────────────────────────────────────────────────

    /**
     * Inserta datos de prueba solo si la base de datos está vacía.
     * Seed fijo (42) → los resultados son siempre los mismos al reinstalar.
     */
    suspend fun seedIfEmpty(
        patientRepository: PatientRepository,
        monitoringRepository: MonitoringRepository
    ) = withContext(Dispatchers.IO) {
        if (patientRepository.getAllPatientsSnapshot().isNotEmpty()) return@withContext

        val rng          = Random(42)
        val YEAR         = 2026
        val TODAY_MONTH  = 3
        val TODAY_DAY    = 12
        val NUM_PATIENTS = 20

        // 1 · Distribución de meses de inicio (~35% Ene, ~35% Feb, ~30% Mar)
        val startMonths = buildStartMonths(NUM_PATIENTS)

        // 2 · Crear y persistir pacientes
        val usedDNIs = mutableSetOf<String>()
        val ids = startMonths.map { startMonth ->
            patientRepository.insertPatient(buildPatient(rng, usedDNIs, startMonth))
        }

        // 3 · Generar seguimiento mes a mes para cada paciente
        startMonths.forEachIndexed { i, startMonth ->
            var carry = Carry.NONE

            for (month in startMonth..TODAY_MONTH) {
                val maxDay = if (month == TODAY_MONTH) TODAY_DAY
                             else DateUtils.getDaysInMonth(YEAR, month)

                // Meses pasados: 40% terminan ese mes, 60% continúan
                // Mes actual: 50% ya tienen alta, 50% siguen activos
                val preferAlta = rng.nextInt(10) < if (month < TODAY_MONTH) 4 else 5

                val (pattern, nextCarry) = buildMonthPattern(rng, carry, maxDay, preferAlta)
                carry = nextCarry

                pattern.forEach { (day, status) ->
                    monitoringRepository.insertOrUpdate(
                        DailyMonitoringModel(
                            patientId = ids[i],
                            year      = YEAR,
                            month     = month,
                            day       = day,
                            status    = status
                        )
                    )
                }

                if (carry == Carry.NONE) break // EGRESO registrado, no continúa
            }
        }
    }

    // ── Estado de continuación entre meses ───────────────────────────────

    private enum class Carry {
        NONE,            // Paciente tuvo EGRESO, ciclo terminado
        CONNECTED,       // Sigue con dispositivo (INICIO/CONTINUA), continúa el próximo mes
        AWAITING_EGRESO  // Dispositivo retirado (RETIRO/SIN_DISP), necesita EGRESO el próximo mes
    }

    // ── Constructores ────────────────────────────────────────────────────

    private fun buildStartMonths(count: Int): List<Int> {
        val raw = (0 until count).map { i ->
            when {
                i < (count * 0.35).toInt() -> 1
                i < (count * 0.70).toInt() -> 2
                else                       -> 3
            }
        }
        return raw.shuffled(Random(99))
    }

    private fun buildPatient(
        rng: Random,
        usedDNIs: MutableSet<String>,
        startMonth: Int
    ): PatientModel {
        val isMale   = rng.nextBoolean()
        val nombre   = if (isMale) NOMBRES_M.random(rng) else NOMBRES_F.random(rng)
        val fullName = "${AP_PATERNO.random(rng)} ${AP_MATERNO.random(rng)}, $nombre"

        val roll = rng.nextInt(100)
        val (edad, birthDate, isPediatric) = when {
            roll < 4  -> { val d = rng.nextInt(1, 29);  Triple("${d}d", bdDays(d),         true)  }
            roll < 12 -> { val m = rng.nextInt(1, 24);  Triple("${m}m", bdMonths(m),        true)  }
            roll < 25 -> { val y = rng.nextInt(2, 18);  Triple("${y}a", bdYears(y, rng),    true)  }
            else      -> { val y = rng.nextInt(18, 86); Triple("${y}a", bdYears(y, rng),    false) }
        }

        val (diag, interv) = if (isPediatric) DX_PEDIATRICO.random(rng)
                              else            DX_ADULTO.random(rng)

        return PatientModel(
            nombreCompleto        = fullName,
            edad                  = edad,
            sexo                  = if (isMale) "Masculino" else "Femenino",
            dni                   = uniqueDNI(rng, usedDNIs),
            diagnostico           = diag,
            intervencionQuirurgica = interv,
            fechaNacimiento       = birthDate,
            fechaIngreso          = "%02d/%02d/2026".format(rng.nextInt(1, 10), startMonth)
        )
    }

    /**
     * Genera el patrón de seguimiento de un mes según el estado de entrada [carry].
     *
     * - [Carry.NONE]           → paciente empieza de cero este mes (INICIO → CONTINUA → …)
     * - [Carry.CONNECTED]      → sigue con dispositivo desde el mes anterior (CONTINUA → …)
     * - [Carry.AWAITING_EGRESO]→ dispositivo ya retirado, solo falta SIN_DISP + EGRESO
     *
     * Devuelve (mapa día→estado, estado de salida).
     */
    private fun buildMonthPattern(
        rng: Random,
        carry: Carry,
        maxDay: Int,
        preferAlta: Boolean
    ): Pair<Map<Int, MonitoringStatus>, Carry> {
        val result = mutableMapOf<Int, MonitoringStatus>()

        // ── Caso especial: solo necesita cerrar el ciclo con EGRESO ──────
        if (carry == Carry.AWAITING_EGRESO) {
            var day = 1
            val sinDispDays = rng.nextInt(1, 4)
            repeat(sinDispDays) { if (day <= maxDay) result[day++] = MonitoringStatus.SIN_DISPOSITIVO }
            return if (day <= maxDay) {
                result[day] = MonitoringStatus.EGRESO
                result to Carry.NONE
            } else {
                result to Carry.AWAITING_EGRESO // sigue esperando en el próximo mes
            }
        }

        // ── Ciclo normal (NONE = nuevo, CONNECTED = continúa con dispositivo) ─
        var day = if (carry == Carry.CONNECTED) 1
                  else rng.nextInt(1, maxOf(2, (maxDay * 0.55).toInt() + 1))

        // INICIO solo si el paciente empieza este mes
        if (carry == Carry.NONE) {
            result[day++] = MonitoringStatus.INICIO
            if (day > maxDay) return result to Carry.CONNECTED
        }

        // CONTINUA
        val remaining    = maxDay - day + 1
        val continuaDays = if (remaining <= 2) remaining
                           else rng.nextInt(2, maxOf(3, (remaining * 0.8).toInt() + 1))
        repeat(continuaDays) { if (day <= maxDay) result[day++] = MonitoringStatus.CONTINUA }
        if (day > maxDay) return result to Carry.CONNECTED  // sigue conectado el próximo mes

        // RETIRO
        result[day++] = MonitoringStatus.RETIRO
        if (day > maxDay) return result to Carry.AWAITING_EGRESO

        // SIN_DISPOSITIVO + decisión de EGRESO
        if (preferAlta) {
            // Intentar cerrar el ciclo este mismo mes: 1–3 días de SIN_DISP + EGRESO
            val sinDispDays = rng.nextInt(1, 4)
            val egresoDay   = day + sinDispDays
            if (egresoDay <= maxDay) {
                for (d in day until egresoDay) result[d] = MonitoringStatus.SIN_DISPOSITIVO
                result[egresoDay] = MonitoringStatus.EGRESO
                return result to Carry.NONE
            }
            // No hay espacio → cae al relleno completo y carry
        }

        // Sin EGRESO este mes → todos los días restantes son SIN_DISP (sin celdas vacías)
        for (d in day..maxDay) result[d] = MonitoringStatus.SIN_DISPOSITIVO
        return result to Carry.AWAITING_EGRESO
    }

    // ── Helpers de fecha ─────────────────────────────────────────────────

    /** Fecha de nacimiento restando [days] días al 12/03/2026. */
    private fun bdDays(days: Int): String {
        val monthDays = intArrayOf(0, 31, 28, 31) // índices 0-3 (pad, Ene, Feb, Mar)
        var d = 12 - days; var m = 3; var y = 2026
        while (d <= 0) { m--; if (m <= 0) { m = 12; y-- }; d += monthDays.getOrElse(m) { 30 } }
        return "%02d/%02d/%04d".format(d, m, y)
    }

    /** Fecha de nacimiento restando [months] meses al 03/2026. */
    private fun bdMonths(months: Int): String {
        var m = 3 - months; var y = 2026
        while (m <= 0) { m += 12; y-- }
        return "15/%02d/%04d".format(m, y)
    }

    /** Fecha de nacimiento restando [years] años al 2026, día y mes aleatorios. */
    private fun bdYears(years: Int, rng: Random) =
        "%02d/%02d/%04d".format(rng.nextInt(1, 28), rng.nextInt(1, 13), 2026 - years)

    /** Genera un DNI único de 8 dígitos. */
    private fun uniqueDNI(rng: Random, used: MutableSet<String>): String {
        var dni: String
        do { dni = (10_000_000 + rng.nextInt(90_000_000)).toString() } while (!used.add(dni))
        return dni
    }
}
