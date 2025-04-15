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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Clase para imprimir contenido HTML/PDF en impresoras Zebra mediante conversión a imágenes.
 *
 * @property context Contexto de Android para acceder a recursos y almacenamiento.
 */
class PDFToBitmapPrinter(private val context: Context) {

    companion object {
        private const val TAG = "PDFToBitmapPrinter"
        // 99 mm a 200 DPI (99 / 25.4 * 200 ≈ 780 píxeles)
        const val PRINT_WIDTH = 780
        private const val PRINT_TIMEOUT_MS = 8000L
    }

    /**
     * Resultado de la operación de impresión.
     */
    sealed interface PrintResult {
        /**
         * Indica que la impresión fue exitosa.
         * @property printerName Nombre/identificador de la impresora.
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
     * Procesa e imprime contenido HTML como imágenes en una impresora Zebra.
     *
     * @param htmlAssetPath Ruta al archivo HTML en assets (alternativa a htmlContent).
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     * @param outputFileName Nombre del archivo PDF temporal a generar.
     * @param htmlContent Contenido HTML directo (alternativa a htmlAssetPath).
     * @param onStatusUpdate Callback para actualizaciones de estado durante el proceso.
     * @return [PrintResult] con el resultado de la operación.
     *
     * @throws IllegalArgumentException Si no se proporciona contenido HTML válido.
     * @throws SecurityException Si no hay permisos para escribir en almacenamiento.
     */
    suspend fun printHtmlAsBitmap(
        htmlAssetPath: String = "",
        macAddress: String,
        outputFileName: String = "temp_label.pdf",
        htmlContent: String? = null,
        onStatusUpdate: (String) -> Unit = {}
    ): PrintResult = withContext(Dispatchers.IO) {
        try {
            onStatusUpdate("Iniciando...")

            // Determinar el contenido HTML a usar
            val finalHtmlContent = when {
                !htmlContent.isNullOrEmpty() -> htmlContent
                htmlAssetPath.isNotEmpty() -> context.assets.open(htmlAssetPath).use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                else -> throw IllegalArgumentException("Debe proporcionar htmlContent o un htmlAssetPath válido")
            }

            // Generar el PDF desde HTML
            val outputFile = File(context.getExternalFilesDir(null), outputFileName)
            if (outputFile.exists()) outputFile.delete()
            val pdfLabelPrinter = PDFLabelPrinterZebra(context)
            pdfLabelPrinter.generarEtiquetaPdf(finalHtmlContent, outputFile)
            onStatusUpdate("PDF generado en ${outputFile.absolutePath}")

            // Convertir a Bitmaps monocromos
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

            // Imprimir y reciclar
            val printResult = printBitmaps(macAddress, monoBitmaps, onStatusUpdate)
            monoBitmaps.forEach { if (!it.isRecycled) it.recycle() }

            printResult
        } catch (e: Exception) {
            val errorMessage = "Error al procesar: ${e.message}"
            onStatusUpdate(errorMessage)
            Log.e(TAG, errorMessage, e)
            PrintResult.Error(errorMessage, e)
        }
    }

    /**
     * Envía los bitmaps a la impresora Zebra.
     *
     * @param macAddress Dirección MAC de la impresora Bluetooth.
     * @param bitmaps Lista de bitmaps a imprimir.
     * @param onStatusUpdate Callback para actualizaciones de estado.
     * @return [PrintResult] con el resultado de la impresión.
     *
     * @note Maneja automáticamente la segmentación de imágenes grandes para impresoras RW420.
     */
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

            // Dimensiones objetivo: 99 mm x 280 mm a 200 DPI
            val targetWidth = PRINT_WIDTH // 99 mm
            val targetHeight = ((280f / 99f) * targetWidth).toInt() // Proporcional a 280 mm
            val maxHeightRW420 = 1024 // Límite conservador para RW420

            bitmaps.forEachIndexed { index, bitmap ->
                // Ajustar el bitmap al ancho objetivo
                val scaledBitmap = if (bitmap.width != targetWidth) {
                    Bitmap.createScaledBitmap(bitmap, targetWidth, (bitmap.height * targetWidth.toFloat() / bitmap.width).toInt(), true)
                } else {
                    bitmap
                }

                // Determinar si segmentar (solo para RW420 si excede memoria)
                val segments = if (scaledBitmap.height > maxHeightRW420 && macAddress.contains("RW", ignoreCase = true)) {
                    segmentBitmap(scaledBitmap, targetWidth, maxHeightRW420)
                } else {
                    listOf(scaledBitmap)
                }

                segments.forEachIndexed { segmentIndex, segmentBitmap ->
                    val zebraImage = ZebraImageFactory.getImage(segmentBitmap) as ZebraImageAndroid
                    Log.d(TAG, "Segmento $segmentIndex de página $index: ${segmentBitmap.width}x${segmentBitmap.height}")

                    if (printerLanguage == PrinterLanguage.CPCL) {
                        connection.write("! U1 setvar \"device.languages\" \"cpcl\"\r\n".toByteArray())
                        delay(500)

                        val cpclCommand = """
                            ! U1 JOURNAL
                            ! U1 CLR
                            ! U1 DEL R:TEMP.PCX
                            PW $targetWidth
                            PCX 0 0 R:TEMP.PCX
                            PRINT
                        """.trimIndent()

                        printer.storeImage("R:TEMP.PCX", zebraImage, segmentBitmap.width, segmentBitmap.height)
                        connection.write(cpclCommand.toByteArray())
                    } else {
                        printer.printImage(zebraImage, 0, 0, segmentBitmap.width, segmentBitmap.height, false)
                    }
                    delay(2000) // Espera entre segmentos
                }

                // Reciclar el bitmap escalado si no es el original
                if (scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
                    scaledBitmap.recycle()
                }
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

    /**
     * Divide un bitmap en segmentos verticales para impresoras con memoria limitada.
     *
     * @param bitmap Bitmap original a segmentar.
     * @param maxWidth Ancho máximo de los segmentos.
     * @param maxHeight Altura máxima de cada segmento.
     * @return Lista de bitmaps segmentados.
     */
    private fun segmentBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): List<Bitmap> {
        val segments = mutableListOf<Bitmap>()
        var remainingHeight = bitmap.height
        var yOffset = 0

        while (remainingHeight > 0) {
            val segmentHeight = minOf(remainingHeight, maxHeight)
            val segmentBitmap = Bitmap.createBitmap(bitmap, 0, yOffset, bitmap.width, segmentHeight)
            segments.add(segmentBitmap)
            yOffset += segmentHeight
            remainingHeight -= segmentHeight
        }

        return segments
    }

    /**
     * Convierte un bitmap a escala de grises (monocromo) usando dithering.
     *
     * @param bitmap Bitmap original en color.
     * @return Nuevo bitmap en blanco y negro.
     */
    private fun convertToMonochrome(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val monoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) * 0.3 + Color.green(pixel) * 0.59 + Color.blue(pixel) * 0.11).toInt()
                val color = if (brightness < 128) Color.BLACK else Color.WHITE
                monoBitmap.setPixel(x, y, color)
            }
        }
        return monoBitmap
    }
}