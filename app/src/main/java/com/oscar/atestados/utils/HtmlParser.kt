package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.itextpdf.styledxmlparser.jsoup.Jsoup
import com.itextpdf.styledxmlparser.jsoup.nodes.Element
import com.itextpdf.styledxmlparser.jsoup.nodes.Node
import com.oscar.atestados.data.DocumentDataProvider
import com.oscar.atestados.utils.PDFA4Printer.DocumentElement
import java.io.File
import java.nio.charset.StandardCharsets

private const val TAG = "HtmlParser"

/**
 * Clase para analizar y procesar documentos HTML.
 *
 * Proporciona funcionalidades para:
 * - Generar archivos HTML a partir de plantillas y datos
 * - Reemplazar marcadores de posición en plantillas HTML
 * - Convertir elementos HTML a una estructura de datos DocumentElement
 * - Manejar firmas digitales y checkboxes en formularios
 *
 * @property context Contexto de Android para acceder a recursos y almacenamiento
 */
class HtmlParser(private val context: Context) {

    /**
     * Genera un archivo HTML procesando una plantilla con datos proporcionados.
     *
     * @param templateAssetPath Ruta de la plantilla HTML en los assets
     * @param dataProvider Proveedor de datos para reemplazar en la plantilla
     * @return Ruta absoluta del archivo HTML generado
     * @throws Exception Si ocurre algún error durante el procesamiento
     */
    fun generateHtmlFile(templateAssetPath: String, dataProvider: DocumentDataProvider): String {
        Log.d(TAG, "Iniciando generación de archivo HTML con plantilla: $templateAssetPath")
        try {
            val template = readTemplateFromAssets(templateAssetPath)
            Log.d(TAG, "Plantilla leída correctamente, tamaño: ${template.length} caracteres")

            val data = dataProvider.getData()
            Log.d(TAG, "Datos obtenidos del proveedor: $data")

            val modifiedHtml = replacePlaceholders(template, data)
            Log.d(TAG, "Plantilla modificada correctamente")
            Log.v(TAG, "HTML modificado (primeros 200 chars): ${modifiedHtml.take(200)}...")

            val filePath = writeHtmlToFile(modifiedHtml)
            Log.d(TAG, "Archivo HTML generado exitosamente en: $filePath")

            return filePath
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la generación del archivo HTML", e)
            throw Exception("Error al generar el archivo HTML: ${e.message}", e)
        }
    }

