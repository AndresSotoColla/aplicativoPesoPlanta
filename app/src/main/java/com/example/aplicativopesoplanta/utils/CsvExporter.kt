package com.example.aplicativopesoplanta.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.aplicativopesoplanta.data.SamplingEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportAndShare(context: Context, samplings: List<SamplingEntity>) {
        if (samplings.isEmpty()) return

        val csvHeader = "ID,Bloque,Fecha,Peso(g),Sistema Radicular,Fusarium,Meristemo,Hallazgos,Observaciones\n"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        val csvBody = StringBuilder()
        samplings.forEach {
            csvBody.append("${it.id},")
            csvBody.append("${it.block},")
            csvBody.append("${sdf.format(Date(it.date))},")
            csvBody.append("${it.weight},")
            csvBody.append("${it.rootSystem},")
            csvBody.append("${it.fusarium},")
            csvBody.append("${it.meristem},")
            csvBody.append("\"${it.findings}\",")
            csvBody.append("\"${it.observations}\"\n")
        }

        val fileName = "Muestreo_Peso_Planta_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csvHeader + csvBody.toString())

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Muestreos Peso Planta")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir CSV"))
    }
}
