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
    
    // 15 slots for weights and categories
    val plantWeights = remember { mutableStateListOf<String>().apply { repeat(15) { add("") } } }
    val plantCategories = remember { mutableStateListOf<String>().apply { repeat(15) { add("") } } }

    fun checkIsRowDeviated(weightStr: String, category: String): Boolean {
        val w = weightStr.toDoubleOrNull() ?: return false
        if (category.isEmpty()) return false
        
        val (expectedAvg, maxSd) = when (category) {
            "Muy Pequeño" -> Pair(500.0, 150.0)
            "Pequeño" -> Pair(1000.0, 200.0)
            "Mediano" -> Pair(1500.0, 250.0)
            "Grande" -> Pair(2000.0, 300.0)
            "Muy Grande" -> Pair(2500.0, 400.0)
            else -> Pair(0.0, 0.0)
        }
        
        return Math.abs(w - expectedAvg) > maxSd
    }

    // Real-time Category Counts
    val categoryCounts by remember(plantCategories, plantWeights) {
        derivedStateOf {
            val counts = categories.associateWith { 0 }.toMutableMap()
            plantCategories.indices.forEach { idx ->
                val w = plantWeights[idx].toDoubleOrNull()
                val cat = plantCategories[idx]
                if (w != null && w > 0 && cat.isNotEmpty()) {
                    counts[cat] = (counts[cat] ?: 0) + 1
                }
            }
            counts
        }
    }

    // Statistics of the entered sample
    val sampleWeights by remember(plantWeights, plantCategories) {
        derivedStateOf {
            plantWeights.indices.mapNotNull { idx ->
                val w = plantWeights[idx].toDoubleOrNull()
                val cat = plantCategories[idx]
                if (w != null && w > 0 && cat.isNotEmpty()) w else null
            }
        }
    }

    val sampleCount by remember(sampleWeights) { derivedStateOf { sampleWeights.size } }
    val sampleAverage by remember(sampleWeights) { derivedStateOf { if (sampleWeights.isEmpty()) 0.0 else sampleWeights.average() } }
    val sampleStdDev by remember(sampleWeights, sampleAverage) {
        derivedStateOf {
            if (sampleWeights.size <= 1) 0.0 else {
                val sumSquares = sampleWeights.sumOf { (it - sampleAverage) * (it - sampleAverage) }
                Math.sqrt(sumSquares / (sampleWeights.size - 1))
            }
        }
    }

    val hasAnyDeviated by remember(plantWeights, plantCategories) {
        derivedStateOf {
            plantWeights.indices.any { idx ->
                checkIsRowDeviated(plantWeights[idx], plantCategories[idx])
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
        // Block Selector
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

        // 15 Plants Slots Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Registro de 15 Plantas",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
                
                for (i in 0 until 15) {
                    val isDeviated = checkIsRowDeviated(plantWeights[i], plantCategories[i])
                    PlantInputRow(
                        index = i,
                        weight = plantWeights[i],
                        onWeightChange = { plantWeights[i] = it },
                        category = plantCategories[i],
                        onCategoryChange = { plantCategories[i] = it },
                        categories = categories,
                        isDeviated = isDeviated
                    )
                    if (i < 14) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        // Meta Progress Card
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
                        text = "$sampleCount / 15",
                        fontWeight = FontWeight.Bold,
                        color = if (sampleCount >= 15) Color(0xFF2E7D32) else Color.Black,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (sampleCount / 15f).coerceAtMost(1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (sampleCount >= 15) Color(0xFF2E7D32) else Color.Black,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }
        }

        // Live stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkBeige),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        "$sampleCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = DarkBeige),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Promedio",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        "${String.format(Locale.getDefault(), "%.1f", sampleAverage)} g",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = DarkBeige),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Desviación",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        "${String.format(Locale.getDefault(), "%.1f", sampleStdDev)} g",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Deviation Alert Box
        if (hasAnyDeviated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Text(
                        text = "¡Alerta! Hay plantas con pesos muy desviados para su categoría.",
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Category Count Card
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
                
                categories.forEachIndexed { index, cat ->
                    val count = categoryCounts[cat] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat, fontSize = 14.sp, color = Color.Black)
                        Text("$count", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    }
                    if (index < categories.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        )
                    }
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
                    val count = categoryCounts[category] ?: 0
                    val pct = if (sampleCount == 0) 0f else count.toFloat() / sampleCount
                    
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
                val weights = plantWeights.map { it.toDoubleOrNull() }
                val finalCategories = plantCategories.toList()
                viewModel.saveBatchSamplingsList(
                    batchBlock = viewModel.block,
                    batchDate = viewModel.samplingDate,
                    plantWeights = weights,
                    plantCategories = finalCategories,
                    onSuccess = { errorMsg ->
                        scope.launch {
                            if (errorMsg != null) {
                                snackbarHostState.showSnackbar(errorMsg)
                            } else {
                                snackbarHostState.showSnackbar("Lote de $sampleCount plantas guardado correctamente")
                                // Clear the lists
                                plantWeights.indices.forEach { idx ->
                                    plantWeights[idx] = ""
                                    plantCategories[idx] = ""
                                }
                            }
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isBlockValid && viewModel.block.isNotEmpty() && sampleCount > 0,
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
fun PlantInputRow(
    index: Int,
    weight: String,
    onWeightChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    categories: List<String>,
    isDeviated: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label (e.g. "01")
        Text(
            text = String.format(Locale.getDefault(), "%02d", index + 1),
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontSize = 14.sp,
            modifier = Modifier.width(24.dp)
        )

        // Weight Input Field
        OutlinedTextField(
            value = weight,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                    onWeightChange(newValue)
                }
            },
            label = { Text("Peso (g)", fontSize = 11.sp, color = Color.Gray) },
            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1.2f).height(52.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.LightGray
            )
        )

        // Category Selector Box
        Box(
            modifier = Modifier
                .weight(1.5f)
                .height(52.dp)
        ) {
            OutlinedButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 8.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.LightGray)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.ifEmpty { "Categoría" },
                        fontSize = 12.sp,
                        color = if (category.isEmpty()) Color.Gray else Color.Black
                    )
                    Text("▼", fontSize = 8.sp, color = Color.Gray)
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                categories.forEach { catOption ->
                    DropdownMenuItem(
                        text = { Text(catOption, color = Color.Black, fontSize = 13.sp) },
                        onClick = {
                            onCategoryChange(catOption)
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        // Deviation Alert Column
        Box(
            modifier = Modifier.width(85.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isDeviated) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("⚠️", fontSize = 12.sp)
                    Text(
                        text = "Muy desviado",
                        color = Color.Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
