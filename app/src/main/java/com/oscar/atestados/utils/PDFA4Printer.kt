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
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.itextpdf.io.image.ImageDataFactory
import java.io.File
import java.io.FileOutputStream

private const val TAG = "PDFA4Printer"

class PDFA4Printer(
    private val context: Context,
    private val config: DocumentConfig = DocumentConfig()
) {
    companion object {
        private val PAGE_SIZE = PageSize.A4
    }

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

    data class DocumentElement(
        val type: String,
        val content: String,
        val children: List<DocumentElement>? = emptyList(),
        val attributes: Map<String?, String?>? = emptyMap()
    )

    interface ElementProcessor {
        fun process(
            element: DocumentElement,
            div: Div,
            fonts: Map<String, PdfFont>,
            indentLevel: Int
        ): Boolean
    }

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
                    Log.d(
                        TAG,
                        "Tamaño de página configurado: A4 (${PAGE_SIZE.width}pt x ${PAGE_SIZE.height}pt)"
                    )

                    Document(pdfDocument).use { document ->
                        val marginPt = config.marginMm * 2.83465f
                        document.setMargins(marginPt, marginPt, marginPt, marginPt)
                        Log.d(TAG, "Márgenes configurados: ${config.marginMm}mm")

                        val fonts = loadFonts()
                        Log.d(TAG, "Fuentes cargadas: ${fonts.keys}")

                        val htmlElements = HtmlUtils.extractHtmlElements(htmlContent)
                        Log.d(TAG, "Elementos HTML parseados: ${htmlElements.size}")
                        htmlElements.forEachIndexed { index, element ->
                            Log.v(TAG, "Elemento HTML #$index: tag=${element.tag}, content=${element.content.take(50)}..., attributes=${element.attributes}, children=${element.children.size}")
                        }

                        val elements = htmlElements.map { convertHtmlElementToDocumentElement(it) }
                        Log.d(TAG, "Elementos convertidos a DocumentElement: ${elements.size}")
                        elements.forEachIndexed { index, element ->
                            Log.v(TAG, "DocumentElement #$index: type=${element.type}, content=${element.content.take(50)}..., attributes=${element.attributes}, children=${element.children?.size ?: 0}")
                        }

                        val titleElement = elements.find { element: DocumentElement ->
                            element.type == "section" && element.attributes?.get("class")
                                ?.contains("title") == true
                        }
                        val contentElements = elements.filterNot { element: DocumentElement ->
                            element === titleElement
                        }

                        val contentAreaX = 20f * 2.83465f
                        val contentAreaY = 9f * 2.83465f
                        val contentWidth = PAGE_SIZE.width - 2 * marginPt
                        val contentHeight = 272f * 2.83465f

                        val tempDiv =
                            Div().setWidth(UnitValue.createPointValue(contentWidth)).setPadding(0f)
                        contentElements.forEach { element: DocumentElement ->
                            processor.process(element, tempDiv, fonts, 0)
                        }

                        val contentParts = splitContentToFitPage(
                            tempDiv,
                            contentHeight,
                            pdfDocument,
                            document,
                            contentAreaX,
                            contentAreaY,
                            contentWidth,
                            fonts
                        )
                        Log.d(TAG, "Contenido dividido en ${contentParts.size} páginas")

                        contentParts.forEachIndexed { index: Int, part: List<com.itextpdf.layout.element.IElement> ->
                            if (index > 0) {
                                Log.d(TAG, "Añadiendo nueva página #${index + 1}")
                                document.add(AreaBreak())
                            }

                            val contentDiv = Div()
                                .setFixedPosition(contentAreaX, contentAreaY, contentWidth)
                                .setHeight(UnitValue.createPointValue(contentHeight))
                                .setBorder(SolidBorder(ColorConstants.WHITE, 0f))
                                .setPadding(2f)

                            part.forEach { element ->
                                contentDiv.add(element as IBlockElement)
                            }
                            document.add(contentDiv)

                            val pdfPage = pdfDocument.getPage(index + 1)
                            val pdfCanvas = PdfCanvas(pdfPage)
                            pdfCanvas.setLineWidth(1f)

                            val lineLeftX = 17f * 2.83465f
                            pdfCanvas.moveTo(
                                lineLeftX.toDouble(),
                                (PAGE_SIZE.height - (6f * 2.83465f)).toDouble()
                            )
                            pdfCanvas.lineTo(lineLeftX.toDouble(), (15f * 2.83465f).toDouble())
                            pdfCanvas.stroke()

                            val lineRightX = PAGE_SIZE.width - (15f * 2.83465f)
                            pdfCanvas.moveTo(
                                lineRightX.toDouble(),
                                (PAGE_SIZE.height - (6f * 2.83465f)).toDouble()
                            )
                            pdfCanvas.lineTo(lineRightX.toDouble(), (15f * 2.83465f).toDouble())
                            pdfCanvas.stroke()

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

                            if (titleElement != null && titleElement.content.isNotEmpty()) {
                                document.add(
                                    Paragraph(titleElement.content.uppercase())
                                        .setFont(fonts["bold"] ?: fonts["regular"]!!)
                                        .setFontSize(16f)
                                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setFixedPosition(
                                            marginPt,
                                            PAGE_SIZE.height - marginPt - config.shieldSizePt - 40f,
                                            contentWidth
                                        )
                                        .setPadding(5f)
                                )
                            }

                            val box1X = 20f * 2.83465f
                            val box1Y = 283f * 2.83465f
                            val box1Width = 75f * 2.83465f
                            val box1Height = 7f * 2.83465f

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

                            val box2X = 167f * 2.83465f
                            val box2Y = 283f * 2.83465f
                            val box2Width = 25f * 2.83465f
                            val box2Height = 7f * 2.83465f

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

    private fun convertHtmlElementToDocumentElement(htmlElement: HtmlUtils.HtmlElement): DocumentElement {
        Log.v(TAG, "Convirtiendo HTML tag: ${htmlElement.tag} a DocumentElement")
        Log.v(TAG, "Contenido original: ${htmlElement.content.take(50)}...")
        Log.v(TAG, "Atributos originales: ${htmlElement.attributes}")

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

        val attributes = when (htmlElement.tag) {
            "ul" -> htmlElement.attributes + ("type" to "bullet")
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
                htmlElement.attributes + ("ordered" to "true") + ("listType" to listType)
            }
            else -> htmlElement.attributes
        }

        val children = htmlElement.children.map { convertHtmlElementToDocumentElement(it) }

        return DocumentElement(
            type = type,
            content = htmlElement.content,
            attributes = attributes as Map<String?, String?>?,
            children = children
        )
    }

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

    private fun splitContentToFitPage(
        tempDiv: Div,
        maxHeight: Float,
        pdfDocument: PdfDocument,
        document: Document,
        contentAreaX: Float,
        contentAreaY: Float,
        contentWidth: Float,
        fonts: Map<String, PdfFont>
    ): List<List<com.itextpdf.layout.element.IElement>> {
        val parts = mutableListOf<MutableList<com.itextpdf.layout.element.IElement>>()
        var currentPart = mutableListOf<com.itextpdf.layout.element.IElement>()
        var currentHeight = 0f

        tempDiv.children.forEach { element ->
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

        if (currentPart.isNotEmpty()) {
            parts.add(currentPart)
        }

        return parts
    }

    private fun estimateElementHeight(element: com.itextpdf.layout.element.IElement): Float {
        return when (element) {
            is Paragraph -> {
                val fontSize = element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.FONT_SIZE)?.getValue() ?: 10f
                val leading = element.getProperty<Leading>(com.itextpdf.layout.properties.Property.LEADING)?.value ?: 1.4f
                val textElements = element.children.filterIsInstance<Text>()
                val text = textElements.joinToString("") { it.text }
                val contentWidth = PAGE_SIZE.width - (2 * (config.marginMm * 2.83465f))
                val avgCharsPerLine = (contentWidth / (fontSize * 0.55f)).toInt().coerceAtLeast(1)
                val estimatedLines = if (avgCharsPerLine > 0) (text.length / avgCharsPerLine).coerceAtLeast(1) else 1
                val actualLines = text.count { it == '\n' } + 1
                val lineCount = maxOf(estimatedLines, actualLines)
                val height = fontSize * leading * lineCount
                val marginBottom = element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.MARGIN_BOTTOM)?.getValue() ?: 0f
                height + marginBottom
            }
            is com.itextpdf.layout.element.List -> {
                element.children.sumOf { estimateElementHeight(it).toDouble() }
                    .toFloat() + (element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.MARGIN_BOTTOM)?.getValue() ?: 0f)
            }
            is Table -> {
                var totalHeight = 0f
                for (rowIndex in 0 until element.numberOfRows) {
                    for (colIndex in 0 until element.numberOfColumns) {
                        val cell = element.getCell(rowIndex, colIndex)
                        if (cell != null) {
                            totalHeight += estimateElementHeight(cell)
                        }
                    }
                }
                totalHeight + (element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.MARGIN_BOTTOM)?.getValue() ?: 0f)
            }
            is Cell -> {
                var totalHeight = element.children.sumOf { estimateElementHeight(it).toDouble() }.toFloat()
                element.children.filterIsInstance<Image>().forEach { image ->
                    val height = image.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.HEIGHT)?.getValue() ?: 100f
                    totalHeight += height
                }
                totalHeight + (element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.MARGIN_BOTTOM)?.getValue() ?: 0f)
            }
            is Image -> {
                element.getProperty<UnitValue>(com.itextpdf.layout.properties.Property.HEIGHT)?.getValue() ?: 100f
            }
            is Div -> element.children.sumOf { estimateElementHeight(it).toDouble() }.toFloat()
            else -> 20f
        }
    }

    inner class DefaultElementProcessor : ElementProcessor {
        private var currentTable: Table? = null

        override fun process(
            element: DocumentElement,
            div: Div,
            fonts: Map<String, PdfFont>,
            indentLevel: Int
        ): Boolean {
            Log.v(TAG, "Procesando ${element.type} (nivel $indentLevel): ${element.content.take(20)}...")
            Log.v(TAG, "Atributos del elemento: ${element.attributes}")
            Log.v(TAG, "Número de hijos: ${element.children?.size ?: 0}")

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
                    element.children?.forEach { process(it, div, fonts, indentLevel + 1) }
                    return true
                }

                "paragraph" -> {
                    val paragraph = Paragraph(element.content)
                        .setFont(fonts["regular"]!!)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMultipliedLeading(1.4f)
                        .setMarginBottom(6f)
                        .setMarginLeft(indentLevel * 10f)
                    div.add(paragraph)
                    return true
                }

                "div" -> {
                    element.children?.forEach { process(it, div, fonts, indentLevel) }
                    return true
                }

                "list" -> {
                    Log.d(TAG, "Procesando lista con ${element.children?.size} elementos")
                    Log.d(TAG, "Atributos de lista: ${element.attributes}")

                    val list = com.itextpdf.layout.element.List()
                        .setMarginBottom(6f)
                        .setMarginLeft(maxOf(0f, indentLevel * 10f))
                        .setPadding(2f)
                        .setFont(fonts["regular"]!!)
                        .setFontSize(10f)
                        .setSymbolIndent(10f)

                    if (element.attributes?.get("ordered") == "true") {
                        val listType = element.attributes?.get("listType") ?: "decimal"
                        Log.d(TAG, "Lista ordenada detectada")
                        val numberingType = when (listType) {
                            "upper-alpha" -> ListNumberingType.ENGLISH_UPPER
                            "lower-alpha" -> ListNumberingType.ENGLISH_LOWER
                            "upper-roman" -> ListNumberingType.ROMAN_UPPER
                            "lower-roman" -> ListNumberingType.ROMAN_LOWER
                            else -> ListNumberingType.DECIMAL
                        }
                        list.setListSymbol(numberingType)
                    } else {
                        Log.d(TAG, "Lista no ordenada detectada")
                        list.setListSymbol("\u2022")
                    }

                    element.children?.forEach { child ->
                        if (child.type == "listItem") {
                            Log.d(TAG, "Procesando listItem: ${child.content.take(50)}...")
                            Log.d(TAG, "Atributos de listItem: ${child.attributes}")
                            Log.d(TAG, "Hijos de listItem: ${child.children?.size ?: 0}")

                            val listItem = ListItem()
                            if (child.content.isNotBlank()) {
                                Log.d(TAG, "Añadiendo contenido principal del listItem")
                                listItem.add(
                                    Paragraph(child.content)
                                        .setFont(fonts["regular"]!!)
                                        .setFontSize(10f)
                                )
                            }

                            child.children?.forEach { grandChild ->
                                Log.d(TAG, "Procesando hijo de listItem: ${grandChild.type}")
                                when (grandChild.type) {
                                    "span" -> {
                                        Log.d(TAG, "Procesando span dentro de listItem")
                                        if (grandChild.attributes?.get("id") in listOf(
                                                "op_1_checkbox",
                                                "op_2_checkbox"
                                            )
                                        ) {
                                            val isChecked = grandChild.attributes?.get("data-checked") == "true"
                                            val checkboxSymbol = if (isChecked) "✔" else "☐"
                                            Log.d(TAG, "Checkbox procesado: ${grandChild.attributes?.get("id")} = $checkboxSymbol")
                                            listItem.add(
                                                Paragraph(checkboxSymbol)
                                                    .setFont(fonts["regular"]!!)
                                                    .setFontSize(10f)
                                            )
                                        } else if (grandChild.content.isNotBlank()) {
                                            Log.d(TAG, "Añadiendo contenido de span")
                                            listItem.add(
                                                Paragraph(grandChild.content)
                                                    .setFont(fonts["regular"]!!)
                                                    .setFontSize(10f)
                                            )
                                        }
                                    }
                                    "list" -> {
                                        Log.d(TAG, "Procesando lista anidada")
                                        val nestedList = com.itextpdf.layout.element.List()
                                            .setMarginBottom(6f)
                                            .setMarginLeft((indentLevel + 1) * 10f)
                                            .setPadding(2f)
                                            .setFont(fonts["regular"]!!)
                                            .setFontSize(10f)
                                            .setSymbolIndent(10f)
                                        if (grandChild.attributes?.get("ordered") == "true") {
                                            nestedList.setListSymbol(ListNumberingType.DECIMAL)
                                        } else {
                                            nestedList.setListSymbol("\u2022")
                                        }
                                        grandChild.children?.forEach { greatGrandChild ->
                                            if (greatGrandChild.type == "listItem" && greatGrandChild.content.isNotBlank()) {
                                                Log.d(TAG, "Añadiendo elemento a lista anidada")
                                                val nestedListItem = ListItem()
                                                nestedListItem.add(
                                                    Paragraph(greatGrandChild.content)
                                                        .setFont(fonts["regular"]!!)
                                                        .setFontSize(10f)
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
                                Log.d(TAG, "Añadiendo listItem a la lista")
                                list.add(listItem)
                            } else {
                                Log.d(TAG, "listItem está vacío, no se añade")
                            }
                        }
                    }

                    if (!list.isEmpty) {
                        Log.d(TAG, "Añadiendo lista al div con ${list.children.size} elementos")
                        div.add(list)
                    } else {
                        Log.d(TAG, "La lista está vacía, no se añade")
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
                                    Text(checkboxSymbol).setFont(fonts["regular"]!!)
                                        .setFontSize(10f)
                                )
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMarginBottom(6f)
                                .setMarginLeft(indentLevel * 10f)
                        )
                        Log.d(TAG, "Checkbox procesado: ${element.attributes?.get("id")} = $checkboxSymbol")
                        return true
                    } else if (element.content.isNotBlank()) {
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
                    return false
                }

                "table" -> {
                    currentTable = Table(UnitValue.createPercentArray(3)) // 3 columnas para la tabla
                        .useAllAvailableWidth()
                        .setKeepTogether(true)
                    div.add(currentTable)
                    element.children?.forEach { process(it, div, fonts, indentLevel) }
                    currentTable = null // Resetear después de procesar la tabla
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
                    if (element.content.isNotBlank()) {
                        cell.add(
                            Paragraph(element.content)
                                .setFont(fonts["regular"]!!)
                                .setFontSize(8f)
                                .setTextAlignment(TextAlignment.CENTER)
                        )
                    }
                    element.children?.forEach { child ->
                        when (child.type) {
                            "img" -> {
                                val src = child.attributes?.get("src")
                                if (src != null) {
                                    try {
                                        val imageFile = File(src.replace("file://", ""))
                                        if (imageFile.exists()) {
                                            val imageData = ImageDataFactory.create(imageFile.readBytes())
                                            val image = Image(imageData)
                                            val width = child.attributes?.get("width")?.toFloatOrNull() ?: 200f
                                            val height = child.attributes?.get("height")?.toFloatOrNull() ?: 100f
                                            image.setWidth(UnitValue.createPointValue(width / 2.83465f)) // Convertir px a pt
                                            image.setHeight(UnitValue.createPointValue(height / 2.83465f))
                                            image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER) // Centrar horizontalmente
                                            cell.add(image)
                                        } else {
                                            Log.w(TAG, "Imagen no encontrada: $src")
                                            cell.add(Paragraph("Imagen no disponible").setFont(fonts["regular"]!!).setFontSize(10f))
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error al cargar imagen: $src", e)
                                        cell.add(Paragraph("Error al cargar imagen").setFont(fonts["regular"]!!).setFontSize(10f))
                                    }
                                }
                            }
                            "span" -> {
                                if (child.content.isNotBlank()) {
                                    cell.add(
                                        Paragraph(child.content)
                                            .setFont(fonts["regular"]!!)
                                            .setFontSize(10f)
                                    )
                                }
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
                            val imageFile = File(src.replace("file://", ""))
                            if (imageFile.exists()) {
                                val imageData = ImageDataFactory.create(imageFile.readBytes())
                                val image = Image(imageData)
                                val width = element.attributes?.get("width")?.toFloatOrNull() ?: 200f
                                val height = element.attributes?.get("height")?.toFloatOrNull() ?: 100f
                                image.setWidth(UnitValue.createPointValue(width / 2.83465f))
                                image.setHeight(UnitValue.createPointValue(height / 2.83465f))
                                div.add(image)
                            } else {
                                Log.w(TAG, "Imagen no encontrada: $src")
                                div.add(Paragraph("Imagen no disponible").setFont(fonts["regular"]!!).setFontSize(10f))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al cargar imagen: $src", e)
                            div.add(Paragraph("Error al cargar imagen").setFont(fonts["regular"]!!).setFontSize(10f))
                        }
                    }
                    return true
                }

                else -> return false
            }
        }
    }
}