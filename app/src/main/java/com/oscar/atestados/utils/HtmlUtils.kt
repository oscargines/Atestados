package com.oscar.atestados.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import android.util.Log

/**
 * Utilidades para el procesamiento y extracción de elementos HTML.
 */
object HtmlUtils {
    private const val TAG = "HtmlUtils"

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
    ) {
        override fun toString(): String {
            return "HtmlElement(tag='$tag', content='${content.take(20)}...', attributes=$attributes, children=${children.size})"
        }
    }

    /**
     * Extrae elementos HTML relevantes de una cadena HTML dada.
     *
     * Procesa los siguientes elementos: h1, h2, h3, p, ul, li, span, table, tbody, tr, td, img.
     * Ignora otros elementos como scripts.
     *
     * @param html La cadena HTML a procesar.
     * @return Lista de [HtmlElement] extraídos.
     */
    fun extractHtmlElements(html: String): List<HtmlElement> {
        Log.d(TAG, "Iniciando extracción de elementos HTML")
        Log.v(TAG, "HTML de entrada (primeros 200 chars): ${html.take(200)}...")

        return try {
            val doc = Jsoup.parse(html)
            val body = doc.body()
            val elements = mutableListOf<HtmlElement>()

            fun processElement(element: Element, parentTag: String? = null): HtmlElement? {
                val tagName = element.tagName().lowercase() // Normaliza a minúsculas
                Log.v(TAG, "Procesando elemento: $tagName, padre: $parentTag")

                return when (tagName) {
                    "h1", "h2", "h3", "div" -> {
                        val textContent = element.text().trim()
                        val children = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() in listOf("ul", "span", "img", "table")) {
                                processElement(child, tagName)
                            } else null
                        }
                        Log.d(TAG, "Elemento de texto encontrado: $tagName, contenido: '${textContent.take(50)}...'")
                        HtmlElement(
                            tag = tagName,
                            content = textContent,
                            attributes = element.attributes().associate { it.key to it.value },
                            children = children
                        )
                    }
                    "p" -> {
                        val textContent = element.text().trim()
                        val children = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() in listOf("ul", "span", "img", "table")) {
                                processElement(child, tagName)
                            } else null
                        }
                        Log.d(TAG, "Elemento de texto encontrado: $tagName, contenido: '${textContent.take(50)}...'")
                        HtmlElement(
                            tag = tagName,
                            content = textContent,
                            attributes = element.attributes().associate { it.key to it.value },
                            children = children
                        )
                    }
                    "ul" -> {
                        Log.d(TAG, "Lista no ordenada encontrada")
                        val children = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() == "li") {
                                processElement(child, "ul")
                            } else null
                        }
                        HtmlElement(
                            tag = "ul",
                            content = "",
                            attributes = element.attributes().associate { it.key to it.value },
                            children = children
                        )
                    }
                    "li" -> {
                        val textContent = element.text().trim()
                        val subChildren = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() in listOf("ul", "span", "img", "table")) {
                                processElement(child, "li")
                            } else null
                        }
                        Log.d(TAG, "Elemento de lista encontrado, texto: '${textContent.take(50)}...'")
                        HtmlElement(
                            tag = "li",
                            content = textContent,
                            attributes = element.attributes().associate { it.key to it.value },
                            children = subChildren
                        )
                    }
                    "span" -> {
                        val textContent = element.text().trim()
                        Log.d(TAG, "Span encontrado, texto: '${textContent.take(50)}...'")
                        HtmlElement(
                            tag = "span",
                            content = textContent,
                            attributes = element.attributes().associate { it.key to it.value }
                        )
                    }
                    "img" -> {
                        Log.d(TAG, "Imagen encontrada, id: ${element.attr("id")}, src: ${element.attr("src")}")
                        HtmlElement(
                            tag = "img",
                            content = "",
                            attributes = element.attributes().associate { it.key to it.value }
                        )
                    }
                    "table" -> {
                        Log.d(TAG, "Tabla encontrada")
                        val children = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() in listOf("tbody", "tr")) {
                                processElement(child, "table")
                            } else null
                        }
                        HtmlElement(
                            tag = "table",
                            content = "",
                            attributes = element.attributes().associate { it.key to it.value },
                            children = children
                        )
                    }
                    "tbody" -> {
                        Log.d(TAG, "Tbody encontrado")
                        val children = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() == "tr") {
                                processElement(child, "tbody")
                            } else null
                        }
                        HtmlElement(
                            tag = "tbody",
                            content = "",
                            attributes = element.attributes().associate { it.key to it.value },
                            children = children
                        )
                    }
                    "tr" -> {
                        Log.d(TAG, "Fila de tabla (tr) encontrada")
                        val children = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() == "td") {
                                processElement(child, "tr")
                            } else null
                        }
                        HtmlElement(
                            tag = "tr",
                            content = "",
                            attributes = element.attributes().associate { it.key to it.value },
                            children = children
                        )
                    }
                    "td" -> {
                        val textContent = element.text().trim()
                        val children = element.children().mapNotNull { child ->
                            if (child.tagName().lowercase() in listOf("span", "img")) {
                                processElement(child, "td")
                            } else null
                        }
                        Log.d(TAG, "Celda (td) encontrada, contenido: '${textContent.take(50)}...'")
                        HtmlElement(
                            tag = "td",
                            content = textContent,
                            attributes = element.attributes().associate { it.key to it.value },
                            children = children
                        )
                    }
                    else -> {
                        Log.v(TAG, "Elemento ignorado: $tagName")
                        null
                    }
                }
            }

            body.children().forEach { element ->
                processElement(element)?.let {
                    elements.add(it)
                    Log.d(TAG, "Elemento añadido: ${it.tag}")
                }
            }

            if (elements.isEmpty()) {
                Log.w(TAG, "No se encontraron elementos relevantes en el HTML")
                listOf(HtmlElement("p", "SIN CONTENIDO"))
            } else {
                Log.d(TAG, "Extracción completada. Elementos encontrados: ${elements.size}")
                elements.forEachIndexed { index, element ->
                    Log.v(TAG, "Elemento $index: $element")
                }
                elements
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar HTML: ${e.message}", e)
            listOf(HtmlElement("p", "ERROR AL PROCESAR HTML: ${e.message}"))
        }
    }
}