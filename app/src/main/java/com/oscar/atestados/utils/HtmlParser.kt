package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.oscar.atestados.viewModel.CitacionViewModel
import java.io.File
import java.nio.charset.StandardCharsets

class HtmlParser(private val context: Context) {

    /**
     * Genera un archivo HTML temporal basado en una plantilla y datos del CitacionViewModel.
     * @param templateAssetPath Ruta del archivo HTML en assets (ej. "documents/acta_citacion.html").
     * @param citacionViewModel ViewModel con los datos a insertar en el HTML.
     * @return Ruta absoluta del archivo temporal generado.
     */
    fun generateHtmlFile(templateAssetPath: String, citacionViewModel: CitacionViewModel): String {
        try {
            // Leer la plantilla HTML desde assets
            val htmlContent = context.assets.open(templateAssetPath)
                .use { it.readBytes().toString(StandardCharsets.UTF_8) }

            // Obtener datos del CitacionViewModel
            val provincia = citacionViewModel.provincia.value ?: ""
            val localidad = citacionViewModel.localidad.value ?: ""
            val juzgado = citacionViewModel.juzgado.value ?: ""
            val fechaInicio = citacionViewModel.fechaInicio.value ?: ""
            val hora = citacionViewModel.hora.value ?: ""
            val numeroDiligencias = citacionViewModel.numeroDiligencias.value ?: ""

            // Sustituir el contenido de los <span> según su id
            var modifiedHtml = htmlContent
            modifiedHtml = modifiedHtml.replaceSpanText("provincia", provincia)
            modifiedHtml = modifiedHtml.replaceSpanText("localidad", localidad)
            modifiedHtml = modifiedHtml.replaceSpanText("juzgado", juzgado)
            modifiedHtml = modifiedHtml.replaceSpanText("fechaInicio", fechaInicio)
            modifiedHtml = modifiedHtml.replaceSpanText("hora", hora)
            modifiedHtml = modifiedHtml.replaceSpanText("numeroDiligencias", numeroDiligencias)

            // Crear un archivo temporal en el directorio de caché
            val tempFile = File(context.cacheDir, "acta_citacion_temp_${System.currentTimeMillis()}.html")
            tempFile.writeText(modifiedHtml, StandardCharsets.UTF_8)

            Log.d("HtmlParser", "Archivo HTML temporal generado en: ${tempFile.absolutePath}")
            return tempFile.absolutePath
        } catch (e: Exception) {
            Log.e("HtmlParser", "Error al generar archivo HTML: ${e.message}", e)
            throw e
        }
    }

    /**
     * Reemplaza el texto dentro de un <span> según su id.
     * @param spanId ID del <span> a modificar.
     * @param newText Texto a insertar dentro del <span>.
     * @return String con el HTML modificado.
     */
    private fun String.replaceSpanText(spanId: String, newText: String): String {
        val regex = Regex("<span\\s+id=\"$spanId\"[^>]*>(.*?)</span>")
        return replace(regex) { matchResult ->
            "<span id=\"$spanId\">$newText</span>"
        }
    }
}