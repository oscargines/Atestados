package com.oscar.atestados.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.itextpdf.kernel.xmp.PdfConst.Date
import com.oscar.atestados.data.AlcoholemiaDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AtestadoPrinter"

/**
 * Clase para generar y unir los PDFs del atestado de alcoholemia a partir de plantillas HTML.
 *
 * @param context Contexto de Android para acceder a assets y directorio de caché.
 * @param dataProvider Proveedor de datos para rellenar las plantillas HTML.
 * @param useZebraPrinter Si es true, genera PDFs en formato Zebra; si es false, usa formato A4.
 */
class AtestadoPrinter(
    private val context: Context,
    private val dataProvider: AlcoholemiaDataProvider,
    private val useZebraPrinter: Boolean = false
) {
    /**
     * Genera los PDFs a partir de las plantillas HTML (ah01.html a ah06.html) y los une en un solo archivo,
     * guardándolo en Documents/Atestados usando MediaStore.
     *
     * @param baseFileName Nombre base del archivo PDF (ej. "atestado"). Se añadirá una marca de tiempo.
     * @return Ruta absoluta del archivo PDF generado, o null si falla.
     * @throws Exception si falla la generación, unión o escritura del PDF.
     */
    suspend fun generateAtestado(baseFileName: String): String? {
        Log.d(TAG, "Iniciando generación del atestado con nombre base: $baseFileName")

        // Validar datos antes de generar los PDFs
        val (isValid, missingFields) = dataProvider.validateData()
        if (!isValid) {
            val errorMessage = "Faltan campos obligatorios: ${missingFields.joinToString(", ")}"
            Log.e(TAG, errorMessage)
            throw IllegalStateException(errorMessage)
        }

        // Lista de plantillas HTML a procesar
        val templateNames = (1..6).map { "templates/ah0$it.html" }
        val tempFiles = mutableListOf<File>()

        return withContext(Dispatchers.IO) {
            try {
                // Instanciar las utilidades necesarias
                val htmlParser = HtmlParser(context)
                val pdfPrinter: PDFPrinter = if (useZebraPrinter) {
                    ZebraPrinterAdapter(PDFLabelPrinterZebra(context))
                } else {
                    A4PrinterAdapter(PDFA4Printer(context))
                }
                val pdfMerger = PDFMerger()

                // Generar un PDF por cada plantilla
                templateNames.forEachIndexed { index, templatePath ->
                    Log.d(TAG, "Procesando plantilla: $templatePath")

                    // Generar archivo HTML temporal
                    val htmlFilePath = htmlParser.generateHtmlFile(templatePath, dataProvider)
                    val htmlFile = File(htmlFilePath)
                    if (!htmlFile.exists()) {
                        Log.e(TAG, "Archivo HTML no generado: $htmlFilePath")
                        throw IllegalStateException("Error al generar archivo HTML para $templatePath")
                    }

                    // Generar archivo PDF temporal
                    val tempPdfFile = File(context.cacheDir, "temp_ah0${index + 1}_${System.currentTimeMillis()}.pdf")
                    pdfPrinter.generarDocumento(htmlFile.readText(), tempPdfFile)

                    if (!tempPdfFile.exists()) {
                        Log.e(TAG, "Archivo PDF no generado: ${tempPdfFile.absolutePath}")
                        throw IllegalStateException("Error al generar PDF para $templatePath")
                    }

                    tempFiles.add(tempPdfFile)
                    Log.d(TAG, "PDF generado: ${tempPdfFile.absolutePath}")
                }

                // Unir los PDFs generados
                if (tempFiles.isEmpty()) {
                    Log.e(TAG, "No se generaron archivos PDF")
                    throw IllegalStateException("No se generaron archivos PDF")
                }

                // Usar el primer PDF como base y unir los demás
                val principalFile = tempFiles.first()
                tempFiles.drop(1).forEach { secondaryFile ->
                    pdfMerger.unirPDFs(principalFile, secondaryFile)
                }

                // Guardar el PDF final usando PdfUtils
                val pdfA4Printer = PDFA4Printer(context) // Necesario para PdfUtils.writePdfToStorage
                val finalHtml = "<p>Temporal content for MediaStore</p>" // No se usa realmente, pero es requerido por la API
                val finalFile = PdfUtils.writePdfToStorage(finalHtml, baseFileName, pdfA4Printer, context) {
                    // Proveer el contenido del PDF unido
                    principalFile.inputStream()
                }

                if (finalFile != null && finalFile.exists()) {
                    Log.d(TAG, "Atestado final guardado en: ${finalFile.absolutePath}")
                } else {
                    Log.e(TAG, "No se pudo guardar el atestado final")
                    throw IllegalStateException("Error al guardar el atestado final")
                }

                // Limpiar archivos temporales
                tempFiles.forEach { it.delete() }
                Log.d(TAG, "Archivos temporales eliminados")

                finalFile?.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error al generar el atestado: ${e.message}", e)
                // Limpiar archivos temporales en caso de error
                tempFiles.forEach { it.delete() }
                throw e
            }
        }
    }
}

/**
 * Adaptador para PDFA4Printer que implementa la interfaz PDFPrinter.
 */
class A4PrinterAdapter(private val printer: PDFA4Printer) : PDFPrinter {
    override fun generarDocumento(htmlContent: String, outputFile: File) {
        printer.generarDocumentoA4(htmlContent, outputFile)
    }
}

/**
 * Adaptador para PDFLabelPrinterZebra que implementa la interfaz PDFPrinter.
 */
class ZebraPrinterAdapter(private val printer: PDFLabelPrinterZebra) : PDFPrinter {
    override fun generarDocumento(htmlContent: String, outputFile: File) {
        printer.generarEtiquetaPdf(htmlContent, outputFile)
    }
}

/**
 * Extensión para PdfUtils.writePdfToStorage que permite proveer el contenido del PDF directamente.
 */
suspend fun PdfUtils.writePdfToStorage(
    content: String,
    fileName: String,
    pdfA4Printer: PDFA4Printer,
    context: Context,
    contentProvider: () -> java.io.InputStream
): File? {
    return withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/Atestados"
            val queryUri = MediaStore.Files.getContentUri("external")

            // Generar nombre único con marca de tiempo
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val baseName = fileName.substringBeforeLast(".pdf")
            val uniqueFileName = "${baseName}_${timestamp}.pdf"
            Log.d(TAG, "Nombre único generado: $uniqueFileName")

            // Eliminar archivos existentes con el mismo nombre base
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf("$baseName%.pdf", relativePath)

            contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                    val deleteUri = ContentUris.withAppendedId(queryUri, id)
                    contentResolver.delete(deleteUri, null, null)
                }
            }

            // Crear nueva entrada en MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, uniqueFileName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.Files.FileColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(queryUri, contentValues) ?: return@withContext null

            // Escribir el contenido del PDF
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                contentProvider().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Finalizar escritura
            contentValues.clear()
            contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)

            // Obtener el archivo final
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Files.FileColumns.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                    val file = File(path)
                    if (file.exists()) {
                        file.setReadable(true, false)
                        file.setWritable(true, false)
                        return@withContext file
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir PDF: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al guardar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }
}