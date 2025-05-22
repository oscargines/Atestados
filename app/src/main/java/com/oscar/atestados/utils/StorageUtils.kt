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
 * Abre el directorio de almacenamiento Documents/Atestados usando un intent ACTION_VIEW como prioridad.
 * Crea el directorio si no existe, refresca MediaStore y maneja errores mostrando mensajes al usuario.
 * Usa fallbacks con ACTION_OPEN_DOCUMENT_TREE y ACTION_GET_CONTENT si ACTION_VIEW falla.
 *
 * @param context Contexto de la aplicación.
 */
fun openStorageDirectory(context: Context) {
    Log.d(TAG, "openStorageDirectory: Iniciando apertura de Documents/Atestados")
    try {
        val atestadosDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Atestados"
        )
        if (!atestadosDir.exists()) {
            atestadosDir.mkdirs()
            Log.d(TAG, "Directorio Atestados creado en ${atestadosDir.absolutePath}")
        }
        // Log files in directory for debugging
        atestadosDir.listFiles()?.forEach { file ->
            Log.d(
                TAG,
                "Archivo en Atestados: ${file.name}, readable: ${file.canRead()}, size: ${file.length()}"
            )
        } ?: Log.d(TAG, "No hay archivos en Atestados o directorio inaccesible")

        // Listar archivos en MediaStore
        CoroutineScope(Dispatchers.IO).launch {
            val files = PdfUtils.listAtestadosFiles(context)
            Log.d(TAG, "Archivos en MediaStore (Atestados): $files")
        }

        // Refrescar MediaStore y esperar a que termine
        refreshAtestadosFolder(context)
        Thread.sleep(500) // Esperar 500ms para que MediaScanner termine

        // Crear una DocumentsContract URI
        val documentsUri =
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FAtestados")
        Log.d(TAG, "openStorageDirectory: Usando URI $documentsUri")

        // Intent con ACTION_VIEW usando DocumentsContract URI
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            data = documentsUri
            type = "vnd.android.document/directory"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Intent para DocumentsUI con ACTION_OPEN_DOCUMENT_TREE
        val documentsUiIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra("android.provider.extra.INITIAL_URI", documentsUri)
            setPackage("com.google.android.documentsui")
        }

        // Intent para Archivos de Google
        val filesAppIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra("android.provider.extra.INITIAL_URI", documentsUri)
            setPackage("com.google.android.apps.nbu.files")
        }

        // Intent genérico con ACTION_GET_CONTENT como fallback, optimizado para PDFs
        val genericIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra("android.content.extra.SHOW_ADVANCED", true)
            putExtra("android.provider.extra.INITIAL_URI", documentsUri)
        }

        // Verificar disponibilidad
        val packageManager = context.packageManager
        val viewAvailable = packageManager.queryIntentActivities(viewIntent, 0).isNotEmpty()
        val documentsUiAvailable = packageManager.queryIntentActivities(documentsUiIntent, 0).isNotEmpty()
        val filesAppAvailable = packageManager.queryIntentActivities(filesAppIntent, 0).isNotEmpty()
        val genericAvailable = packageManager.queryIntentActivities(genericIntent, 0).isNotEmpty()

        val chosenIntent = when {
            viewAvailable -> viewIntent
            documentsUiAvailable -> documentsUiIntent
            filesAppAvailable -> filesAppIntent
            genericAvailable -> genericIntent
            else -> genericIntent // Último recurso
        }

        Log.d(TAG, "openStorageDirectory: Usando intent ${
            when {
                viewAvailable -> "ACTION_VIEW"
                documentsUiAvailable -> "DocumentsUI"
                filesAppAvailable -> "Archivos de Google"
                else -> "Genérico (ACTION_GET_CONTENT)"
            }
        }")

        // Log apps que pueden manejar el intent
        val resolveInfoList = packageManager.queryIntentActivities(chosenIntent, 0)
        if (resolveInfoList.isNotEmpty()) {
            Log.d(TAG, "Apps que pueden manejar el intent:")
            resolveInfoList.forEach {
                Log.d(TAG, "- ${it.activityInfo.packageName}: ${it.activityInfo.name}")
            }
        } else {
            Log.w(TAG, "Ninguna app puede manejar el intent elegido")
        }

        // Mostrar toast de guía
        Toast.makeText(context, "Busque los PDFs en Documents > Atestados", Toast.LENGTH_LONG).show()
        try {
            context.startActivity(
                Intent.createChooser(chosenIntent, "Seleccionar aplicación para abrir carpeta")
            )
            Log.d(TAG, "openStorageDirectory: Intent lanzado, URI: $documentsUri")
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "openStorageDirectory: No se encontró aplicación para abrir directorio", e)
            Toast.makeText(
                context,
                "No hay administrador de archivos instalado. Instale 'Archivos de Google' desde Google Play.",
                Toast.LENGTH_LONG
            ).show()
            try {
                val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=com.google.android.apps.nbu.files")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(playStoreIntent)
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No se pudo abrir Google Play Store", e)
                Toast.makeText(
                    context,
                    "No se pudo abrir Google Play. Busque 'Archivos de Google' manualmente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "openStorageDirectory: Error al abrir directorio: ${e.message}", e)
        Toast.makeText(context, "Error al abrir el directorio: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Refresca la carpeta Atestados en MediaStore para asegurar que los archivos sean visibles.
 *
 * @param context Contexto de Android.
 */
fun refreshAtestadosFolder(context: Context) {
    val atestadosDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "Atestados"
    )
    if (atestadosDir.exists()) {
        // Escanear la carpeta
        MediaScannerConnection.scanFile(
            context,
            arrayOf(atestadosDir.absolutePath),
            null
        ) { path, uri ->
            Log.d(TAG, "refreshAtestadosFolder: Escaneado $path, URI: $uri")
        }
        // Escanear archivos individuales
        atestadosDir.listFiles()?.forEach { file ->
            if (file.extension.lowercase() == "pdf") {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("application/pdf")
                ) { path, uri ->
                    Log.d(TAG, "refreshAtestadosFolder: Escaneado archivo $path, URI: $uri")
                }
            }
        }
    }
}