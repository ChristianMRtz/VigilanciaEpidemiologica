package com.chrismr.vigilancia.ui.monitoring

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    patientId: Long,
    patientRepository: PatientRepository,
    monitoringRepository: MonitoringRepository,
    onBack: () -> Unit
) {
    val vm: MonitoringViewModel = viewModel(
        factory = MonitoringViewModel.factory(patientId, patientRepository, monitoringRepository)
    )
    val context = LocalContext.current

    val patient by vm.patient.collectAsState()
    val year by vm.year.collectAsState()
    val month by vm.month.collectAsState()
    val monitoringMap by vm.monitoringMap.collectAsState()
    val days by vm.daysInMonth.collectAsState()
    val firstOffset by vm.firstWeekdayOffset.collectAsState()
    val isEntireMonthLocked by vm.isEntireMonthLocked.collectAsState()
    val egresoRecord by vm.egresoRecord.collectAsState()
    val selectedDays by vm.selectedDays.collectAsState()
    val isSelectionMode by vm.isSelectionMode.collectAsState()
    val hasInicio by vm.hasInicioRecord.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val statusError by vm.statusError.collectAsState()

    LaunchedEffect(statusError) {
        statusError?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
            vm.clearError()
        }
    }

    var dayToEdit by remember { mutableStateOf<Int?>(null) }
    var showExportPicker by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionActionBar(
                    count = selectedDays.size,
                    onContinua = { vm.applyStatusToSelected(MonitoringStatus.CONTINUA) },
                    onSinDispositivo = { vm.applyStatusToSelected(MonitoringStatus.SIN_DISPOSITIVO) },
                    onLimpiar = { vm.applyStatusToSelected(MonitoringStatus.VACIO) },
                    onCancel = { vm.clearSelection() }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = patient?.nombreCompleto ?: "Seguimiento",
                            style = MaterialTheme.typography.titleMedium
                        )
                        patient?.diagnostico?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar mes")
                    }
                    IconButton(onClick = { showExportPicker = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar Excel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Navegador de mes ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = vm::previousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
                }
                Text(
                    text = DateUtils.formatMonthYear(month, year),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = vm::nextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
                }
            }

            // ── Leyenda ───────────────────────────────────────────────────
            StatusLegend()

            HorizontalDivider()

            // ── Banner: mes bloqueado por Alta en mes anterior ────────────
            if (isEntireMonthLocked && egresoRecord != null) {
                val r = egresoRecord!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF44336))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Alta dada el " +
                            "${r.day.toString().padStart(2,'0')}/" +
                            "${r.month.toString().padStart(2,'0')}/${r.year}" +
                            " — este mes no se puede modificar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Cabecera días de semana ───────────────────────────────────
            val weekDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                weekDays.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Grid del calendario ───────────────────────────────────────
            val totalCells = firstOffset + days.size
            val rows = (totalCells + 6) / 7
            val egresoDay = monitoringMap.values
                .firstOrNull { it.status == MonitoringStatus.EGRESO }?.day

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - firstOffset + 1
                            if (day < 1 || day > days.size) {
                                Box(modifier = Modifier.weight(1f).height(40.dp))
                            } else {
                                val status = monitoringMap[day]?.status ?: MonitoringStatus.VACIO
                                val isLocked = isEntireMonthLocked || (egresoDay != null && day > egresoDay)
                                val isSelected = day in selectedDays
                                CalendarDayCell(
                                    day = day,
                                    status = status,
                                    locked = isLocked,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isSelectionMode) vm.toggleDaySelection(day)
                                        else if (!isLocked) {
                                            val currentStatus = monitoringMap[day]?.status
                                                ?: MonitoringStatus.VACIO
                                            if (!hasInicio && currentStatus == MonitoringStatus.VACIO) {
                                                // Primer toque sin INICIO previo → INICIO automático
                                                vm.setStatus(day, MonitoringStatus.INICIO)
                                            } else {
                                                dayToEdit = day
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!isLocked) vm.toggleDaySelection(day)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Diálogo de confirmación: limpiar mes ──────────────────────────────
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Limpiar mes") },
            text = {
                Text(
                    "Se eliminarán todos los registros de vigilancia de " +
                        "${DateUtils.formatMonthYear(month, year)} para este paciente.\n\n" +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearAllMonth()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Limpiar todo") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo de selección de estado ───────────────────────────────────
    dayToEdit?.let { day ->
        StatusPickerDialog(
            day = day,
            currentStatus = monitoringMap[day]?.status ?: MonitoringStatus.VACIO,
            onSelect = { status ->
                vm.setStatus(day, status)
                dayToEdit = null
            },
            onDismiss = { dayToEdit = null }
        )
    }

    // Export month/year picker dialog
    if (showExportPicker) {
        MonthYearPickerDialog(
            initialYear = year,
            initialMonth = month,
            onConfirm = { selectedYear, selectedMonth ->
                vm.exportToExcel(context, selectedYear, selectedMonth)
                showExportPicker = false
            },
            onDismiss = { showExportPicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayCell(
    day: Int,
    status: MonitoringStatus,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val (bg, fg) = statusColors(status)
    val bgFinal = if (locked) Color(0xFFF5F5F5) else bg
    val fgFinal = if (locked) Color(0xFFBDBDBD) else fg
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bgFinal)
            .then(
                if (isSelected)
                    Modifier.border(2.5.dp, Color.White, RoundedCornerShape(5.dp))
                else Modifier
            )
            .then(
                if (!locked)
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isSelected) Color.White else fgFinal,
                lineHeight = 12.sp
            )
            if (status != MonitoringStatus.VACIO && !locked) {
                Text(
                    text = status.shortName,
                    fontSize = 7.sp,
                    color = (if (isSelected) Color.White else fgFinal).copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 8.sp
                )
            }
        }
        // Indicador de selección en esquina superior derecha
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onContinua: () -> Unit,
    onSinDispositivo: () -> Unit,
    onLimpiar: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$count día${if (count != 1) "s" else ""} seleccionado${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = onContinua,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White
                    )
                ) { Text("Continúa", fontSize = 12.sp) }

                FilledTonalButton(
                    onClick = onSinDispositivo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF9E9E9E),
                        contentColor = Color.White
                    )
                ) { Text("Sin disp. ( - )", fontSize = 12.sp) }

                FilledTonalButton(
                    onClick = onLimpiar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Limpiar", fontSize = 12.sp) }
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar selección") }
        }
    }
}

