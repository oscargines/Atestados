package com.oscar.atestados.utils

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "StorageUtils"

/**
 * Utilidades para manejar operaciones de almacenamiento relacionadas con la carpeta Atestados,
 * incluyendo:
 * - Apertura del directorio usando diferentes estrategias
 * - Refresco de MediaStore
 * - Manejo de fallbacks para diferentes gestores de archivos
 * - Escritura de archivos PDF con sobrescritura
 */
object StorageUtils {

    /**
     * Escribe un archivo PDF en la carpeta Documents/Atestados usando MediaStore, sobrescribiendo si ya existe.
     *
     * @param context Contexto de la aplicación.
     * @param fileName Nombre del archivo (por ejemplo, "acta_citacion_a4.pdf").
     * @param pdfData Contenido del archivo PDF en bytes.
     */
    fun writePdfToStorage(context: Context, fileName: String, pdfData: ByteArray) {
        Log.d(TAG, "writePdfToStorage: Intentando escribir $fileName")
        try {
            val contentResolver = context.contentResolver
            val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/Atestados"
            val baseName = fileName.substringBeforeLast(".pdf")

            // Generar nombre único con marca de tiempo
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val uniqueFileName = "${baseName}_${timestamp}.pdf"
            Log.d(TAG, "Nombre único generado: $uniqueFileName")

            // Limpiar archivos antiguos con el mismo nombre base
            cleanOldFiles(context, baseName)

            // Crear nueva entrada en MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, uniqueFileName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.Files.FileColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: throw IllegalStateException("No se pudo crear URI en MediaStore")

            // Escribir datos en el URI
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(pdfData)
                Log.d(TAG, "writePdfToStorage: Archivo escrito en MediaStore, URI: $uri")
            } ?: throw IllegalStateException("No se pudo abrir OutputStream para URI $uri")

            // Finalizar escritura
            contentValues.clear()
            contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            val updatedRows = contentResolver.update(uri, contentValues, null, null)
            if (updatedRows == 0) {
                throw IllegalStateException("No se pudo actualizar IS_PENDING=0 para URI $uri")
            }

            // Verificar en MediaStore
            CoroutineScope(Dispatchers.IO).launch {
                delay(500)
                PdfUtils.listAtestadosFiles(context).also { files ->
                    Log.d(TAG, "Archivos en MediaStore después de escribir $uniqueFileName: $files")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permisos insuficientes para escribir $fileName: ${e.message}", e)
            Toast.makeText(context, "Faltan permisos para guardar el archivo", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir $fileName: ${e.message}", e)
            Toast.makeText(context, "Error al guardar el archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    /**
     * Abre el directorio Documents/Atestados usando la mejor estrategia disponible en el dispositivo.
     */
    fun openStorageDirectory(context: Context) {
        Log.d(TAG, "openStorageDirectory: Iniciando apertura de Documents/Atestados")
        try {
            val atestadosDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Atestados"
            ).apply {
                if (!exists() && !mkdirs()) {
                    throw SecurityException("No se pudo crear el directorio Atestados")
                }
                Log.d(TAG, "Directorio Atestados: ${absolutePath}")
            }

            logDirectoryContents(atestadosDir)

            CoroutineScope(Dispatchers.IO).launch {
                PdfUtils.listAtestadosFiles(context).also { files ->
                    Log.d(TAG, "Archivos en MediaStore: ${files.size}")
                }
            }

            refreshAtestadosFolder(context)
            CoroutineScope(Dispatchers.IO).launch {
                delay(500)
                val documentsUri =
                    "content://com.android.externalstorage.documents/document/primary%3ADocuments%2FAtestados".toUri()
                Log.d(TAG, "Usando URI: $documentsUri")

                val intents = listOf(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = documentsUri
                        type = "vnd.android.document/directory"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                        setPackage("com.google.android.documentsui")
                    },
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                        setPackage("com.google.android.apps.nbu.files")
                    },
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra("android.content.extra.SHOW_ADVANCED", true)
                        putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                    }
                )

                val chosenIntent = intents.firstOrNull { intent ->
                    context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
                } ?: intents.last().also {
                    Log.w(TAG, "Usando intent genérico como fallback")
                }

                logIntentDetails(context, chosenIntent)

                Toast.makeText(context, "Busque los PDFs en Documents > Atestados", Toast.LENGTH_LONG).show()

                try {
                    context.startActivity(Intent.createChooser(chosenIntent, "Abrir carpeta con"))
                    Log.d(TAG, "Intent lanzado para URI: $documentsUri")
                } catch (e: ActivityNotFoundException) {
                    handleFileManagerNotFound(context, e)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permisos insuficientes: ${e.message}", e)
            Toast.makeText(context, "Faltan permisos para acceder al almacenamiento", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir directorio: ${e.message}", e)
            Toast.makeText(context, "Error al abrir el directorio: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Refresca la carpeta Atestados en MediaStore para asegurar visibilidad de archivos.
     */
    fun refreshAtestadosFolder(context: Context, specificFile: File? = null) {
        val atestadosDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Atestados"
        ).takeIf { it.exists() } ?: run {
            Log.w(TAG, "El directorio Atestados no existe")
            return
        }

        if (specificFile != null && specificFile.exists()) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(specificFile.absolutePath),
                arrayOf("application/pdf")
            ) { path, uri ->
                Log.d(TAG, "Archivo escaneado: $path, URI: $uri")
            }
        } else {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(atestadosDir.absolutePath),
                null
            ) { path, uri ->
                Log.d(TAG, "Carpeta escaneada: $path, URI: $uri")
            }
        }
    }

    /**
     * Elimina archivos antiguos con el mismo nombre base en MediaStore, excepto el archivo objetivo.
     */
    fun cleanOldFiles(context: Context, baseName: String, extension: String = "pdf") {
        val contentResolver = context.contentResolver
        val queryUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME)
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("$baseName%.pdf", "${Environment.DIRECTORY_DOCUMENTS}/Atestados/", "application/pdf")

        contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                val deleteUri = ContentUris.withAppendedId(queryUri, id)
                contentResolver.delete(deleteUri, null, null)
                Log.d(TAG, "Eliminado archivo antiguo de MediaStore: $name")
            }
        } ?: Log.w(TAG, "cleanOldFiles: Cursor nulo al consultar MediaStore")

        // Limpiar en el sistema de archivos como respaldo
        val atestadosDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Atestados"
        ).takeIf { it.exists() } ?: run {
            Log.w(TAG, "El directorio Atestados no existe")
            return
        }

