package com.chrismr.vigilancia.ui.patients

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Brush
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val avatarPalette = listOf(Avatar1, Avatar2, Avatar3, Avatar4, Avatar5, Avatar6, Avatar7, Avatar8)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    var patientToPermanentlyDelete by remember { mutableStateOf<PatientModel?>(null) }
    var showExportPicker by remember { mutableStateOf(false) }
    var showYearPicker   by remember { mutableStateOf(false) }
    var menuExpanded     by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { vm.importBackup(context, it) { menuExpanded = false } }
    }

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
                            DropdownMenuItem(
                                text = { Text("📥  Restaurar backup (Importar)") },
                                onClick = {
                                    menuExpanded = false
                                    importLauncher.launch("application/zip")
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
            PatientListHeader(
                patientCount = patients.size,
                currentFilter = currentFilter,
                query = query,
                onQueryChange = vm::updateSearch,
                onClearQuery = { vm.updateSearch("") },
                onFilterSelected = vm::setFilter
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (patients.isEmpty()) {
                EmptyPatientsState(
                    query = query,
                    currentFilter = currentFilter,
                    onAddPatient = onAddPatient
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(patients, key = { it.patient.id }) { item ->
                        PatientCard(
                            patient = item.patient,
                            isDischarged = item.isDischarged,
                            isDeleted = currentFilter == PatientFilter.PAPELERA,
                            onEdit = { onEditPatient(item.patient.id) },
                            onMonitoring = { onMonitoring(item.patient.id) },
                            onDelete = {
                                if (currentFilter == PatientFilter.PAPELERA) {
                                    patientToPermanentlyDelete = item.patient
                                } else {
                                    patientToDelete = item.patient
                                }
                            },
                            onRestore = { vm.restorePatient(item.patient.id) }
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo de confirmación de borrado lógico (Mover a Papelera) ──────────
    patientToDelete?.let { patient ->
        AlertDialog(
            onDismissRequest = { patientToDelete = null },
            title = { Text("Mover a la papelera", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "¿Deseas mover a ${patient.nombreCompleto} a la papelera? Podrás restaurarlo más tarde desde la sección \"Papelera\".",
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
                    Text("Mover a papelera")
                }
            },
            dismissButton = {
                TextButton(onClick = { patientToDelete = null }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Diálogo de confirmación de borrado permanente ─────────────────────────
    patientToPermanentlyDelete?.let { patient ->
        AlertDialog(
            onDismissRequest = { patientToPermanentlyDelete = null },
            title = { Text("Eliminar permanentemente", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "¿Estás seguro de eliminar a ${patient.nombreCompleto} de forma permanente? Esta acción no se puede deshacer y borrará todo su historial.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deletePatientPermanently(patient)
                        patientToPermanentlyDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Eliminar para siempre")
                }
            },
            dismissButton = {
                TextButton(onClick = { patientToPermanentlyDelete = null }) { Text("Cancelar") }
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

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun PatientListHeader(
    patientCount: Int,
    currentFilter: PatientFilter,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterSelected: (PatientFilter) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(top = 12.dp, bottom = 24.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (currentFilter == PatientFilter.PAPELERA) "Papelera" else "Pacientes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "$patientCount registro${if (patientCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = currentFilter.label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Buscar nombre o DNI...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PatientFilter.entries.forEach { f ->
                    val isSelected = currentFilter == f
                    Surface(
                        onClick = { onFilterSelected(f) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f) else Color.Transparent)
                    ) {
                        Text(
                            f.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPatientsState(
    query: String,
    currentFilter: PatientFilter,
    onAddPatient: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (currentFilter == PatientFilter.PAPELERA) Icons.Default.Delete else Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = when {
                        query.isNotBlank() -> "Sin resultados para \"$query\""
                        currentFilter == PatientFilter.ACTIVOS -> "No hay pacientes activos este mes"
                        currentFilter == PatientFilter.CON_ALTA -> "Ningún paciente tiene alta este mes"
                        currentFilter == PatientFilter.PAPELERA -> "La papelera está vacía"
                        else -> "Todavía no hay pacientes registrados"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (query.isNotBlank()) {
                        "Prueba con otro nombre o elimina el filtro para ver más resultados."
                    } else if (currentFilter == PatientFilter.PAPELERA) {
                        "Aquí aparecerán los pacientes que elimines. Podrás restaurarlos o borrarlos definitivamente."
                    } else {
                        "Agrega un paciente para comenzar el seguimiento diario desde una interfaz más rápida y ordenada."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (query.isBlank() && currentFilter != PatientFilter.PAPELERA) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAddPatient,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(56.dp).fillMaxWidth(0.7f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar paciente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Tarjeta de paciente ────────────────────────────────────────────────────
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun PatientCard(
    patient: PatientModel,
    isDischarged: Boolean,
    isDeleted: Boolean = false,
    onEdit: () -> Unit,
    onMonitoring: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isDeleted) onMonitoring() },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PatientAvatar(name = patient.nombreCompleto)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.nombreCompleto,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DNI ${patient.dni}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                        if (isDischarged && !isDeleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                shape = CircleShape
                            ) {
                                Text(
                                    "ALTA",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
                Icon(
                    imageVector = if (isDeleted) Icons.Default.Delete else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!isDeleted && (patient.numeroCama.isNotBlank() || patient.diagnostico.isNotBlank())) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (patient.numeroCama.isNotBlank()) PatientMetaChip("Cama", patient.numeroCama)
                    if (patient.diagnostico.isNotBlank()) PatientMetaChip("Dx", patient.diagnostico)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDeleted) {
                    PatientActionButton(
                        label = "Restaurar",
                        icon = Icons.Default.Refresh,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onRestore
                    )
                    PatientActionButton(
                        label = "Eliminar",
                        icon = Icons.Default.Delete,
                        iconTint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = onDelete
                    )
                } else {
                    PatientActionButton(
                        label = "Editar",
                        icon = Icons.Default.Edit,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onEdit
                    )
                    PatientActionButton(
                        label = "Borrar",
                        icon = Icons.Default.Delete,
                        iconTint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun PatientMetaChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun PatientActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
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
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
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
                        Text("Compartir")
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
                        Text("Compartir")
                    }
                }
            }
        }
    }
}
