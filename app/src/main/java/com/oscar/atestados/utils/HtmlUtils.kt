package com.oscar.atestados.utils

import android.util.Log
import java.io.File

object HtmlUtils {
    /**
     * Utilidad para trabajar con archivos HTML y sustituir contenido mediante identificadores.
     */
    object HtmlUtils {
        private const val TAG = "HtmlUtils"

        /**
         * Sustituye los valores de los identificadores en un archivo HTML con los datos proporcionados.
         *
         * @param htmlFile Archivo HTML base.
         * @param replacements Mapa de identificadores y sus valores de reemplazo.
         * @param tempFile Archivo temporal donde se guardará el HTML modificado.
         * @return Archivo temporal con el HTML sustituido.
         */
        fun replaceHtmlIds(htmlFile: File, replacements: Map<String, String>, tempFile: File): File {
            Log.i(TAG, "Sustituyendo identificadores en ${htmlFile.absolutePath}")
            val htmlContent = htmlFile.readText()

            var updatedContent = htmlContent
            replacements.forEach { (id, value) ->
                val regex = """<span class="underline" id="$id">[^<]*</span>""".toRegex()
                val replacement = """<span class="underline" id="$id">$value</span>"""
                updatedContent = updatedContent.replace(regex, replacement)
                Log.d(TAG, "Sustituido id='$id' con valor='$value'")
            }

            tempFile.writeText(updatedContent)
            Log.i(TAG, "HTML modificado guardado en ${tempFile.absolutePath}")
            return tempFile
        }
    }
}