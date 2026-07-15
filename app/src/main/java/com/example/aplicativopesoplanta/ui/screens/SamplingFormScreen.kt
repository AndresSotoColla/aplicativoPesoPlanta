package com.example.aplicativopesoplanta.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Individual", "Por Lotes")

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
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = LightBeige,
                contentColor = Color.Black
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    IndividualSamplingForm(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
                1 -> {
                    BatchSamplingForm(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualSamplingForm(
    viewModel: SamplingViewModel,
    snackbarHostState: SnackbarHostState
) {
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

    // Block Validation: Must be exactly in the list
    val isBlockValid = remember(viewModel.block, availableBlocks) {
        viewModel.block.isEmpty() || availableBlocks.contains(viewModel.block)
    }

    // Stats
    val (count, avg) = viewModel.todayBlockStats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Header
        if (viewModel.block.isNotEmpty() && isBlockValid) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkBeige),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Muestreos hoy (Este bloque)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Peso Promedio",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                        Text(
                            "${String.format(Locale.getDefault(), "%.2f", avg)} g",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        val blackTextStyle = TextStyle(color = Color.Black, fontSize = 16.sp)

        // Block (Filterable Dropdown) with validation
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
                label = { Text("Escriba o seleccione bloque", color = if (isBlockValid) Color.Black else Color.Red) },
                textStyle = blackTextStyle,
                isError = !isBlockValid,
                supportingText = {
                    if (!isBlockValid) {
                        Text("El bloque no existe en el cronograma", color = Color.Red)
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockMenuExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isBlockValid) Color.Black else Color.Red,
                    unfocusedBorderColor = if (isBlockValid) Color.Black else Color.Red,
                    errorBorderColor = Color.Red
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

        // Date Picker
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

        // Weight with validation
        val weightVal = viewModel.weightInput.toDoubleOrNull() ?: 0.0
        val isWeightError = weightVal > 4000
        
        OutlinedTextField(
            value = viewModel.weightInput,
            onValueChange = { viewModel.updateWeight(it) },
            label = { Text("Peso (g)", color = if (isWeightError) Color.Red else Color.Black) },
            textStyle = blackTextStyle,
            isError = isWeightError,
            supportingText = {
                if (isWeightError) {
                    Text("El peso no puede superar los 4000g", color = Color.Red)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isWeightError) Color.Red else Color.Black,
                unfocusedBorderColor = if (isWeightError) Color.Red else Color.Black,
                errorBorderColor = Color.Red
            )
        )

        // Root System
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

        // Checkboxes
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

        // Findings
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
                viewModel.saveSampling { errorMsg ->
                    scope.launch {
                        if (errorMsg != null) {
                            snackbarHostState.showSnackbar(errorMsg)
                        } else {
                            snackbarHostState.showSnackbar("Muestreo guardado correctamente")
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isWeightError.not() && isBlockValid && viewModel.block.isNotEmpty(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                disabledContainerColor = Color.Gray
            )
        ) {
            Text("GUARDAR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchSamplingForm(
    viewModel: SamplingViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    
    // States for dropdowns
    var blockMenuExpanded by remember { mutableStateOf(false) }

    val availableBlocks by viewModel.availableBlocks.collectAsState()
    val filteredBlocks = remember(viewModel.block, availableBlocks) {
        if (viewModel.block.isEmpty()) {
            availableBlocks
        } else {
            availableBlocks.filter { it.contains(viewModel.block, ignoreCase = true) }
        }
    }

    // Block Validation: Must be exactly in the list
    val isBlockValid = remember(viewModel.block, availableBlocks) {
        viewModel.block.isEmpty() || availableBlocks.contains(viewModel.block)
    }

    val categories = listOf("Muy Pequeño", "Pequeño", "Mediano", "Grande", "Muy Grande")
    val categoryWeights = mapOf(
        "Muy Pequeño" to 500.0,
        "Pequeño" to 1000.0,
        "Mediano" to 1500.0,
        "Grande" to 2000.0,
        "Muy Grande" to 2500.0
    )

    // Remember counts as string inputs to allow direct text entry
    val categoryInputs = remember {
        mutableStateMapOf<String, String>().apply {
            categories.forEach { this[it] = "0" }
        }
    }

    fun resetCategoryInputs() {
        categories.forEach { categoryInputs[it] = "0" }
    }

    val totalCount by remember(categoryInputs) {
        derivedStateOf {
            categoryInputs.values.sumOf { it.toIntOrNull() ?: 0 }
        }
    }

    val weightedAverage by remember(categoryInputs) {
        derivedStateOf {
            val total = categoryInputs.values.sumOf { it.toIntOrNull() ?: 0 }
            if (total == 0) 0.0 else {
                val sum = categoryInputs.entries.sumOf { (category, input) ->
                    val count = input.toIntOrNull() ?: 0
                    val weight = categoryWeights[category] ?: 0.0
                    count * weight
                }
                sum / total
            }
        }
    }

    val blackTextStyle = TextStyle(color = Color.Black, fontSize = 16.sp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Block (Filterable Dropdown) with validation
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
                label = { Text("Escriba o seleccione bloque", color = if (isBlockValid) Color.Black else Color.Red) },
                textStyle = blackTextStyle,
                isError = !isBlockValid,
                supportingText = {
                    if (!isBlockValid) {
                        Text("El bloque no existe en el cronograma", color = Color.Red)
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockMenuExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isBlockValid) Color.Black else Color.Red,
                    unfocusedBorderColor = if (isBlockValid) Color.Black else Color.Red,
                    errorBorderColor = Color.Red
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

        // Date Picker
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

        // Categories Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Conteo por Categoría",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                categories.forEachIndexed { index, category ->
                    val weight = categoryWeights[category] ?: 0.0
                    val inputVal = categoryInputs[category] ?: "0"
                    
                    CategoryCounterRow(
                        category = category,
                        weight = weight,
                        value = inputVal,
                        onValueChange = { newValue ->
                            categoryInputs[category] = newValue
                        },
                        onIncrement = {
                            val current = inputVal.toIntOrNull() ?: 0
                            categoryInputs[category] = (current + 1).toString()
                        },
                        onDecrement = {
                            val current = inputVal.toIntOrNull() ?: 0
                            if (current > 0) {
                                categoryInputs[category] = (current - 1).toString()
                            }
                        }
                    )
                    
                    if (index < categories.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // Progress toward 15 plants
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progreso del Lote (Meta: 15)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$totalCount / 15",
                        fontWeight = FontWeight.Bold,
                        color = if (totalCount >= 15) Color(0xFF2E7D32) else Color.Black,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (totalCount / 15f).coerceAtMost(1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (totalCount >= 15) Color(0xFF2E7D32) else Color.Black,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }
        }

        // Stats Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = DarkBeige),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Total Plantas",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        "$totalCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Card(
                modifier = Modifier.weight(2f),
                colors = CardDefaults.cardColors(containerColor = DarkBeige),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Media Ponderada",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        "${String.format(Locale.getDefault(), "%.1f", weightedAverage)} g",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Histogram Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Distribución (Histograma)",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                categories.forEach { category ->
                    val count = categoryInputs[category]?.toIntOrNull() ?: 0
                    val pct = if (totalCount == 0) 0f else count.toFloat() / totalCount
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, fontSize = 13.sp, color = Color.Black)
                            Text("$count (${(pct * 100).toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                        ) {
                            if (pct > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pct)
                                        .height(12.dp)
                                        .background(DarkBeige, shape = RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Batch Button
        Button(
            onClick = {
                val finalCounts = categoryInputs.mapValues { it.value.toIntOrNull() ?: 0 }
                viewModel.saveBatchSamplings(
                    batchBlock = viewModel.block,
                    batchDate = viewModel.samplingDate,
                    categoryCounts = finalCounts,
                    categoryWeights = categoryWeights,
                    onSuccess = { errorMsg ->
                        scope.launch {
                            if (errorMsg != null) {
                                snackbarHostState.showSnackbar(errorMsg)
                            } else {
                                snackbarHostState.showSnackbar("Lote guardado correctamente")
                                resetCategoryInputs()
                            }
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isBlockValid && viewModel.block.isNotEmpty() && totalCount > 0,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                disabledContainerColor = Color.Gray
            )
        ) {
            Text("GUARDAR LOTE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCounterRow(
    category: String,
    weight: Double,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 15.sp
            )
            Text(
                text = "${weight.toInt()} g",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = (value.toIntOrNull() ?: 0) > 0,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if ((value.toIntOrNull() ?: 0) > 0) DarkBeige else Color.LightGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                textStyle = TextStyle(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(70.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(36.dp)
                    .background(color = DarkBeige, shape = RoundedCornerShape(8.dp))
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
