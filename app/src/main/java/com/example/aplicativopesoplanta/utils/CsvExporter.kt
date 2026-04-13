package com.example.aplicativopesoplanta.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.example.aplicativopesoplanta.data.SamplingEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportAndShare(context: Context, samplings: List<SamplingEntity>) {
        if (samplings.isEmpty()) return

        // UTF-8 BOM for Excel compatibility
        val bom = "\uFEFF"
        val csvHeader = "bloque,peso,sistema_radicular,fusarium,fecha,fecha_envio,meristemo,hallazgos,observaciones,fecha_muestreo,usuario\n"
        
        val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sdfShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = sdfFull.format(Date())
        val deviceUser = "${Build.MANUFACTURER} ${Build.MODEL}"
        
        val csvBody = StringBuilder()
        samplings.forEach {
            csvBody.append("${it.block},")
            csvBody.append("${it.weight},")
            csvBody.append("${it.rootSystem},")
            csvBody.append("${if (it.fusarium) "Si" else "No"},")
            csvBody.append("${sdfFull.format(Date(it.date))},")
            csvBody.append("$now,") // fecha_envio (current export time)
            csvBody.append("${if (it.meristem) "Si" else "No"},")
            csvBody.append("\"${it.findings}\",")
            csvBody.append("\"${it.observations}\",")
            csvBody.append("${sdfShort.format(Date(it.date))},")
            csvBody.append("$deviceUser\n")
        }

        val fileName = "Muestreo_Peso_Planta_${System.currentTimeMillis()}.csv"
        // We still use .csv but with BOM and these columns it opens perfectly in Excel
        val file = File(context.cacheDir, fileName)
        file.writeText(bom + csvHeader + csvBody.toString())

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

        context.startActivity(Intent.createChooser(intent, "Compartir Reporte"))
    }
}
