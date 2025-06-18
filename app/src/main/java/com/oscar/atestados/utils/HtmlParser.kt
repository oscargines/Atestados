package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.oscar.atestados.data.DocumentDataProvider
import java.io.File

class HtmlParser(private val context: Context) {
    private val TAG = "HtmlParser"

    fun generateHtmlFile(templatePath: String, dataProvider: DocumentDataProvider): String {
        Log.d(TAG, "Generando archivo HTML para plantilla: $templatePath")
        val htmlContent = context.assets.open(templatePath).bufferedReader().use { it.readText() }
        val modifiedHtml = if (templatePath.endsWith("ah04.html")) {
            htmlContent
        } else {
            replacePlaceholders(htmlContent, dataProvider.getData())
        }
        val outputFile = File(context.cacheDir, "temp_${templatePath.replace("/", "_")}_${System.currentTimeMillis()}.html")
        outputFile.writeText(modifiedHtml)
        Log.d(TAG, "Archivo HTML generado: ${outputFile.absolutePath}")
        return outputFile.absolutePath
    }

    private fun replacePlaceholders(htmlContent: String, data: Map<String, String>): String {
        var result = htmlContent

        // Mapear motivo desde opcionMotivo
        val opcionMotivo = data["opcionMotivo"]?.lowercase() ?: ""
        val motivo = when (opcionMotivo) {
            "accidente" -> "Implicado en accidente de circulación"
            "sintomas" -> "Conducción con síntomas de influencia de bebidas alcohólicas"
            "infraccion" -> "Comisión de infracción a las normas de circulación"
            "control", "control preventivo" -> "Control preventivo de alcoholemia"
            else -> "No especificado"
        }

        // Derivar campos compuestos
        val lugarInvestigacion = "${data["lugar_investigacion"] ?: ""}, ${data["momento_lectura"] ?: ""}"
        val lugarDelito = "${data["lugar_delito"] ?: ""}, ${data["fecha_diligencia"] ?: ""} ${data["hora_diligencia"] ?: ""}"

        // Datos con valores predeterminados y aliases
        val completeData = data.toMutableMap().apply {
            putIfAbsent("motivo", motivo)
            putIfAbsent("fecha_diligencia_2", data["fecha_diligencia"] ?: "")
            putIfAbsent("hora_diligencia_3", data["hora_diligencia"] ?: "")
            putIfAbsent("serie_etilometro", data["num_serie_eti"] ?: "")
            putIfAbsent("termino_minicipal", data["termino_municipal"] ?: "") // Corregir error tipográfico
            putIfAbsent("hora_final", data["momento_lectura"] ?: data["hora_diligencia"] ?: "")
        }

        completeData.forEach { (key, value) ->
            when {
                key.startsWith("firma_") -> {
                    // Reemplazar firmas
                    result = result.replace(
                        """<img id="$key" src="">""",
                        """<img id="$key" src="$value">"""
                    )
                }
                key.startsWith("boolean_") -> {
                    // Reemplazar checkboxes
                    val checked = value.toBoolean() || value.uppercase() in listOf("SÍ", "SI")
                    result = result.replace(
                        """<span id="$key"></span>""",
                        """<span id="${key}_checkbox" data-checked="$checked"></span>"""
                    )
                }
                else -> {
                    // Reemplazar texto
                    result = result.replace("""<span id="$key"></span>""", value)
                }
            }
        }

        return result
    }
}