package com.oscar.atestados.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.graphics.ZebraImageFactory
import com.zebra.sdk.printer.PrinterLanguage
import com.zebra.sdk.printer.ZebraPrinterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clase auxiliar para gestionar la impresión en impresoras Zebra a través de Bluetooth.
 *
 * @param context Contexto de la aplicación Android.
 */
class ZebraPrinterHelper(private val context: Context) {

    companion object {
        private const val TAG = "ZebraPrinterHelper"
        private const val ASSET_SUBFOLDER = "formatos/"
        private const val RW420_WIDTH = 768 // Ancho en dots para RW420 (98 mm a 200 DPI)
        private const val PRINT_DELAY_MS = 3000L // Retardo tras enviar datos de impresión
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    /**
     * Imprime un archivo desde los assets en una impresora Zebra a través de Bluetooth.
     *
     * @param assetFileName Nombre del archivo en la carpeta de assets (subcarpeta 'formatos').
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     * @param onStatusUpdate Callback opcional para actualizar el estado en la UI.
     * @return Resultado de la operación con un mensaje de éxito o error.
     */
    @SuppressLint("MissingPermission")
    suspend fun printFromAsset(
        assetFileName: String,
        macAddress: String,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var printerName = "Impresora desconocida"
        try {
            Log.i(TAG, "Iniciando impresión de $assetFileName en $macAddress")
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth no habilitado")
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Bluetooth no está habilitado") }
                return@withContext Result.failure(Exception("Bluetooth no está habilitado"))
            }

            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            if (device == null) {
                Log.e(TAG, "Dispositivo con MAC $macAddress no encontrado")
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Dispositivo con MAC $macAddress no encontrado") }
                return@withContext Result.failure(Exception("Dispositivo no encontrado"))
            }

            printerName = device.name ?: "Impresora desconocida"
            Log.i(TAG, "Impresora detectada: $printerName ($macAddress)")
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Conectando a $printerName...") }

            connection = BluetoothConnection(macAddress)
            Log.d(TAG, "Abriendo conexión Bluetooth")
            connection.open()
            if (!connection.isConnected) {
                Log.e(TAG, "Fallo al conectar con $printerName")
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error de conexión con $printerName") }
                return@withContext Result.failure(ConnectionException("No se pudo conectar"))
            }
            Log.i(TAG, "Conexión establecida con $printerName")

            // Forzar CPCL para RW420 y ZQ521
            val printer = ZebraPrinterFactory.getInstance(PrinterLanguage.CPCL, connection)
            val printerModel = if (printerName.contains("ZQ521", ignoreCase = true)) "ZQ521" else "RW420"
            Log.i(TAG, "Modelo asumido: $printerModel (forzando CPCL)")

            // Limpiar memoria antes de imprimir
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Limpiando memoria de $printerName...") }
            clearPrinterMemory(connection, printerModel)
            Thread.sleep(1000)

            val fullAssetPath = "$ASSET_SUBFOLDER$assetFileName"
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Leyendo $fullAssetPath...") }
            Log.d(TAG, "Cargando archivo desde assets: $fullAssetPath")
            val inputStream = context.assets.open(fullAssetPath)
            val labelContent = inputStream.readBytes().toString(Charsets.UTF_8)
            inputStream.close()
            Log.v(TAG, "Contenido leído (primeros 100 caracteres): ${labelContent.take(100)}...")

            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Enviando datos a $printerName...") }
            Log.d(TAG, "Enviando datos para $printerModel")
            Log.i(TAG, "Enviando CPCL a $printerModel")
            connection.write(labelContent.toByteArray())

            // Esperar más tiempo para asegurar que la impresora procese el trabajo
            Log.d(TAG, "Esperando $PRINT_DELAY_MS ms para completar impresión")
            Thread.sleep(PRINT_DELAY_MS)

            // Verificar estado después de imprimir
            val status = connection.sendAndWaitForResponse(
                "! U1 getvar \"device.status\"\n".toByteArray(),
                1000, 1000, null
            )?.toString(Charsets.UTF_8)?.trim()
            Log.i(TAG, "Estado de la impresora: $status")

            val successMsg = "Impresión enviada en $printerName"
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke(successMsg) }
            Log.i(TAG, successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Error en printFromAsset: ${e.message}", e)
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error: ${e.message}") }
            Result.failure(e)
        } finally {
            connection?.closeSafe(printerName)
        }
    }

