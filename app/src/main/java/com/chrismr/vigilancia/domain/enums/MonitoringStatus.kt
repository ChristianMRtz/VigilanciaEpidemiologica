package com.chrismr.vigilancia.domain.enums

/**
 * Estados posibles para el seguimiento diario de un paciente.
 */
enum class MonitoringStatus(val displayName: String, val shortName: String) {
    INICIO("Inicio",           "INI"),
    CONTINUA("Continúa",       "CON"),
    RETIRO("Retiro",           "RET"),
    SIN_DISPOSITIVO("Sin dispositivo", "-"),
    EGRESO("Alta",             "ALT"),
    VACIO("-",                 "")
}
