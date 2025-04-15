package com.oscar.atestados.utils

import com.oscar.atestados.data.DniData
import android.util.Log
import java.util.regex.Pattern

/**
 * Clase para analizar y extraer datos estructurados de códigos QR de documentos de identidad.
 *
 * Implementa el parsing de contenido de QR según el formato estándar de DNIs españoles,
 * extrayendo información como nombre, apellidos, número de documento, etc.
 */
class QrDataParser {

    /**
     * Analiza el contenido de un código QR y extrae los datos de identificación.
     *
     * @param qrContent Cadena de texto cruda obtenida del escaneo del QR.
     * @return [DniData] con los campos extraídos. En caso de error, los campos serán nulos
     *         y se incluirá un mensaje en la propiedad `error`.
     *
     * @throws IllegalArgumentException Si el contenido del QR está vacío o es inválido.
     *
     * @sample Ejemplo de uso:
     * ```
     * val parser = QrDataParser()
     * val dniData = parser.parseQrContent(qrString)
     * if (dniData.error != null) {
     *     // Manejar error
     * }
     * ```
     *
     * @note Los patrones de regex están diseñados para el formato específico de DNIs españoles.
     *       Puede requerir ajustes para otros formatos de documentos.
     */
    fun parseQrContent(qrContent: String): DniData {
        try {
            // Limpiar caracteres no imprimibles
            val cleanedContent = qrContent.replace("[^\\x20-\\x7E]".toRegex(), "")
            Log.d("QrDataParser", "Contenido limpio: $cleanedContent") // Log para depurar

            // Patrones ajustados al formato del QR
            val documentoPattern = Pattern.compile("@\\s*([0-9XYZ]\\d{7}[A-Z])(?=B)") // Después de @, antes de B
            val nombrePattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}.*?D\\s*([A-Z\\s]+)(?=F)") // Después de fecha y D, antes de F
            val apellidosPattern = Pattern.compile("(?<=F)[A-Z\\s]+(?=H)") // Entre F y H
            val generoPattern = Pattern.compile("(?<=H)M[L]?") // Después de H
            val fechaNacimientoPattern = Pattern.compile("\\d{2}-\\d{2}-\\d{4}") // Primera fecha
            val direccionPattern = Pattern.compile("AVDA\\..+?(?=b)") // Desde AVDA. hasta b
            val lugarNacimientoPattern = Pattern.compile("b\\s*([A-Z\\s]+)(?=x\\s*[A-Z])") // Después de b, antes de x
            val nacionalidadPattern = Pattern.compile("d\\s*(ESP)(?=f)") // Después de d, antes de f
            val nomPadresPattern = Pattern.compile("f\\s*([A-Z\\s]+ / [A-Z\\s]+)(?=@)") // Después de f, antes de @

            val documentoMatcher = documentoPattern.matcher(cleanedContent)
            val nombreMatcher = nombrePattern.matcher(cleanedContent)
            val apellidosMatcher = apellidosPattern.matcher(cleanedContent)
            val generoMatcher = generoPattern.matcher(cleanedContent)
            val fechaNacimientoMatcher = fechaNacimientoPattern.matcher(cleanedContent)
            val direccionMatcher = direccionPattern.matcher(cleanedContent)
            val lugarNacimientoMatcher = lugarNacimientoPattern.matcher(cleanedContent)
            val nacionalidadMatcher = nacionalidadPattern.matcher(cleanedContent)
            val nomPadresMatcher = nomPadresPattern.matcher(cleanedContent)

            // Extraer documento y determinar tipo
            val documento = if (documentoMatcher.find()) documentoMatcher.group(1)?.trim() else null
            val tipoDocumento = when {
                documento?.matches(Regex("\\d{8}[A-Z]")) == true -> "DNI"
                documento?.matches(Regex("[XYZ]\\d{7}[A-Z]")) == true -> "NIE"
                else -> null
            }

            // Extraer nombre (grupo 1)
            val nombre = if (nombreMatcher.find()) nombreMatcher.group(1)?.trim() else null

            // Extraer apellidos
            val apellidos = if (apellidosMatcher.find()) apellidosMatcher.group(0)?.trim() else null

            // Extraer género
            val genero = if (generoMatcher.find()) {
                when (generoMatcher.group(0)) { "M", "ML" -> "Masculino" else -> null }
            } else null

            // Extraer fecha de nacimiento (primera aparición)
            val fechaNacimiento = if (fechaNacimientoMatcher.find()) fechaNacimientoMatcher.group(0) else null

            // Extraer dirección (sin grupo, captura directa)
            val domicilio = if (direccionMatcher.find()) direccionMatcher.group(0)?.trim() else null

            // Extraer lugar de nacimiento (grupo 1)
            val lugarNacimiento = if (lugarNacimientoMatcher.find()) lugarNacimientoMatcher.group(1)?.trim() else null

            // Extraer nacionalidad (grupo 1)
            val nacionalidad = if (nacionalidadMatcher.find()) nacionalidadMatcher.group(1)?.trim() else null

            // Extraer nombres de padres y dividirlos (grupo 1)
            val (nombrePadre, nombreMadre) = if (nomPadresMatcher.find()) {
                nomPadresMatcher.group(1)?.split("/")?.let {
                    it.getOrNull(0)?.trim() to it.getOrNull(1)?.trim()
                } ?: (null to null)
            } else null to null

            return DniData(
                genero = genero,
                nacionalidad = nacionalidad,
                tipoDocumento = tipoDocumento,
                numeroDocumento = documento,
                numeroSoporte = null,
                nombre = nombre,
                apellidos = apellidos,
                nombrePadre = nombrePadre,
                nombreMadre = nombreMadre,
                fechaNacimiento = fechaNacimiento,
                lugarNacimiento = lugarNacimiento,
                domicilio = domicilio,
                uid = null,
                can = null,
                error = null
            )
        } catch (e: Exception) {
            Log.e("QrDataParser", "Error al procesar QR: ${e.message}")
            return DniData(
                genero = null,
                nacionalidad = null,
                tipoDocumento = null,
                numeroDocumento = null,
                numeroSoporte = null,
                nombre = null,
                apellidos = null,
                nombrePadre = null,
                nombreMadre = null,
                fechaNacimiento = null,
                lugarNacimiento = null,
                domicilio = null,
                uid = null,
                can = null,
                error = "Error al procesar el QR: ${e.message}"
            )
        }
    }
}