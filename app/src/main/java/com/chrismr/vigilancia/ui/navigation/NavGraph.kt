package com.chrismr.vigilancia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.ui.monitoring.MonitoringScreen
import com.chrismr.vigilancia.ui.patients.PatientFormScreen
import com.chrismr.vigilancia.ui.patients.PatientListScreen

sealed class Screen(val route: String) {
    object PatientList : Screen("patients")
    object AddPatient : Screen("patients/add")
    object EditPatient : Screen("patients/{patientId}/edit") {
        fun createRoute(id: Long) = "patients/$id/edit"
    }
    object Monitoring : Screen("patients/{patientId}/monitoring") {
        fun createRoute(id: Long) = "patients/$id/monitoring"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    patientRepository: PatientRepository,
    monitoringRepository: MonitoringRepository
) {
    NavHost(navController = navController, startDestination = Screen.PatientList.route) {
        composable(Screen.PatientList.route) {
            PatientListScreen(
                patientRepository = patientRepository,
                monitoringRepository = monitoringRepository,
                onAddPatient = { navController.navigate(Screen.AddPatient.route) },
                onEditPatient = { id -> navController.navigate(Screen.EditPatient.createRoute(id)) },
                onMonitoring = { id -> navController.navigate(Screen.Monitoring.createRoute(id)) }
            )
        }
        composable(Screen.AddPatient.route) {
            PatientFormScreen(
                patientId = null,
                patientRepository = patientRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditPatient.route,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { backStack ->
            val patientId = backStack.arguments?.getLong("patientId") ?: return@composable
            PatientFormScreen(
                patientId = patientId,
                patientRepository = patientRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Monitoring.route,
            arguments = listOf(navArgument("patientId") { type = NavType.LongType })
        ) { backStack ->
            val patientId = backStack.arguments?.getLong("patientId") ?: return@composable
            MonitoringScreen(
                patientId = patientId,
                patientRepository = patientRepository,
                monitoringRepository = monitoringRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

