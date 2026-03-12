package com.chrismr.vigilancia.ui.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.model.PatientModel
import com.chrismr.vigilancia.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    patientRepository: PatientRepository,
    monitoringRepository: MonitoringRepository,
    onAddPatient: () -> Unit,
    onEditPatient: (Long) -> Unit,
    onMonitoring: (Long) -> Unit
) {
    val vm: PatientListViewModel = viewModel(
        factory = PatientListViewModel.factory(patientRepository, monitoringRepository)
    )
    val context = LocalContext.current
    val patients by vm.patients.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val currentFilter by vm.filter.collectAsState()
    var patientToDelete by remember { mutableStateOf<PatientModel?>(null) }
    var showExportPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vigilancia Epidemiológica") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showExportPicker = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Descargar consolidado")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPatient) {
                Icon(Icons.Default.Add, contentDescription = "Agregar paciente")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::updateSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por nombre o DNI") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.updateSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                }
            )

            // ── Chips de filtro ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PatientFilter.entries.forEach { f ->
                    FilterChip(
                        selected = currentFilter == f,
                        onClick = { vm.setFilter(f) },
                        label = { Text(f.label) }
                    )
                }
            }
            if (patients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            query.isNotBlank() -> "Sin resultados para \"$query\""
                            currentFilter == PatientFilter.ACTIVOS -> "No hay pacientes activos este mes."
                            currentFilter == PatientFilter.CON_ALTA -> "Ningún paciente tiene Alta este mes."
                            else -> "No hay pacientes registrados.\nPresiona + para agregar uno."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(patients, key = { it.id }) { patient ->
                        PatientItem(
                            patient = patient,
                            onEdit = { onEditPatient(patient.id) },
                            onMonitoring = { onMonitoring(patient.id) },
                            onDelete = { patientToDelete = patient }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    patientToDelete?.let { patient ->
        AlertDialog(
            onDismissRequest = { patientToDelete = null },
            title = { Text("Eliminar paciente") },
            text = {
                Text(
                    "¿Deseas eliminar a ${patient.nombreCompleto}? " +
                            "Esta acción eliminará también todos sus registros de seguimiento."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePatient(patient)
                    patientToDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { patientToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showExportPicker) {
        ConsolidadoMonthYearPicker(
            onConfirm = { year, month ->
                vm.exportConsolidado(context, year, month)
                showExportPicker = false
            },
            onDismiss = { showExportPicker = false }
        )
    }
}

@Composable
private fun PatientItem(    patient: PatientModel,
    onEdit: () -> Unit,
    onMonitoring: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                patient.nombreCompleto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                "DNI: ${patient.dni}  ·  ${patient.diagnostico}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onMonitoring) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Seguimiento",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        modifier = Modifier.clickable { onMonitoring() }
    )
}

private val MONTH_NAMES = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

@Composable
private fun ConsolidadoMonthYearPicker(
    onConfirm: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val nowYear = DateUtils.getCurrentYear()
    val nowMonth = DateUtils.getCurrentMonth()

    var selectedYear by remember { mutableIntStateOf(nowYear) }
    var selectedMonth by remember { mutableIntStateOf(nowMonth) }

    fun clampMonth(year: Int, month: Int): Int =
        if (year == nowYear && month > nowMonth) nowMonth else month

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar mes del consolidado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // ── Selector de año ───────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        selectedYear--
                        selectedMonth = clampMonth(selectedYear, selectedMonth)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Año anterior")
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            if (selectedYear < nowYear) {
                                selectedYear++
                                selectedMonth = clampMonth(selectedYear, selectedMonth)
                            }
                        },
                        enabled = selectedYear < nowYear
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Año siguiente")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // ── Grid de meses ─────────────────────────────────────────
                MONTH_NAMES.chunked(3).forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, name ->
                            val monthNumber = rowIndex * 3 + colIndex + 1
                            val isFuture = selectedYear == nowYear && monthNumber > nowMonth
                            val isSelected = monthNumber == selectedMonth
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (!isFuture) Modifier.clickable { selectedMonth = monthNumber }
                                        else Modifier
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isFuture   -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else       -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = name,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = when {
                                        isFuture   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else       -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    if (rowIndex < 3) Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))

                // ── Botones ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                        Text("Descargar")
                    }
                }
            }
        }
    }
}