    /**
     * Lee una plantilla HTML desde los assets de la aplicación.
     *
     * @param templateAssetPath Ruta relativa de la plantilla en assets
     * @return Contenido de la plantilla como String
     * @throws Exception Si no se puede leer el archivo de plantilla
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
     * Reemplaza los marcadores de posición en una plantilla HTML con datos reales.
     *
     * Maneja diferentes tipos de marcadores:
     * - Checkboxes (terminados en _checkbox)
     * - Firmas digitales (comienzan con firma_)
     * - Spans normales (para texto simple)
     *
     * @param template Plantilla HTML como String
     * @param data Mapa de datos para reemplazar los marcadores
     * @return HTML procesado con los datos insertados
     */
    private fun replacePlaceholders(template: String, data: Map<String, String>): String {
        Log.d(TAG, "Iniciando reemplazo de marcadores en plantilla")
        val doc = Jsoup.parse(template)

        data.forEach { (key, value) ->
            try {
                when {
                    key.endsWith("_checkbox") -> {
                        Log.d(TAG, "Procesando checkbox: $key")
                        val isChecked = value == "checked"
                        val checkbox = doc.select("input[type=checkbox][id=$key]").first()
                        if (checkbox != null) {
                            if (isChecked) checkbox.attr("checked", "checked")
                            else checkbox.removeAttr("checked")
                            Log.d(TAG, "Checkbox actualizado: $key, Estado: $isChecked")
                        } else {
                            Log.w(TAG, "Checkbox no encontrado: $key")
                        }
                    }
                    key.startsWith("firma_") -> {
                        Log.d(TAG, "Procesando firma: $key, valor: ${value.take(50)}...")
                        val img = doc.select("img[id=$key]").first()
                        if (img != null) {
                            if (value.startsWith("data:image/png;base64,") && value.length > 50) {
                                img.attr("src", value)
                                Log.d(TAG, "Firma actualizada: $key con Base64")
                            } else {
                                Log.e(TAG, "Base64 inválido o vacío para $key: $value")
                                img.attr("src", "") // Evitar src inválido
                            }
                        } else {
                            Log.e(TAG, "Imagen no encontrada en plantilla: $key")
                        }
                    }
                    key == "op_1_checked" || key == "op_2_checked" -> {
                        Log.d(TAG, "Procesando span checkbox: $key")
                        val spans = doc.select("span[id=$key]")
                        spans.forEach { span ->
                            span.text(if (value == "true") "X" else "")
                            span.attr("data-checked", value)
                            span.addClass("underline")
                            Log.d(TAG, "Span checkbox actualizado: $key, Valor: ${span.text()}")
                        }
                    }
                    else -> {
                        Log.d(TAG, "Procesando span: $key")
                        val escapedValue = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        val spans = doc.select("span[id=$key]")
                        if (spans.isNotEmpty()) {
                            spans.forEach { span ->
                                span.text(escapedValue)
                                span.addClass("underline")
                                Log.d(TAG, "Span actualizado: $key, Valor: $escapedValue")
                            }
                        } else {
                            Log.w(TAG, "Span no encontrado: $key")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando marcador: $key", e)
            }
        }

        // Nuevo: Dividir párrafos largos con <br>
        splitLongParagraphs(doc)

        val outputHtml = doc.outerHtml()
        Log.d(TAG, "HTML generado: ${outputHtml.take(200)}...")
        // Guardar HTML para depuración
        File(context.cacheDir, "debug_temp.html").writeText(outputHtml, StandardCharsets.UTF_8)
        return outputHtml
    }

    private fun splitLongParagraphs(doc: com.itextpdf.styledxmlparser.jsoup.nodes.Document) {
        Log.d(TAG, "Iniciando marcado de párrafos divisibles")
        val paragraphs = doc.select("p")
        val maxWords = 50
        val maxChars = 200

        paragraphs.forEach { p ->
            val textContent = p.text().trim()
            if (textContent.isEmpty()) return@forEach

            val wordCount = textContent.split("\\s+".toRegex()).size
            if (wordCount > maxWords || textContent.length > maxChars) {
                p.attr("data-splitable", "true")
                Log.v(TAG, "Marcado párrafo como divisible: ${textContent.take(50)}...")
            }
        }
        Log.d(TAG, "Marcado de párrafos completado, párrafos procesados: ${paragraphs.size}")
    }

    /**
     * Escribe el contenido HTML en un archivo temporal en el directorio de caché.
     *
     * @param htmlContent Contenido HTML a escribir
     * @return Ruta absoluta del archivo creado
     * @throws Exception Si no se puede escribir el archivo
     */
    private fun writeHtmlToFile(htmlContent: String): String {
        Log.d(TAG, "Intentando escribir archivo HTML temporal")
        val cacheDir = context.cacheDir

        // Eliminar archivos HTML temporales previos
        cacheDir.listFiles()?.filter {
            it.name.startsWith("document_temp_") && it.extension == "html"
        }?.forEach { file ->
            try {
                if (file.delete()) {
                    Log.d(TAG, "Eliminado archivo HTML temporal antiguo: ${file.absolutePath}")
                } else {
                    Log.w(TAG, "No se pudo eliminar archivo HTML temporal antiguo: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar archivo HTML temporal antiguo: ${file.name}", e)
            }
        }

        // Crear nuevo archivo HTML temporal
        val tempFile = File(cacheDir, "document_temp_${System.currentTimeMillis()}.html")
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

    /**
     * Extrae elementos HTML estructurados de un documento HTML.
     *
     * @param htmlContent Contenido HTML a analizar
     * @return Lista de elementos del documento convertidos a DocumentElement
     */
    private fun extractHtmlElements(htmlContent: String): List<DocumentElement> {
        val doc = Jsoup.parse(htmlContent)
        return doc.body().children().map { convertJsoupElementToDocumentElement(it) }
    }

    /**
     * Convierte un elemento Jsoup a un DocumentElement estructurado.
     *
     * @param element Elemento Jsoup a convertir
     * @return DocumentElement con la estructura convertida
     */
    private fun convertJsoupElementToDocumentElement(element: Element?): DocumentElement {
        val type = when (element?.tagName()?.lowercase()) {
            "ul", "ol" -> "list"
            "li" -> "listItem"
            "p" -> "paragraph"
            "div" -> "div"
            "span" -> "span"
            "h1", "h2", "h3" -> "section"
            else -> "paragraph"
        }
        val attributes = element?.attributes()?.associate { it.key to it.value }?.toMutableMap() ?: mutableMapOf()
        if (element?.tagName()?.lowercase() == "ol") {
            attributes["ordered"] = "true"
        }
        val content = element?.ownText()
        val children = element?.children()?.map { convertJsoupElementToDocumentElement(it) }
        return DocumentElement(type, content.toString(), children, attributes)
    }
}