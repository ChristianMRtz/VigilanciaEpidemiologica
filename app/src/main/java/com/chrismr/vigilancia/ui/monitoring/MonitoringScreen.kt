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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        patient?.diagnostico?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
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
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Navegador de mes ──────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = vm::previousMonth) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Mes anterior",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = DateUtils.formatMonthYear(month, year),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = vm::nextMonth) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Mes siguiente",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── Leyenda ───────────────────────────────────────────────────
            StatusLegend()

            // ── Banner: mes bloqueado por Alta ────────────────────────────
            if (isEntireMonthLocked && egresoRecord != null) {
                val r = egresoRecord!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alta el ${r.day.toString().padStart(2,'0')}/" +
                                "${r.month.toString().padStart(2,'0')}/${r.year} — mes bloqueado",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Calendario (ocupa todo el espacio restante) ───────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    // Cabecera días de semana
                    val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        weekDays.forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val totalCells = firstOffset + days.size
                    val rows = (totalCells + 6) / 7
                    val egresoDay = monitoringMap.values
                        .firstOrNull { it.status == MonitoringStatus.EGRESO }?.day

                    // Cada fila ocupa 1/numRows del espacio disponible
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
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
    val bgFinal = if (locked) Color(0xFFF0F4F8) else bg
    val fgFinal = if (locked) Color(0xFFB0BEC5) else fg

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(bgFinal)
            .then(
                if (isSelected)
                    Modifier.border(2.5.dp, Color.White, RoundedCornerShape(8.dp))
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
                fontSize = 18.sp,
                color = fgFinal,
                lineHeight = 19.sp
            )
            if (status != MonitoringStatus.VACIO && !locked) {
                Text(
                    text = status.shortName,
                    fontSize = 12.sp,
                    color = fgFinal.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
            )
        }
    }
}

// ── Barra de acción de selección ───────────────────────────────────────────
@Composable
private fun SelectionActionBar(
    count: Int,
    onContinua: () -> Unit,
    onSinDispositivo: () -> Unit,
    onLimpiar: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$count día${if (count != 1) "s" else ""} seleccionado${if (count != 1) "s" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onCancel) {
                    Text("Cancelar", style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onContinua,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1E88E5),
                        contentColor = Color.White
                    )
                ) { Text("Continúa", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

                FilledTonalButton(
                    onClick = onSinDispositivo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF78909C),
                        contentColor = Color.White
                    )
                ) { Text("Sin disp.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

                FilledTonalButton(
                    onClick = onLimpiar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Limpiar", fontSize = 12.sp) }
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
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.displayName,
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Leyenda de estados ─────────────────────────────────────────────────────
@Composable
private fun StatusLegend() {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(MonitoringStatus.entries.filter { it != MonitoringStatus.VACIO }) { s ->
            StatusChip(s)
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
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) fg else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
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
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Text("✓", color = fg, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        if (fila.size == 1) Box(modifier = Modifier.weight(1f))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelect(MonitoringStatus.VACIO) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Limpiar día",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(20.dp)
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
                    ) { Text("Exportar") }
                }
            }
        }
    }
}
