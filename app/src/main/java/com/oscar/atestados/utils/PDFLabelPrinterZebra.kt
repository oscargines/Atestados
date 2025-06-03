package com.oscar.atestados.utils

import android.content.Context
import android.util.Base64
import android.util.Log
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.IBlockElement
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.ListItem
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.Leading
import com.itextpdf.layout.properties.ListNumberingType
import com.itextpdf.layout.properties.Property
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.itextpdf.io.image.ImageDataFactory
import java.io.File
import java.io.FileOutputStream

private const val TAG = "PDFLabelPrinterZebra"

/**
 * Clase utilitaria para generar documentos PDF en formato de etiqueta Zebra.
 * Esta clase maneja la creación de PDFs a partir de contenido HTML, incluyendo formato de texto,
 * tablas, listas, imágenes y otros elementos, optimizados para impresión en etiquetas Zebra.
 *
 * @property context Contexto de Android para acceder a recursos y assets.
 * @property config Configuración del documento, incluyendo márgenes, fuentes y tamaños de elementos.
 */
class PDFLabelPrinterZebra(
    private val context: Context,
    private val config: DocumentConfig = DocumentConfig()
) {
    companion object {
        private const val PAGE_WIDTH = 100 * 2.83465f  // ≈ 283.465 pt (100 mm)
        private const val PAGE_HEIGHT = 280 * 2.83465f // ≈ 793.70 pt (280 mm)
    }

    /**
     * Configuración del documento PDF para etiquetas Zebra.
     *
     * @property marginMm Márgenes del documento en milímetros.
     * @property fontPaths Map de rutas de fuentes para diferentes estilos (regular, bold, boldItalic, shield).
     * @property titleHeightPt Altura de la sección de título en puntos.
     * @property shieldSizePt Tamaño del símbolo de escudo en puntos.
     * @property signatureWidthPt Ancho de las imágenes de firma en puntos.
     * @property signatureHeightPt Altura de las imágenes de firma en puntos.
     * @property columnWidthsPercent Array de anchos de columnas en porcentaje para tablas.
     * @property cellHeightPt Altura de las celdas de tabla en puntos.
     */
    data class DocumentConfig(
        val marginMm: Float = 2f,
        val fontPaths: Map<String, String> = mapOf(
            "regular" to "fonts/calibri-regular.ttf",
            "bold" to "fonts/calibri-bold.ttf",
            "boldItalic" to "fonts/calibri-bold-italic.ttf",
            "shield" to "fonts/escudo.ttf"
        ),
        val titleHeightPt: Float = 40f,
        val shieldSizePt: Float = 36f,
        val signatureWidthPt: Float = 80f,
        val signatureHeightPt: Float = 40f,
        val columnWidthsPercent: FloatArray = floatArrayOf(33.3f, 33.3f, 33.3f),
        val cellHeightPt: Float = 50f,
        val cellWidthPt: Float = 100f
    )

    /**
     * Representa un elemento del documento PDF.
     *
     * @property type Tipo del elemento (ej. "section", "paragraph", "list").
     * @property content Contenido de texto del elemento.
     * @property children Lista de elementos hijos.
     * @property attributes Map de atributos del elemento (ej. class, id).
     */
    data class DocumentElement(
        val type: String,
        val content: String,
        val children: List<DocumentElement>? = emptyList(),
        val attributes: Map<String?, String?>? = emptyMap()
    )

    /**
     * Interfaz para procesar elementos del documento con lógica personalizada.
     */
    interface ElementProcessor {
        /**
         * Procesa un elemento del documento y lo añade al PDF.
         *
         * @param element Elemento a procesar.
         * @param div Contenedor al que se añadirá el elemento.
         * @param fonts Map de fuentes disponibles.
         * @param indentLevel Nivel de indentación para elementos anidados.
         * @return True si el elemento se procesó correctamente, false en caso contrario.
         */
        fun process(
            element: DocumentElement,
            div: Div,
            fonts: Map<String, PdfFont>,
            indentLevel: Int
        ): Boolean
    }

    /**
     * Genera un PDF de etiqueta Zebra a partir de contenido HTML y lo guarda en el archivo especificado.
     *
     * @param htmlContent Contenido HTML a convertir a PDF.
     * @param outputFile Archivo donde se guardará el PDF.
     * @param processor Procesador de elementos para manejo personalizado (por defecto usa DefaultElementProcessor).
     * @throws IllegalArgumentException Si el contenido HTML está vacío.
     * @throws Exception Si ocurre un error durante la generación del PDF.
     */
    fun generarEtiquetaPdf(
        htmlContent: String,
        outputFile: File,
        processor: ElementProcessor = DefaultElementProcessor()
    ) {
        if (htmlContent.isBlank()) {
            Log.e(TAG, "El contenido HTML está vacío")
            throw IllegalArgumentException("El contenido HTML no puede estar vacío")
        }

        Log.d(TAG, "Iniciando generación de PDF en ${outputFile.absolutePath}")

        try {
            PdfWriter(FileOutputStream(outputFile)).use { writer ->
                PdfDocument(writer).use { pdfDocument ->
                    pdfDocument.defaultPageSize = PageSize(PAGE_WIDTH, PAGE_HEIGHT)
                    Log.d(TAG, "Tamaño de página configurado: ${PAGE_WIDTH}pt x ${PAGE_HEIGHT}pt")

                    Document(pdfDocument).use { document ->
                        val marginPt = config.marginMm * 2.83465f
                        document.setMargins(marginPt, marginPt, marginPt, marginPt)
                        Log.d(TAG, "Márgenes configurados: ${config.marginMm}mm")

                        val fonts = loadFonts()
                        Log.d(TAG, "Fuentes cargadas: ${fonts.keys}")

                        val htmlElements = HtmlUtils.extractHtmlElements(htmlContent)
                        Log.d(TAG, "Elementos HTML parseados: ${htmlElements.size}")

                        val elements = htmlElements.map { convertHtmlElementToDocumentElement(it) }
                        Log.d(TAG, "Elementos convertidos a DocumentElement: ${elements.size}")

                        val titleElement = elements.find { element ->
                            element.type == "section" && element.attributes?.get("class")
                                ?.contains("title") == true
                        } ?: elements.find { it.type == "section" } // Fallback a cualquier h1/h2/h3
                        val contentElements = elements.filterNot { it === titleElement }

                        val contentAreaX = marginPt
                        val contentAreaY = marginPt
                        val contentWidth = PAGE_WIDTH - 2 * marginPt
                        val contentHeight = PAGE_HEIGHT - 2 * marginPt - 60f

                        val tempDiv =
                            Div().setWidth(UnitValue.createPointValue(contentWidth)).setPadding(0f)
                        contentElements.forEach { element ->
                            processor.process(element, tempDiv, fonts, 0)
                        }

                        val contentParts = splitContentToFitPage(
                            tempDiv,
                            contentHeight,
                            pdfDocument,
                            document,
                            contentAreaX,
                            contentAreaY,
                            contentWidth
                        )
                        Log.d(TAG, "Contenido dividido en ${contentParts.size} páginas")

                        contentParts.forEachIndexed { index, part ->
                            if (index > 0) {
                                Log.d(TAG, "Añadiendo nueva página #${index + 1}")
                                document.add(AreaBreak())
                            }

                            addDecorativeElements(document, fonts["shield"] ?: PdfFontFactory.createFont())

                            if (titleElement != null && titleElement.content.isNotEmpty()) {
                                document.add(
                                    Paragraph(titleElement.content.uppercase())
                                        .setFont(fonts["bold"] ?: fonts["regular"] ?: PdfFontFactory.createFont())
                                        .setFontSize(10f)
                                        .setUnderline()
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setFixedPosition(marginPt + 30f, PAGE_HEIGHT - marginPt - config.titleHeightPt, 212f)
                                        .setMultipliedLeading(1.2f)
                                )
                            }

                            val contentDiv = Div()
                                .setFixedPosition(contentAreaX, contentAreaY, contentWidth)
                                .setHeight(UnitValue.createPointValue(contentHeight))
                                .setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                                .setPadding(0f)

                            part.forEach { element ->
                                contentDiv.add(element as IBlockElement)
                            }
                            document.add(contentDiv)
                            Log.d(TAG, "Página #${index + 1} añadida")
                        }
                    }
                }
            }
            Log.d(TAG, "PDF generado en ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar PDF", e)
            throw e
        }
    }

    /**
     * Carga las fuentes especificadas en la configuración.
     *
     * @return Map de nombres de fuentes a objetos PdfFont.
     */
    private fun loadFonts(): Map<String, PdfFont> {
        val fonts = mutableMapOf<String, PdfFont>()
        config.fontPaths.forEach { (key, path) ->
            try {
                fonts[key] = PdfFontFactory.createFont(
                    context.assets.open(path).readBytes(),
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar fuente $path", e)
            }
        }
        if (fonts.isEmpty()) {
            Log.w(TAG, "No se cargaron fuentes personalizadas, usando fuente predeterminada")
            fonts["regular"] = PdfFontFactory.createFont()
        }
        return fonts
    }

    /**
     * Añade elementos decorativos (escudos A y G) al documento.
     *
     * @param document Documento PDF al que añadir los elementos.
     * @param escudoFont Fuente a usar para los símbolos decorativos.
     */
    private fun addDecorativeElements(document: Document, escudoFont: PdfFont) {
        Log.v(TAG, "Añadiendo elementos decorativos A y G")
        document.add(
            Paragraph("A")
                .setFont(escudoFont)
                .setFontSize(config.shieldSizePt)
                .setFixedPosition(config.marginMm * 2.83465f, PAGE_HEIGHT - (config.marginMm * 2.83465f) - 50f, 50f)
        )
        document.add(
            Paragraph("G")
                .setFont(escudoFont)
                .setFontSize(config.shieldSizePt)
                .setFixedPosition(PAGE_WIDTH - (config.marginMm * 2.83465f) - 30f, PAGE_HEIGHT - (config.marginMm * 2.83465f) - 50f, 50f)
        )
    }

    /**
     * Convierte un HtmlElement a un DocumentElement.
     *
     * @param htmlElement Elemento HTML a convertir.
     * @return DocumentElement convertido.
     */
    private fun convertHtmlElementToDocumentElement(htmlElement: HtmlUtils.HtmlElement): DocumentElement {
        val type = when (htmlElement.tag) {
            "h1", "h2", "h3" -> "section"
            "p" -> "paragraph"
            "ul", "ol" -> "list"
            "li" -> "listItem"
            "span" -> "span"
            "div" -> "div"
            "table" -> "table"
            "tbody" -> "tbody"
            "tr" -> "tr"
            "td" -> "td"
            "img" -> "img"
            else -> "paragraph"
        }

        val attributes: Map<String?, String?> = when (htmlElement.tag) {
            "ul" -> htmlElement.attributes + mapOf<String?, String>("type" to "bullet")
            "ol" -> {
                val listType = htmlElement.attributes["type"]?.let {
                    when (it) {
                        "A" -> "upper-alpha"
                        "a" -> "lower-alpha"
                        "I" -> "upper-roman"
                        "i" -> "lower-roman"
                        else -> "decimal"
                    }
                } ?: "decimal"
                htmlElement.attributes + mapOf<String?, String?>(
                    "ordered" to "true",
                    "listType" to listType
                )
            }
            else -> htmlElement.attributes
        } as Map<String?, String?>

        val children = htmlElement.children.map { convertHtmlElementToDocumentElement(it) }

        return DocumentElement(
            type = type,
            content = htmlElement.content,
            attributes = attributes,
            children = children
        )
    }

    /**
     * Divide el contenido en partes que caben en una página.
     *
     * @param tempDiv Contenedor con todos los elementos.
     * @param maxHeight Altura máxima disponible para el contenido en una página.
     * @param pdfDocument Documento PDF que se está generando.
     * @param document Layout del documento.
     * @param contentAreaX Coordenada X del área de contenido.
     * @param contentAreaY Coordenada Y del área de contenido.
     * @param contentWidth Ancho del área de contenido.
     * @return Lista de listas de elementos, cada una representando el contenido de una página.
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
            if (element is Table) {
                val table = element
                val rowHeights = mutableListOf<Float>()
                for (rowIndex in 0 until table.numberOfRows) {
                    var maxCellHeightInRow = 0f
                    for (colIndex in 0 until table.numberOfColumns) {
                        val cell = table.getCell(rowIndex, colIndex)
                        if (cell != null) {
                            var cellContentHeight = 0f
                            cell.getChildren().forEach { child ->
                                cellContentHeight += estimateElementHeight(child)
                            }
                            maxCellHeightInRow = maxOf(maxCellHeightInRow, cellContentHeight, 20f)
                        }
                    }
                    rowHeights.add(maxCellHeightInRow)
                }

                var currentTable = Table(UnitValue.createPercentArray(config.columnWidthsPercent))
                    .useAllAvailableWidth()
                    .setKeepTogether(false)
                    .setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                var tableHeight = 0f
                var rowIndex = 0

                while (rowIndex < table.numberOfRows) {
                    if (currentHeight + rowHeights[rowIndex] > maxHeight && currentPart.isNotEmpty()) {
                        if (currentTable.numberOfRows > 0) {
                            currentPart.add(currentTable)
                        }
                        parts.add(currentPart)
                        currentPart = mutableListOf()
                        currentHeight = 0f
                        currentTable = Table(UnitValue.createPercentArray(config.columnWidthsPercent))
                            .useAllAvailableWidth()
                            .setKeepTogether(false)
                            .setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                    }

                    for (colIndex in 0 until table.numberOfColumns) {
                        val cell = table.getCell(rowIndex, colIndex)
                        if (cell != null) {
                            val newCell = Cell()
                                .setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                                .setTextAlignment(TextAlignment.CENTER)
                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                .setHeight(rowHeights[rowIndex])
                            cell.getChildren().forEach { child ->
                                if (child is IBlockElement) {
                                    newCell.add(child)
                                } else if (child is Image) {
                                    val padding = 2f
                                    val contentWidth = PAGE_WIDTH - 2 * (config.marginMm * 2.83465f)
                                    val maxImageWidth = (contentWidth * (config.columnWidthsPercent[colIndex] / 100f)) - 2 * padding
                                    child.setWidth(UnitValue.createPointValue(maxImageWidth))
                                    newCell.add(child)
                                } else {
                                    Log.w(TAG, "Elemento de hijo no soportado en celda: ${child.javaClass.simpleName}")
                                }
                            }
                            currentTable.addCell(newCell)
                        }
                    }
                    tableHeight += rowHeights[rowIndex]
                    currentHeight += rowHeights[rowIndex]
                    rowIndex++
                }
                if (currentTable.numberOfRows > 0) {
                    currentPart.add(currentTable)
                }
            } else {
                val elementHeight = estimateElementHeight(element)
                Log.v(TAG, "Elemento: ${element.javaClass.simpleName}, Altura: $elementHeight, Total: $currentHeight")
                if (currentHeight + elementHeight > maxHeight && currentPart.isNotEmpty()) {
                    parts.add(currentPart)
                    currentPart = mutableListOf()
                    currentHeight = 0f
                }
                currentPart.add(element)
                currentHeight += elementHeight
            }
        }

        if (currentPart.isNotEmpty()) {
            parts.add(currentPart)
        }
        return parts.map { part ->
            val processedPart = mutableListOf<com.itextpdf.layout.element.IElement>()
            part.forEach { element ->
                if (element is IBlockElement) {
                    processedPart.add(element)
                } else if (element is Image) {
                    processedPart.add(element)
                } else {
                    Log.w(TAG, "Elemento no soportado en splitContentToFitPage: ${element.javaClass.simpleName}")
                }
            }
            processedPart
        }
    }


    /**
     * Estima la altura de un elemento PDF.
     *
     * @param element Elemento a estimar.
     * @return Altura estimada en puntos.
     */
    private fun estimateElementHeight(element: com.itextpdf.layout.element.IElement): Float {
        return when (element) {
            is Paragraph -> {
                val fontSize = element.getProperty<UnitValue>(Property.FONT_SIZE)?.value ?: 8f
                val leading = element.getProperty<Leading>(Property.LEADING)?.value ?: 1.2f
                val textElements = element.getChildren().filterIsInstance<Text>()
                val text = textElements.joinToString("") { it.text }
                val contentWidth = PAGE_WIDTH - (2 * (config.marginMm * 2.83465f))
                val avgCharsPerLine = (contentWidth / (fontSize * 0.45f)).toInt().coerceAtLeast(1)
                val estimatedLines = if (avgCharsPerLine > 0) (text.length / avgCharsPerLine).coerceAtLeast(1) else 1
                val actualLines = text.count { it == '\n' } + 1
                val lineCount = maxOf(estimatedLines, actualLines)
                val height = fontSize * leading * lineCount
                val marginBottom = element.getProperty<UnitValue>(Property.MARGIN_BOTTOM)?.value ?: 0f
                height + marginBottom + 5f
            }
            is com.itextpdf.layout.element.List -> {
                element.getChildren().sumOf { estimateElementHeight(it).toDouble() }
                    .toFloat() + (element.getProperty<UnitValue>(Property.MARGIN_BOTTOM)?.value ?: 0f) + 5f
            }
            is Table -> {
                var totalHeight = 0f
                for (rowIndex in 0 until element.numberOfRows) {
                    var maxCellHeightInRow = 0f
                    for (colIndex in 0 until element.numberOfColumns) {
                        val cell = element.getCell(rowIndex, colIndex)
                        if (cell != null) {
                            var cellContentHeight = 0f
                            cell.getChildren().forEach { child ->
                                cellContentHeight += estimateElementHeight(child)
                            }
                            maxCellHeightInRow = maxOf(maxCellHeightInRow, cellContentHeight, 20f) // Altura mínima
                        }
                    }
                    totalHeight += maxCellHeightInRow
                }
                totalHeight + (element.getProperty<UnitValue>(Property.MARGIN_BOTTOM)?.value ?: 0f)
            }
            is Cell -> {
                var totalHeight = element.getChildren().sumOf { estimateElementHeight(it).toDouble() }.toFloat()
                totalHeight + (element.getProperty<UnitValue>(Property.MARGIN_BOTTOM)?.value ?: 0f)
            }
            is Image -> {
                // Calcular la altura de la imagen, escalarla a la celda, similar a PDFA4Printer
                val padding = 5f
                val maxImageHeight = config.cellHeightPt - 2 * padding
                val imageWidth = config.signatureWidthPt // o usar el ancho disponible
                val imageHeight = config.signatureHeightPt
                val aspectRatio = imageWidth / imageHeight
                val maxImageWidth = (PAGE_WIDTH - 2 * (config.marginMm * 2.83465f)) / 3 - 2 * padding
                var scaledHeight = imageHeight
                var scaledWidth = imageWidth
                if (scaledWidth > maxImageWidth) {
                    scaledWidth = maxImageWidth
                    scaledHeight = scaledWidth / aspectRatio
                }
                if (scaledHeight > maxImageHeight) {
                    scaledHeight = maxImageHeight
                    scaledWidth = scaledHeight * aspectRatio
                }
                scaledHeight + 2 * padding
            }
            is Div -> element.getChildren().sumOf { estimateElementHeight(it).toDouble() }.toFloat()
            else -> 15f
        }
    }

    /**
     * Implementación por defecto de ElementProcessor para manejar elementos estándar del PDF.
     */
    inner class DefaultElementProcessor : ElementProcessor {
        private var currentTable: Table? = null

        /**
         * Procesa un elemento del documento y lo añade al PDF.
         *
         * @param element Elemento a procesar.
         * @param div Contenedor al que se añadirá el elemento.
         * @param fonts Map de fuentes disponibles.
         * @param indentLevel Nivel de indentación para elementos anidados.
         * @return True si el elemento se procesó correctamente, false en caso contrario.
         */
        override fun process(
            element: DocumentElement,
            div: Div,
            fonts: Map<String, PdfFont>,
            indentLevel: Int
        ): Boolean {
            Log.v(TAG, "Procesando ${element.type} (nivel $indentLevel): ${element.content.take(20)}...")

            val defaultFont = fonts["regular"] ?: PdfFontFactory.createFont()

            when (element.type) {
                "section" -> {
                    div.add(
                        Paragraph(element.content)
                            .setFont(fonts["boldItalic"] ?: defaultFont)
                            .setFontSize(8f)
                            .setTextAlignment(TextAlignment.LEFT)
                            .setMultipliedLeading(1.2f)
                            .setMarginBottom(5f)
                            .setMarginLeft(indentLevel * 8f)
                    )
                    element.children?.forEach { process(it, div, fonts, indentLevel + 1) }
                    return true
                }

                "paragraph" -> {
                    div.add(
                        Paragraph(element.content)
                            .setFont(defaultFont)
                            .setFontSize(8f)
                            .setTextAlignment(TextAlignment.LEFT)
                            .setMultipliedLeading(1.2f)
                            .setMarginBottom(5f)
                            .setMarginLeft(indentLevel * 8f)
                    )
                    return true
                }

                "div" -> {
                    element.children?.forEach { process(it, div, fonts, indentLevel) }
                    return true
                }

                "list" -> {
                    val list = com.itextpdf.layout.element.List()
                        .setMarginBottom(5f)
                        .setMarginLeft(maxOf(0f, indentLevel * 8f))
                        .setPadding(1f)
                        .setFont(defaultFont)
                        .setFontSize(8f)
                        .setSymbolIndent(6f)
                        .setKeepTogether(true)

                    if (element.attributes?.get("ordered") == "true") {
                        val listType = element.attributes?.get("listType") ?: "decimal"
                        val numberingType = when (listType) {
                            "upper-alpha" -> ListNumberingType.ENGLISH_UPPER
                            "lower-alpha" -> ListNumberingType.ENGLISH_LOWER
                            "upper-roman" -> ListNumberingType.ROMAN_UPPER
                            "lower-roman" -> ListNumberingType.ROMAN_LOWER
                            else -> ListNumberingType.DECIMAL
                        }
                        list.setListSymbol(numberingType)
                    } else {
                        list.setListSymbol("\u2022")
                    }

                    element.children?.forEach { child ->
                        if (child.type == "listItem") {
                            val listItem = ListItem()
                            if (child.content.isNotBlank()) {
                                listItem.add(
                                    Paragraph(child.content)
                                        .setFont(defaultFont)
                                        .setFontSize(8f)
                                        .setMultipliedLeading(1.2f)
                                )
                            }

                            child.children?.forEach { grandChild ->
                                when (grandChild.type) {
                                    "span" -> {
                                        if (grandChild.attributes?.get("id") in listOf(
                                                "op_1_checkbox",
                                                "op_2_checkbox"
                                            )
                                        ) {
                                            val isChecked = grandChild.attributes?.get("data-checked") == "true"
                                            val checkboxSymbol = if (isChecked) "✔" else "☐"
                                            listItem.add(
                                                Paragraph(checkboxSymbol)
                                                    .setFont(defaultFont)
                                                    .setFontSize(8f)
                                                    .setMultipliedLeading(1.2f)
                                            )
                                        } else if (grandChild.content.isNotBlank()) {
                                            listItem.add(
                                                Paragraph(grandChild.content)
                                                    .setFont(defaultFont)
                                                    .setFontSize(8f)
                                                    .setMultipliedLeading(1.2f)
                                            )
                                        }
                                    }
                                    "list" -> {
                                        val nestedList = com.itextpdf.layout.element.List()
                                            .setMarginBottom(5f)
                                            .setMarginLeft((indentLevel + 1) * 8f)
                                            .setPadding(1f)
                                            .setFont(defaultFont)
                                            .setFontSize(8f)
                                            .setSymbolIndent(6f)
                                        if (grandChild.attributes?.get("ordered") == "true") {
                                            nestedList.setListSymbol(ListNumberingType.DECIMAL)
                                        } else {
                                            nestedList.setListSymbol("\u2022")
                                        }
                                        grandChild.children?.forEach { greatGrandChild ->
                                            if (greatGrandChild.type == "listItem" && greatGrandChild.content.isNotBlank()) {
                                                val nestedListItem = ListItem()
                                                nestedListItem.add(
                                                    Paragraph(greatGrandChild.content)
                                                        .setFont(defaultFont)
                                                        .setFontSize(8f)
                                                        .setMultipliedLeading(1.2f)
                                                )
                                                nestedList.add(nestedListItem)
                                            }
                                        }
                                        if (!nestedList.isEmpty) {
                                            listItem.add(nestedList)
                                        }
                                    }
                                }
                            }

                            if (!listItem.isEmpty) {
                                list.add(listItem)
                            }
                        }
                    }

                    if (!list.isEmpty) {
                        div.add(list)
                    }
                    return true
                }

                "span" -> {
                    if (element.attributes?.get("id") in listOf("op_1_checkbox", "op_2_checkbox")) {
                        val isChecked = element.attributes?.get("data-checked") == "true"
                        val checkboxSymbol = if (isChecked) "✔" else "☐"
                        div.add(
                            Paragraph()
                                .add(
                                    Text(checkboxSymbol).setFont(defaultFont)
                                        .setFontSize(8f)
                                )
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMultipliedLeading(1.2f)
                                .setMarginBottom(5f)
                                .setMarginLeft(indentLevel * 8f)
                        )
                        return true
                    } else if (element.content.isNotBlank()) {
                        div.add(
                            Paragraph(element.content)
                                .setFont(defaultFont)
                                .setFontSize(8f)
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMultipliedLeading(1.2f)
                                .setMarginBottom(5f)
                                .setMarginLeft(indentLevel * 8f)
                        )
                        return true
                    }
                    return false
                }

                "table" -> {
                    currentTable = Table(UnitValue.createPercentArray(config.columnWidthsPercent))
                        .useAllAvailableWidth()
                        .setKeepTogether(true)
                        .setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                    div.add(currentTable)
                    element.children?.forEach { process(it, div, fonts, indentLevel) }
                    currentTable = null
                    return true
                }

                "tbody" -> {
                    element.children?.forEach { process(it, div, fonts, indentLevel) }
                    return true
                }

                "tr" -> {
                    element.children?.forEach { process(it, div, fonts, indentLevel) }
                    return true
                }

                "td" -> {
                    val cell = Cell()
                        .setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setHeight(config.cellHeightPt)
                        .setWidth(UnitValue.createPointValue(config.cellWidthPt))

                    if (element.content.isNotBlank()) {
                        cell.add(
                            Paragraph(element.content)
                                .setFont(defaultFont)
                                .setFontSize(10f)
                                .setTextAlignment(TextAlignment.CENTER)
                        )
                    }

                    element.children?.forEach { child ->
                        when (child.type) {
                            "img" -> {
                                val src = child.attributes?.get("src")
                                if (src != null) {
                                    try {
                                        val imageData = if (src.startsWith("data:image")) {
                                            val base64Data = src.substringAfter(",")
                                            ImageDataFactory.create(Base64.decode(base64Data, Base64.DEFAULT))
                                        } else {
                                            ImageDataFactory.create(src)
                                        }
                                        val image = Image(imageData)

                                        val padding = 2f
                                        val maxImageWidth = config.cellWidthPt - 2 * padding
                                        image.setWidth(UnitValue.createPointValue(maxImageWidth))
                                        image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)

                                        Log.d(TAG, "Imagen en celda con ancho fijo: ${maxImageWidth}pt")

                                        cell.add(image)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error al procesar imagen: ${e.message}", e)
                                        cell.add(
                                            Paragraph("Error al cargar imagen")
                                                .setFont(defaultFont)
                                                .setFontSize(10f)
                                                .setTextAlignment(TextAlignment.CENTER)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Log.w(TAG, "Elemento hijo no soportado en celda: ${child.type}")
                            }
                        }
                    }
                    currentTable?.addCell(cell)
                    return true
                }

                "img" -> {
                    val src = element.attributes?.get("src")
                    if (src != null) {
                        try {
                            val imageData = if (src.startsWith("data:image")) {
                                val base64Data = src.substringAfter(",")
                                ImageDataFactory.create(Base64.decode(base64Data, Base64.DEFAULT))
                            } else {
                                ImageDataFactory.create(src)
                            }
                            val image = Image(imageData)

                            val maxWidth = config.signatureWidthPt
                            image.setWidth(UnitValue.createPointValue(maxWidth))
                            image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)

                            val imageContainer = Paragraph()
                                .add(image)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginBottom(5f)

                            div.add(imageContainer)

                            Log.d(TAG, "Imagen fuera de celda con ancho fijo: ${maxWidth}pt")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al procesar imagen (src: ${src.take(50)}): ${e.message}", e)
                        }
                    } else {
                        Log.w(TAG, "Atributo src vacío o nulo en elemento img")
                    }
                    return true
                }

                else -> return false
            }
        }
    }
}