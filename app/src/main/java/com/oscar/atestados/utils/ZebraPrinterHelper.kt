package com.oscar.atestados.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.graphics.ZebraImageFactory
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ZebraPrinterHelper(private val context: Context) {

    companion object {
        private const val TAG = "ZebraPrinterHelper"
        private const val ASSET_SUBFOLDER = "formatos/"
        private const val RW420_WIDTH = 783 // Ancho en dots para RW420
        private const val PRINT_DELAY_MS = 1000L // Tiempo para RW420 con imágenes
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    @SuppressLint("MissingPermission")
    suspend fun printFromAsset(
        assetFileName: String,
        macAddress: String,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var printerName = "Impresora desconocida"
        try {
            Log.v(TAG, "Iniciando impresión de $assetFileName para MAC $macAddress")
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                val errorMsg = "Bluetooth no está habilitado"
                Log.e(TAG, errorMsg)
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke(errorMsg) }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            if (device == null) {
                val errorMsg = "Dispositivo con MAC $macAddress no encontrado"
                Log.e(TAG, errorMsg)
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke(errorMsg) }
                return@withContext Result.failure(Exception(errorMsg))
            }

            printerName = device.name ?: "Impresora desconocida" // Aquí está el acceso a device.name
            Log.d(TAG, "Impresora detectada: $printerName ($macAddress)")

            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Conectando a $printerName...") }
            connection = BluetoothConnection(macAddress)
            connection.open() // Ejecutar en IO directamente
            if (!connection.isConnected) {
                val errorMsg = "No se pudo conectar a $printerName"
                Log.e(TAG, errorMsg)
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error de conexión con la impresora: No se pudo conectar") }
                return@withContext Result.failure(ConnectionException(errorMsg))
            }
            Log.i(TAG, "Conexión establecida con $printerName")

            val printer = ZebraPrinterFactory.getInstance(connection)
            val printerModel = detectPrinterModel(printer, connection)
            Log.i(TAG, "Modelo detectado: $printerModel")

            val fullAssetPath = "$ASSET_SUBFOLDER$assetFileName"
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Leyendo archivo $fullAssetPath para $printerName...") }
            Log.d(TAG, "Leyendo archivo desde assets: $fullAssetPath")
            val inputStream = context.assets.open(fullAssetPath)
            val zplContent = inputStream.readBytes().toString(Charsets.UTF_8)
            inputStream.close()
            Log.v(TAG, "ZPL leído (primeros 100 caracteres): ${zplContent.take(100)}...")

            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Procesando datos para $printerName...") }
            Log.d(TAG, "Procesando para $printerModel")

            when {
                printerModel.contains("ZQ521", ignoreCase = true) -> {
                    Log.i(TAG, "Enviando ZPL a $printerName (ZQ521)")
                    connection.write(zplContent.toByteArray())
                }
                printerModel.contains("RW420", ignoreCase = true) -> {
                    Log.i(TAG, "Enviando CPCL directamente a $printerName (RW420)")
                    connection.write(zplContent.toByteArray()) // Enviar CPCL sin conversión
                }
                else -> {
                    val errorMsg = "Modelo no soportado: $printerModel"
                    Log.w(TAG, errorMsg)
                    throw Exception(errorMsg)
                }
            }

            Log.v(TAG, "Esperando $PRINT_DELAY_MS ms para completar la impresión")
            Thread.sleep(PRINT_DELAY_MS)

            val successMsg = "Impresión enviada correctamente a $printerName"
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke(successMsg) }
            Log.i(TAG, successMsg)
            Result.success(successMsg)
        } catch (e: ConnectionException) {
            val errorMsg = "Error de conexión con $macAddress: ${e.message}"
            Log.e(TAG, errorMsg, e)
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error de conexión con la impresora: ${e.message}") }
            Result.failure(e)
        } catch (e: IOException) {
            val errorMsg = "Error al leer $assetFileName: ${e.message}"
            Log.e(TAG, errorMsg, e)
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error al leer archivo para $macAddress: ${e.message}") }
            Result.failure(e)
        } catch (e: Exception) {
            val errorMsg = "Error inesperado: ${e.message}"
            Log.e(TAG, errorMsg, e)
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error inesperado con $macAddress: ${e.message}") }
            Result.failure(e)
        } finally {
            connection?.closeSafe(printerName)
        }
    }

    private fun detectPrinterModel(printer: ZebraPrinter, connection: Connection): String {
        Log.v(TAG, "Detectando modelo de impresora")
        return try {
            val response = connection.sendAndWaitForResponse(
                "! U1 getvar \"appl.name\"\n".toByteArray(),
                1000,
                1000,
                null
            )?.toString(Charsets.UTF_8)?.trim()
            when {
                response?.contains("ZQ521", ignoreCase = true) == true -> "ZQ521"
                response?.contains("RW420", ignoreCase = true) == true -> "RW420"
                else -> {
                    Log.w(TAG, "Respuesta SGD desconocida: $response, usando fallback")
                    val language = printer.getPrinterControlLanguage().toString()
                    if (language.contains("ZPL")) "ZQ521" else "RW420"
                }
            }.also { Log.d(TAG, "Modelo detectado: $it (SGD: $response)") }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo en SGD: ${e.message}, usando lenguaje como fallback")
            val language = printer.getPrinterControlLanguage().toString()
            if (language.contains("ZPL")) "ZQ521" else "RW420"
        }
    }

    private suspend fun convertZplToBitmap(
        zpl: String,
        printerName: String,
        onStatusUpdate: ((String) -> Unit)?
    ): Bitmap {
        Log.v(TAG, "Convirtiendo ZPL a bitmap para $printerName")
        try {
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Convirtiendo ZPL a imagen con Labelary para $printerName...") }
            val url = URL("http://api.labelary.com/v1/printers/8dpmm/labels/4x6/0/")
            val http = url.openConnection() as HttpURLConnection
            http.requestMethod = "POST"
            http.doOutput = true
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            Log.d(TAG, "Enviando ZPL a Labelary")
            http.outputStream.write(zpl.toByteArray(Charsets.UTF_8))
            val stream = http.inputStream
            val bitmap = BitmapFactory.decodeStream(stream)
            http.disconnect()
            Log.i(TAG, "Conversión a bitmap exitosa")
            return bitmap ?: throw IOException("No se pudo generar la imagen desde Labelary")
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir ZPL a imagen: ${e.message}", e)
            throw e
        }
    }

    private fun Connection.closeSafe(printerName: String) {
        Log.v(TAG, "Cerrando conexión con $printerName")
        try {
            if (isConnected) {
                close()
                Log.i(TAG, "Conexión con $printerName cerrada correctamente")
            } else {
                Log.w(TAG, "La conexión con $printerName ya estaba cerrada")
            }
        } catch (e: ConnectionException) {
            Log.e(TAG, "Error al cerrar conexión con $printerName: ${e.message}", e)
        }
    }
}