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
    private val prefs = context.getSharedPreferences("sampling_prefs", Context.MODE_PRIVATE)

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
    var block by mutableStateOf(prefs.getString("block", "") ?: "")
    var samplingDate by mutableLongStateOf(System.currentTimeMillis())
    var weightInput by mutableStateOf(prefs.getString("weight", "") ?: "")
    var rootSystem by mutableStateOf(prefs.getString("rootSystem", "Normal") ?: "Normal")
    var fusarium by mutableStateOf(prefs.getBoolean("fusarium", false))
    var meristem by mutableStateOf(prefs.getBoolean("meristem", false))
    var observations by mutableStateOf(prefs.getString("observations", "") ?: "")
    
    val selectedFindings = mutableStateListOf<String>()
    val findingOptions = listOf("Sinfilido", "Caracol", "Babosa", "Hormiga", "Cochinilla", "Ninguna")

    // Data from Database
    private val _samplings = dao.getAllSamplings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val samplings: StateFlow<List<SamplingEntity>> = _samplings
    var samplingsList by mutableStateOf<List<SamplingEntity>>(emptyList())

    val availableBlocks: StateFlow<List<String>> = dao.getCachedBlocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics (Derived from samplingsList, block, and date)
    val todayBlockStats by derivedStateOf {
        val currentBlock = block
        val currentDate = samplingDate
        if (currentBlock.isEmpty()) return@derivedStateOf Pair(0, 0.0)

        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        val filtered = samplingsList.filter {
            val sCalendar = Calendar.getInstance().apply { timeInMillis = it.date }
            it.block == currentBlock && 
            sCalendar.get(Calendar.DAY_OF_YEAR) == day &&
            sCalendar.get(Calendar.YEAR) == year
        }

        val count = filtered.size
        val avg = if (count > 0) filtered.map { it.weight }.average() else 0.0
        Pair(count, avg)
    }

    init {
        // Load findings from prefs
        val savedFindings = prefs.getStringSet("findings", setOf("Ninguna")) ?: setOf("Ninguna")
        selectedFindings.addAll(savedFindings)
        
        viewModelScope.launch {
            _samplings.collect {
                samplingsList = it
            }
        }
        
        syncBlocks()
    }

    private fun saveFormToPrefs() {
        prefs.edit().apply {
            putString("block", block)
            putString("weight", weightInput)
            putString("rootSystem", rootSystem)
            putBoolean("fusarium", fusarium)
            putBoolean("meristem", meristem)
            putString("observations", observations)
            putStringSet("findings", selectedFindings.toSet())
            apply()
        }
    }

    // Wrap state updates to auto-save
    fun updateBlock(value: String) { block = value; saveFormToPrefs() }
    fun updateWeight(value: String) { weightInput = value; saveFormToPrefs() }
    fun updateRootSystem(value: String) { rootSystem = value; saveFormToPrefs() }
    fun updateFusarium(value: Boolean) { fusarium = value; saveFormToPrefs() }
    fun updateMeristem(value: Boolean) { meristem = value; saveFormToPrefs() }
    fun updateObservations(value: String) { observations = value; saveFormToPrefs() }

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
                    fecha_envio = sdf.format(Date()),
                    meristemo = if (sampling.meristem) "Si" else "No",
                    hallazgos = sampling.findings,
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
        saveFormToPrefs()
    }

    fun saveSampling(onSuccess: (String?) -> Unit) {
        if (!availableBlocks.value.contains(block)) {
            onSuccess("El bloque no existe en el cronograma")
            return
        }

        val weight = weightInput.toDoubleOrNull()
        
        if (weight == null || weight <= 0) {
            onSuccess("Por favor ingrese un peso válido")
            return
        }
        
        if (weight > 4000) {
            onSuccess("El peso no puede superar los 4000g")
            return
        }

        viewModelScope.launch {
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
            onSuccess(null)
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
        // User wants to KEEP the block but clear everything else
        weightInput = ""
        samplingDate = System.currentTimeMillis()
        rootSystem = "Normal"
        fusarium = false
        meristem = false
        observations = ""
        selectedFindings.clear()
        selectedFindings.add("Ninguna")
        saveFormToPrefs()
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val currentSamplings = samplings.value
            CsvExporter.exportAndShare(context, currentSamplings)
        }
    }
}
