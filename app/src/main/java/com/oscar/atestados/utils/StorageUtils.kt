package com.oscar.atestados.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "StorageUtils"

/**
 * Utilidades para manejar operaciones de almacenamiento relacionadas con la carpeta Atestados,
 * incluyendo:
 * - Apertura del directorio usando diferentes estrategias
 * - Refresco de MediaStore
 * - Manejo de fallbacks para diferentes gestores de archivos
 */
object StorageUtils {

    /**
     * Abre el directorio Documents/Atestados usando la mejor estrategia disponible en el dispositivo.
     *
     * Este método intenta abrir la carpeta usando diferentes enfoques en este orden de prioridad:
     * 1. Intent ACTION_VIEW con URI de DocumentsContract (mejor compatibilidad)
     * 2. Intent ACTION_OPEN_DOCUMENT_TREE con DocumentsUI (gestor de archivos de Android)
     * 3. Intent ACTION_OPEN_DOCUMENT_TREE con Archivos de Google
     * 4. Intent genérico ACTION_GET_CONTENT (fallback)
     *
     * @param context Contexto de la aplicación para lanzar intents y mostrar mensajes.
     *
     * El método realiza las siguientes operaciones:
     * 1. Crea el directorio si no existe
     * 2. Lista archivos para depuración
     * 3. Refresca MediaStore
     * 4. Construye y prueba diferentes intents
     * 5. Maneja fallos mostrando mensajes apropiados
     * 6. Sugiere instalar gestor de archivos si es necesario
     *
     * @throws SecurityException Si no se tienen permisos de almacenamiento
     */
    fun openStorageDirectory(context: Context) {
        Log.d(TAG, "openStorageDirectory: Iniciando apertura de Documents/Atestados")
        try {
            // 1. Verificar/Crear directorio
            val atestadosDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Atestados"
            ).apply {
                if (!exists()) {
                    mkdirs()
                    Log.d(TAG, "Directorio Atestados creado en ${absolutePath}")
                }
            }

            // 2. Depuración: listar archivos
            logDirectoryContents(atestadosDir)

            // 3. Consultar MediaStore asíncronamente
            CoroutineScope(Dispatchers.IO).launch {
                PdfUtils.listAtestadosFiles(context).also { files ->
                    Log.d(TAG, "Archivos en MediaStore (Atestados): $files")
                }
            }

            // 4. Refrescar MediaStore
            refreshAtestadosFolder(context)
            Thread.sleep(500) // Esperar para completar el escaneo

            // 5. Construir URI del directorio
            val documentsUri = Uri.parse(
                "content://com.android.externalstorage.documents/document/primary%3ADocuments%2FAtestados"
            ).also {
                Log.d(TAG, "openStorageDirectory: Usando URI $it")
            }

            // 6. Construir intents con diferentes estrategias
            val intents = listOf(
                // Intent principal con ACTION_VIEW
                Intent(Intent.ACTION_VIEW).apply {
                    data = documentsUri
                    type = "vnd.android.document/directory"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                // Intent para DocumentsUI
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                    setPackage("com.google.android.documentsui")
                },
                // Intent para Archivos de Google
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                    setPackage("com.google.android.apps.nbu.files")
                },
                // Intent genérico de fallback
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra("android.content.extra.SHOW_ADVANCED", true)
                    putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                }
            )

            // 7. Seleccionar el primer intent disponible
            val chosenIntent = intents.firstOrNull { intent ->
                context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
            } ?: intents.last().also {
                Log.w(TAG, "Ningún intent específico disponible, usando fallback genérico")
            }

            logIntentDetails(context, chosenIntent)

            // 8. Guía para el usuario
            Toast.makeText(context, "Busque los PDFs en Documents > Atestados", Toast.LENGTH_LONG).show()

            // 9. Lanzar intent con manejo de errores
            try {
                context.startActivity(
                    Intent.createChooser(chosenIntent, "Seleccionar aplicación para abrir carpeta")
                )
                Log.d(TAG, "Intent lanzado, URI: $documentsUri")
            } catch (e: ActivityNotFoundException) {
                handleFileManagerNotFound(context, e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir directorio: ${e.message}", e)
            Toast.makeText(context, "Error al abrir el directorio: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Refresca la carpeta Atestados en MediaStore para asegurar visibilidad de archivos.
     *
     * @param context Contexto de Android para acceder a MediaScanner.
     *
     * Realiza dos operaciones:
     * 1. Escaneo recursivo de la carpeta completa
     * 2. Escaneo individual de cada archivo PDF
     */
    fun refreshAtestadosFolder(context: Context) {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Atestados"
        ).takeIf { it.exists() }?.let { dir ->
            // Escanear carpeta completa
            MediaScannerConnection.scanFile(
                context,
                arrayOf(dir.absolutePath),
                null
            ) { path, uri ->
                Log.d(TAG, "Escaneado $path, URI: $uri")
            }

            // Escanear archivos PDF individualmente
            dir.listFiles()?.filter { it.extension.equals("pdf", ignoreCase = true) }?.forEach { file ->
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("application/pdf")
                ) { path, uri ->
                    Log.d(TAG, "Escaneado archivo $path, URI: $uri")
                }
            }
        }
    }

    /** Método privado para registrar contenido del directorio */
    private fun logDirectoryContents(directory: File) {
        directory.listFiles()?.forEach { file ->
            Log.d(
                TAG,
                "Archivo en Atestados: ${file.name}, readable: ${file.canRead()}, size: ${file.length()}"
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