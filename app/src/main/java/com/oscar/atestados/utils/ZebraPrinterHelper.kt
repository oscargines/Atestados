import android.content.Context
import android.util.Log
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.PrinterLanguage
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException

// Tag para los logs
private const val TAG = "ZebraPrinterHelper"

/**
 * Clase auxiliar para manejar la impresión en impresoras Zebra a través de Bluetooth.
 * Soporta modelos como RW420 y ZQ521, ajustando dinámicamente el lenguaje (ZPL o CPCL)
 * según el archivo .prn y el estado de la impresora.
 *
 * @param context Contexto de la aplicación para acceder a assets y DataStore.
 */
class ZebraPrinterHelper(private val context: Context) {

    /**
     * Obtiene la dirección MAC de la impresora desde DataStore.
     *
     * @return Dirección MAC configurada en DataStore.
     * @throws IllegalStateException si no hay una MAC configurada.
     */
    private suspend fun getPrinterMacAddress(): String {
        Log.d(TAG, "Obteniendo dirección MAC de la impresora desde DataStore")
        val preferences = context.dataStoreImp.data.first()
        return preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
            ?: throw IllegalStateException("No hay dirección MAC de impresora configurada")
    }

    /**
     * Detecta el lenguaje del archivo .prn (CPCL o ZPL) basado en su contenido.
     *
     * @param prnData Datos del archivo .prn en formato de bytes.
     * @return [PrinterLanguage] detectado (CPCL o ZPL).
     * @throws IllegalArgumentException si el formato no es CPCL ni ZPL.
     */
    private fun detectFileLanguage(prnData: ByteArray): PrinterLanguage {
        Log.d(TAG, "Detectando lenguaje del archivo .prn")
        val prnText = String(prnData, Charsets.UTF_8).trim()
        return when {
            prnText.startsWith("!") || prnText.contains("PW") || prnText.contains("BAR-SENSE") -> {
                Log.i(TAG, "Lenguaje detectado: CPCL")
                PrinterLanguage.CPCL
            }
            prnText.startsWith("^XA") || prnText.contains("^FO") || prnText.contains("^XZ") -> {
                Log.i(TAG, "Lenguaje detectado: ZPL")
                PrinterLanguage.ZPL
            }
            else -> {
                Log.e(TAG, "Formato .prn no reconocido: $prnText")
                throw IllegalArgumentException("Formato .prn no reconocido (ni CPCL ni ZPL)")
            }
        }
    }

