package com.example.aplicativopesoplanta.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.example.aplicativopesoplanta.data.AppDatabase
import com.example.aplicativopesoplanta.data.CachedBlockEntity
import com.example.aplicativopesoplanta.data.SamplingEntity
import com.example.aplicativopesoplanta.data.network.NetworkModule
import com.example.aplicativopesoplanta.data.network.SamplingUploadRequest
import com.example.aplicativopesoplanta.utils.CsvExporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SamplingViewModel(context: Context) : ViewModel() {
    private val dao = AppDatabase.getDatabase(context).samplingDao()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return SamplingViewModel(application) as T
            }
        }
    }

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

    val availableBlocks: StateFlow<List<String>> = dao.getCachedBlocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
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
                e.printStackTrace()
            }
        }
    }

    fun uploadSampling(sampling: SamplingEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val sdfShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                val request = SamplingUploadRequest(
                    bloque = sampling.block,
                    peso = sampling.weight,
                    sistemaRadicular = sampling.rootSystem,
                    fusarium = if (sampling.fusarium) "Si" else "No",
                    fecha = sdf.format(Date(sampling.date)),
                    fecha_envio = sdf.format(Date()), // Actual time
                    meristemo = if (sampling.meristem) "Si" else "No",
                    hallagzos = sampling.findings, // JSON key hallazgos from snippet
                    Observaciones = sampling.observations,
                    fecha_muestreo = sdfShort.format(Date(sampling.date)),
                    usuario = "${Build.MANUFACTURER} ${Build.MODEL}"
                )

                val response = NetworkModule.apiService.uploadSampling(request)
                if (response.isSuccessful) {
                    dao.updateSyncStatus(sampling.id, true)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun uploadAllSamplings() {
        viewModelScope.launch {
            samplings.value.filter { !it.isSynced }.forEach { sampling ->
                uploadSampling(sampling)
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
