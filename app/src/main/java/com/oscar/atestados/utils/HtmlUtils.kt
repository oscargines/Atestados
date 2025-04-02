package com.oscar.atestados.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object HtmlUtils {
    data class HtmlElement(
        val tag: String,
        val content: String,
        val attributes: Map<String, String> = emptyMap(),
        val children: List<HtmlElement> = emptyList()
    )

    fun extractHtmlElements(html: String): List<HtmlElement> {
        val doc = Jsoup.parse(html)
        val body = doc.body()
        val elements = mutableListOf<HtmlElement>()

        fun processElement(element: Element, parentTag: String? = null): HtmlElement? {
            return when (element.tagName()) {
                "h1", "h2", "h3", "p" -> {
                    HtmlElement(
                        tag = element.tagName(),
                        content = element.text().trim(),
                        attributes = element.attributes().associate { it.key to it.value }
                    )
                }
                "ul" -> {
                    val children = element.children().mapNotNull { child ->
                        if (child.tagName() == "li") processElement(child, "ul") else null
                    }
                    HtmlElement(
                        tag = "ul",
                        content = "",
                        attributes = element.attributes().associate { it.key to it.value },
                        children = children
                    )
                }
                "li" -> {
                    val subChildren = element.children().mapNotNull { child ->
                        if (child.tagName() == "ul") processElement(child) else null
                    }
                    HtmlElement(
                        tag = "li",
                        content = element.ownText().trim(),
                        children = subChildren
                    )
                }
                else -> null // Ignorar otros tags como script
            }
        }

        body.children().forEach { element ->
            processElement(element)?.let { elements.add(it) }
        }

        return elements.ifEmpty { listOf(HtmlElement("p", "SIN CONTENIDO")) }
    }
}