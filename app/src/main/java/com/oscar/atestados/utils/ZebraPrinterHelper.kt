
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oscar.atestados.screens.dataStoreImp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ZebraPrinterHelper(
    private val context: Context
) {
    // Obtener la MAC address desde DataStore
    private fun getPrinterMacAddress(): String {
        return runBlocking {
            val preferences = context.dataStoreImp.data.first()
            preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
                ?: throw IllegalStateException("No hay dirección MAC de impresora configurada")
        }
    }

    suspend fun printZplFromAsset(assetFileName: String): Result<Unit> {
        return try {
            // Obtener la dirección MAC desde DataStore
            val macAddress = getPrinterMacAddress()

            // Establecer conexión Bluetooth
            val connection: Connection = BluetoothConnection(macAddress)
            connection.open()

            // Verificar que la conexión esté establecida
            if (!connection.isConnected) {
                throw ConnectionException("No se pudo establecer conexión Bluetooth")
            }

            // Obtener el archivo ZPL desde assets/formatos/
            val filePath = "formatos/$assetFileName"
            val inputStream = context.assets.open(filePath)
            val zplData = inputStream.bufferedReader().use { it.readText() }

            // Obtener instancia de la impresora
            val printer: ZebraPrinter = ZebraPrinterFactory.getInstance(connection)

            // Enviar el ZPL a imprimir
            printer.sendCommand(zplData)

            // Cerrar conexión
            connection.close()
            Result.success(Unit)
        } catch (e: ConnectionException) {
            Result.failure(Exception("Error de conexión Bluetooth: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error al imprimir: ${e.message}"))
        }
    }
}

@Composable
fun ZebraPrinterComposable(
    assetFileName: String,
    onPrintResult: (Result<Unit>) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE) }
    val printerHelper = remember { ZebraPrinterHelper(context) }

    LaunchedEffect(Unit) {
        printerHelper.printZplFromAsset(assetFileName)
            .also { result -> onPrintResult(result) }
    }
}

// Ejemplo de uso
@Composable
fun PrintLabelScreen() {
    ZebraPrinterComposable(
        assetFileName = "label.zpl",
        onPrintResult = { result ->
            result.onSuccess {
                // Éxito al imprimir
            }.onFailure { exception ->
                // Manejar error
            }
        }
    )
}