        atestadosDir.listFiles()?.filter {
            it.name.startsWith(baseName) && it.extension.equals(extension, ignoreCase = true)
        }?.forEach { file ->
            try {
                if (file.delete()) {
                    Log.d(TAG, "Eliminado archivo antiguo del sistema de archivos: ${file.absolutePath}")
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { path, _ ->
                        Log.d(TAG, "Eliminado de MediaStore: $path")
                    }
                } else {
                    Log.w(TAG, "No se pudo eliminar archivo antiguo: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar archivo antiguo ${file.name}: ${e.message}", e)
            }
        }
    }

    /** Método privado para registrar contenido del directorio */
    private fun logDirectoryContents(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            Log.w(TAG, "El directorio no existe o no es accesible: ${directory.absolutePath}")
            return
        }
        directory.listFiles()?.forEach { file ->
            Log.d(
                TAG,
                "Archivo: ${file.name}, readable: ${file.canRead()}, writable: ${file.canWrite()}, " +
                        "size: ${file.length()} bytes, lastModified: ${file.lastModified()}"
            )
        } ?: Log.d(TAG, "No hay archivos en Atestados o directorio inaccesible")
    }

    /** Método privado para registrar detalles del intent */
    private fun logIntentDetails(context: Context, intent: Intent) {
        val resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
        if (resolveInfo.isNotEmpty()) {
            Log.d(TAG, "Apps que pueden manejar el intent:")
            resolveInfo.forEach {
                Log.d(TAG, "- ${it.activityInfo.packageName}: ${it.activityInfo.name}")
            }
        } else {
            Log.w(TAG, "Ninguna app puede manejar el intent elegido")
        }
    }

    /** Método privado para manejar gestor de archivos no encontrado */
    private fun handleFileManagerNotFound(context: Context, e: ActivityNotFoundException) {
        Log.w(TAG, "No se encontró aplicación para abrir directorio", e)
        Toast.makeText(
            context,
            "No hay administrador de archivos instalado. Instale 'Archivos de Google' desde Google Play.",
            Toast.LENGTH_LONG
        ).show()

        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=com.google.android.apps.nbu.files")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No se pudo abrir Google Play Store", e)
            Toast.makeText(
                context,
                "No se pudo abrir Google Play. Busque 'Archivos de Google' manualmente.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}