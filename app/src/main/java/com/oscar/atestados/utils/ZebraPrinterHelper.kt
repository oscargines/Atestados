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
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Clase helper para manejar la comunicación e impresión con impresoras Zebra vía Bluetooth.
 *
 * Proporciona métodos para imprimir desde diferentes fuentes:
 * - Archivos de assets (formato ZPL)
 * - Imágenes Bitmap
 * - Comandos ZPL directos
 * - Archivos locales
 *
 * @property context Contexto de Android para acceder a recursos y funcionalidades del sistema.
 */
class ZebraPrinterHelper(private val context: Context) {

    companion object {
        private const val TAG = "ZebraPrinterHelper"
        private const val ASSET_SUBFOLDER = "formatos/"
        private const val DEFAULT_PRINT_WIDTH = 768 // Ancho en dots para RW420 (98 mm a 200 DPI)
        private const val PRINT_TIMEOUT_MS = 5000L
        private const val CONNECTION_TIMEOUT_MS = 10000L
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    /**
     * Resultado de las operaciones de impresión.
     */
    sealed interface PrintResult {
        /**
         * Indica que la impresión fue exitosa.
         * @property printerName Nombre de la impresora.
         * @property details Detalles adicionales del resultado.
         */
        data class Success(val printerName: String, val details: String) : PrintResult

        /**
         * Indica que ocurrió un error durante la impresión.
         * @property message Descripción del error.
         * @property cause Excepción que causó el error (opcional).
         */
        data class Error(val message: String, val cause: Exception? = null) : PrintResult
    }

    /**
     * Imprime un archivo ZPL desde los assets de la aplicación.
     *
     * @param assetFileName Nombre del archivo en la carpeta assets/formatos/.
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     * @param onStatusUpdate Callback para actualizaciones de estado durante el proceso.
     * @return [PrintResult] con el resultado de la operación.
     */
    @SuppressLint("MissingPermission")
    suspend fun printFromAsset(
        assetFileName: String,
        macAddress: String,
        onStatusUpdate: (String) -> Unit = {}
    ): PrintResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            updateStatus(onStatusUpdate, "Iniciando impresión...")

            verifyBluetoothEnabled() ?: return@withContext PrintResult.Error("Bluetooth no habilitado")

            val device = getBluetoothDevice(macAddress) ?:
            return@withContext PrintResult.Error("Dispositivo no encontrado")

            val printerName = device.name ?: "Impresora Zebra"
            updateStatus(onStatusUpdate, "Conectando a $printerName...")

            connection = establishConnection(macAddress, printerName, onStatusUpdate) ?:
                    return@withContext PrintResult.Error("Error de conexión")

            val printer = ZebraPrinterFactory.getInstance(PrinterLanguage.ZPL, connection)
            updateStatus(onStatusUpdate, "Preparando impresión...")

            clearPrinterMemory(connection)

            val labelContent = readAssetContent(assetFileName, onStatusUpdate)
            sendPrintJob(connection, labelContent, onStatusUpdate)

            verifyPrintStatus(connection, printerName, onStatusUpdate)

            PrintResult.Success(printerName, "Impresión completada con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error en printFromAsset", e)
            PrintResult.Error("Error al imprimir: ${e.message}", e)
        } finally {
            connection?.close()
        }
    }

    /**
     * Imprime una imagen Bitmap en la impresora Zebra.
     *
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     * @param bitmap Imagen a imprimir.
     * @param onStatusUpdate Callback para actualizaciones de estado durante el proceso.
     * @return [PrintResult] con el resultado de la operación.
     */
    @SuppressLint("MissingPermission")
    suspend fun printBitmap(
        macAddress: String,
        bitmap: Bitmap,
        onStatusUpdate: (String) -> Unit = {}
    ): PrintResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            updateStatus(onStatusUpdate, "Iniciando impresión de imagen...")

            verifyBluetoothEnabled() ?: return@withContext PrintResult.Error("Bluetooth no habilitado")

            val device = getBluetoothDevice(macAddress) ?:
            return@withContext PrintResult.Error("Dispositivo no encontrado")

            val printerName = device.name ?: "Impresora Zebra"
            updateStatus(onStatusUpdate, "Conectando a $printerName...")

            connection = establishConnection(macAddress, printerName, onStatusUpdate) ?:
                    return@withContext PrintResult.Error("Error de conexión")

            val printer = ZebraPrinterFactory.getInstance(PrinterLanguage.ZPL, connection)
            updateStatus(onStatusUpdate, "Preparando imagen...")

            clearPrinterMemory(connection)

            val zebraImage = ZebraImageFactory.getImage(bitmap)
            val height = (bitmap.height * DEFAULT_PRINT_WIDTH) / bitmap.width
            printer.printImage(zebraImage, 0, 0, DEFAULT_PRINT_WIDTH, height, false)
            TimeUnit.MILLISECONDS.sleep(PRINT_TIMEOUT_MS)

            verifyPrintStatus(connection, printerName, onStatusUpdate)

            PrintResult.Success(printerName, "Imagen impresa con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error en printBitmap", e)
            PrintResult.Error("Error al imprimir imagen: ${e.message}", e)
        } finally {
            connection?.close()
        }
    }

    // ========== Funciones auxiliares privadas ========== //

    /**
     * Actualiza el estado de la operación y registra en log.
     * @param onStatusUpdate Callback para enviar actualizaciones.
     * @param message Mensaje de estado.
     */
    private suspend fun updateStatus(onStatusUpdate: (String) -> Unit, message: String) {
        withContext(Dispatchers.Main) { onStatusUpdate(message) }
        Log.i(TAG, message)
    }

    /**
     * Verifica si el Bluetooth está habilitado.
     * @return Boolean? true si está habilitado, false si no, null si no hay adaptador.
     */
    private suspend fun verifyBluetoothEnabled(): Boolean? {
        return if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth no habilitado")
            false
        } else true
    }

    /**
     * Obtiene el dispositivo Bluetooth por dirección MAC.
     * @param macAddress Dirección MAC del dispositivo.
     * @return Dispositivo Bluetooth o null si no se encuentra.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getBluetoothDevice(macAddress: String) =
        bluetoothAdapter?.getRemoteDevice(macAddress)

    /**
     * Establece conexión con la impresora Zebra.
     * @param macAddress Dirección MAC de la impresora.
     * @param printerName Nombre de la impresora para logs.
     * @param onStatusUpdate Callback para actualizaciones.
     * @return Conexión establecida o null si falla.
     */
    private suspend fun establishConnection(
        macAddress: String,
        printerName: String,
        onStatusUpdate: (String) -> Unit
    ): Connection? {
        return try {
            val connection = BluetoothConnection(macAddress).apply {
                open()
                if (!isConnected) throw ConnectionException("No se pudo conectar")
            }
            updateStatus(onStatusUpdate, "Conectado a $printerName")
            connection
        } catch (e: Exception) {
            updateStatus(onStatusUpdate, "Error de conexión: ${e.message}")
            null
        }
    }

    /**
     * Limpia la memoria de la impresora con comando ZPL.
     * @param connection Conexión activa con la impresora.
     */
    private fun clearPrinterMemory(connection: Connection) {
        try {
            connection.write("^XA^MCY^XZ".toByteArray()) // Limpia buffer en ZPL
            TimeUnit.MILLISECONDS.sleep(1000)
        } catch (e: Exception) {
            Log.w(TAG, "Error al limpiar memoria: ${e.message}")
        }
    }

    /**
     * Lee el contenido de un archivo desde assets.
     * @param assetFileName Nombre del archivo en assets/formatos/.
     * @param onStatusUpdate Callback para actualizaciones.
     * @return Contenido del archivo como String.
     * @throws Exception Si falla la lectura del archivo.
     */
    private suspend fun readAssetContent(assetFileName: String, onStatusUpdate: (String) -> Unit): String {
        return try {
            updateStatus(onStatusUpdate, "Leyendo archivo...")
            val fullAssetPath = "$ASSET_SUBFOLDER$assetFileName"
            context.assets.open(fullAssetPath).use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            updateStatus(onStatusUpdate, "Error al leer archivo")
            throw e
        }
    }

    /**
     * Envía un trabajo de impresión a la impresora.
     * @param connection Conexión activa con la impresora.
     * @param content Contenido ZPL a imprimir.
     * @param onStatusUpdate Callback para actualizaciones.
     */
    private suspend fun sendPrintJob(
        connection: Connection,
        content: String,
        onStatusUpdate: (String) -> Unit
    ) {
        updateStatus(onStatusUpdate, "Enviando datos...")
        connection.write(content.toByteArray())
        TimeUnit.MILLISECONDS.sleep(PRINT_TIMEOUT_MS)
    }

    /**
     * Verifica el estado de la impresora después de imprimir.
     * @param connection Conexión activa con la impresora.
     * @param printerName Nombre de la impresora para logs.
     * @param onStatusUpdate Callback para actualizaciones.
     */
    private suspend fun verifyPrintStatus(
        connection: Connection,
        printerName: String,
        onStatusUpdate: (String) -> Unit
    ) {
        val status = try {
            connection.sendAndWaitForResponse(
                "~HS".toByteArray(), // Comando ZPL para obtener estado
                1000, 1000, null
            )?.toString(Charsets.UTF_8)?.trim()
        } catch (e: Exception) {
            null
        }
        updateStatus(onStatusUpdate, "Estado de $printerName: ${status ?: "desconocido"}")
    }

    /**
     * Envía un comando ZPL directamente a la impresora.
     * @param macAddress Dirección MAC de la impresora.
     * @param zplCommand Comando ZPL a enviar.
     */
    fun sendRawCommand(macAddress: String, zplCommand: String) {
        val connection = BluetoothConnection(macAddress)
        try {
            connection.open()
            val printer = ZebraPrinterFactory.getInstance(PrinterLanguage.ZPL, connection)
            printer.sendCommand(zplCommand)
        } finally {
            try {
                connection.close()
            } catch (e: Exception) {
                Log.w("ZebraPrinterHelper", "Error al cerrar conexión: ${e.message}")
            }
        }
    }

    /**
     * Configura la orientación de impresión.
     * @param connection Conexión activa con la impresora.
     */
    private fun setPrintOrientation(connection: Connection) {
        try {
            // Para ZPL
            connection.write("^XA^POI^XZ".toByteArray()) // POI = Print Orientation Inverse
            // O alternativamente:
            // connection.write("^XA^POA^XZ".toByteArray()) // POA = Print Orientation Normal
            TimeUnit.MILLISECONDS.sleep(500)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando orientación", e)
        }
    }

    /**
     * Imprime el contenido de un archivo local.
     * @param filePath Ruta completa al archivo.
     * @param macAddress Dirección MAC de la impresora.
     * @param onStatusUpdate Callback para actualizaciones.
     * @return [PrintResult] con el resultado de la operación.
     */
    @SuppressLint("MissingPermission")
    fun printFromFile(filePath: String, macAddress: String, onStatusUpdate: (String) -> Unit): PrintResult {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                onStatusUpdate("Archivo no encontrado: $filePath")
                return PrintResult.Error("Archivo no encontrado")
            }

            // Obtener el dispositivo Bluetooth para el nombre de la impresora
            val device = bluetoothAdapter?.getRemoteDevice(macAddress)
            val printerName = device?.name ?: "Impresora Zebra"

            onStatusUpdate("Imprimiendo archivo desde $filePath a $printerName")
            val content = file.readText(Charsets.UTF_8)

            // Conectar y enviar el contenido
            val connection = BluetoothConnection(macAddress)
            connection.open()
            connection.write(content.toByteArray(Charsets.UTF_8))
            TimeUnit.MILLISECONDS.sleep(PRINT_TIMEOUT_MS)
            connection.close()

            PrintResult.Success(printerName, "Archivo impreso correctamente")
        } catch (e: Exception) {
            onStatusUpdate("Error al imprimir: ${e.message}")
            PrintResult.Error(e.message ?: "Error desconocido", e)
        }
    }
}