@Composable
fun StatusChip(status: MonitoringStatus) {
    val (bg, fg) = statusColors(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, bg.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.displayName,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MonitoringStatus.entries
            .filter { it != MonitoringStatus.VACIO }
            .forEach { s ->
                StatusChip(s)
            }
    }
}

@Composable
private fun StatusPickerDialog(
    day: Int,
    currentStatus: MonitoringStatus,
    onSelect: (MonitoringStatus) -> Unit,
    onDismiss: () -> Unit
) {
    // Opciones visibles (excluir VACIO del grid principal)
    val opciones = MonitoringStatus.entries.filter { it != MonitoringStatus.VACIO }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Día $day — ¿Qué estado asignar?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Grid 2 columnas con botones de color
                opciones.chunked(2).forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fila.forEach { s ->
                            val (bg, fg) = statusColors(s)
                            val isSelected = s == currentStatus
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelect(s) }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = s.displayName,
                                        color = fg,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (isSelected) {
                                        Text("✓", color = fg, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        // Rellenar fila impar con espacio vacío
                        if (fila.size == 1) Box(modifier = Modifier.weight(1f))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                // Botón de limpiar separado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE))
                        .clickable { onSelect(MonitoringStatus.VACIO) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Limpiar día",
                        color = Color(0xFF757575),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun statusColors(status: MonitoringStatus): Pair<Color, Color> = when (status) {
    MonitoringStatus.INICIO -> Color(0xFF4CAF50) to Color.White
    MonitoringStatus.CONTINUA -> Color(0xFF2196F3) to Color.White
    MonitoringStatus.RETIRO -> Color(0xFFFF9800) to Color.White
    MonitoringStatus.SIN_DISPOSITIVO -> Color(0xFF9E9E9E) to Color.White
    MonitoringStatus.EGRESO -> Color(0xFFF44336) to Color.White
    MonitoringStatus.VACIO -> Color(0xFFEEEEEE) to Color(0xFF757575)
}

private val MONTH_NAMES = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

@Composable
private fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onConfirm: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

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
                    text = "Seleccionar mes para exportar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Año anterior"
                        )
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Año siguiente"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Month grid (3 columns × 4 rows)
                val rows = MONTH_NAMES.chunked(3)
                rows.forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, name ->
                            val monthNumber = rowIndex * 3 + colIndex + 1
                            val isSelected = monthNumber == selectedMonth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedMonth = monthNumber }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    if (rowIndex < rows.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                        Text("Exportar")
                    }
                }
            }
        }
    }
}
