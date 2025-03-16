package com.oscar.atestados.utils

import android.content.Context
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.io.image.ImageDataFactory
import org.commonmark.node.*
import org.commonmark.parser.Parser
import java.io.File
import java.io.FileOutputStream

/**
 * Clase interna que ayuda a crear archivos PDF a partir de texto en formato Markdown.
 */
internal class PDFCreaterHelper {

    /**
     * Convierte texto en formato Markdown a un archivo PDF con diseño personalizado.
     *
     * @param context Contexto de la aplicación Android necesario para acceder al almacenamiento y assets.
     * @param markdownText Texto en formato Markdown que se convertirá a PDF.
     * @param fileName Nombre del archivo PDF que se generará (sin la extensión).
     * @return El archivo PDF generado.
     * @throws Exception Si ocurre un error durante la creación del PDF.
     */
    @Throws(Exception::class)
    fun convertMarkdownToPdf(context: Context, markdownText: String?, fileName: String): File {
        val file = File(context.getExternalFilesDir(null), "$fileName.pdf")

        try {
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4)

            // Define las fuentes GillSans desde assets
            val boldFont: PdfFont = PdfFontFactory.createFont(context.assets.open("GillSans-Bold.otf").readBytes(), PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)
            val regularFont: PdfFont = PdfFontFactory.createFont(context.assets.open("GillSans.otf").readBytes(), PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)

            // Encabezado con dos cajas
            val headerLeft = Paragraph("Caja Izquierda")
                .setFont(boldFont)
                .setFontSize(12f)
                .setBorder(com.itextpdf.layout.borders.SolidBorder(1f))
                .setWidth(100f)
                .setFixedPosition(36f, 780f, 100f)
            val headerRight = Paragraph("Caja Derecha")
                .setFont(boldFont)
                .setFontSize(12f)
                .setBorder(com.itextpdf.layout.borders.SolidBorder(1f))
                .setWidth(100f)
                .setFixedPosition(458f, 780f, 100f)
            document.add(headerLeft)
            document.add(headerRight)

            // Imágenes en los laterales (asegúrate de tener las imágenes en assets)
            val leftImage = Image(ImageDataFactory.create(context.assets.open("escudo.png").readBytes()))
                .setFixedPosition(10f, 400f)
                .scaleToFit(30f, 30f)
            val rightImage = Image(ImageDataFactory.create(context.assets.open("escudo_gc.png").readBytes()))
                .setFixedPosition(554f, 400f)
                .scaleToFit(30f, 30f)
            document.add(leftImage)
            document.add(rightImage)

            // Ajustar márgenes para que el texto quede entre las líneas
            document.setMargins(50f, 60f, 50f, 60f)

            val parser = Parser.builder().build()
            val parsedDocument = parser.parse(markdownText)

            parsedDocument.accept(object : AbstractVisitor() {
                override fun visit(heading: Heading) {
                    val headingText = extractText(heading)
                    val text: Text = Text(headingText)
                        .setFont(boldFont)
                        .setFontSize(14f + (6 - heading.level) * 2)
                    val paragraph = Paragraph(text)
                        .setTextAlignment(TextAlignment.LEFT)
                    document.add(paragraph)
                }

                override fun visit(paragraph: org.commonmark.node.Paragraph) {
                    val paragraphText = extractText(paragraph)
                    val text: Text = Text(paragraphText)
                        .setFont(regularFont)
                    val paragraphElement = Paragraph(text)
                        .setTextAlignment(TextAlignment.JUSTIFIED)
                    document.add(paragraphElement)
                }

                override fun visit(listItem: ListItem) {
                    val listItemText = extractText(listItem)
                    val text: Text = Text("• $listItemText")
                        .setFont(regularFont)
                    val paragraph = Paragraph(text)
                        .setTextAlignment(TextAlignment.LEFT)
                    document.add(paragraph)
                }

                private fun extractText(node: Node): String {
                    val text = StringBuilder()
                    var child = node.firstChild
                    while (child != null) {
                        if (child is org.commonmark.node.Text) {
                            text.append(child.literal)
                        }
                        child = child.next
                    }
                    return text.toString()
                }
            })

            // Dibujar líneas laterales en todas las páginas
            for (i in 1..pdf.numberOfPages) {
                val canvas = PdfCanvas(pdf.getPage(i))
                canvas.setLineWidth(1.0f)
                canvas.moveTo(50.0, 50.0).lineTo(50.0, 792.0).stroke() // Línea izquierda
                canvas.moveTo(544.0, 50.0).lineTo(544.0, 792.0).stroke() // Línea derecha
            }

            document.close()
            return file
        } catch (e: Exception) {
            throw Exception("Error creando el PDF: " + e.message)
        }
    }
}