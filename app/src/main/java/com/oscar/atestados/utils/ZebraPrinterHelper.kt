package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oscar.atestados.screens.dataStoreImp
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionBuilder
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.graphics.ZebraImageFactory
import com.zebra.sdk.graphics.ZebraImageI
import com.zebra.sdk.printer.FontConverterZpl
import com.zebra.sdk.printer.PrinterAlert
import com.zebra.sdk.printer.PrinterLanguage
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLinkOs
import com.zebra.sdk.settings.AlertCondition
import com.zebra.sdk.settings.AlertDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "ZebraPrinterHelper"
private const val DEFAULT_READ_TIMEOUT = 5000
private const val DEFAULT_WAIT_DATA = 500

class ZebraPrinterHelper(private val context: Context) {

    private val _printStatus = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
    val printStatus: StateFlow<PrintStatus> = _printStatus

    private suspend fun getPrinterMacAddress(): String {
        Log.d(TAG, "Obteniendo dirección MAC desde DataStore")
        val preferences = context.dataStoreImp.data.first()
        return preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
            ?: throw IllegalStateException("No hay dirección MAC configurada")
    }

    private fun detectFileLanguage(prnData: ByteArray): PrinterLanguage {
        Log.d(TAG, "Detectando lenguaje del archivo .prn")
        val prnText = String(prnData, Charsets.UTF_8).trim()
        return when {
            prnText.startsWith("!") || prnText.contains("PW") || prnText.contains("BAR-SENSE") -> PrinterLanguage.CPCL
            prnText.startsWith("^XA") || prnText.contains("^FO") || prnText.contains("^XZ") -> PrinterLanguage.ZPL
            else -> throw IllegalArgumentException("Formato .prn no reconocido: ${prnText.take(50)}")
        }.also { Log.i(TAG, "Lenguaje detectado: $it") }
    }

    private fun getPrinterStatus(printer: ZebraPrinter): String? {
        return try {
            val status: PrinterStatus = printer.getCurrentStatus()
            when {
                status.isReadyToPrint -> "Impresora lista"
                status.isHeadOpen -> "Cabezal abierto"
                status.isPaperOut -> "Sin papel"
                else -> "Estado desconocido: paused=${status.isPaused}, headCold=${status.isHeadCold}"
            }.also { Log.i(TAG, it) }
        } catch (e: ConnectionException) {
            Log.e(TAG, "Error al obtener estado: ${e.message}", e)
            null
        }
    }

    private fun listStoredFonts(printer: ZebraPrinter): List<String> {
        val printerLanguage = printer.getPrinterControlLanguage()
        val fontExtensions = if (printerLanguage == PrinterLanguage.CPCL) arrayOf("CPF") else arrayOf("TTF", "FNT")
        val fonts = mutableListOf<String>()
        fontExtensions.forEach { ext ->
            try {
                val fileNames = printer.retrieveFileNames(arrayOf(ext))
                if (fileNames.isEmpty()) {
                    Log.w(TAG, "No se encontraron tipografías con extensión $ext")
                } else {
                    fonts.addAll(fileNames)
                    Log.i(TAG, "Tipografías $ext disponibles: ${fileNames.joinToString()}")
                }
            } catch (e: ConnectionException) {
                Log.e(TAG, "Error al consultar tipografías $ext: ${e.message}", e)
            }
        }
        return fonts
    }

