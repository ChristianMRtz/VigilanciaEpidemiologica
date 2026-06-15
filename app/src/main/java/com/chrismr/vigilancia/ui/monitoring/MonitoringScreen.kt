package com.chrismr.vigilancia.ui.monitoring

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import com.chrismr.vigilancia.domain.enums.MonitoringStatus
import com.chrismr.vigilancia.ui.theme.StatusBlue
import com.chrismr.vigilancia.ui.theme.StatusBlueBg
import com.chrismr.vigilancia.ui.theme.StatusEmpty
import com.chrismr.vigilancia.ui.theme.StatusEmptyFg
import com.chrismr.vigilancia.ui.theme.StatusGray
import com.chrismr.vigilancia.ui.theme.StatusGrayBg
import com.chrismr.vigilancia.ui.theme.StatusGreen
import com.chrismr.vigilancia.ui.theme.StatusGreenBg
import com.chrismr.vigilancia.ui.theme.StatusOrange
import com.chrismr.vigilancia.ui.theme.StatusOrangeBg
import com.chrismr.vigilancia.ui.theme.StatusRed
import com.chrismr.vigilancia.ui.theme.StatusRedBg
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
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = patient?.nombreCompleto ?: "Seguimiento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            patient?.diagnostico?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showClearConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar mes",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { showExportPicker = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar Excel",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                MonthSummaryCard(
                    month = month,
                    year = year,
                    onPreviousMonth = vm::previousMonth,
                    onNextMonth = vm::nextMonth
                )

                // ── Leyenda de estados (más compacta) ─────────────────────────
                StatusLegend()

                // ── Banner: mes bloqueado por Alta ────────────────────────────
                if (isEntireMonthLocked && egresoRecord != null) {
                    val r = egresoRecord!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alta el ${r.day.toString().padStart(2,'0')}/" +
                                    "${r.month.toString().padStart(2,'0')}/${r.year} — mes bloqueado",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── Calendario (ocupa todo el espacio restante) ───────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        val availableH = maxHeight
                        val totalCells = firstOffset + days.size
                        val rows = (totalCells + 6) / 7
                        val egresoDay = monitoringMap.values
                            .firstOrNull { it.status == MonitoringStatus.EGRESO }?.day
                        val cellSpacing = 6.dp
                        val cellHeight = remember(availableH, rows) {
                            ((availableH - 40.dp - (cellSpacing * (rows - 1).coerceAtLeast(0))) / rows)
                                .coerceAtLeast(48.dp)
                        }

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Cabecera días de semana
                            val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                            ) {
                                weekDays.forEach { label ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            textAlign = TextAlign.Center,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Grid
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(cellSpacing)
                            ) {
                                for (row in 0 until rows) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                                    ) {
                                        for (col in 0 until 7) {
                                            val cellIndex = row * 7 + col
                                            val day = cellIndex - firstOffset + 1
                                            if (day < 1 || day > days.size) {
                                                Box(modifier = Modifier.weight(1f).fillMaxHeight())
                                            } else {
                                                val status = monitoringMap[day]?.status ?: MonitoringStatus.VACIO
                                                val isLocked = isEntireMonthLocked || (egresoDay != null && day > egresoDay)
                                                val isSelected = day in selectedDays
                                                CalendarDayCell(
                                                    day = day,
                                                    status = status,
                                                    locked = isLocked,
                                                    isSelected = isSelected,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight(),
                                                    minHeight = cellHeight,
                                                    onClick = {
                                                        if (isSelectionMode) vm.toggleDaySelection(day)
                                                        else if (!isLocked) {
                                                            val currentStatus = monitoringMap[day]?.status
                                                                ?: MonitoringStatus.VACIO
                                                            if (!hasInicio && currentStatus == MonitoringStatus.VACIO) {
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
                        }
                    }
                }
            }

            // ── Barra de herramientas flotante (Glassmorphism) ────────────
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    SelectionActionBar(
                        count = selectedDays.size,
                        onContinua = { vm.applyStatusToSelected(MonitoringStatus.CONTINUA) },
                        onSinDispositivo = { vm.applyStatusToSelected(MonitoringStatus.SIN_DISPOSITIVO) },
                        onLimpiar = { vm.applyStatusToSelected(MonitoringStatus.VACIO) },
                        onCancel = { vm.clearSelection() }
                    )
                }
            }
        }
    }

    // ── Diálogo limpiar mes ───────────────────────────────────────────────
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Limpiar mes", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Se eliminarán todos los registros de vigilancia de " +
                        "${DateUtils.formatMonthYear(month, year)} para este paciente.\n\n" +
                        "Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearAllMonth()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Limpiar todo") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(20.dp)
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

