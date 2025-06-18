package com.oscar.atestados.utils

import android.util.Log
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger
import java.io.File

private const val TAG = "PDFMerger"

class PDFMerger {
    /**
     * Une el pdfSecundario al pdfPrincipal después de su última página y guarda el resultado en pdfPrincipal.
     * @param pdfPrincipal Archivo del PDF principal que será modificado.
     * @param pdfSecundario Archivo del PDF secundario a añadir.
     * @throws Exception Si ocurre un error al leer, unir o escribir los PDFs.
     */
    fun unirPDFs(pdfPrincipal: File, pdfSecundario: File) {
        if (!pdfPrincipal.exists() || !pdfSecundario.exists()) {
            Log.e(TAG, "Uno o ambos archivos PDF no existen: principal=${pdfPrincipal.absolutePath}, secundario=${pdfSecundario.absolutePath}")
            throw IllegalArgumentException("Los archivos PDF deben existir")
        }

        Log.d(TAG, "Iniciando unión de PDFs: ${pdfPrincipal.absolutePath} + ${pdfSecundario.absolutePath}")

        try {
            // Crear un archivo temporal para el resultado
            val tempFile = File(pdfPrincipal.parent, "temp_${pdfPrincipal.name}")
            PdfWriter(tempFile).use { writer ->
                PdfDocument(writer).use { pdfDocDestino ->
                    val merger = PdfMerger(pdfDocDestino)

                    // Leer y añadir el PDF principal
                    PdfReader(pdfPrincipal).use { readerPrincipal ->
                        PdfDocument(readerPrincipal).use { pdfPrincipalDoc ->
                            merger.merge(pdfPrincipalDoc, 1, pdfPrincipalDoc.numberOfPages)
                            Log.d(TAG, "PDF principal añadido: ${pdfPrincipalDoc.numberOfPages} páginas")
                        }
                    }

                    // Leer y añadir el PDF secundario
                    PdfReader(pdfSecundario).use { readerSecundario ->
                        PdfDocument(readerSecundario).use { pdfSecundarioDoc ->
                            merger.merge(pdfSecundarioDoc, 1, pdfSecundarioDoc.numberOfPages)
                            Log.d(TAG, "PDF secundario añadido: ${pdfSecundarioDoc.numberOfPages} páginas")
                        }
                    }
                }
            }

            // Reemplazar el archivo principal con el archivo temporal
            if (tempFile.exists()) {
                pdfPrincipal.delete()
                tempFile.renameTo(pdfPrincipal)
                Log.d(TAG, "PDF unido guardado en: ${pdfPrincipal.absolutePath}")
            } else {
                Log.e(TAG, "El archivo temporal no se creó correctamente")
                throw IllegalStateException("Error al guardar el PDF unido")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al unir PDFs: ${e.message}", e)
            throw e
        }
    }
}