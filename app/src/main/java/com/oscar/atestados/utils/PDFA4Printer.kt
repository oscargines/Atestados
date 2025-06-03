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
import com.itextpdf.layout.properties.Property
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
        val shieldSizePt: Float = 30f,
        val signatureWidthPx: Float = 400f,
        val signatureHeightPx: Float = 200f,
        val columnWidthsPercent: FloatArray = floatArrayOf(33.3f, 33.3f, 33.3f),
        val cellHeightPt: Float = 80f,
        val cellWidthPt: Float = 100f,
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
        if (htmlContent.isBlank()) {
            Log.e(TAG, "El contenido HTML está vacío")
            throw IllegalArgumentException("El contenido HTML no puede estar vacío")
        }

        // Log temporal para inspeccionar imágenes
        val imgTags = htmlContent.split("<img").drop(1).map { it.substringBefore(">") }
        imgTags.forEach { tag ->
            Log.d(TAG, "Tag img encontrado: <img$tag>")
        }

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

                        val elements = htmlElements.map { convertHtmlElementToDocumentElement(it) }
                        Log.d(TAG, "Elementos convertidos a DocumentElement: ${elements.size}")

                        val titleElement = elements.find { element ->
                            element.type == "section" && element.attributes?.get("class")
                                ?.contains("title") == true
                        }
                        val contentElements = elements.filterNot { element ->
                            element === titleElement
                        }

                        val contentAreaX = 20f * 2.83465f
                        val contentAreaY = 9f * 2.83465f
                        val contentWidth = PAGE_SIZE.width - 2 * marginPt
                        val contentHeight = 272f * 2.83465f

                        val tempDiv =
                            Div().setWidth(UnitValue.createPointValue(contentWidth)).setPadding(0f)
                        contentElements.forEach { element ->
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

                        contentParts.forEachIndexed { index, part ->
                            if (index > 0) {
                                Log.d(TAG, "Añadiendo nueva página #${index + 1}")
                                document.add(AreaBreak())
                            }

                            val contentDiv = Div()
                                .setFixedPosition(contentAreaX, contentAreaY, contentWidth)
                                .setHeight(UnitValue.createPointValue(contentHeight))
                                .setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 0f))
                                .setPadding(2f)

                            part.forEach { element ->
                                if (element is IBlockElement) {
                                    contentDiv.add(element)
                                } else if (element is Image) {
                                    contentDiv.add(element)
                                } else {
                                    Log.w(TAG, "Elemento no soportado al añadir a contentDiv: ${element.javaClass.simpleName}")
                                }
                            }
                            document.add(contentDiv)

                            val pdfPage = pdfDocument.getPage(index + 1)
                            val pdfCanvas = PdfCanvas(pdfPage)
                            pdfCanvas.setLineWidth(1f)

                            val lineLeftX = marginPt - 3f * 2.83465f
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
                                    .setFont(fonts["shield"] ?: PdfFontFactory.createFont())
                                    .setFontSize(config.shieldSizePt)
                                    .setFixedPosition(
                                        marginPt - 15f * 2.83465f,
                                        PAGE_SIZE.height - (10f * 2.83465f) - config.shieldSizePt,
                                        config.shieldSizePt
                                    )
                                    .setMarginBottom(0f)
                                    .setPadding(0f)
                            )
                            document.add(
                                Paragraph("G")
                                    .setFont(fonts["shield"] ?: PdfFontFactory.createFont())
                                    .setFontSize(config.shieldSizePt)
                                    .setFixedPosition(
                                        PAGE_SIZE.width - (3f * 2.83465f) - config.shieldSizePt,
                                        PAGE_SIZE.height - (10f * 2.83465f) - config.shieldSizePt,
                                        config.shieldSizePt
                                    )
                                    .setMarginBottom(0f)
                                    .setPadding(0f)
                            )
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

    private fun estimateElementHeight(element: com.itextpdf.layout.element.IElement): Float {
        return when (element) {
            is Paragraph -> {
                val fontSize = element.getProperty<UnitValue>(Property.FONT_SIZE)?.value ?: 10f
                val leading = element.getProperty<Leading>(Property.LEADING)?.value ?: 1.4f
                val textElements = element.getChildren().filterIsInstance<Text>()
                val text = textElements.joinToString("") { it.text }
                val contentWidth = PAGE_SIZE.width - (2 * (config.marginMm * 2.83465f))
                val avgCharsPerLine = (contentWidth / (fontSize * 0.55f)).toInt().coerceAtLeast(1)
                val estimatedLines = if (avgCharsPerLine > 0) (text.length / avgCharsPerLine).coerceAtLeast(1) else 1
                val actualLines = text.count { it == '\n' } + 1
                val lineCount = maxOf(estimatedLines, actualLines)
                val height = fontSize * leading * lineCount
                val marginBottom = element.getProperty<UnitValue>(Property.MARGIN_BOTTOM)?.value ?: 0f
                height + marginBottom
            }
            is com.itextpdf.layout.element.List -> {
                element.getChildren().sumOf { estimateElementHeight(it).toDouble() }
                    .toFloat() + (element.getProperty<UnitValue>(Property.MARGIN_BOTTOM)?.value ?: 0f)
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
                            maxCellHeightInRow = maxOf(maxCellHeightInRow, cellContentHeight, 20f)
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
                val padding = 5f
                val maxImageHeight = config.cellHeightPt - 2 * padding
                val imageWidth = config.signatureWidthPx / 2.83465f // Usar la config
                val imageHeight = config.signatureHeightPx / 2.83465f // Usar la config
                val aspectRatio = imageWidth / imageHeight
                val maxImageWidth = (PAGE_SIZE.width - 2 * (config.marginMm * 2.83465f)) / 3 - 2 * padding
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
            else -> 20f
        }
    }

    private inner class DefaultElementProcessor : ElementProcessor {
        private var currentTable: Table? = null

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
                            .setFont(fonts["bold"] ?: defaultFont)
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
                        .setFont(defaultFont)
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
                    val list = com.itextpdf.layout.element.List()
                        .setMarginBottom(6f)
                        .setMarginLeft(maxOf(0f, indentLevel * 10f))
                        .setPadding(2f)
                        .setFont(defaultFont)
                        .setFontSize(10f)
                        .setSymbolIndent(10f)

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
                                        .setFontSize(10f)
                                        .setMultipliedLeading(1.4f)
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
                                                    .setFontSize(10f)
                                            )
                                        } else if (grandChild.content.isNotBlank()) {
                                            listItem.add(
                                                Paragraph(grandChild.content)
                                                    .setFont(defaultFont)
                                                    .setFontSize(10f)
                                                    .setMultipliedLeading(1.4f)
                                            )
                                        }
                                    }
                                    "list" -> {
                                        val nestedList = com.itextpdf.layout.element.List()
                                            .setMarginBottom(6f)
                                            .setMarginLeft((indentLevel + 1) * 10f)
                                            .setPadding(2f)
                                            .setFont(defaultFont)
                                            .setFontSize(10f)
                                            .setSymbolIndent(10f)
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
                                                        .setFontSize(10f)
                                                        .setMultipliedLeading(1.4f)
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
                                        .setFontSize(10f)
                                )
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMarginBottom(6f)
                                .setMarginLeft(indentLevel * 10f)
                        )
                        return true
                    } else if (element.content.isNotBlank()) {
                        div.add(
                            Paragraph(element.content)
                                .setFont(defaultFont)
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
                    currentTable = Table(UnitValue.createPercentArray(config.columnWidthsPercent))
                        .useAllAvailableWidth()
                        .setKeepTogether(false)
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
                                .setFontSize(8f)
                                .setTextAlignment(TextAlignment.CENTER)
                        )
                    }

                    element.children?.forEach { child ->
                        when (child.type) {
                            "img" -> {
                                val src = child.attributes?.get("src")
                                val imgId = child.attributes?.get("id") ?: "sin_id"
                                Log.d(TAG, "Procesando imagen en celda, id: $imgId, src: ${src?.take(50) ?: "null"}")

                                if (src != null && src.isNotBlank()) {
                                    try {
                                        val image = when {
                                            src.startsWith("data:image") -> {
                                                val base64Data = src.substringAfter("base64,")
                                                val imageData = Base64.decode(base64Data, Base64.DEFAULT)
                                                Image(ImageDataFactory.create(imageData))
                                            }
                                            else -> {
                                                val cleanPath = src.removePrefix("file://")
                                                Image(ImageDataFactory.create(cleanPath))
                                            }
                                        }

                                        val padding = 2f
                                        val maxImageWidth = config.cellWidthPt - 2 * padding
                                        image.setWidth(UnitValue.createPointValue(maxImageWidth))
                                        image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)

                                        Log.d(TAG, "Imagen en celda con ancho fijo: ${maxImageWidth}pt (id: $imgId)")

                                        cell.add(image)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error al procesar imagen (id: $imgId, src: ${src.take(50)}): ${e.message}", e)
                                        cell.add(
                                            Paragraph("Error al cargar imagen")
                                                .setFont(defaultFont)
                                                .setFontSize(6f)
                                                .setTextAlignment(TextAlignment.CENTER)
                                        )
                                    }
                                } else {
                                    Log.w(TAG, "Atributo src vacío o nulo en elemento img (id: $imgId)")
                                }
                            }
                            "span" -> {
                                if (child.content.isNotBlank()) {
                                    cell.add(
                                        Paragraph(child.content)
                                            .setFont(defaultFont)
                                            .setFontSize(8f)
                                            .setTextAlignment(TextAlignment.CENTER)
                                    )
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
                    val imgId = element.attributes?.get("id") ?: "sin_id"
                    Log.d(TAG, "Procesando imagen independiente, id: $imgId, src: ${src?.take(50) ?: "null"}")

                    if (src != null && src.isNotBlank()) {
                        try {
                            val image = when {
                                src.startsWith("data:image") -> {
                                    val base64Data = src.substringAfter("base64,")
                                    val imageData = Base64.decode(base64Data, Base64.DEFAULT)
                                    Image(ImageDataFactory.create(imageData))
                                }
                                else -> {
                                    val cleanPath = src.removePrefix("file://")
                                    Image(ImageDataFactory.create(cleanPath))
                                }
                            }

                            val maxWidth = config.signatureWidthPx / 2.83465f
                            image.setWidth(UnitValue.createPointValue(maxWidth))
                            image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)

                            Log.d(TAG, "Imagen independiente con ancho fijo: ${maxWidth}pt (id: $imgId)")

                            div.add(image)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al procesar imagen independiente (id: $imgId, src: ${src.take(50)}): ${e.message}", e)
                            div.add(
                                Paragraph("Error al cargar imagen")
                                    .setFont(defaultFont)
                                    .setFontSize(10f)
                            )
                        }
                    } else {
                        Log.w(TAG, "Atributo src vacío o nulo en elemento img independiente (id: $imgId)")
                    }
                    return true
                }

                else -> return false
            }
        }
    }
}