    suspend fun connectAndListFonts(macAddress: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val targetMac = macAddress ?: getPrinterMacAddress()
                Log.d(TAG, "Conectando a $targetMac")
                val connection: Connection = ConnectionBuilder.build("BT:$targetMac")
                try {
                    connection.open()
                    val printer = ZebraPrinterFactory.getInstance(connection)
                    val printerId = SGD.GET("device.unique_id", connection) ?: "Unknown"
                    Log.i(TAG, "Conectado a $printerId, Lenguaje: ${printer.getPrinterControlLanguage()}")
                    listStoredFonts(printer)
                    Result.success(getPrinterStatus(printer) ?: "Conexión establecida")
                } catch (e: ConnectionException) {
                    Log.e(TAG, "Error al conectar: ${e.message}", e)
                    Result.failure(ConnectionException("Error al conectar o listar tipografías: ${e.message}", e))
                } finally {
                    connection.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun printFromAsset(assetFileName: String, replacements: Map<String, String> = emptyMap()): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val macAddress = getPrinterMacAddress()
                _printStatus.value = PrintStatus.Connecting
                val connection: Connection = ConnectionBuilder.build("BT:$macAddress").apply {
                    setMaxTimeoutForRead(DEFAULT_READ_TIMEOUT)
                    setTimeToWaitForMoreData(DEFAULT_WAIT_DATA)
                }
                try {
                    connection.open()
                    val printer = ZebraPrinterFactory.getInstance(connection)
                    val printerId = SGD.GET("device.unique_id", connection) ?: "Unknown"
                    Log.i(TAG, "Conectado a $printerId")

                    listStoredFonts(printer)
                    _printStatus.value = PrintStatus.PreparingData

                    val filePath = "formatos/$assetFileName"
                    var prnData = context.assets.open(filePath).readBytes()
                    var prnText = String(prnData, Charsets.UTF_8)
                    replacements.forEach { (key, value) -> prnText = prnText.replace(key, value) }
                    prnData = prnText.toByteArray(Charsets.UTF_8)
                    saveModifiedPrn(assetFileName, prnData)

                    val fileLanguage = detectFileLanguage(prnData)
                    if (printer.getPrinterControlLanguage() != fileLanguage) {
                        val command = if (fileLanguage == PrinterLanguage.CPCL) "!\r\n" else "^XA^SZ2^XZ\r\n"
                        connection.write(command.toByteArray())
                        delay(500)
                    }

                    check(getPrinterStatus(printer) == "Impresora lista") { "Impresora no está lista" }
                    _printStatus.value = PrintStatus.Printing(progress = 0)

                    val chunkSize = minOf(1024, (connection as? BluetoothConnection)?.getMaxDataToWrite() ?: 1024)
                    val totalBytes = prnData.size
                    var offset = 0
                    while (offset < totalBytes) {
                        val bytesToWrite = minOf(chunkSize, totalBytes - offset)
                        connection.write(prnData, offset, bytesToWrite)
                        offset += bytesToWrite
                        _printStatus.value = PrintStatus.Printing(progress = offset * 100 / totalBytes)
                        delay(50)
                    }

                    // Ajustar el tiempo de espera dinámicamente
                    val waitTime = calculateWaitTime(printerId, totalBytes)
                    delay(waitTime)
                    _printStatus.value = PrintStatus.Finished(status = getPrinterStatus(printer) ?: "Impresión enviada")
                    Result.success("Impresión completada")
                } catch (e: ConnectionException) {
                    _printStatus.value = PrintStatus.Error(message = e.message ?: "Error desconocido")
                    Log.e(TAG, "Error en impresión: ${e.message}", e)
                    Result.failure(ConnectionException("Error al imprimir: ${e.message}", e))
                } finally {
                    connection.close()
                }
            } catch (e: Exception) {
                _printStatus.value = PrintStatus.Error(message = e.message ?: "Error inesperado")
                Log.e(TAG, "Error inesperado: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private fun calculateWaitTime(printerId: String, totalBytes: Int): Long {
        return when {
            printerId.contains("ZQ521") -> maxOf(2000L, totalBytes / 1000L) // Ajuste dinámico para ZQ521
            else -> maxOf(8000L, totalBytes / 500L) // Ajuste dinámico para RW420
        }
    }

    suspend fun printImage(imagePath: String, x: Int, y: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val macAddress = getPrinterMacAddress()
                _printStatus.value = PrintStatus.Connecting
                val connection: Connection = ConnectionBuilder.build("BT:$macAddress")
                try {
                    connection.open()
                    val printer = ZebraPrinterFactory.getInstance(connection)
                    val image: ZebraImageI = ZebraImageFactory.getImage(imagePath)
                    Log.i(TAG, "Imagen cargada: ${image.getWidth()}x${image.getHeight()} px")

                    check(getPrinterStatus(printer) == "Impresora lista") { "Impresora no está lista" }
                    _printStatus.value = PrintStatus.Printing(progress = 0)

                    printer.printImage(image, x, y, image.getWidth(), image.getHeight(), false)
                    _printStatus.value = PrintStatus.Finished(status = "Imagen impresa")
                    Result.success("Imagen impresa")
                } catch (e: ConnectionException) {
                    _printStatus.value = PrintStatus.Error(message = e.message ?: "Error al imprimir imagen")
                    Log.e(TAG, "Error: ${e.message}", e)
                    Result.failure(e)
                } finally {
                    connection.close()
                }
            } catch (e: Exception) {
                _printStatus.value = PrintStatus.Error(message = e.message ?: "Error inesperado")
                Log.e(TAG, "Error inesperado: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun printStoredFormat(formatPath: String, textVars: Map<Int, String>, imgVars: Map<Int, ZebraImageI> = emptyMap()): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val macAddress = getPrinterMacAddress()
                _printStatus.value = PrintStatus.Connecting
                val connection: Connection = ConnectionBuilder.build("BT:$macAddress")
                try {
                    connection.open()
                    val printer = ZebraPrinterFactory.getLinkOsPrinter(connection) as? ZebraPrinterLinkOs
                        ?: throw IllegalStateException("Impresora no compatible con Link-OS")

                    check(getPrinterStatus(printer) == "Impresora lista") { "Impresora no está lista" }
                    _printStatus.value = PrintStatus.Printing(progress = 0)

                    printer.printStoredFormatWithVarGraphics(formatPath, imgVars, textVars, "UTF-8")
                    _printStatus.value = PrintStatus.Finished(status = "Formato impreso")
                    Result.success("Formato impreso")
                } catch (e: ConnectionException) {
                    _printStatus.value = PrintStatus.Error(message = e.message ?: "Error al imprimir formato")
                    Log.e(TAG, "Error: ${e.message}", e)
                    Result.failure(e)
                } finally {
                    connection.close()
                }
            } catch (e: Exception) {
                _printStatus.value = PrintStatus.Error(message = e.message ?: "Error inesperado")
                Log.e(TAG, "Error inesperado: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun uploadFont(fontPath: String, printerPath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val macAddress = getPrinterMacAddress()
                _printStatus.value = PrintStatus.Connecting
                val connection: Connection = ConnectionBuilder.build("BT:$macAddress")
                try {
                    connection.open()
                    val printer = ZebraPrinterFactory.getInstance(connection)
                    val fontData = File(fontPath).readBytes()
                    val zplCommand = "! 0 200 200 210 1\r\n" +
                            "T ${printerPath} ${fontData.size}\r\n" +
                            fontData.toString(Charsets.UTF_8) +
                            "\r\nEND\r\n"
                    connection.write(zplCommand.toByteArray(Charsets.UTF_8))
                    Log.i(TAG, "Fuente cargada en $printerPath")
                    _printStatus.value = PrintStatus.Finished(status = "Fuente cargada")
                    Result.success("Fuente cargada")
                } catch (e: ConnectionException) {
                    _printStatus.value = PrintStatus.Error(message = e.message ?: "Error al cargar fuente")
                    Log.e(TAG, "Error: ${e.message}", e)
                    Result.failure(e)
                } finally {
                    connection.close()
                }
            } catch (e: Exception) {
                _printStatus.value = PrintStatus.Error(message = e.message ?: "Error inesperado")
                Log.e(TAG, "Error inesperado: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun configurePaperOutAlert(destinationIp: String, port: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val macAddress = getPrinterMacAddress()
                val connection: Connection = ConnectionBuilder.build("BT:$macAddress")
                try {
                    connection.open()
                    val printer = ZebraPrinterFactory.getInstance(connection)
                    val alert = PrinterAlert(
                        AlertCondition.PAPER_OUT,
                        AlertDestination.TCP,
                        true,  // onSet
                        false, // onClear
                        destinationIp,
                        port,
                        false  // quelling
                    )
                    SGD.SET("alerts.configured", alert.getDestinationAsSgdString(), connection)
                    Log.i(TAG, "Alerta de 'sin papel' configurada para $destinationIp:$port")
                    _printStatus.value = PrintStatus.Finished(status = "Alerta configurada")
                    Result.success("Alerta configurada")
                } catch (e: ConnectionException) {
                    _printStatus.value = PrintStatus.Error(message = e.message ?: "Error al configurar alerta")
                    Log.e(TAG, "Error: ${e.message}", e)
                    Result.failure(e)
                } finally {
                    connection.close()
                }
            } catch (e: Exception) {
                _printStatus.value = PrintStatus.Error(message = e.message ?: "Error inesperado")
                Log.e(TAG, "Error inesperado: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private fun saveModifiedPrn(fileName: String, modifiedData: ByteArray) {
        try {
            val file = File(context.filesDir, "modified_$fileName")
            FileOutputStream(file).use { it.write(modifiedData) }
            Log.i(TAG, "Archivo modificado guardado en: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Error al guardar archivo: ${e.message}", e)
        }
    }
}

@Composable
fun ZebraPrinterComposable(
    assetFileName: String,
    onPrintResult: (Result<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val printerHelper = remember { ZebraPrinterHelper(context) }
    var isPrinting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isPrinting = true
        val result = printerHelper.printFromAsset(assetFileName)
        isPrinting = false
        onPrintResult(result)
        result.onSuccess { Log.i(TAG, "Impresión completada: $it") }
            .onFailure { Log.e(TAG, "Error: ${it.message}", it) }
    }

    if (isPrinting) CircularProgressIndicator()
}

sealed class PrintStatus {
    object Idle : PrintStatus()
    object Connecting : PrintStatus()
    object PreparingData : PrintStatus()
    data class Printing(val progress: Int = 0) : PrintStatus()
    data class Finished(val status: String) : PrintStatus()
    data class Error(val message: String) : PrintStatus()
}