package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.itextpdf.styledxmlparser.jsoup.Jsoup
import com.oscar.atestados.data.DocumentDataProvider
import java.io.File
import java.nio.charset.StandardCharsets

private const val TAG = "HtmlParser"

/**
 * Clase utilitaria para analizar plantillas HTML y reemplazar marcadores con valores reales.
 *
 * Esta clase proporciona funcionalidad para:
 * - Leer plantillas HTML desde los assets
 * - Reemplazar elementos marcadores con datos dinámicos
 * - Generar archivos HTML temporales con el contenido procesado
 *
 * @property context El contexto de Android utilizado para acceder a assets y directorio cache
 */
class HtmlParser(private val context: Context) {

    /**
     * Genera un archivo HTML procesando una plantilla con datos dinámicos.
     *
     * @param templateAssetPath Ruta al archivo plantilla HTML en assets
     * @param dataProvider Proveedor de datos dinámicos para reemplazar marcadores
     * @return Ruta absoluta al archivo HTML generado
     * @throws Exception si falla la lectura de la plantilla o escritura del archivo
     */
    fun generateHtmlFile(templateAssetPath: String, dataProvider: DocumentDataProvider): String {
        Log.d(TAG, "Iniciando generación de archivo HTML con plantilla: $templateAssetPath")
        try {
            val template = readTemplateFromAssets(templateAssetPath)
            Log.d(TAG, "Plantilla leída correctamente, tamaño: ${template.length} caracteres")

            val data = dataProvider.getData()
            Log.d(TAG, "Datos obtenidos del proveedor, cantidad de elementos: ${data.size}")

            val modifiedHtml = replacePlaceholders(template, data)
            Log.d(TAG, "Plantilla modificada correctamente")

            val filePath = writeHtmlToFile(modifiedHtml)
            Log.d(TAG, "Archivo HTML generado exitosamente en: $filePath")

            return filePath
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la generación del archivo HTML", e)
            throw Exception("Error al generar el archivo HTML: ${e.message}", e)
        }
    }

    /**
     * Lee una plantilla HTML desde la carpeta de assets.
     *
     * @param templateAssetPath Ruta al archivo plantilla en assets
     * @return Contenido del archivo plantilla como String
     * @throws Exception si no se puede leer el archivo
     */
    private fun readTemplateFromAssets(templateAssetPath: String): String {
        Log.d(TAG, "Intentando leer plantilla desde: $templateAssetPath")
        return try {
            context.assets.open(templateAssetPath).use { inputStream ->
                val content = inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
                Log.d(TAG, "Plantilla leída exitosamente desde assets")
                content
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer plantilla desde assets: $templateAssetPath", e)
            throw Exception("No se pudo leer la plantilla HTML: ${e.message}", e)
        }
    }

    /**
     * Reemplaza marcadores en la plantilla HTML con valores reales del mapa de datos.
     *
     * Maneja dos tipos de marcadores:
     * 1. Inputs de tipo checkbox (elementos con type="checkbox")
     * 2. Elementos span (elementos con atributos id)
     *
     * @param template Contenido de la plantilla HTML
     * @param data Mapa de claves de marcadores a sus valores de reemplazo
     * @return HTML modificado con los marcadores reemplazados
     */
    private fun replacePlaceholders(template: String, data: Map<String, String>): String {
        Log.d(TAG, "Iniciando reemplazo de marcadores en plantilla")
        val doc = Jsoup.parse(template)

        data.forEach { (key, value) ->
            try {
                if (key.endsWith("_checkbox")) {
                    Log.d(TAG, "Procesando checkbox con key: $key")
                    val isChecked = value == "checked"
                    val checkbox = doc.select("input[type=checkbox][id=$key]").first()
                    if (checkbox != null) {
                        if (isChecked) checkbox.attr("checked", "checked")
                        else checkbox.removeAttr("checked")
                        Log.d(TAG, "Checkbox reemplazado - ID: $key, Estado: ${if (isChecked) "marcado" else "no marcado"}")
                    } else {
                        Log.w(TAG, "No se encontró el checkbox con ID: $key en la plantilla")
                    }
                } else if (key.startsWith("firma_")) {
                    Log.d(TAG, "Procesando imagen con key: $key")
                    val img = doc.select("img[id=$key]").first()
                    if (img != null) {
                        img.attr("src", value) // Asume que 'value' es la ruta de la imagen
                        Log.d(TAG, "Imagen reemplazada - ID: $key, Valor: $value")
                    } else {
                        Log.w(TAG, "No se encontró la imagen con ID: $key en la plantilla")
                    }
                } else {
                    Log.d(TAG, "Procesando span con key: $key")
                    val escapedValue = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    val spans = doc.select("span[id=$key]")
                    if (spans.isNotEmpty()) {
                        spans.forEach { span ->
                            span.text(escapedValue)
                            span.addClass("underline")
                            Log.d(TAG, "Span reemplazado - ID: $key, Valor: $escapedValue")
                        }
                    } else {
                        Log.w(TAG, "No se encontró el span con ID: $key en la plantilla")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar marcador con key: $key", e)
            }
        }

        Log.d(TAG, "Reemplazo de marcadores completado")
        return doc.outerHtml()
    }

    /**
     * Escribe el contenido HTML en un archivo temporal.
     *
     * @param htmlContent Contenido HTML a escribir
     * @return Ruta absoluta al archivo temporal generado
     * @throws Exception si no se puede escribir el archivo
     */
    private fun writeHtmlToFile(htmlContent: String): String {
        Log.d(TAG, "Intentando escribir archivo HTML temporal")
        val tempFile = File(context.cacheDir, "document_temp_${System.currentTimeMillis()}.html")

        try {
            tempFile.writeText(htmlContent, StandardCharsets.UTF_8)
            Log.d(TAG, "Archivo HTML escrito exitosamente. Tamaño: ${htmlContent.length} caracteres")
            Log.d(TAG, "Ubicación del archivo: ${tempFile.absolutePath}")
            return tempFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir archivo HTML en: ${tempFile.absolutePath}", e)
            throw Exception("No se pudo escribir el archivo HTML: ${e.message}", e)
        }
    }
}