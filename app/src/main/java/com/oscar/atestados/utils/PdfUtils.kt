package com.tuapp.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.oscar.atestados.utils.PDFA4Printer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfUtils {
    private const val TAG = "PdfUtils"

    suspend fun writePdfToStorage(
        content: String,
        fileName: String,
        pdfA4Printer: PDFA4Printer,
        context: Context
    ): File? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "writePdfToStorage: Iniciando escritura de PDF, fileName: $fileName")
            try {
                val documentsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs()
                }
                val atestadosDir = File(documentsDir, "Atestados")
                if (!atestadosDir.exists()) {
                    atestadosDir.mkdirs()
                    Log.d(
                        TAG,
                        "writePdfToStorage: Directorio Atestados creado en ${atestadosDir.absolutePath}"
                    )
                }
                val outputFile = File(atestadosDir, fileName)
                pdfA4Printer.generarDocumentoA4(htmlContent = content, outputFile = outputFile)
                Log.d(TAG, "writePdfToStorage: PDF generado en ${outputFile.absolutePath}")

                if (!outputFile.exists() || outputFile.length() == 0L) {
                    Log.e(
                        TAG,
                        "writePdfToStorage: El archivo PDF no se creó correctamente o está vacío"
                    )
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Error: El archivo PDF no se creó correctamente",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@withContext null
                }

                try {
                    outputFile.setReadable(true, false)
                    outputFile.setWritable(true, false)
                    Log.d(
                        TAG,
                        "writePdfToStorage: Permisos establecidos para ${outputFile.absolutePath}"
                    )
                } catch (e: SecurityException) {
                    Log.w(
                        TAG,
                        "writePdfToStorage: No se pudieron establecer permisos: ${e.message}"
                    )
                }

                try {
                    val contentUri = MediaStore.Files.getContentUri("external")
                    val values = ContentValues().apply {
                        put(MediaStore.Files.FileColumns.DATA, outputFile.absolutePath)
                        put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                        put(
                            MediaStore.Files.FileColumns.DATE_MODIFIED,
                            System.currentTimeMillis() / 1000
                        )
                    }
                    context.contentResolver.insert(contentUri, values)
                    Log.d(
                        TAG,
                        "writePdfToStorage: Archivo indexado en MediaStore: ${outputFile.absolutePath}"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "writePdfToStorage: Error al indexar en MediaStore: ${e.message}")
                }

                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(outputFile.absolutePath),
                        arrayOf("application/pdf")
                    ) { path, uri ->
                        Log.d(
                            TAG,
                            "writePdfToStorage: MediaScanner completado para $path, URI: $uri"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "writePdfToStorage: Error al escanear archivo: ${e.message}")
                }

                outputFile
            } catch (e: SecurityException) {
                Log.e(TAG, "writePdfToStorage: Error de permisos al escribir PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Error de permisos al guardar PDF",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "writePdfToStorage: Error al escribir PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Error al guardar PDF: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                null
            }
        }
    }
}