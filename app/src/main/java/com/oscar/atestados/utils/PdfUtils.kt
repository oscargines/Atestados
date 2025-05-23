package com.oscar.atestados.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Objeto utilitario para manejar operaciones relacionadas con archivos PDF, incluyendo:
 * - Escritura de PDFs en el almacenamiento usando MediaStore
 * - Listado de archivos PDF existentes
 * - Manejo de permisos y actualización de MediaStore
 */
object PdfUtils {
    private const val TAG = "PdfUtils"

    /**
     * Escribe un PDF en el almacenamiento externo dentro de la carpeta Documents/Atestados,
     * sobrescribiendo cualquier archivo existente con el mismo nombre.
     *
     * @param content Contenido HTML que será convertido a PDF.
     * @param fileName Nombre del archivo PDF (ej. "acta_citacion_a4.pdf").
     * @param pdfA4Printer Instancia de [PDFA4Printer] para generar el PDF a partir del HTML.
     * @param context Contexto de Android para acceder a ContentResolver y mostrar notificaciones.
     * @return [File] que representa el archivo PDF creado, o null si ocurrió un error.
     *
     * @throws SecurityException Si no se tienen los permisos necesarios para escribir en el almacenamiento.
     * @throws IOException Si ocurre un error durante la escritura del archivo.
     *
     * El método realiza las siguientes operaciones:
     * 1. Elimina cualquier archivo existente con el mismo nombre
     * 2. Crea una nueva entrada en MediaStore
     * 3. Genera el PDF en un archivo temporal
     * 4. Copia el contenido al destino final
     * 5. Actualiza MediaStore y ejecuta MediaScanner
     * 6. Establece los permisos adecuados en el archivo final
     */
    suspend fun writePdfToStorage(
        content: String,
        fileName: String,
        pdfA4Printer: PDFA4Printer,
        context: Context
    ): File? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "writePdfToStorage: Iniciando escritura de PDF, fileName: $fileName")
            try {
                val contentResolver = context.contentResolver
                val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/Atestados"

                // Eliminar archivo existente si existe
                val queryUri = MediaStore.Files.getContentUri("external")
                val projection = arrayOf(MediaStore.Files.FileColumns._ID)
                val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, relativePath)

                contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                        val id = cursor.getLong(idColumn)
                        val deleteUri = ContentUris.withAppendedId(queryUri, id)
                        contentResolver.delete(deleteUri, null, null)
                        Log.d(TAG, "writePdfToStorage: Archivo existente $fileName eliminado de MediaStore")
                    }
                }

                // Crear nueva entrada en MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.Files.FileColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                }

                val uri = contentResolver.insert(queryUri, contentValues) ?: run {
                    Log.e(TAG, "writePdfToStorage: No se pudo crear URI en MediaStore")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al crear archivo en almacenamiento", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext null
                }

                // Crear archivo temporal y garantizar su eliminación
                val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
                try {
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        if (outputStream == null) {
                            Log.e(TAG, "writePdfToStorage: No se pudo abrir OutputStream para URI $uri")
                            return@use
                        }
                        pdfA4Printer.generarDocumentoA4(htmlContent = content, outputFile = tempFile)
                        if (!tempFile.exists() || tempFile.length() == 0L) {
                            Log.e(TAG, "writePdfToStorage: El archivo PDF temporal no se creó correctamente")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error: El archivo PDF no se creó correctamente", Toast.LENGTH_SHORT).show()
                            }
                            return@use
                        }
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)

                    Log.d(TAG, "writePdfToStorage: PDF guardado en MediaStore con URI: $uri")
                    Log.d(TAG, "writePdfToStorage: PDF creado en $relativePath/$fileName")

                    // Obtener la ruta física para MediaScanner
                    val filePath = contentResolver.query(
                        uri,
                        arrayOf(MediaStore.Files.FileColumns.DATA),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                        } else {
                            null
                        }
                    }

                    if (filePath != null) {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(filePath),
                            arrayOf("application/pdf")
                        ) { path, scannedUri ->
                            Log.d(TAG, "writePdfToStorage: MediaScanner completado para $path, URI: $scannedUri")
                            // Verificar si el archivo aparece en MediaStore después del escaneo
                            CoroutineScope(Dispatchers.IO).launch {
                                val files = listAtestadosFiles(context)
                                Log.d(TAG, "writePdfToStorage: Archivos en MediaStore después de escanear $path: $files")
                            }
                        }
                    } else {
                        Log.w(TAG, "writePdfToStorage: No se pudo obtener la ruta del archivo para escanear")
                        // Forzar escaneo de la carpeta como fallback
                        val atestadosDir = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                            "Atestados"
                        )
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(atestadosDir.absolutePath),
                            null
                        ) { path, scannedUri ->
                            Log.d(TAG, "writePdfToStorage: Escaneo de carpeta completado para $path, URI: $scannedUri")
                        }
                    }

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
                                Log.d(TAG, "writePdfToStorage: Permisos establecidos para ${file.absolutePath}, size: ${file.length()}")
                                return@withContext file
                            }
                        }
                    }
                    Log.w(TAG, "writePdfToStorage: No se pudo obtener el archivo desde el URI")
                    null
                } finally {
                    if (tempFile.exists()) {
                        if (tempFile.delete()) {
                            Log.d(TAG, "writePdfToStorage: Archivo temporal ${tempFile.absolutePath} eliminado")
                        } else {
                            Log.w(TAG, "writePdfToStorage: No se pudo eliminar archivo temporal ${tempFile.absolutePath}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "writePdfToStorage: Error al escribir PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al guardar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                null
            }
        }
    }

    /**
     * Lista los archivos PDF disponibles en la carpeta Documents/Atestados usando MediaStore.
     *
     * @param context Contexto de Android para acceder al ContentResolver.
     * @return Lista de nombres de archivos PDF encontrados en la carpeta.
     *
     * @throws SecurityException Si no se tienen los permisos necesarios para leer el almacenamiento.
     *
     * Notas:
     * - Solo devuelve archivos con extensión PDF
     * - La consulta es sensible a la ruta exacta (Documents/Atestados/)
     * - Los resultados incluyen metadatos como ruta completa e ID en MediaStore
     */
    suspend fun listAtestadosFiles(context: Context): List<String> {
        return withContext(Dispatchers.IO) {
            val files = mutableListOf<String>()
            val contentResolver = context.contentResolver
            val queryUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns._ID
            )
            val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("${Environment.DIRECTORY_DOCUMENTS}/Atestados/", "application/pdf")
            try {
                contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    Log.d(TAG, "listAtestadosFiles: Encontrados ${cursor.count} archivos en MediaStore")
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                        files.add(name)
                        Log.d(TAG, "listAtestadosFiles: Encontrado $name, path=$path, id=$id")
                    }
                } ?: Log.w(TAG, "listAtestadosFiles: Cursor nulo al consultar MediaStore")
            } catch (e: Exception) {
                Log.e(TAG, "listAtestadosFiles: Error al consultar MediaStore: ${e.message}", e)
            }
            files
        }
    }
}