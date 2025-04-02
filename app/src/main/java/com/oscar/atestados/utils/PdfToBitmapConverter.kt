package com.oscar.atestados.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

object PdfToBitmapConverter {
    private const val TAG = "PdfToBitmapConverter"

    /**
     * Convierte todas las páginas de un PDF a Bitmaps con DPI específico
     * @param pdfFile Archivo PDF de entrada
     * @param targetDpi DPI deseado (ej: 200 para impresoras Zebra)
     * @return Lista de Bitmaps (una por página) o lista vacía si hay error
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
            // Limpiar bitmaps generados si hay error
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