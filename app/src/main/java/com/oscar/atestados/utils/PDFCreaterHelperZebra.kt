package com.oscar.atestados.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.font.FontProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

internal class PDFCreaterHelperZebra {

    companion object {
        private const val TAG = "PDFCreaterHelperZebra"
        private const val MM_TO_POINTS = 2.83465f
        private val PAGE_WIDTH_MM = 98f  // Ancho en mm
        private val PAGE_HEIGHT_MM = 279f // Alto en mm
        private val MARGIN_MM = 2f       // Márgenes en mm
        private const val TITLE_FONT_SIZE = "10pt"  // Tamaño del título
        private const val BODY_FONT_SIZE = "8pt"    // Tamaño del cuerpo
    }

    fun convertHtmlToImage(context: Context, htmlFile: File, replacements: Map<String, String>): Bitmap? {
        Log.i(TAG, "Iniciando conversión de HTML a imagen")
        val tempHtmlFile = File(context.cacheDir, "temp_zebra.html")

        try {
            var htmlContent = htmlFile.readText()
            replacements.forEach { (key, value) ->
                htmlContent = htmlContent.replace("{{$key}}", value)
                Log.d(TAG, "Sustituido '{{$key}}' con '$value'")
            }

            htmlContent = htmlContent.replace(
                "font-size: 11pt;",
                "font-size: $BODY_FONT_SIZE;"
            ).replace(
                "font-size: 12pt;",
                "font-size: $TITLE_FONT_SIZE;"
            )
            tempHtmlFile.writeText(htmlContent)
            Log.d(TAG, "HTML temporal guardado en: ${tempHtmlFile.absolutePath}")

            val baos = ByteArrayOutputStream()
            val writer = PdfWriter(baos)
            val pdf = PdfDocument(writer)
            val pageWidth = PAGE_WIDTH_MM * MM_TO_POINTS
            val pageHeight = PAGE_HEIGHT_MM * MM_TO_POINTS
            pdf.setDefaultPageSize(PageSize(pageWidth, pageHeight))

            val document = Document(pdf)
            val marginPoints = MARGIN_MM * MM_TO_POINTS
            document.setMargins(marginPoints, marginPoints, marginPoints, marginPoints)

            val fontProvider = FontProvider()
            fontProvider.addFont(context.assets.open("fonts/calibri-regular.ttf").readBytes())
            val converterProperties = ConverterProperties().apply {
                this.fontProvider = fontProvider
                this.baseUri = "file:///android_asset/"
            }

            HtmlConverter.convertToDocument(tempHtmlFile.readText(), pdf, converterProperties)

            document.close()
            pdf.close()

            val bitmap = pdfToBitmap(context, baos.toByteArray())
            tempHtmlFile.delete()
            Log.i(TAG, "Imagen generada con éxito")
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar imagen: ${e.message}", e)
            tempHtmlFile.delete()
            return null
        }
    }

    private fun pdfToBitmap(context: Context, pdfBytes: ByteArray): Bitmap? {
        try {
            val tempPdfFile = File(context.cacheDir, "temp_pdf_zebra.pdf")
            FileOutputStream(tempPdfFile).use { it.write(pdfBytes) }

            val fileDescriptor = ParcelFileDescriptor.open(tempPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            val page = pdfRenderer.openPage(0)
            val bitmap = Bitmap.createBitmap(
                (PAGE_WIDTH_MM * MM_TO_POINTS).toInt(),
                (PAGE_HEIGHT_MM * MM_TO_POINTS).toInt(),
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            pdfRenderer.close()
            fileDescriptor.close()
            tempPdfFile.delete()

            Log.d(TAG, "Bitmap creado con éxito: ${bitmap.width}x${bitmap.height}")
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir PDF a Bitmap: ${e.message}", e)
            return null
        }
    }
}