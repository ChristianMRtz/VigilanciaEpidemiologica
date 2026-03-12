package com.chrismr.vigilancia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.chrismr.vigilancia.ui.navigation.NavGraph
import com.chrismr.vigilancia.ui.theme.VigilanciaEpidemiologicaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VigilanciaApp
        setContent {
            VigilanciaEpidemiologicaTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    patientRepository = app.patientRepository,
                    monitoringRepository = app.monitoringRepository
                )
            }
        }
    }
}
