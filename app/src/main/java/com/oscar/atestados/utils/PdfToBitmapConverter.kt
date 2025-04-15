package com.oscar.atestados.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

/**
 * Objeto utilitario para convertir páginas de un archivo PDF en imágenes Bitmap.
 *
 * Proporciona funcionalidad para renderizar todas las páginas de un PDF
 * en imágenes de mapa de bits con un DPI objetivo específico.
 */
object PdfToBitmapConverter {
    private const val TAG = "PdfToBitmapConverter"

    /**
     * Convierte todas las páginas de un archivo PDF en una lista de Bitmaps.
     *
     * @param pdfFile Archivo PDF a convertir.
     * @param targetDpi Resolución objetivo en DPI (puntos por pulgada). Por defecto es 200 DPI.
     * @return Lista de Bitmaps, uno por cada página del PDF. Lista vacía si hay errores.
     *
     * @throws SecurityException Si no se tienen permisos para leer el archivo.
     * @throws IllegalStateException Si el PDF está corrupto o no se puede leer.
     *
     * @sample Ejemplo de uso:
     * ```
     * val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(File("ruta/al/archivo.pdf"))
     * bitmaps.forEach { imageView.setImageBitmap(it) }
     * ```
     *
     * @note Los recursos (PdfRenderer y FileDescriptor) se manejan automáticamente
     * y se liberan correctamente incluso en caso de error.
     */
    fun convertAllPagesToBitmaps(pdfFile: File, targetDpi: Int = 200): List<Bitmap> {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            Log.e(TAG, "Archivo PDF no existe o está vacío: ${pdfFile.absolutePath}")
            return emptyList()
        }

        val bitmaps = mutableListOf<Bitmap>()
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)

            for (pageIndex in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                try {
                    // Calcular dimensiones para el DPI objetivo
                    val width = (page.width * (targetDpi / 72f)).toInt()
                    val height = (page.height * (targetDpi / 72f)).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                    }

                    // Renderizar escalando al tamaño calculado
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)

                    Log.d(TAG, "Página $pageIndex: ${bitmap.width}x${bitmap.height} px @ $targetDpi DPI")
                } finally {
                    page.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir PDF: ${e.message}", e)
            bitmaps.forEach { it.recycle() }
            return emptyList()
        } finally {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error al cerrar recursos", e)
            }
        }

        return bitmaps
    }
}