package com.example.aplicativopesoplanta.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aplicativopesoplanta.data.AppDatabase
import com.example.aplicativopesoplanta.data.SamplingEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SamplingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.samplingDao()

    val samplings: StateFlow<List<SamplingEntity>> = dao.getAllSamplings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form State
    var block by mutableStateOf("")
    var samplingDate by mutableLongStateOf(System.currentTimeMillis())
    var weightInput by mutableStateOf("")
    var rootSystem by mutableStateOf("Normal")
    var fusarium by mutableStateOf(false)
    var meristem by mutableStateOf(false)
    var selectedFindings by mutableStateOf(setOf("Ninguna"))
    var observations by mutableStateOf("")

    val findingOptions = listOf("Sinfilido", "Caracol", "Babosa", "Hormiga", "Cochinilla", "Ninguna")

    fun onFindingToggle(finding: String) {
        val current = selectedFindings.toMutableSet()
        if (finding == "Ninguna") {
            current.clear()
            current.add("Ninguna")
        } else {
            if (current.contains(finding)) {
                current.remove(finding)
                if (current.isEmpty()) current.add("Ninguna")
            } else {
                current.remove("Ninguna")
                current.add(finding)
            }
        }
        selectedFindings = current
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
        selectedFindings = setOf("Ninguna")
        observations = ""
    }

    fun exportToCSV(context: Context) {
        val data = samplings.value
        if (data.isEmpty()) return

        val fileName = "muestreos_peso_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        try {
            file.writer().use { writer ->
                writer.write("ID,Bloque,Fecha,Peso (g),Sistema Radicular,Fusarium,Meristemo,Hallazgos,Observaciones\n")
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                data.forEach { s ->
                    writer.write("${s.id},${s.block},${sdf.format(Date(s.date))},${s.weight},${s.rootSystem},${s.fusarium},${s.meristem},\"${s.findings}\",\"${s.observations}\"\n")
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Exportación de Muestreos")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Descargar Información"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
