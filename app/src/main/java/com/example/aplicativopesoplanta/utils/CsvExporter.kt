package com.example.aplicativopesoplanta.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.aplicativopesoplanta.data.SamplingEntity
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportAndShare(context: Context, samplings: List<SamplingEntity>) {
        if (samplings.isEmpty()) return

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
            csvBody.append("$now,")
            csvBody.append("${if (it.meristem) "Si" else "No"},")
            csvBody.append("\"${it.findings}\",")
            csvBody.append("\"${it.observations}\",")
            csvBody.append("${sdfShort.format(Date(it.date))},")
            csvBody.append("$deviceUser\n")
        }

        val fileName = "Muestreo_Peso_Planta_${System.currentTimeMillis()}.csv"
        val content = bom + csvHeader + csvBody.toString()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        outputStream?.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(context, "Error al crear el archivo", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Legacy support for older Android (using public directory)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                Toast.makeText(context, "Guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
