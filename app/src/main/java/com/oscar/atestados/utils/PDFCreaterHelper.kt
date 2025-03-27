package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.font.FontProvider
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.io.FileOutputStream

internal class PDFCreaterHelper {

    companion object {
        private const val TAG = "PDFCreaterHelper"
        private const val MM_TO_POINTS = 2.83465f
    }

    @Throws(Exception::class)
    fun convertHtmlToPdf(context: Context, htmlFile: File, replacements: Map<String, String>, fileName: String): File {
        Log.i(TAG, "Iniciando conversión de HTML a PDF para el archivo: $fileName")
        val outputFile = File(context.getExternalFilesDir(null), "$fileName.pdf")
        val tempHtmlFile = File(context.cacheDir, "temp_$fileName.html")
        val tempPdfFile = File(context.cacheDir, "temp_$fileName.pdf")

        try {
            // Paso 1: Preparar el HTML con reemplazos
            var htmlContent = htmlFile.readText()
            replacements.forEach { (key, value) ->
                htmlContent = htmlContent.replace("{{$key}}", value)
                Log.d(TAG, "Sustituido '{{$key}}' con '$value'")
            }
            tempHtmlFile.writeText(htmlContent)
            Log.d(TAG, "HTML temporal guardado en: ${tempHtmlFile.absolutePath}")

            // Paso 2: Convertir HTML a PDF temporal
            val writer = PdfWriter(FileOutputStream(tempPdfFile))
            val pdf = PdfDocument(writer)
            pdf.setDefaultPageSize(PageSize.A4)
            val document = Document(pdf)
            document.setMargins(5 * MM_TO_POINTS, 29 * MM_TO_POINTS, 5 * MM_TO_POINTS, 33 * MM_TO_POINTS)

            val fontProvider = FontProvider()
            fontProvider.addFont(context.assets.open("fonts/calibri-regular.ttf").readBytes())
            val converterProperties = ConverterProperties().apply {
                this.fontProvider = fontProvider
                this.baseUri = "file:///android_asset/"
            }
            HtmlConverter.convertToDocument(tempHtmlFile.readText(), pdf, converterProperties)
            document.close()

            // Paso 3: Abrir el PDF temporal y añadir elementos fijos
            val pdfReader = PdfReader(tempPdfFile)
            val pdfWriter = PdfWriter(FileOutputStream(outputFile))
            val finalPdf = PdfDocument(pdfReader, pdfWriter)
            val finalDocument = Document(finalPdf)

            val escudoFont: PdfFont = PdfFontFactory.createFont(
                context.assets.open("fonts/escudo.ttf").readBytes(),
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )
            val boldFont: PdfFont = PdfFontFactory.createFont(
                context.assets.open("fonts/calibri-bold.ttf").readBytes(),
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )

            val numberOfPages = finalPdf.numberOfPages
            Log.d(TAG, "Añadiendo elementos fijos a $numberOfPages páginas")
            for (i in 1..numberOfPages) {
                val page = finalPdf.getPage(i)
                if (page == null) {
                    Log.e(TAG, "Página $i es nula, saltando...")
                    continue
                }
                val canvas = PdfCanvas(page)

                val atestadoBox = Paragraph("ATESTADO NÚMERO:")
                    .setFont(boldFont)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setBorder(com.itextpdf.layout.borders.SolidBorder(0.8f))
                    .setHeight(6 * MM_TO_POINTS)
                    .setWidth(75 * MM_TO_POINTS)
                    .setMarginLeft(3 * MM_TO_POINTS)
                    .setPaddingLeft(3 * MM_TO_POINTS)
                    .setFixedPosition(i, 28 * MM_TO_POINTS, 283 * MM_TO_POINTS, 75 * MM_TO_POINTS)
                val folioBox = Paragraph("FOLIO Nº")
                    .setFont(boldFont)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setBorder(com.itextpdf.layout.borders.SolidBorder(0.8f))
                    .setHeight(6 * MM_TO_POINTS)
                    .setWidth(25 * MM_TO_POINTS)
                    .setPaddingLeft(3 * MM_TO_POINTS)
                    .setFixedPosition(i, 163 * MM_TO_POINTS, 283 * MM_TO_POINTS, 25 * MM_TO_POINTS)
                finalDocument.add(atestadoBox)
                finalDocument.add(folioBox)

                val leftLetter = Paragraph(Text("A").setFont(escudoFont).setFontSize(48f))
                    .setFixedPosition(i, 5 * MM_TO_POINTS, 268 * MM_TO_POINTS, 60f)
                val rightLetter = Paragraph(Text("G").setFont(escudoFont).setFontSize(36f))
                    .setFixedPosition(i, 198 * MM_TO_POINTS, 272 * MM_TO_POINTS, 36f)
                finalDocument.add(leftLetter)
                finalDocument.add(rightLetter)

                canvas.setLineWidth(0.8f)
                canvas.moveTo((25 * MM_TO_POINTS).toDouble(), (20 * MM_TO_POINTS).toDouble())
                    .lineTo((25 * MM_TO_POINTS).toDouble(), (290 * MM_TO_POINTS).toDouble())
                    .stroke()
                canvas.moveTo((195 * MM_TO_POINTS).toDouble(), (20 * MM_TO_POINTS).toDouble())
                    .lineTo((194 * MM_TO_POINTS).toDouble(), (290 * MM_TO_POINTS).toDouble())
                    .stroke()
            }

            finalDocument.close()
            pdfReader.close()

            Log.i(TAG, "PDF generado en ${outputFile.absolutePath}")
            tempHtmlFile.delete()
            tempPdfFile.delete()
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar PDF: ${e.message}", e)
            tempHtmlFile.delete()
            tempPdfFile.delete()
            throw Exception("Error creando el PDF: ${e.message}")
        }
    }
}