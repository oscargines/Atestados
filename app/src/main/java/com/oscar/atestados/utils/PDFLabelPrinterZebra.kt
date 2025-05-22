package com.oscar.atestados.utils

import android.content.Context
import android.util.Log
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream

/**
 * Clase para generar etiquetas PDF compatibles con impresoras Zebra.
 *
 * @property context Contexto de Android para acceder a recursos como fuentes.
 */
class PDFLabelPrinterZebra(private val context: Context) {
    companion object {
        private const val TAG = "PDFLabelPrinterZebra"
        private const val PAGE_WIDTH = 100 * 2.83465f  // ≈ 283.465 pt (100 mm)
        private const val PAGE_HEIGHT = 280 * 2.83465f // ≈ 793.70 pt
        private const val MARGIN_MM = 2f * 2.83465f   // ≈ 5.67 pt
    }

    /**
     * Genera un archivo PDF con el contenido HTML formateado para impresión en etiquetas Zebra.
     *
     * @param htmlContent Contenido HTML a convertir en PDF.
     * @param outputFile Archivo de destino donde se guardará el PDF generado.
     * @throws Exception Si ocurre algún error durante la generación del PDF.
     */
    fun generarEtiquetaPdf(htmlContent: String, outputFile: File) {
        Log.d(TAG, "Iniciando generación de PDF en ${outputFile.absolutePath}")

        try {
            PdfWriter(FileOutputStream(outputFile)).use { writer ->
                PdfDocument(writer).use { pdfDocument ->
                    pdfDocument.defaultPageSize = PageSize(PAGE_WIDTH, PAGE_HEIGHT)
                    Log.d(TAG, "Tamaño de página configurado: ${PAGE_WIDTH}pt x ${PAGE_HEIGHT}pt")

                    Document(pdfDocument).use { document ->
                        document.setMargins(MARGIN_MM, MARGIN_MM, MARGIN_MM, MARGIN_MM)
                        Log.d(TAG, "Márgenes del documento configurados")

                        val fonts = loadFonts()
                        Log.d(TAG, "Fuentes cargadas correctamente")

                        val htmlElements = HtmlUtils.extractHtmlElements(htmlContent)
                        Log.d(TAG, "Se encontraron ${htmlElements.size} elementos HTML")

                        val titleContent =
                            htmlElements.find { it.tag == "h1" }?.content?.toUpperCase() ?: ""
                        val contentAreaX = MARGIN_MM
                        val contentAreaY = MARGIN_MM
                        val contentWidth = PAGE_WIDTH - (2 * MARGIN_MM)
                        val contentHeight = PAGE_HEIGHT - (2 * MARGIN_MM) - 60f
                        Log.d(
                            TAG,
                            "Área de contenido: X=$contentAreaX, Y=$contentAreaY, Ancho=$contentWidth, Alto=$contentHeight"
                        )

                        val nonH1Elements = htmlElements.filter { it.tag != "h1" }
                        val tempDiv = Div().setWidth(contentWidth).setPadding(0f)
                        nonH1Elements.forEach { element ->
                            processElement(
                                element,
                                tempDiv,
                                fonts["regular"]!!,
                                fonts["boldItalic"]!!
                            )
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
                        Log.d(TAG, "Contenido dividido en ${contentParts.size} partes/páginas")

                        contentParts.forEachIndexed { index, part ->
                            if (index > 0) {
                                Log.d(TAG, "Añadiendo nueva página #${index + 1}")
                                document.add(com.itextpdf.layout.element.AreaBreak())
                            }

                            addDecorativeElements(document, fonts["escudo"]!!)

                            if (titleContent.isNotEmpty()) {
                                document.add(
                                    Paragraph(titleContent)
                                        .setFont(fonts["title"]!!)
                                        .setFontSize(10f)
                                        .setUnderline()
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setFixedPosition(45f, PAGE_HEIGHT - 40f, PAGE_WIDTH - 90f)
                                        .setMultipliedLeading(1.2f)
                                )
                            }

                            val contentDiv = Div()
                                .setFixedPosition(contentAreaX, contentAreaY, contentWidth)
                                .setHeight(contentHeight)
                                .setPadding(0f)

                            part.forEach { element ->
                                contentDiv.add(element as com.itextpdf.layout.element.IBlockElement)
                            }
                            document.add(contentDiv)
                            Log.d(TAG, "Parte #${index + 1} añadida al documento")
                        }
                    }
                }
            }
            Log.d(TAG, "PDF generado exitosamente en ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar PDF: ${e.message}", e)
            throw e
        }
    }

    /**
     * Carga las fuentes necesarias desde los assets de la aplicación.
     *
     * @return Mapa de fuentes con claves: "escudo", "title", "regular", "boldItalic".
     */
    private fun loadFonts(): Map<String, PdfFont> {
        return try {
            mapOf(
                "escudo" to PdfFontFactory.createFont(
                    context.assets.open("fonts/escudo.ttf").readBytes(),
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                ),
                "title" to PdfFontFactory.createFont(
                    context.assets.open("fonts/calibri-bold.ttf").readBytes(),
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                ),
                "regular" to PdfFontFactory.createFont(
                    context.assets.open("fonts/calibri-regular.ttf").readBytes(),
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                ),
                "boldItalic" to PdfFontFactory.createFont(
                    context.assets.open("fonts/calibri-bold-italic.ttf").readBytes(),
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar fuentes: ${e.message}", e)
            throw e
        }
    }

    /**
     * Añade elementos decorativos (A y G) al documento PDF.
     *
     * @param document Documento PDF donde se añadirán los elementos.
     * @param escudoFont Fuente especial para los caracteres decorativos.
     */
    private fun addDecorativeElements(document: Document, escudoFont: PdfFont) {
        Log.v(TAG, "Añadiendo elementos decorativos A y G")
        document.add(
            Paragraph("A")
                .setFont(escudoFont)
                .setFontSize(36f)
                .setFixedPosition(MARGIN_MM, PAGE_HEIGHT - MARGIN_MM - 50f, 50f)
        )
        document.add(
            Paragraph("G")
                .setFont(escudoFont)
                .setFontSize(36f)
                .setFixedPosition(PAGE_WIDTH - MARGIN_MM - 30f, PAGE_HEIGHT - MARGIN_MM - 50f, 50f)
        )
    }

    /**
     * Divide el contenido en partes que caben en una página PDF.
     *
     * @param tempDiv Div que contiene todo el contenido a dividir.
     * @param maxHeight Altura máxima disponible por página.
     * @param pdfDocument Documento PDF actual.
     * @param document Documento de layout actual.
     * @param contentAreaX Posición X del área de contenido.
     * @param contentAreaY Posición Y del área de contenido.
     * @param contentWidth Ancho del área de contenido.
     * @return Lista de partes de contenido, cada una representando lo que cabe en una página.
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
            Log.v(
                TAG,
                "Elemento: ${element.javaClass.simpleName}, Altura estimada: $elementHeight, Altura acumulada: $currentHeight"
            )

            if (currentHeight + elementHeight > maxHeight && currentPart.isNotEmpty()) {
                Log.d(
                    TAG,
                    "Límite de página alcanzado. Altura actual: $currentHeight, Máximo: $maxHeight"
                )
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

        Log.d(TAG, "Total de páginas generadas: ${parts.size}")
        return parts
    }

    /**
     * Estima la altura que ocupará un elemento en el PDF.
     *
     * @param element Elemento del cual estimar la altura.
     * @return Altura estimada en puntos.
     */
    private fun estimateElementHeight(element: com.itextpdf.layout.element.IElement): Float {
        return try {
            when (element) {
                is Paragraph -> {
                    val fontSize =
                        element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.FONT_SIZE)?.value
                            ?: 10f
                    // Obtener el objeto Leading
                    val leadingProperty =
                        element.getProperty<com.itextpdf.layout.properties.Leading>(com.itextpdf.layout.properties.Property.LEADING)
                    // Usar el valor de leading directamente, asumiendo que es un multiplicador
                    val leading =
                        leadingProperty?.value ?: 1.2f // Valor por defecto si no hay leading
                    val text =
                        element.children.filterIsInstance<Text>().joinToString("") { it.text }
                    val avgCharsPerLine =
                        ((PAGE_WIDTH - (2 * MARGIN_MM)) / (fontSize * 0.5f)).toInt()
                    val estimatedLines = text.length / avgCharsPerLine.coerceAtLeast(1)
                    val actualLines = text.count { it == '\n' } + 1
                    val lineCount = maxOf(estimatedLines, actualLines).coerceAtLeast(1)
                    val height = fontSize * leading * lineCount
                    // Obtener MARGIN_BOTTOM como UnitValue
                    val marginBottomProperty =
                        element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.MARGIN_BOTTOM)
                    val marginBottom = marginBottomProperty?.value
                        ?: 0f // Usar el valor de UnitValue o 0f por defecto
                    Log.v(
                        TAG,
                        "Paragraph: fontSize=$fontSize, leading=$leading, chars=${text.length}, líneas estimadas=$lineCount, height=$height, marginBottom=$marginBottom"
                    )
                    height + marginBottom + 10f
                }

                is Div -> {
                    var divHeight = 0f
                    element.children.forEach {
                        divHeight += estimateElementHeight(it)
                    }
                    Log.v(TAG, "Div height: $divHeight")
                    divHeight
                }

                else -> {
                    Log.v(TAG, "Altura por defecto (20f) para ${element.javaClass.simpleName}")
                    20f
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al estimar altura: ${e.message}", e)
            30f
        }
    }

    /**
     * Procesa un elemento HTML y lo convierte en elementos PDF.
     *
     * @param element Elemento HTML a procesar.
     * @param div Contenedor Div donde se añadirán los elementos resultantes.
     * @param regularFont Fuente regular para texto normal.
     * @param boldItalicFont Fuente para títulos y énfasis.
     * @param indentLevel Nivel de indentación para elementos anidados.
     */
    private fun processElement(
        element: HtmlUtils.HtmlElement,
        div: Div,
        regularFont: PdfFont,
        boldItalicFont: PdfFont,
        indentLevel: Int = 0
    ) {
        Log.v(
            TAG,
            "Procesando elemento ${element.tag} (nivel $indentLevel): ${element.content.take(20)}..."
        )

        when (element.tag) {
            "h1", "h2", "h3" -> {
                div.add(
                    Paragraph(element.content)
                        .setFont(boldItalicFont)
                        .setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMultipliedLeading(1.2f)
                        .setMarginBottom(5f)
                        .setMarginLeft(indentLevel * 10f)
                )
            }

            "p" -> {
                div.add(
                    Paragraph(element.content)
                        .setFont(regularFont)
                        .setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMultipliedLeading(1.2f)
                        .setMarginBottom(5f)
                        .setMarginLeft(indentLevel * 10f)
                )
            }

            "div" -> {
                // Procesar hijos del div (por ejemplo, <ul> dentro de <div id="denunciado">)
                element.children.forEach { child ->
                    processElement(child, div, regularFont, boldItalicFont, indentLevel)
                }
            }

            "span" -> {
                if (element.attributes["id"] in listOf("op_1_checkbox", "op_2_checkbox")) {
                    val isChecked = element.attributes["data-checked"] == "true"
                    val checkboxSymbol = if (isChecked) "✔" else "☐"
                    div.add(
                        Paragraph()
                            .add(Text(checkboxSymbol).setFont(regularFont).setFontSize(8f))
                            .setTextAlignment(TextAlignment.LEFT)
                            .setMultipliedLeading(1.2f)
                            .setMarginBottom(5f)
                            .setMarginLeft(indentLevel * 10f)
                    )
                    Log.d(TAG, "Checkbox procesado: ${element.attributes["id"]} = $checkboxSymbol")
                }
            }

            "table" -> {
                element.children.forEach { tr ->
                    if (tr.tag == "tr") {
                        tr.children.forEach { td ->
                            if (td.tag == "td") {
                                td.children.forEach { child ->
                                    processElement(
                                        child,
                                        div,
                                        regularFont,
                                        boldItalicFont,
                                        indentLevel + 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "ul" -> {
                element.children.forEachIndexed { index, li ->
                    if (li.tag == "li") {
                        val paragraph = Paragraph()
                        li.children.forEach { child ->
                            if (child.tag == "span" && child.attributes["id"] in listOf(
                                    "op_1_checkbox",
                                    "op_2_checkbox"
                                )
                            ) {
                                val isChecked = child.attributes["data-checked"] == "true"
                                val checkboxSymbol = if (isChecked) "✔" else "☐"
                                paragraph.add(
                                    Text(checkboxSymbol).setFont(regularFont).setFontSize(8f)
                                )
                                paragraph.add(Text(" ").setFont(regularFont).setFontSize(8f))
                            } else {
                                // Añadir el contenido del <li> directamente
                                paragraph.add(
                                    Text(child.content).setFont(regularFont).setFontSize(8f)
                                )
                                child.children.forEach { grandChild ->
                                    processElement(
                                        grandChild,
                                        div,
                                        regularFont,
                                        boldItalicFont,
                                        indentLevel + 1
                                    )
                                }
                            }
                        }
                        paragraph.setMultipliedLeading(1.2f)
                        paragraph.setMarginBottom(5f)
                        paragraph.setTextAlignment(TextAlignment.LEFT)
                        paragraph.setMarginLeft(indentLevel * 10f)
                        div.add(paragraph)
                    }
                }
            }

            else -> {
                Log.w(TAG, "Elemento no soportado: ${element.tag}")
            }
        }
    }
}