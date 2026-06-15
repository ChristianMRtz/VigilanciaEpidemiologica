package com.chrismr.vigilancia.ui.patients

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chrismr.vigilancia.ui.theme.VigilanciaEpidemiologicaTheme

@Preview(showBackground = true)
@Composable
fun PatientListHeaderPreview() {
    VigilanciaEpidemiologicaTheme {
        PatientListHeader(
            patientCount = 4,
            currentFilter = PatientFilter.ACTIVOS,
            query = "",
            onQueryChange = {},
            onClearQuery = {},
            onFilterSelected = {}
        )
    }
}
