package com.oscar.atestados.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.graphics.ZebraImageFactory
import com.zebra.sdk.graphics.internal.ZebraImageAndroid
import com.zebra.sdk.printer.PrinterLanguage
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class PDFToBitmapPrinter(private val context: Context) {

    companion object {
        private const val TAG = "PDFToBitmapPrinter"
        const val PRINT_WIDTH = 787  // 100 mm a 200 DPI (100 / 25.4 * 200 ≈ 787 píxeles)
        private const val PRINT_TIMEOUT_MS = 8000L
    }

    sealed interface PrintResult {
        data class Success(val printerName: String, val details: String) : PrintResult
        data class Error(val message: String, val cause: Exception? = null) : PrintResult
    }

    suspend fun printHtmlAsBitmap(
        htmlAssetPath: String,
        macAddress: String,
        outputFileName: String = "temp_label.pdf",
        onStatusUpdate: (String) -> Unit = {}
    ): PrintResult = withContext(Dispatchers.IO) {
        try {
            onStatusUpdate("Iniciando...")

            // 1. Generar el PDF desde HTML (sobrescribe el archivo si ya existe)
            val htmlInputStream = context.assets.open(htmlAssetPath)
            val htmlContent = htmlInputStream.readBytes().toString(Charsets.UTF_8)
            htmlInputStream.close()
            val outputFile = File(context.getExternalFilesDir(null), outputFileName)
            if (outputFile.exists()) outputFile.delete() // Sobrescribir el PDF anterior
            val pdfLabelPrinter = PDFLabelPrinterZebra(context)
            pdfLabelPrinter.generarEtiquetaPdf(htmlContent, outputFile)
            onStatusUpdate("PDF generado en ${outputFile.absolutePath}")

            // 2. Convertir todas las páginas a Bitmaps monocromos
            val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(outputFile)

            if (bitmaps.isEmpty()) {
                onStatusUpdate("Error al convertir PDF a imágenes")
                return@withContext PrintResult.Error("No se pudo convertir el PDF a imágenes")
            }

            val monoBitmaps = bitmaps.mapNotNull { bitmap ->
                bitmap?.let {
                    val monoBitmap = convertToMonochrome(it)
                    onStatusUpdate("Página convertida a imagen monocroma (${monoBitmap.width}x${monoBitmap.height})")
                    monoBitmap
                }
            }

            // 3. Imprimir cada Bitmap y luego reciclarlo
            val printResult = printBitmaps(macAddress, monoBitmaps, onStatusUpdate)

            // 4. Reciclar los Bitmaps después de imprimir
            monoBitmaps.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                    Log.d(TAG, "Bitmap reciclado")
                }
            }

            printResult
        } catch (e: Exception) {
            val errorMessage = "Error al procesar: ${e.message}"
            onStatusUpdate(errorMessage)
            Log.e(TAG, errorMessage, e)
            PrintResult.Error(errorMessage, e)
        }
    }

    private suspend fun printBitmaps(
        macAddress: String,
        bitmaps: List<Bitmap>,
        onStatusUpdate: (String) -> Unit
    ): PrintResult {
        var connection: Connection? = null
        try {
            onStatusUpdate("Conectando a la impresora ($macAddress)...")
            connection = BluetoothConnection(macAddress).apply {
                maxTimeoutForRead = 5000
                timeToWaitForMoreData = 1000
            }
            connection.open()
            if (!connection.isConnected) {
                onStatusUpdate("No se pudo conectar a la impresora")
                return PrintResult.Error("No se pudo conectar a la impresora")
            }
            onStatusUpdate("Conectado a la impresora ($macAddress)")

            val printer: ZebraPrinter = ZebraPrinterFactory.getInstance(connection)
            val printerLanguage = printer.getPrinterControlLanguage()
            onStatusUpdate("Impresora inicializada: $printerLanguage")

            bitmaps.forEachIndexed { index, bitmap ->
                val zebraImage = ZebraImageFactory.getImage(bitmap) as ZebraImageAndroid
                Log.d(TAG, "Dimensiones originales Bitmap: ${bitmap.width}x${bitmap.height}")

                if (printerLanguage == PrinterLanguage.CPCL) {
                    // Configuración para impresoras CPCL
                    connection.write("! U1 setvar \"device.languages\" \"cpcl\"\r\n".toByteArray())
                    TimeUnit.MILLISECONDS.sleep(500)

                    // Comando CPCL corregido (sin rotación y con dimensiones exactas)
                    val cpclCommand = """
                    ! U1 JOURNAL
                    ! U1 CLR
                    ! U1 DEL R:TEMP.PCX
                    PW ${bitmap.width}
                    PCX 0 0 R:TEMP.PCX
                    PRINT
                """.trimIndent()

                    printer.storeImage("R:TEMP.PCX", zebraImage, bitmap.width, bitmap.height)
                    connection.write(cpclCommand.toByteArray())
                } else {
                    // Configuración para impresoras ZPL
                    connection.write("^XA^POI^XZ".toByteArray()) // Establece orientación normal
                    TimeUnit.MILLISECONDS.sleep(500)

                    // Imprimir con dimensiones exactas
                    printer.printImage(
                        zebraImage,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        false
                    )
                }
                TimeUnit.MILLISECONDS.sleep(2000)
            }

            return PrintResult.Success("Zebra ($macAddress)", "Impresión de ${bitmaps.size} páginas completada")
        } catch (e: Exception) {
            val errorMessage = "Error al imprimir: ${e.message}"
            onStatusUpdate(errorMessage)
            Log.e(TAG, errorMessage, e)
            return PrintResult.Error(errorMessage, e)
        } finally {
            connection?.close()
        }
    }

    private fun convertToMonochrome(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val monoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val brightness =
                    (Color.red(pixel) * 0.3 + Color.green(pixel) * 0.59 + Color.blue(pixel) * 0.11).toInt()
                val color = if (brightness < 128) Color.BLACK else Color.WHITE
                monoBitmap.setPixel(x, y, color)
            }
        }
        return monoBitmap
    }
}