package com.oscar.atestados.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Xml
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.XmlPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * Clase auxiliar para imprimir XML en impresoras Zebra usando CPCL.
 *
 * @param context Contexto de la aplicación Android.
 */
class XmlPrinterHelperZebra(private val context: Context) {

    companion object {
        private const val TAG = "XmlPrinterHelperZebra"
        private const val TEMPLATE_NAME = "AsistenciaJuridicaGratuita.cpl"
        private const val DEFAULT_QUANTITY = "1"
    }

    /**
     * Imprime un XML en una impresora Zebra a través de Bluetooth.
     *
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     * @param xmlContent Contenido XML a imprimir.
     * @param onStatusUpdate Callback opcional para actualizar el estado en la UI.
     * @return Resultado de la operación con un mensaje de éxito o error.
     */
    @SuppressLint("MissingPermission")
    suspend fun printXml(
        macAddress: String,
        xmlContent: String,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            Log.i(TAG, "Iniciando impresión de XML en $macAddress")
            connection = BluetoothConnection(macAddress)
            connection.open()
            if (!connection.isConnected) {
                Log.e(TAG, "Fallo al conectar con la impresora")
                withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error de conexión con la impresora") }
                return@withContext Result.failure(ConnectionException("No se pudo conectar"))
            }
            Log.i(TAG, "Conexión establecida con $macAddress")

            // Parsear el XML manualmente
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")
            var eventType = parser.eventType
            val cpclCommands = StringBuilder("! 0 200 200 1050 1\n")
            var yPosition = 10
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "variable") {
                    val name = parser.getAttributeValue(null, "name")
                    parser.next()
                    val value = parser.text.trim()
                    cpclCommands.append("TEXT 4 0 10 $yPosition {$name}\n")
                    yPosition += 40 // Incrementar posición vertical
                }
                eventType = parser.next()
            }
            cpclCommands.append("FORM\nPRINT\n")

            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Enviando datos a la impresora...") }
            connection.write(cpclCommands.toString().toByteArray(Charsets.UTF_8))

            val successMsg = "Impresión CPCL enviada correctamente"
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke(successMsg) }
            Log.i(TAG, successMsg)
            Result.success(successMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Error al imprimir XML: ${e.message}", e)
            withContext(Dispatchers.Main) { onStatusUpdate?.invoke("Error: ${e.message}") }
            Result.failure(e)
        } finally {
            connection?.close()
        }
    }

    /**
     * Envía la plantilla CPCL a la impresora si no está ya almacenada.
     *
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     */
    suspend fun sendTemplateToPrinter(macAddress: String) = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = BluetoothConnection(macAddress)
            connection.open()
            val templateContent = """
            ! 0 200 200 1050 1
            TEXT 4 0 10 10 {Title}
            TEXT 4 0 10 50 {Intro}
            TEXT 4 0 10 150 {Article2Title}
            TEXT 4 0 10 190 {Article2Intro}
            TEXT 4 0 10 230 {Article2Item1}
            TEXT 4 0 10 270 {Article2Item2}
            TEXT 4 0 10 310 {Article2Item3}
            TEXT 4 0 10 350 {Article2Item4}
            TEXT 4 0 10 450 {Article12Title}
            TEXT 4 0 10 490 {Article12Item1}
            TEXT 4 0 10 590 {Article9Title}
            TEXT 4 0 10 630 {Article9Item1}
            TEXT 4 0 10 730 {Footer}
            FORM
            PRINT
        """.trimIndent()
            // Almacenar la plantilla en la impresora
            connection.write("! 0 200 200 1050 1\nFILE $TEMPLATE_NAME\n$templateContent\n".toByteArray())
            Log.i(TAG, "Plantilla $TEMPLATE_NAME enviada a la impresora")

            // Verificar si la plantilla está almacenada
            val response = connection.sendAndWaitForResponse("! U1 DIR\n".toByteArray(), 1000, 1000, null)
            val responseString = response?.toString(Charsets.UTF_8)
            Log.d(TAG, "Archivos en la impresora: $responseString")
            if (responseString?.contains(TEMPLATE_NAME) == true) {
                Log.i(TAG, "Plantilla $TEMPLATE_NAME confirmada en la impresora")
            } else {
                Log.w(TAG, "Plantilla $TEMPLATE_NAME no encontrada en la impresora")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar plantilla: ${e.message}", e)
        } finally {
            connection?.close()
        }
    }
}