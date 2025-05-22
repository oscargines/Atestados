package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.IBlockElement
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.Leading
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.File
import java.io.FileOutputStream

private const val TAG = "PDFA4Printer"

/**
 * Clase para generar documentos PDF en formato A4 a partir de contenido HTML.
 * Utiliza HtmlUtils para parsear HTML y soporta personalización mediante procesadores de elementos.
 *
 * @property context Contexto de Android para acceder a recursos como fuentes.
 * @property config Configuración del documento (márgenes, fuentes, etc.).
 */
class PDFA4Printer(
    private val context: Context,
    private val config: DocumentConfig = DocumentConfig()
) {
    companion object {
        private const val TAG = "PDFA4Printer"
        private val PAGE_SIZE = PageSize.A4 // A4: 595 x 842 pt
    }

    /**
     * Configuración del documento A4.
     *
     * @property marginMm Márgenes en milímetros.
     * @property fontPaths Rutas de las fuentes en assets (clave: nombre, valor: ruta).
     * @property titleHeightPt Altura reservada para el título en puntos.
     * @property shieldSizePt Tamaño de los escudos (letras) en puntos.
     */
    data class DocumentConfig(
        val marginMm: Float = 20f,
        val fontPaths: Map<String, String> = mapOf(
            "regular" to "fonts/calibri-regular.ttf",
            "bold" to "fonts/calibri-bold.ttf",
            "shield" to "fonts/escudo.ttf"
        ),
        val titleHeightPt: Float = 60f,
        val shieldSizePt: Float = 30f
    )

    /**
     * Representa un elemento del documento con su tipo, contenido, hijos y atributos.
     */
    data class DocumentElement(
        val type: String,
        val content: String,
        val children: List<DocumentElement> = emptyList(),
        val attributes: Map<String, String> = emptyMap()
    )

    /**
     * Interfaz para procesar elementos del documento en elementos PDF.
     */
    interface ElementProcessor {
        fun process(
            element: DocumentElement,
            div: Div,
            fonts: Map<String, PdfFont>,
            indentLevel: Int
        ): Boolean
    }

    /**
     * Genera un archivo PDF en formato A4 a partir de contenido HTML.
     *
     * @param htmlContent Contenido HTML a convertir.
     * @param outputFile Archivo de destino para el PDF.
     * @param processor Procesador de elementos personalizado (por defecto usa DefaultElementProcessor).
     * @throws Exception Si ocurre un error durante la generación del PDF.
     */
    fun generarDocumentoA4(
        htmlContent: String,
        outputFile: File,
        processor: ElementProcessor = DefaultElementProcessor()
    ) {
        Log.d(TAG, "Iniciando generación de PDF A4 en ${outputFile.absolutePath}")

        try {
            PdfWriter(FileOutputStream(outputFile)).use { writer ->
                PdfDocument(writer).use { pdfDocument ->
                    pdfDocument.defaultPageSize = PAGE_SIZE
                    Log.d(TAG, "Tamaño de página configurado: A4 (${PAGE_SIZE.width}pt x ${PAGE_SIZE.height}pt)")

                    Document(pdfDocument).use { document ->
                        val marginPt = config.marginMm * 2.83465f
                        document.setMargins(marginPt, marginPt, marginPt, marginPt)
                        Log.d(TAG, "Márgenes configurados: ${config.marginMm}mm")

                        val fonts = loadFonts()
                        Log.d(TAG, "Fuentes cargadas: ${fonts.keys}")

                        // Parsear HTML usando HtmlUtils y convertir a DocumentElement
                        val htmlElements = HtmlUtils.extractHtmlElements(htmlContent)
                        val elements = htmlElements.map { convertHtmlElementToDocumentElement(it) }
                        Log.d(TAG, "Elementos parseados: ${elements.size}")

                        val titleElement = elements.find { element: DocumentElement ->
                            element.type == "section" && element.attributes["class"]?.contains("title") == true
                        }
                        val contentElements = elements.filterNot { element: DocumentElement ->
                            element === titleElement
                        }

                        // Definir dimensiones y posición de la caja de contenido
                        val contentAreaX = 20f * 2.83465f // 20 mm
                        val contentAreaY = 9f * 2.83465f // 9 mm
                        val contentWidth = PAGE_SIZE.width - 2 * marginPt // Mantener ancho actual
                        val contentHeight = 272f * 2.83465f // 279 mm

                        val tempDiv = Div().setWidth(UnitValue.createPointValue(contentWidth)).setPadding(0f)
                        contentElements.forEach { element: DocumentElement ->
                            processor.process(element, tempDiv, fonts, 0)
                        }

                        val contentParts = splitContentToFitPage(
                            tempDiv,
                            contentHeight * 0.9f,
                            pdfDocument,
                            document,
                            contentAreaX,
                            contentAreaY,
                            contentWidth
                        )
                        Log.d(TAG, "Contenido dividido en ${contentParts.size} páginas")

                        contentParts.forEachIndexed { index: Int, part: List<com.itextpdf.layout.element.IElement> ->
                            if (index > 0) {
                                Log.d(TAG, "Añadiendo nueva página #${index + 1}")
                                document.add(AreaBreak())
                            }

                            // Añadir contenido en una "caja" para crear la página
                            val contentDiv = Div()
                                .setFixedPosition(contentAreaX, contentAreaY, contentWidth)
                                .setHeight(UnitValue.createPointValue(contentHeight))
                                .setBorder(SolidBorder(ColorConstants.BLACK, 0f))
                                .setPadding(2f) // Reducir padding para minimizar espacio

                            part.forEach { element ->
                                contentDiv.add(element as IBlockElement)
                            }
                            document.add(contentDiv)

                            // Dibujar líneas verticales
                            val pdfPage = pdfDocument.getPage(index + 1)
                            val pdfCanvas = PdfCanvas(pdfPage)
                            pdfCanvas.setLineWidth(1f)

                            // Línea izquierda: a 17 mm del margen izquierdo
                            val lineLeftX = 17f * 2.83465f
                            pdfCanvas.moveTo(lineLeftX.toDouble(), (PAGE_SIZE.height - (6f * 2.83465f)).toDouble())
                            pdfCanvas.lineTo(lineLeftX.toDouble(), (15f * 2.83465f).toDouble())
                            pdfCanvas.stroke()

                            // Línea derecha: a 15 mm del margen derecho
                            val lineRightX = PAGE_SIZE.width - (15f * 2.83465f)
                            pdfCanvas.moveTo(lineRightX.toDouble(), (PAGE_SIZE.height - (6f * 2.83465f)).toDouble())
                            pdfCanvas.lineTo(lineRightX.toDouble(), (15f * 2.83465f).toDouble())
                            pdfCanvas.stroke()

                            // Añadir "escudos" como letras con la fuente escudo.ttf
                            document.add(
                                Paragraph("A")
                                    .setFont(fonts["shield"]!!)
                                    .setFontSize(config.shieldSizePt)
                                    .setFixedPosition(
                                        (5f * 2.83465f),
                                        PAGE_SIZE.height - (10f * 2.83465f) - config.shieldSizePt,
                                        config.shieldSizePt
                                    )
                                    .setMargin(0f)
                                    .setPadding(0f)
                            )

                            document.add(
                                Paragraph("G")
                                    .setFont(fonts["shield"]!!)
                                    .setFontSize(config.shieldSizePt)
                                    .setFixedPosition(
                                        PAGE_SIZE.width - (3f * 2.83465f) - config.shieldSizePt,
                                        PAGE_SIZE.height - (10f * 2.83465f) - config.shieldSizePt,
                                        config.shieldSizePt
                                    )
                                    .setMargin(0f)
                                    .setPadding(0f)
                            )

                            // Añadir título con estilo
                            if (titleElement != null && titleElement.content.isNotEmpty()) {
                                document.add(
                                    Paragraph(titleElement.content.uppercase())
                                        .setFont(fonts["bold"] ?: fonts["regular"]!!)
                                        .setFontSize(16f)
                                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setFixedPosition(marginPt, PAGE_SIZE.height - marginPt - config.shieldSizePt - 40f, contentWidth)
                                        .setPadding(5f)
                                )
                            }

                            // Añadir la caja a 20mm en X, 283mm en Y, con 75mm de ancho y 7mm de alto
                            // Dentro de la caja añadir texto "ATESTADO NÚMERO:" dentro de box1
                            val box1X = 20f * 2.83465f // 20 mm
                            val box1Y = 283f * 2.83465f // 283 mm
                            val box1Width = 75f * 2.83465f // 75 mm
                            val box1Height = 7f * 2.83465f // 7 mm

                            val box1 = Div()
                                .setFixedPosition(box1X, box1Y, box1Width)
                                .setHeight(UnitValue.createPointValue(box1Height))
                                .setBackgroundColor(ColorConstants.WHITE)
                                .setBorder(SolidBorder(ColorConstants.BLACK, 1f))
                            val textCaja1 = Paragraph("ATESTADO NÚMERO: ")
                                .setTextAlignment(TextAlignment.LEFT)
                                .setFontColor(ColorConstants.BLACK)
                                .setFont(fonts["bold"] ?: fonts["regular"]!!)
                                .setFontSize(8f)
                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                .setHeight(50f)
                                .setPaddingLeft(5f)
                            box1.add(textCaja1)
                            document.add(box1)

                            // Añadir la caja a 167mm en X, 283mm en Y, con 25mm de ancho y 7mm de alto
                            // Dentro de la caja añadir texto "FOLIO Nº:" dentro de box2
                            val box2X = 167f * 2.83465f // 167 mm
                            val box2Y = 283f * 2.83465f // 283 mm
                            val box2Width = 25f * 2.83465f // 25 mm
                            val box2Height = 7f * 2.83465f // 7 mm

                            val box2 = Div()
                                .setFixedPosition(box2X, box2Y, box2Width)
                                .setHeight(UnitValue.createPointValue(box2Height))
                                .setBackgroundColor(ColorConstants.WHITE)
                                .setBorder(SolidBorder(ColorConstants.BLACK, 1f))
                            val textCaja2 = Paragraph("FOLIO Nº: ")
                                .setTextAlignment(TextAlignment.LEFT)
                                .setFontColor(ColorConstants.BLACK)
                                .setFont(fonts["bold"] ?: fonts["regular"]!!)
                                .setFontSize(8f)
                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                .setHeight(50f)
                                .setPaddingLeft(5f)
                            box2.add(textCaja2)
                            document.add(box2)

                            Log.d(TAG, "Página #${index + 1} añadida")
                        }

                        Log.d(TAG, "PDF A4 generado en ${outputFile.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar PDF A4", e)
            throw e
        }
    }

    /**
     * Convierte un HtmlElement en DocumentElement, mapeando tags HTML a tags compatibles.
     */
    private fun convertHtmlElementToDocumentElement(htmlElement: HtmlUtils.HtmlElement): DocumentElement {
        val type = when (htmlElement.tag) {
            "h1", "h2", "h3" -> "section"
            "p" -> "paragraph"
            "ul" -> "list"
            "li" -> "item"
            "span" -> "span"
            "div" -> "div"
            else -> "paragraph"
        }

        val attributes = when (htmlElement.tag) {
            "ul" -> htmlElement.attributes + ("type" to "bullet")
            else -> htmlElement.attributes
        }

        val children = htmlElement.children.map { convertHtmlElementToDocumentElement(it) }

        return DocumentElement(
            type = type,
            content = htmlElement.content,
            attributes = attributes,
            children = children
        )
    }

    /**
     * Carga las fuentes especificadas en la configuración.
     *
     * @return Mapa de fuentes cargadas.
     */
    private fun loadFonts(): Map<String, PdfFont> {
        return config.fontPaths.mapValues { (name, path) ->
            try {
                PdfFontFactory.createFont(
                    context.assets.open(path).readBytes(),
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar fuente $path", e)
                throw e
            }
        }
    }

    /**
     * Divide el contenido en partes que caben en una página A4.
     */
    private fun splitContentToFitPage(
        tempDiv: Div,
        maxHeight: Float,
        pdfDocument: PdfDocument,
        document: Document,
        contentAreaX: Float,
        contentAreaY: Float,
        contentWidth: Float
    ): List<List<com.itextpdf.layout.element.IElement>> {
        val parts = mutableListOf<MutableList<com.itextpdf.layout.element.IElement>>()
        var currentPart = mutableListOf<com.itextpdf.layout.element.IElement>()
        var currentHeight = 0f

        tempDiv.children.forEach { element ->
            val elementHeight = estimateElementHeight(element) * 1.1f
            Log.v(TAG, "Elemento: ${element.javaClass.simpleName}, Altura: $elementHeight, Total: $currentHeight")

            if (currentHeight + elementHeight > maxHeight && currentPart.isNotEmpty()) {
                parts.add(currentPart)
                currentPart = mutableListOf()
                currentHeight = 0f
            }

            currentPart.add(element)
            currentHeight += elementHeight
        }

        if (currentPart.isNotEmpty()) {
            parts.add(currentPart)
        }

        return parts
    }

    /**
     * Estima la altura de un elemento PDF.
     */
    private fun estimateElementHeight(element: com.itextpdf.layout.element.IElement): Float {
        return when (element) {
            is Paragraph -> {
                val fontSize = element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.FONT_SIZE)?.getValue() ?: 12f
                val leading = element.getProperty<Leading>(com.itextpdf.layout.properties.Property.LEADING)?.value ?: 1.2f
                val textElements = element.children.filterIsInstance<Text>()
                val text = textElements.joinToString("") { it.text }
                val avgCharsPerLine = ((PAGE_SIZE.width - (2 * (config.marginMm * 2.83465f))) / (fontSize * 0.5f)).toInt()
                val estimatedLines = if (avgCharsPerLine > 0) text.length / avgCharsPerLine else 1
                val actualLines = text.count { it == '\n' } + 1
                val lineCount = maxOf(estimatedLines, actualLines).coerceAtLeast(1)
                val height = fontSize * leading * lineCount
                val marginBottom = element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.MARGIN_BOTTOM)?.getValue() ?: 0f
                height + marginBottom + 10f
            }
            is Div -> element.children.sumOf { estimateElementHeight(it).toDouble() }.toFloat()
            else -> 20f
        }
    }

    /**
     * Procesador de elementos por defecto con formato básico para HTML.
     */
    inner class DefaultElementProcessor : ElementProcessor {
        override fun process(
            element: DocumentElement,
            div: Div,
            fonts: Map<String, PdfFont>,
            indentLevel: Int
        ): Boolean {
            Log.v(TAG, "Procesando ${element.type} (nivel $indentLevel): ${element.content.take(20)}...")

            when (element.type) {
                "section" -> {
                    div.add(
                        Paragraph(element.content)
                            .setFont(fonts["bold"] ?: fonts["regular"]!!)
                            .setFontSize(12f)
                            .setTextAlignment(TextAlignment.LEFT)
                            .setMarginBottom(8f)
                            .setMarginLeft(indentLevel * 10f)
                    )
                    element.children.forEach { process(it, div, fonts, indentLevel + 1) }
                    return true
                }
                "paragraph" -> {
                    div.add(
                        Paragraph(element.content)
                            .setFont(fonts["regular"]!!)
                            .setFontSize(10f)
                            .setTextAlignment(TextAlignment.LEFT)
                            .setMarginBottom(6f)
                            .setMarginLeft(indentLevel * 10f)
                    )
                    return true
                }
                "div" -> {
                    // Procesar hijos del div (por ejemplo, <list> dentro de <div id="denunciado">)
                    element.children.forEach { process(it, div, fonts, indentLevel) }
                    return true
                }
                "list" -> {
                    element.children.forEachIndexed { index, child ->
                        if (child.type == "item") {
                            val paragraph = Paragraph()
                            child.children.forEach { grandChild ->
                                if (grandChild.type == "span" && grandChild.attributes["id"] in listOf("op_1_checkbox", "op_2_checkbox")) {
                                    val isChecked = grandChild.attributes["data-checked"] == "true"
                                    val checkboxSymbol = if (isChecked) "✔" else "☐"
                                    paragraph.add(Text(checkboxSymbol).setFont(fonts["regular"]!!).setFontSize(10f))
                                    paragraph.add(Text(" ").setFont(fonts["regular"]!!).setFontSize(10f))
                                } else {
                                    paragraph.add(Text(grandChild.content).setFont(fonts["regular"]!!).setFontSize(10f))
                                    grandChild.children.forEach { greatGrandChild ->
                                        process(greatGrandChild, div, fonts, indentLevel + 1)
                                    }
                                }
                            }
                            paragraph.setMarginBottom(6f)
                            paragraph.setMarginLeft(indentLevel * 10f)
                            div.add(paragraph)
                        }
                    }
                    return true
                }
                "span" -> {
                    if (element.attributes["id"] in listOf("op_1_checkbox", "op_2_checkbox")) {
                        val isChecked = element.attributes["data-checked"] == "true"
                        val checkboxSymbol = if (isChecked) "✔" else "☐"
                        div.add(
                            Paragraph()
                                .add(Text(checkboxSymbol).setFont(fonts["regular"]!!).setFontSize(10f))
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMarginBottom(6f)
                                .setMarginLeft(indentLevel * 10f)
                        )
                        Log.d(TAG, "Checkbox procesado: ${element.attributes["id"]} = $checkboxSymbol")
                        return true
                    }
                    return false
                }
                else -> return false
            }
        }
    }
}