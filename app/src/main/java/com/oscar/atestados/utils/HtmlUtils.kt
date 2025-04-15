package com.oscar.atestados.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Utilidades para el procesamiento y extracción de elementos HTML.
 */
object HtmlUtils {
    /**
     * Representa un elemento HTML con su estructura básica.
     *
     * @property tag El nombre de la etiqueta HTML (ej. "h1", "p", "ul").
     * @property content El contenido de texto del elemento.
     * @property attributes Map de atributos del elemento (clave-valor).
     * @property children Lista de elementos hijos anidados.
     */
    data class HtmlElement(
        val tag: String,
        val content: String,
        val attributes: Map<String, String> = emptyMap(),
        val children: List<HtmlElement> = emptyList()
    )

    /**
     * Extrae elementos HTML relevantes de una cadena HTML dada.
     *
     * Procesa los siguientes elementos: h1, h2, h3, p, ul, li.
     * Ignora otros elementos como scripts.
     *
     * @param html La cadena HTML a procesar.
     * @return Lista de [HtmlElement] extraídos. Si no se encuentran elementos,
     *         retorna un único elemento con tag "p" y contenido "SIN CONTENIDO".
     */
    fun extractHtmlElements(html: String): List<HtmlElement> {
        val doc = Jsoup.parse(html)
        val body = doc.body()
        val elements = mutableListOf<HtmlElement>()

        /**
         * Función interna para procesar recursivamente un elemento HTML.
         *
         * @param element El elemento actual a procesar.
         * @param parentTag El tag del elemento padre (usado para contexto).
         * @return El [HtmlElement] procesado o null si el tag no es relevante.
         */
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