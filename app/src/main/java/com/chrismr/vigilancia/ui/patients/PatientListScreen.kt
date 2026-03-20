package com.chrismr.vigilancia.ui.patients

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.model.PatientModel
import com.chrismr.vigilancia.ui.theme.Avatar1
import com.chrismr.vigilancia.ui.theme.Avatar2
import com.chrismr.vigilancia.ui.theme.Avatar3
import com.chrismr.vigilancia.ui.theme.Avatar4
import com.chrismr.vigilancia.ui.theme.Avatar5
import com.chrismr.vigilancia.ui.theme.Avatar6
import com.chrismr.vigilancia.ui.theme.Avatar7
import com.chrismr.vigilancia.ui.theme.Avatar8
import com.chrismr.vigilancia.util.DateUtils

private val avatarPalette = listOf(Avatar1, Avatar2, Avatar3, Avatar4, Avatar5, Avatar6, Avatar7, Avatar8)

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
    var showYearPicker   by remember { mutableStateOf(false) }
    var menuExpanded     by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vigilancia",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Epidemiológica",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("📊  Exportar consolidado") },
                                onClick = {
                                    menuExpanded = false
                                    showExportPicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📅  Exportar año completo") },
                                onClick = {
                                    menuExpanded = false
                                    showYearPicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("💾  Guardar backup en Descargas") },
                                onClick = {
                                    menuExpanded = false
                                    vm.backupToDevice(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📧  Enviar backup por correo / app") },
                                onClick = {
                                    menuExpanded = false
                                    vm.backupAndShare(context)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPatient,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar paciente")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Campo de búsqueda ──────────────────────────────────────────
            OutlinedTextField(
                value = query,
                onValueChange = vm::updateSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = {
                    Text(
                        "Buscar por nombre o DNI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.updateSearch("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            // ── Chips de filtro ────────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PatientFilter.entries) { f ->
                    FilterChip(
                        selected = currentFilter == f,
                        onClick = { vm.setFilter(f) },
                        label = {
                            Text(
                                f.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (currentFilter == f) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contador pequeño
            if (patients.isNotEmpty()) {
                Text(
                    text = "${patients.size} paciente${if (patients.size != 1) "s" else ""} registrado${if (patients.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Lista de pacientes ─────────────────────────────────────────
            if (patients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when {
                                query.isNotBlank() -> "Sin resultados para\n\"$query\""
                                currentFilter == PatientFilter.ACTIVOS -> "No hay pacientes activos este mes."
                                currentFilter == PatientFilter.CON_ALTA -> "Ningún paciente tiene Alta este mes."
                                else -> "No hay pacientes registrados.\nPresiona + para agregar uno."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(patients, key = { it.id }) { patient ->
                        PatientCard(
                            patient = patient,
                            onEdit = { onEditPatient(patient.id) },
                            onMonitoring = { onMonitoring(patient.id) },
                            onDelete = { patientToDelete = patient }
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo de confirmación de eliminación ─────────────────────────────
    patientToDelete?.let { patient ->
        AlertDialog(
            onDismissRequest = { patientToDelete = null },
            title = { Text("Eliminar paciente", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "¿Deseas eliminar a ${patient.nombreCompleto}? " +
                            "Esta acción eliminará también todos sus registros de seguimiento.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deletePatient(patient)
                        patientToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { patientToDelete = null }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(20.dp)
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

    if (showYearPicker) {
        YearPickerDialog(
            onConfirm = { year ->
                vm.exportAnual(context, year)
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }
}

// ── Tarjeta de paciente ────────────────────────────────────────────────────
@Composable
private fun PatientCard(
    patient: PatientModel,
    onEdit: () -> Unit,
    onMonitoring: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMonitoring() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con iniciales
            PatientAvatar(name = patient.nombreCompleto)

            Spacer(modifier = Modifier.width(14.dp))

            // Información del paciente
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.nombreCompleto,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "DNI: ${patient.dni}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (patient.numeroCama.isNotBlank()) {
                    Text(
                        text = "Cama: ${patient.numeroCama}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (patient.diagnostico.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                    ) {
                        Text(
                            text = patient.diagnostico,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Acciones
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onMonitoring, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Seguimiento",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Avatar con iniciales ───────────────────────────────────────────────────
@Composable
private fun PatientAvatar(name: String) {
    val initials = name.trim().split("\\s+".toRegex())
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    val bgColor = remember(name) { avatarPalette[name.length % avatarPalette.size] }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}


// ── Picker solo de año (para exportación anual) ───────────────────────────
@Composable
private fun YearPickerDialog(
    onConfirm: (year: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val nowYear = DateUtils.getCurrentYear()
    val nowMonth = DateUtils.getCurrentMonth()
    var selectedYear by remember { mutableIntStateOf(nowYear) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Exportar año completo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Subtítulo dinámico: indica hasta qué mes si es el año actual
                val subtitle = if (selectedYear == nowYear)
                    "Se incluirán los meses de Enero a ${DateUtils.getMonthName(nowMonth)}"
                else
                    "Se incluirán todos los meses del año"
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Selector de año
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Año anterior")
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (selectedYear < nowYear) selectedYear++ },
                        enabled = selectedYear < nowYear
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Año siguiente")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedYear) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Exportar")
                    }
                }
            }
        }
    }
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
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar mes del consolidado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Selector de año
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Grid de meses
                MONTH_NAMES.chunked(3).forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, name ->
                            val monthNumber = rowIndex * 3 + colIndex + 1
                            val isFuture = selectedYear == nowYear && monthNumber > nowMonth
                            val isSelected = monthNumber == selectedMonth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isFuture   -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else       -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .then(
                                        if (!isFuture) Modifier.clickable { selectedMonth = monthNumber }
                                        else Modifier
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = when {
                                        isFuture   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else       -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (rowIndex < 3) Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedYear, selectedMonth) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Descargar")
                    }
                }
            }
        }
    }
}
