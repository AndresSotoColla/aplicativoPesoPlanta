package com.example.aplicativopesoplanta.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplicativopesoplanta.data.AppDatabase
import com.example.aplicativopesoplanta.data.CachedBlockEntity
import com.example.aplicativopesoplanta.data.SamplingEntity
import com.example.aplicativopesoplanta.data.network.NetworkModule
import com.example.aplicativopesoplanta.utils.CsvExporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SamplingViewModel(context: Context) : ViewModel() {
    private val dao = AppDatabase.getDatabase(context).samplingDao()

    // Form State
    var block by mutableStateOf("")
    var samplingDate by mutableLongStateOf(System.currentTimeMillis())
    var weightInput by mutableStateOf("")
    var rootSystem by mutableStateOf("Normal")
    var fusarium by mutableStateOf(false)
    var meristem by mutableStateOf(false)
    var observations by mutableStateOf("")
    
    val selectedFindings = mutableStateListOf<String>("Ninguna")
    val findingOptions = listOf("Sinfilido", "Caracol", "Babosa", "Hormiga", "Cochinilla", "Ninguna")

    // Data from Database
    val samplings: StateFlow<List<SamplingEntity>> = dao.getAllSamplings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cached Blocks for Dropdown
    val availableBlocks: StateFlow<List<String>> = dao.getCachedBlocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically sync blocks on start
        syncBlocks()
    }

    fun syncBlocks() {
        viewModelScope.launch {
            try {
                val remoteCronograma = NetworkModule.apiService.getCronograma()
                if (remoteCronograma.isNotEmpty()) {
                    val entities = remoteCronograma.map { CachedBlockEntity(it.bloque) }
                    dao.clearCachedBlocks()
                    dao.insertBlocks(entities)
                }
            } catch (e: Exception) {
                // If network fails, we quietly keep using the cached blocks from DAO
                e.printStackTrace()
            }
        }
    }

    fun onFindingToggle(finding: String) {
        if (finding == "Ninguna") {
            selectedFindings.clear()
            selectedFindings.add("Ninguna")
        } else {
            selectedFindings.remove("Ninguna")
            if (selectedFindings.contains(finding)) {
                selectedFindings.remove(finding)
                if (selectedFindings.isEmpty()) selectedFindings.add("Ninguna")
            } else {
                selectedFindings.add(finding)
            }
        }
    }

    fun saveSampling(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val weight = weightInput.toDoubleOrNull() ?: 0.0
            val sampling = SamplingEntity(
                block = block,
                date = samplingDate,
                weight = weight,
                rootSystem = rootSystem,
                fusarium = fusarium,
                meristem = meristem,
                findings = selectedFindings.joinToString(", "),
                observations = observations
            )
            dao.insertSampling(sampling)
            clearForm()
            onSuccess()
        }
    }

    fun deleteSampling(sampling: SamplingEntity) {
        viewModelScope.launch {
            dao.deleteSampling(sampling)
        }
    }

    fun deleteAllSamplings() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    private fun clearForm() {
        block = ""
        samplingDate = System.currentTimeMillis()
        weightInput = ""
        rootSystem = "Normal"
        fusarium = false
        meristem = false
        observations = ""
        selectedFindings.clear()
        selectedFindings.add("Ninguna")
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val currentSamplings = samplings.value
            CsvExporter.exportAndShare(context, currentSamplings)
        }
    }
}
