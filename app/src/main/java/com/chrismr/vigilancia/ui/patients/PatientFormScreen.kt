package com.chrismr.vigilancia.ui.patients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrismr.vigilancia.data.repository.MonitoringRepository
import com.chrismr.vigilancia.data.repository.PatientRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PatientFormScreen(
    patientId: Long?,
    patientRepository: PatientRepository,
    monitoringRepository: MonitoringRepository,
    onBack: () -> Unit
) {
    val vm: PatientFormViewModel = viewModel(
        factory = PatientFormViewModel.factory(patientRepository, monitoringRepository, patientId)
    )

    LaunchedEffect(vm.result) {
        if (vm.result is FormResult.Success) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (patientId == null) "Nuevo Paciente" else "Editar Paciente",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (!vm.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = vm::save,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (patientId == null) "Guardar Registro" else "Actualizar Ficha",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (vm.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormHeroCard(isEditing = patientId != null)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Sección: Identificación ──────────────────────────────
                    Box(modifier = Modifier.weight(1f).widthIn(min = 340.dp)) {
                        FormSection(title = "Identificación") {
                            FormField(
                                "Nombres y Apellidos *",
                                vm.nombreCompleto
                            ) { vm.nombreCompleto = it }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormField(
                                        label = "DNI *",
                                        value = vm.dni,
                                        keyboardType = KeyboardType.Number,
                                        errorMessage = vm.dniError,
                                        maxLength = 8,
                                        numbersOnly = true
                                    ) { vm.dni = it }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SexoDropdown(vm.sexo) { vm.sexo = it }
                                }
                            }
                        }
                    }

                    // ── Sección: Datos clínicos ──────────────────────────────
                    Box(modifier = Modifier.weight(1f).widthIn(min = 340.dp)) {
                        FormSection(title = "Datos clínicos") {
                            EdadField(
                                numero = vm.edadNumero,
                                unidad = vm.edadUnidad,
                                onNumeroChange = { vm.edadNumero = it },
                                onUnidadChange = { vm.edadUnidad = it }
                            )
                            val fechaNacimientoEstimada = remember(vm.edadNumero, vm.edadUnidad) {
                                val n = vm.edadNumero.toIntOrNull() ?: return@remember null
                                val local = Calendar.getInstance().apply {
                                    when (vm.edadUnidad) {
                                        "a" -> add(Calendar.YEAR, -n)
                                        "m" -> add(Calendar.MONTH, -n)
                                        "d" -> add(Calendar.DAY_OF_MONTH, -n)
                                    }
                                }
                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    set(
                                        local.get(Calendar.YEAR),
                                        local.get(Calendar.MONTH),
                                        local.get(Calendar.DAY_OF_MONTH),
                                        0,
                                        0,
                                        0
                                    )
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DatePickerField(
                                        label = "Fecha Nacimiento",
                                        value = vm.fechaNacimiento,
                                        onValueChange = { vm.fechaNacimiento = it },
                                        estimatedMillis = fechaNacimientoEstimada
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    DatePickerField(
                                        label = "Fecha Ingreso",
                                        value = vm.fechaIngreso,
                                        onValueChange = { vm.fechaIngreso = it }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(0.65f)) {
                                    FormField("Diagnóstico", vm.diagnostico) { vm.diagnostico = it }
                                }
                                Box(modifier = Modifier.weight(0.35f)) {
                                    FormField(
                                        label = "Nº Cama",
                                        value = vm.numeroCama,
                                        keyboardType = KeyboardType.Text
                                    ) { vm.numeroCama = it }
                                }
                            }
                            FormField(
                                "Intervención Quirúrgica",
                                vm.intervencionQuirurgica
                            ) { vm.intervencionQuirurgica = it }
                        }
                    }
                }

                // ── Error general ────────────────────────────────────────
                if (vm.result is FormResult.Error) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = (vm.result as FormResult.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FormHeroCard(isEditing: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEditing) "Editar Ficha" else "Nuevo Paciente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Vigilancia Epidemiológica Activa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ── Sección con tarjeta ────────────────────────────────────────────────────
@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            letterSpacing = 1.sp
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    estimatedMillis: Long? = null
) {
    val formatter = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    var showPicker by remember { mutableStateOf(false) }
    val initialMillis = remember(value) {
        if (value.isNotBlank()) runCatching { formatter.parse(value)?.time }.getOrNull() else null
    }
    val startMillis = initialMillis ?: estimatedMillis

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontWeight = FontWeight.Medium) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            trailingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Abrir calendario",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        Box(modifier = Modifier.matchParentSize().clickable { showPicker = true })
    }

    if (showPicker) {
        val todayUtcMillis = remember {
            val local = Calendar.getInstance()
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onValueChange(formatter.format(Date(it))) }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EdadField(
    numero: String,
    unidad: String,
    onNumeroChange: (String) -> Unit,
    onUnidadChange: (String) -> Unit
) {
    val opciones = listOf("a" to "años", "m" to "meses", "d" to "días")
    var expanded by remember { mutableStateOf(false) }
    val etiqueta = opciones.firstOrNull { it.first == unidad }?.second ?: "años"
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = numero,
            onValueChange = { v ->
                val filtered = v.filter { it.isDigit() }
                if (filtered.length <= 3) onNumeroChange(filtered)
            },
            label = { Text("Edad", fontWeight = FontWeight.Medium) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(140.dp)
        ) {
            OutlinedTextField(
                value = etiqueta,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unidad", fontWeight = FontWeight.Medium) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                opciones.forEach { (codigo, nombre) ->
                    DropdownMenuItem(
                        text = { Text(nombre) },
                        onClick = { onUnidadChange(codigo); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String = "",
    maxLength: Int = Int.MAX_VALUE,
    numbersOnly: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = if (numbersOnly) newValue.filter { it.isDigit() } else newValue
            if (filtered.length <= maxLength) onValueChange(filtered)
        },
        label = { Text(label, fontWeight = FontWeight.Medium) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        isError = errorMessage.isNotEmpty(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            errorBorderColor = MaterialTheme.colorScheme.error,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        supportingText = if (errorMessage.isNotEmpty()) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        } else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexoDropdown(value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Masculino", "Femenino")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sexo", fontWeight = FontWeight.Medium) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