// ── Celda del calendario ───────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayCell(
    day: Int,
    status: MonitoringStatus,
    modifier: Modifier = Modifier,
    minHeight: Dp = 56.dp,
    locked: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val (bg, fg) = statusColors(status)
    val bgFinal = if (locked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else bg
    val fgFinal = if (locked) Color(0xFF9AA8B2) else fg

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(bgFinal)
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (!locked)
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = if (minHeight < 48.dp) 13.sp else 15.sp,
                color = fgFinal,
                lineHeight = 16.sp
            )
            if (status != MonitoringStatus.VACIO && !locked) {
                Text(
                    text = status.shortName,
                    fontSize = if (minHeight < 48.dp) 8.sp else 9.sp,
                    color = fgFinal.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ── Barra de acción de selección ───────────────────────────────────────────
@Composable
fun SelectionActionBar(
    count: Int,
    onContinua: () -> Unit,
    onSinDispositivo: () -> Unit,
    onLimpiar: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            FilledTonalButton(
                onClick = onContinua,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = StatusBlueBg,
                    contentColor = StatusBlue
                )
            ) {
                Text("Continúa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(
                onClick = onSinDispositivo,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = StatusGrayBg,
                    contentColor = StatusGray
                )
            ) {
                Text("Sin disp.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = onLimpiar,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Clear, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Chip de estado ─────────────────────────────────────────────────────────
@Composable
fun StatusChip(status: MonitoringStatus) {
    val (bg, fg) = statusColors(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.displayName,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Leyenda de estados ─────────────────────────────────────────────────────
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StatusLegend() {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MonitoringStatus.entries.filter { it != MonitoringStatus.VACIO }.forEach { s ->
            StatusChip(s)
        }
    }
}

@Composable
private fun MonthSummaryCard(
    month: Int,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Mes anterior",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seguimiento del mes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = DateUtils.formatMonthYear(month, year),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Mes siguiente",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Diálogo selector de estado ─────────────────────────────────────────────
@Composable
private fun StatusPickerDialog(
    day: Int,
    currentStatus: MonitoringStatus,
    onSelect: (MonitoringStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val opciones = MonitoringStatus.entries.filter { it != MonitoringStatus.VACIO }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Día $day — Asignar estado",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                opciones.chunked(2).forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        fila.forEach { s ->
                            val (bg, fg) = statusColors(s)
                            val isSelected = s == currentStatus
                            Surface(
                                modifier = Modifier.weight(1f),
                                onClick = { onSelect(s) },
                                shape = RoundedCornerShape(16.dp),
                                color = bg,
                                border = if (isSelected) BorderStroke(3.dp, fg) else BorderStroke(1.dp, fg.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = s.displayName,
                                        color = fg,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("✓", color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    onClick = { onSelect(MonitoringStatus.VACIO) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Limpiar día",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}


// ── Colores de estado ──────────────────────────────────────────────────────
private fun statusColors(status: MonitoringStatus): Pair<Color, Color> = when (status) {
    MonitoringStatus.INICIO         -> StatusGreenBg  to StatusGreen
    MonitoringStatus.CONTINUA       -> StatusBlueBg   to StatusBlue
    MonitoringStatus.RETIRO         -> StatusOrangeBg to StatusOrange
    MonitoringStatus.SIN_DISPOSITIVO -> StatusGrayBg  to StatusGray
    MonitoringStatus.EGRESO         -> StatusRedBg    to StatusRed
    MonitoringStatus.VACIO          -> StatusEmpty    to StatusEmptyFg
}

private val MONTH_NAMES = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

// ── Diálogo mes/año exportar ───────────────────────────────────────────────
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
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar mes para exportar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Año siguiente")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedMonth = monthNumber }
                                    .padding(vertical = 12.dp),
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
                    ) { Text("Compartir") }
                }
            }
        }
    }
}
