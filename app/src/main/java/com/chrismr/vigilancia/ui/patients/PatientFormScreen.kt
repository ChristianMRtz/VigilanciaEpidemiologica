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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrismr.vigilancia.data.repository.PatientRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    patientId: Long?,
    patientRepository: PatientRepository,
    onBack: () -> Unit
) {
    val vm: PatientFormViewModel = viewModel(
        factory = PatientFormViewModel.factory(patientRepository, patientId)
    )

    LaunchedEffect(vm.result) {
        if (vm.result is FormResult.Success) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (patientId == null) "Nuevo Paciente" else "Editar Paciente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (vm.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField("Nombres y Apellidos *", vm.nombreCompleto) { vm.nombreCompleto = it }
                FormField("DNI *", vm.dni, keyboardType = KeyboardType.Number, errorMessage = vm.dniError, maxLength = 8, numbersOnly = true) { vm.dni = it }
                EdadField(
                    numero = vm.edadNumero,
                    unidad = vm.edadUnidad,
                    onNumeroChange = { vm.edadNumero = it },
                    onUnidadChange = { vm.edadUnidad = it }
                )
                SexoDropdown(vm.sexo) { vm.sexo = it }
                // Calcular fecha estimada de nacimiento en base a la edad ingresada
                val fechaNacimientoEstimada = remember(vm.edadNumero, vm.edadUnidad) {
                    val n = vm.edadNumero.toIntOrNull() ?: return@remember null
                    val local = Calendar.getInstance().apply {
                        when (vm.edadUnidad) {
                            "a" -> add(Calendar.YEAR, -n)
                            "m" -> add(Calendar.MONTH, -n)
                            "d" -> add(Calendar.DAY_OF_MONTH, -n)
                        }
                    }
                    // Convertir a UTC midnight (formato que usa DatePicker)
                    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        set(
                            local.get(Calendar.YEAR),
                            local.get(Calendar.MONTH),
                            local.get(Calendar.DAY_OF_MONTH),
                            0, 0, 0
                        )
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                DatePickerField(
                    label = "Fecha de Nacimiento",
                    value = vm.fechaNacimiento,
                    onValueChange = { vm.fechaNacimiento = it },
                    estimatedMillis = fechaNacimientoEstimada
                )
                DatePickerField(
                    label = "Fecha de Ingreso",
                    value = vm.fechaIngreso,
                    onValueChange = { vm.fechaIngreso = it }
                )
                FormField("Diagnóstico", vm.diagnostico) { vm.diagnostico = it }
                FormField(
                    "Intervención Quirúrgica",
                    vm.intervencionQuirurgica
                ) { vm.intervencionQuirurgica = it }

                if (vm.result is FormResult.Error) {
                    Text(
                        text = (vm.result as FormResult.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = vm::save,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (patientId == null) "Guardar Paciente" else "Actualizar Paciente")
                }
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
        if (value.isNotBlank()) {
            try { formatter.parse(value)?.time } catch (_: Exception) { null }
        } else null
    }

    // Si no hay fecha guardada pero sí una estimada, usamos la estimada como punto de partida
    val startMillis = initialMillis ?: estimatedMillis

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = "Abrir calendario")
            }
        )
        // Overlay transparente para capturar el tap en todo el campo
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        val todayUtcMillis = remember {
            // Tomamos año/mes/día en la zona horaria LOCAL del dispositivo
            // y los representamos como UTC midnight (que es lo que usa DatePicker internamente).
            val local = Calendar.getInstance()
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(
                    local.get(Calendar.YEAR),
                    local.get(Calendar.MONTH),
                    local.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
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
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(formatter.format(Date(millis)))
                    }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = numero,
            onValueChange = { v ->
                val filtered = v.filter { it.isDigit() }
                if (filtered.length <= 3) onNumeroChange(filtered)
            },
            label = { Text("Edad") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(130.dp)
        ) {
            OutlinedTextField(
                value = etiqueta,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unidad") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
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
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        isError = errorMessage.isNotEmpty(),
        supportingText = if (errorMessage.isNotEmpty()) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexoDropdown(value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Masculino", "Femenino")
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sexo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