    /**
     * Imprime un bitmap en una impresora Zebra a través de Bluetooth.
     *
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     * @param bitmap Imagen en formato Bitmap a imprimir.
     * @param onStatusUpdate Callback opcional para actualizar el estado en la UI.
     * @return Resultado de la operación con un mensaje de éxito o error.
     */
    @SuppressLint("MissingPermission")
    suspend fun printBitmap(
        macAddress: String,
        bitmap: Bitmap,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var printerName = "Impresora desconocida"
        try {
            Log.i(TAG, "Iniciando impresión de bitmap en $macAddress")
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth no habilitado")
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Bluetooth no está habilitado") }
                return@withContext Result.failure(Exception("Bluetooth no está habilitado"))
            }

            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            if (device == null) {
                Log.e(TAG, "Dispositivo con MAC $macAddress no encontrado")
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Dispositivo con MAC $macAddress no encontrado") }
                return@withContext Result.failure(Exception("Dispositivo no encontrado"))
            }

            printerName = device.name ?: "Impresora desconocida"
            Log.i(TAG, "Impresora detectada: $printerName ($macAddress)")
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Conectando a $printerName...") }

            connection = BluetoothConnection(macAddress)
            Log.d(TAG, "Abriendo conexión Bluetooth")
            connection.open()
            if (!connection.isConnected) {
                Log.e(TAG, "Fallo al conectar con $printerName")
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error de conexión con $printerName") }
                return@withContext Result.failure(ConnectionException("No se pudo conectar"))
            }
            Log.i(TAG, "Conexión establecida con $printerName")

            // Forzar CPCL para RW420 y ZQ521
            val printer = ZebraPrinterFactory.getInstance(PrinterLanguage.CPCL, connection)
            val printerModel = if (printerName.contains("ZQ521", ignoreCase = true)) "ZQ521" else "RW420"
            Log.i(TAG, "Modelo asumido: $printerModel (forzando CPCL)")

            // Limpiar memoria antes de imprimir
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Limpiando memoria de $printerName...") }
            clearPrinterMemory(connection, printerModel)
            Thread.sleep(1000)

            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Preparando imagen para $printerName...") }
            Log.d(TAG, "Convirtiendo bitmap para $printerModel")
            val zebraImage = ZebraImageFactory.getImage(bitmap)
            val width = RW420_WIDTH
            val height = bitmap.height
            printer.printImage(zebraImage, 0, 0, width, height, false)

            Log.d(TAG, "Esperando $PRINT_DELAY_MS ms para completar impresión")
            Thread.sleep(PRINT_DELAY_MS)

            // Verificar estado después de imprimir
            val status = connection.sendAndWaitForResponse(
                "! U1 getvar \"device.status\"\n".toByteArray(),
                1000, 1000, null
            )?.toString(Charsets.UTF_8)?.trim()
            Log.i(TAG, "Estado de la impresora: $status")

            val successMsg = "Impresión enviada en $printerName"
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke(successMsg) }
            Log.i(TAG, successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Error en printBitmap: ${e.message}", e)
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error: ${e.message}") }
            Result.failure(e)
        } finally {
            connection?.closeSafe(printerName)
        }
    }

    /**
     * Detecta el modelo de la impresora Zebra conectado (usado solo para logs, no para lógica crítica).
     */
    private fun detectPrinterModel(connection: Connection, printerName: String): String {
        Log.d(TAG, "Detectando modelo de impresora (solo para logs)")
        return try {
            val response = connection.sendAndWaitForResponse(
                "! U1 getvar \"appl.name\"\n".toByteArray(),
                1000,
                1000,
                null
            )?.toString(Charsets.UTF_8)?.trim()
            when {
                response?.contains("ZQ521", ignoreCase = true) == true || printerName.contains("ZQ521", ignoreCase = true) -> "ZQ521"
                response?.contains("RW420", ignoreCase = true) == true || printerName.contains("RW420", ignoreCase = true) -> "RW420"
                else -> {
                    Log.w(TAG, "Respuesta desconocida: $response, asumiendo RW420 por defecto")
                    "RW420" // Fallback por defecto
                }
            }.also { Log.i(TAG, "Modelo detectado (log): $it (SGD: $response)") }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo en detección SGD: ${e.message}, asumiendo RW420 por defecto")
            "RW420" // Fallback por defecto
        }
    }

    /**
     * Limpia la memoria de la impresora enviando un comando adecuado según el modelo.
     */
    private fun clearPrinterMemory(connection: Connection, printerModel: String) {
        try {
            when {
                printerModel.contains("ZQ521", ignoreCase = true) || printerModel.contains("RW420", ignoreCase = true) -> {
                    Log.i(TAG, "Limpiando memoria CPCL con END para $printerModel")
                    connection.write("! 0 200 200 1 1\nEND\n".toByteArray()) // Finaliza y limpia en CPCL
                }
                else -> Log.w(TAG, "No se puede limpiar memoria: modelo $printerModel no soportado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar memoria: ${e.message}", e)
        }
    }

    /**
     * Cierra la conexión Bluetooth de forma segura.
     *
     * @param printerName Nombre de la impresora para logs.
     */
    private fun Connection.closeSafe(printerName: String) {
        Log.d(TAG, "Cerrando conexión con $printerName")
        try {
            if (isConnected) {
                close()
                Log.i(TAG, "Conexión cerrada con $printerName")
            } else {
                Log.w(TAG, "Conexión con $printerName ya estaba cerrada")
            }
        } catch (e: ConnectionException) {
            Log.e(TAG, "Error al cerrar conexión con $printerName: ${e.message}", e)
        }
    }
}