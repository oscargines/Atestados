package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.oscar.atestados.data.DocumentDataProvider
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Parser para generar archivos HTML a partir de plantillas y datos dinámicos.
 *
 * Esta clase permite cargar plantillas HTML desde los assets de la aplicación,
 * reemplazar contenido dinámico en elementos span identificados por su ID,
 * y guardar el resultado como un archivo HTML temporal.
 *
 * @property context Contexto de la aplicación, necesario para acceder a los assets y al directorio de caché.
 */
class HtmlParser(private val context: Context) {

    /**
     * Genera un archivo HTML a partir de una plantilla y datos dinámicos.
     *
     * @param templateAssetPath Ruta del archivo de plantilla HTML dentro de los assets.
     * @param dataProvider Proveedor de datos que contiene los valores a insertar en la plantilla.
     * @param outputFileName Nombre del archivo de salida (por defecto usa un nombre temporal con timestamp).
     * @return Ruta absoluta del archivo HTML generado.
     * @throws Exception Si ocurre algún error durante la lectura de la plantilla o escritura del archivo.
     */
    fun generateHtmlFile(
        templateAssetPath: String,
        dataProvider: DocumentDataProvider,
        outputFileName: String = "document_temp_${System.currentTimeMillis()}.html"
    ): String {
        try {
            // Leer la plantilla HTML desde assets
            val htmlContent = context.assets.open(templateAssetPath)
                .use { it.readBytes().toString(StandardCharsets.UTF_8) }
            Log.d("HtmlParser", "Contenido original de la plantilla:\n$htmlContent")

            // Obtener datos del proveedor
            val data = dataProvider.getData()
            Log.d("HtmlParser", "Datos recibidos: ${data.entries.joinToString("\n") { "${it.key}=${it.value}" }}")

            // Sustituir el contenido de los <span> según su id
            var modifiedHtml = htmlContent
            data.forEach { (spanId, value) ->
                val before = modifiedHtml
                modifiedHtml = modifiedHtml.replaceSpanText(spanId, value)
                if (before == modifiedHtml) {
                    Log.w("HtmlParser", "Fallo al reemplazar spanId=$spanId con valor=$value")
                } else {
                    Log.d("HtmlParser", "Reemplazado spanId=$spanId con valor=$value")
                }
            }
            Log.d("HtmlParser", "HTML modificado:\n$modifiedHtml")

            // Crear un archivo temporal en el directorio de caché
            val tempFile = File(context.cacheDir, outputFileName)
            tempFile.writeText(modifiedHtml, StandardCharsets.UTF_8)

            Log.d("HtmlParser", "Archivo HTML temporal generado en: ${tempFile.absolutePath}")
            return tempFile.absolutePath
        } catch (e: Exception) {
            Log.e("HtmlParser", "Error al generar archivo HTML: ${e.message}", e)
            throw e
        }
    }

    /**
     * Reemplaza el texto dentro de un elemento span identificado por su ID.
     *
     * @param spanId ID del elemento span a modificar.
     * @param newText Nuevo texto a insertar en el span.
     * @return Cadena HTML con el contenido del span modificado.
     */
    private fun String.replaceSpanText(spanId: String, newText: String): String {
        val regex = Regex("<span\\s+[^>]*id\\s*=\\s*\"$spanId\"[^>]*>(.*?)</span>", RegexOption.IGNORE_CASE)
        val replaced = replace(regex) { matchResult ->
            "<span id=\"$spanId\" class=\"${matchResult.groupValues[0].substringAfter("class=\"").substringBefore("\"")}\">$newText</span>"
        }
        Log.d("HtmlParser", "Reemplazo para spanId=$spanId: ${if (replaced != this) "Éxito" else "Fallo"}")
        return replaced
    }
}