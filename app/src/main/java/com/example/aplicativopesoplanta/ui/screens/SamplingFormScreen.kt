package com.example.aplicativopesoplanta.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplicativopesoplanta.ui.viewmodel.SamplingViewModel
import com.example.aplicativopesoplanta.ui.theme.LightBeige
import com.example.aplicativopesoplanta.ui.theme.DarkBeige
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplingFormScreen(
    viewModel: SamplingViewModel,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    
    // States for dropdowns
    var blockMenuExpanded by remember { mutableStateOf(false) }
    var rootMenuExpanded by remember { mutableStateOf(false) }
    var findingsMenuExpanded by remember { mutableStateOf(false) }

    val availableBlocks by viewModel.availableBlocks.collectAsState()
    val filteredBlocks = remember(viewModel.block, availableBlocks) {
        if (viewModel.block.isEmpty()) {
            availableBlocks
        } else {
            availableBlocks.filter { it.contains(viewModel.block, ignoreCase = true) }
        }
    }

    // Handle Hardware Back Button
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Nuevo Muestreo - Peso Planta", 
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = LightBeige
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LightBeige
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val blackTextStyle = TextStyle(color = Color.Black, fontSize = 16.sp)

            // Block (Filterable Dropdown)
            Text("Bloque a muestrear", fontWeight = FontWeight.Bold, color = Color.Black)
            ExposedDropdownMenuBox(
                expanded = blockMenuExpanded,
                onExpandedChange = { blockMenuExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = viewModel.block,
                    onValueChange = { 
                        viewModel.updateBlock(it) 
                    },
                    label = { Text("Escriba o seleccione bloque", color = Color.Black) },
                    textStyle = blackTextStyle,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockMenuExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black
                    )
                )

                if (filteredBlocks.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = blockMenuExpanded,
                        onDismissRequest = { blockMenuExpanded = false },
                        modifier = Modifier.background(DarkBeige)
                    ) {
                        filteredBlocks.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, color = Color.Black) },
                                onClick = {
                                    viewModel.updateBlock(selectionOption)
                                    blockMenuExpanded = false
                                },
                                modifier = Modifier.background(DarkBeige)
                            )
                        }
                    }
                }
            }

            // Date Picker (Date is usually fresh, but let's keep it centered)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            OutlinedTextField(
                value = sdf.format(Date(viewModel.samplingDate)),
                onValueChange = {},
                label = { Text("Fecha de muestreo", color = Color.Black) },
                textStyle = blackTextStyle,
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Seleccionar fecha", tint = Color.Black)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                )
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = viewModel.samplingDate
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.samplingDate = it
                            }
                            showDatePicker = false
                        }) { Text("OK", color = Color.Black) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = Color.Black) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Weight
            OutlinedTextField(
                value = viewModel.weightInput,
                onValueChange = { viewModel.updateWeight(it) },
                label = { Text("Peso (g)", color = Color.Black) },
                textStyle = blackTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                )
            )

            // Root System (Dropdown Single Selection)
            Text("Sistema Radicular", fontWeight = FontWeight.Bold, color = Color.Black)
            ExposedDropdownMenuBox(
                expanded = rootMenuExpanded,
                onExpandedChange = { rootMenuExpanded = !rootMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = viewModel.rootSystem,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = blackTextStyle,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rootMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black
                    )
                )
                ExposedDropdownMenu(
                    expanded = rootMenuExpanded,
                    onDismissRequest = { rootMenuExpanded = false },
                    modifier = Modifier.background(DarkBeige)
                ) {
                    listOf("Normal", "Regular", "Deficiente").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.Black) },
                            onClick = {
                                viewModel.updateRootSystem(option)
                                rootMenuExpanded = false
                            },
                            modifier = Modifier.background(DarkBeige)
                        )
                    }
                }
            }

            // Checkboxes (Fusarium/Meristem)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.fusarium, 
                        onCheckedChange = { viewModel.updateFusarium(it) },
                        colors = CheckboxDefaults.colors(checkedColor = Color.Black, uncheckedColor = Color.Black)
                    )
                    Text("Fusarium", color = Color.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.meristem, 
                        onCheckedChange = { viewModel.updateMeristem(it) },
                        colors = CheckboxDefaults.colors(checkedColor = Color.Black, uncheckedColor = Color.Black)
                    )
                    Text("Meristemo", color = Color.Black)
                }
            }

            // Findings (Dropdown Multiple Selection)
            Text("Hallazgos", fontWeight = FontWeight.Bold, color = Color.Black)
            ExposedDropdownMenuBox(
                expanded = findingsMenuExpanded,
                onExpandedChange = { findingsMenuExpanded = !findingsMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (viewModel.selectedFindings.isEmpty()) "Ninguna" else viewModel.selectedFindings.joinToString(", "),
                    onValueChange = {},
                    readOnly = true,
                    textStyle = blackTextStyle,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = findingsMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black
                    )
                )
                ExposedDropdownMenu(
                    expanded = findingsMenuExpanded,
                    onDismissRequest = { findingsMenuExpanded = false },
                    modifier = Modifier.background(DarkBeige)
                ) {
                    viewModel.findingOptions.forEach { finding ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = viewModel.selectedFindings.contains(finding),
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = Color.Black)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(finding, color = Color.Black)
                                }
                            },
                            onClick = {
                                viewModel.onFindingToggle(finding)
                                if (finding == "Ninguna") findingsMenuExpanded = false
                            },
                            modifier = Modifier.background(DarkBeige)
                        )
                    }
                }
            }

            // Observations
            OutlinedTextField(
                value = viewModel.observations,
                onValueChange = { viewModel.updateObservations(it) },
                label = { Text("Observaciones", color = Color.Black) },
                textStyle = blackTextStyle,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    viewModel.saveSampling {
                        // Instead of moving back, stay and show success
                        scope.launch {
                            snackbarHostState.showSnackbar("Muestreo guardado correctamente")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("GUARDAR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