    /**
     * Obtiene el estado de la impresora si es compatible (principalmente para ZQ521 Link-OS).
     *
     * @param printer Instancia de [ZebraPrinter] conectada.
     * @param printerId Identificador del modelo de impresora (e.g., "ZQ521" o "Unknown").
     * @return Mensaje de estado o null si no se puede obtener.
     */
    private fun getPrinterStatus(printer: ZebraPrinter, printerId: String): String? {
        Log.d(TAG, "Obteniendo estado de la impresora: $printerId")
        return try {
            if (printerId.contains("ZQ521")) {
                val status = SGD.GET("device.printhead.status", printer.connection)
                Log.d(TAG, "Estado del cabezal (ZQ521): $status")
                val printerStatus = printer.getCurrentStatus()
                when {
                    printerStatus.isReadyToPrint -> {
                        Log.i(TAG, "Impresora ZQ521 lista para imprimir")
                        "Impresora lista"
                    }
                    !printerStatus.isHeadOpen -> {
                        Log.w(TAG, "Cabezal cerrado, verificando estado")
                        "Cabezal cerrado, verificando..."
                    }
                    else -> {
                        Log.w(TAG, "Estado desconocido de la impresora: $status")
                        "Estado: $status"
                    }
                }
            } else {
                val status: PrinterStatus = printer.getCurrentStatus()
                if (status.isReadyToPrint) {
                    Log.i(TAG, "Impresora lista para imprimir (no ZQ521)")
                    "Impresora lista"
                } else {
                    Log.w(TAG, "Impresora no lista (no ZQ521)")
                    "Impresora no lista"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estado de la impresora: ${e.message}", e)
            null
        }
    }

    /**
     * Imprime un archivo .prn desde la carpeta assets/formatos/ en una impresora Zebra.
     * Detecta el lenguaje del archivo y ajusta la configuración de la impresora si es necesario.
     *
     * @param assetFileName Nombre del archivo .prn en assets/formatos/ (e.g., "label.zpl").
     * @return [Result<String>] con mensaje de éxito/estado o fallo.
     * @throws ConnectionException si falla la conexión Bluetooth.
     * @throws IOException si falla la lectura del archivo.
     * @throws IllegalArgumentException si el formato del archivo no es soportado.
     * @throws Exception para otros errores inesperados.
     */
    suspend fun printFromAsset(assetFileName: String): Result<String> {
        var connection: BluetoothConnection? = null
        Log.d(TAG, "Iniciando impresión del archivo: $assetFileName")
        return try {
            val macAddress = getPrinterMacAddress()
            Log.d(TAG, "Conectando a la impresora con MAC: $macAddress")
            connection = BluetoothConnection(macAddress, 5000, 500)
            connection.open()
            if (!connection.isConnected) {
                Log.e(TAG, "No se pudo establecer conexión Bluetooth con $macAddress")
                throw ConnectionException("No se pudo conectar a $macAddress")
            }
            Log.i(TAG, "Conexión Bluetooth establecida con éxito")

            val printer: ZebraPrinter = ZebraPrinterFactory.getInstance(connection)
            val currentLanguage = printer.getPrinterControlLanguage()
            val printerId = try {
                SGD.GET("device.unique_id", connection)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo obtener ID de la impresora: ${e.message}")
                "Unknown"
            }
            Log.i(TAG, "Modelo: $printerId, Lenguaje actual: $currentLanguage")

            val filePath = "formatos/$assetFileName"
            Log.d(TAG, "Leyendo archivo desde assets: $filePath")
            val inputStream = context.assets.open(filePath)
            val prnData = inputStream.readBytes()
            Log.i(TAG, "Archivo leído correctamente, tamaño: ${prnData.size} bytes")

            val fileLanguage = detectFileLanguage(prnData)
            Log.i(TAG, "Lenguaje del archivo detectado: $fileLanguage")

            if (currentLanguage != fileLanguage) {
                val command = when (fileLanguage) {
                    PrinterLanguage.CPCL -> "!\r\n"
                    PrinterLanguage.ZPL -> "^XA^SZ2^XZ\r\n"
                    else -> throw IllegalStateException("Lenguaje no soportado: $fileLanguage")
                }
                Log.d(TAG, "Cambiando lenguaje de la impresora a $fileLanguage con comando: $command")
                connection.write(command.toByteArray())
                Thread.sleep(500)
                Log.i(TAG, "Lenguaje ajustado correctamente")
            }

            Log.d(TAG, "Enviando datos de impresión a la impresora")
            connection.write(prnData)
            Log.i(TAG, "Datos enviados a la impresora")

            val delay = if (printerId.contains("ZQ521")) 1000L else 1500L
            Log.d(TAG, "Esperando $delay ms para completar la impresión")
            delay(delay)

            val status = getPrinterStatus(printer, printerId) ?: "Impresión enviada"
            Log.i(TAG, "Estado final de la impresión: $status")
            Result.success(status)
        } catch (e: ConnectionException) {
            Log.e(TAG, "Error de conexión Bluetooth: ${e.message}", e)
            Result.failure(Exception("Error de conexión Bluetooth: ${e.message}"))
        } catch (e: IOException) {
            Log.e(TAG, "Error al leer el archivo: ${e.message}", e)
            Result.failure(Exception("Error al leer el archivo: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error en el formato del archivo: ${e.message}", e)
            Result.failure(Exception("Error en el formato del archivo: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al imprimir: ${e.message}", e)
            Result.failure(Exception("Error al imprimir: ${e.message}"))
        } finally {
            try {
                connection?.close()
                Log.i(TAG, "Conexión Bluetooth cerrada")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar conexión: ${e.message}", e)
            }
        }
    }
}

/**
 * Composable para imprimir un archivo .prn usando [ZebraPrinterHelper].
 * Muestra un [CircularProgressIndicator] durante la impresión y notifica el resultado.
 *
 * @param assetFileName Nombre del archivo .prn en assets/formatos/ (e.g., "label.zpl").
 * @param onPrintResult Callback que recibe el [Result<String>] con el estado de la impresión.
 */
@Composable
fun ZebraPrinterComposable(
    assetFileName: String,
    onPrintResult: (Result<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val printerHelper = remember { ZebraPrinterHelper(context) }
    var isPrinting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Iniciando ZebraPrinterComposable para archivo: $assetFileName")
        isPrinting = true
        val result = printerHelper.printFromAsset(assetFileName)
        isPrinting = false
        onPrintResult(result)
        result.onSuccess { status ->
            Log.i(TAG, "Impresión completada en composable: $status")
        }.onFailure { exception ->
            Log.e(TAG, "Fallo en composable: ${exception.message}", exception)
        }
    }

    if (isPrinting) {
        Log.d(TAG, "Mostrando CircularProgressIndicator")
        CircularProgressIndicator()
    }
}

/**
 * Ejemplo de pantalla que usa [ZebraPrinterComposable] para imprimir una etiqueta.
 * Muestra un indicador de progreso y maneja el resultado de la impresión.
 */
@Composable
fun PrintLabelScreen() {
    Log.d(TAG, "Renderizando PrintLabelScreen")
    ZebraPrinterComposable(
        assetFileName = "label.zpl",
        onPrintResult = { result ->
            result.onSuccess { status ->
                Log.i(TAG, "Impresión exitosa en PrintLabelScreen: $status")
            }.onFailure { exception ->
                Log.e(TAG, "Error al imprimir en PrintLabelScreen: ${exception.message}", exception)
            }
        }
    )
}