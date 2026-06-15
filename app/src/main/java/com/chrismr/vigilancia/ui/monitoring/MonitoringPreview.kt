package com.chrismr.vigilancia.ui.monitoring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.ui.theme.VigilanciaEpidemiologicaTheme

@Preview(showBackground = true, name = "Calendar Cell - Empty")
@Composable
fun CalendarDayCellEmptyPreview() {
    VigilanciaEpidemiologicaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                CalendarDayCell(
                    day = 15,
                    status = MonitoringStatus.VACIO,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Calendar Cell - Inicio")
@Composable
fun CalendarDayCellInicioPreview() {
    VigilanciaEpidemiologicaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            CalendarDayCell(
                day = 1,
                status = MonitoringStatus.INICIO,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Calendar Cell - Continua")
@Composable
fun CalendarDayCellContinuaPreview() {
    VigilanciaEpidemiologicaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            CalendarDayCell(
                day = 10,
                status = MonitoringStatus.CONTINUA,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Selection Action Bar")
@Composable
fun SelectionActionBarPreview() {
    VigilanciaEpidemiologicaTheme {
        SelectionActionBar(
            count = 5,
            onContinua = {},
            onSinDispositivo = {},
            onLimpiar = {},
            onCancel = {}
        )
    }
}
