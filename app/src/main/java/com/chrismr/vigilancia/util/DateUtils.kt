package com.chrismr.vigilancia.util

import java.util.Calendar

/**
 * Utilidades de fechas para el seguimiento mensual.
 */
object DateUtils {

    fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    /** Devuelve el mes actual en rango 1–12. */
    fun getCurrentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1

    /** Número real de días del mes indicado (contempla bisiesto). */
    fun getDaysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Lista [1, 2, …, N] donde N es el último día del mes. */
    fun getDaysList(year: Int, month: Int): List<Int> =
        (1..getDaysInMonth(year, month)).toList()

    /**
     * Devuelve cuántas celdas vacías van antes del día 1 en un calendario
     * con semana que empieza en Lunes (0=Lun, 1=Mar … 6=Dom).
     */
    fun getFirstWeekdayOffset(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        // Calendar.DAY_OF_WEEK: 1=Dom,2=Lun…7=Sáb → convertir a 0=Lun…6=Dom
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    fun getMonthName(month: Int): String = when (month) {
        1  -> "Enero"
        2  -> "Febrero"
        3  -> "Marzo"
        4  -> "Abril"
        5  -> "Mayo"
        6  -> "Junio"
        7  -> "Julio"
        8  -> "Agosto"
        9  -> "Septiembre"
        10 -> "Octubre"
        11 -> "Noviembre"
        12 -> "Diciembre"
        else -> ""
    }

    fun formatMonthYear(month: Int, year: Int): String =
        "${getMonthName(month)} $year"
}

