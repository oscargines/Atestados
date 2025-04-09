package com.oscar.atestados.utils

import android.util.Log
import com.oscar.atestados.data.DniData
import com.oscar.atestados.data.RawNfcData
import org.bouncycastle.asn1.ASN1ApplicationSpecific
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1TaggedObject
import org.bouncycastle.asn1.DEROctetString
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
/**
 * Clase para parsear datos NFC de documentos electrónicos como DNIe.
 * Procesa los Data Groups (DG) del chip NFC y extrae la información personal.
 */
class NfcDataParser {

    private val TAG = "NfcDataParser"
    /**
     * Parsea los datos brutos NFC y extrae la información personal del documento.
     *
     * @param rawData Datos brutos leídos del chip NFC.
     * @return Objeto [DniData] con la información personal extraída.
     */
    fun parseRawData(rawData: RawNfcData): DniData {
        if (rawData.dg1Bytes == null && rawData.dg13Bytes == null) {
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
                uid = rawData.uid,
                can = rawData.can,
                error = "No se pudo leer los datos del DNI (DG1 y DG13 no disponibles)"
            )
        }

        val dg1Data = rawData.dg1Bytes?.let { parseDG1(it) } ?: emptyMap()
        val dg13Data = rawData.dg13Bytes?.let { parseDG13(it) } ?: emptyMap()

        if (dg1Data.isEmpty() && dg13Data.isEmpty()) {
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
                uid = rawData.uid,
                can = rawData.can,
                error = "No se pudieron parsear los datos del DNI (DG1 y DG13 vacíos)"
            )
        }

        val genero = (dg13Data["sex"] ?: dg1Data["sex"])?.let { sex ->
            when (sex.uppercase()) {
                "F" -> "Femenino"
                "M" -> "Masculino"
                else -> null
            }
        }
        val fechaNacimiento = dg13Data["birthDate"]?.let { formatDate(it) } ?: dg1Data["birthDate"]?.let { formatDate(it) }
        val apellidos = dg13Data["surname1"]?.let { surname1 ->
            dg13Data["surname2"]?.let { surname2 -> "$surname1 $surname2" } ?: surname1
        } ?: dg1Data["surname"]
        val numeroDocumentoRaw = dg13Data["personalNumber"] ?: dg1Data["docNumber"]
        val numeroDocumento = numeroDocumentoRaw?.replace("-", "") // Eliminar el guión
        val nombre = dg13Data["name"] ?: dg1Data["name"]

        val errorMessage = if (numeroDocumento == null && nombre == null && apellidos == null) {
            "No se pudieron extraer datos clave del DNI (número, nombre, apellidos)"
        } else null

        return DniData(
            genero = genero,
            nacionalidad = dg13Data["nationality"] ?: dg1Data["nationality"] ?: "España",
            tipoDocumento = dg1Data["docType"] ?: "DNI",
            numeroDocumento = numeroDocumento,
            numeroSoporte = dg13Data["docNumber"] ?: dg1Data["docNumber"],
            nombre = nombre,
            apellidos = apellidos,
            nombrePadre = dg13Data["fatherName"],
            nombreMadre = dg13Data["motherName"],
            fechaNacimiento = fechaNacimiento,
            lugarNacimiento = dg13Data["birthPlace"],
            domicilio = dg13Data["actualAddress"],
            uid = rawData.uid,
            can = rawData.can,
            error = errorMessage
        ).also {
            Log.d(TAG, "Datos parseados: $it")
        }
    }
    /**
     * Parsea los bytes del Data Group 1 (DG1) que contiene la zona de lectura mecánica (MRZ).
     *
     * @param bytes Bytes del DG1.
     * @return Mapa con los campos extraídos del MRZ.
     */
    private fun parseDG1(bytes: ByteArray): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        try {
            Log.d(TAG, "Datos crudos DG1: ${bytes.joinToString("") { "%02x".format(it) }}")
            val asn1InputStream = ASN1InputStream(ByteArrayInputStream(bytes))
            val root = asn1InputStream.readObject()
            Log.d(TAG, "Objeto ASN1 raíz: $root")

            // Extract MRZ from [APPLICATION 1][APPLICATION 31]
            val mrzBytes = when (root) {
                is ASN1ApplicationSpecific -> {
                    if (root.applicationTag == 1) {
                        val innerStream = ASN1InputStream(root.contents)
                        val innerObj = innerStream.readObject()
                        if (innerObj is ASN1ApplicationSpecific && innerObj.applicationTag == 31) {
                            innerObj.contents
                        } else {
                            Log.w(TAG, "Unexpected inner ASN1 structure: $innerObj")
                            innerObj?.toASN1Primitive()?.encoded ?: bytes
                        }.also { innerStream.close() }
                    } else {
                        Log.w(TAG, "Unexpected application tag: ${root.applicationTag}")
                        root.contents
                    }
                }
                else -> {
                    Log.w(TAG, "DG1 no tiene el formato esperado: $root")
                    bytes // Fallback to raw bytes (may need adjustment)
                }
            }

            val mrzString = String(mrzBytes, Charset.forName("UTF-8")).trim()
            Log.d(TAG, "MRZ extraído (longitud=${mrzString.length}): $mrzString")

            // Parse MRZ (TD3 format, 88 characters)
            if (mrzString.length == 88) {
                val line1 = mrzString.substring(0, 44)
                val line2 = mrzString.substring(44, 88)

                result["docType"] = line1.substring(0, 2)
                result["issuingState"] = line1.substring(2, 5)
                result["docNumber"] = line1.substring(5, 14).replace("<", "")

                result["birthDate"] = line2.substring(0, 6)
                result["sex"] = line2.substring(7, 8)
                result["expiryDate"] = line2.substring(8, 14)
                result["nationality"] = line2.substring(14, 17)
                val nameParts = line2.substring(20).split("<<")
                result["surname"] = nameParts[0].replace("<", " ").trim()
                result["name"] = nameParts.getOrNull(1)?.replace("<", " ")?.trim()
            } else {
                Log.w(TAG, "MRZ tiene longitud inválida: ${mrzString.length}")
                // Log raw MRZ bytes for debugging
                Log.d(TAG, "MRZ bytes: ${mrzBytes.joinToString("") { "%02x".format(it) }}")
            }
            asn1InputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear DG1: ${e.message}", e)
        }
        Log.d(TAG, "Resultado DG1 parseado: $result")
        return result
    }
    /**
     * Parsea los bytes del Data Group 11 (DG11) que contiene información de domicilio.
     *
     * @param bytes Bytes del DG11.
     * @return Mapa con los campos de dirección extraídos.
     */
    private fun parseDG11(bytes: ByteArray): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        try {
            val asn1InputStream = ASN1InputStream(ByteArrayInputStream(bytes))
            val root = asn1InputStream.readObject()

            if (root is ASN1Sequence) {
                val elements = root.toList()
                if (elements.size >= 5) {
                    result["birthPlace"] = getStringFromASN1(elements.getOrNull(0) as ASN1Primitive?) // Lugar de nacimiento
                    result["actualAddress"] = getStringFromASN1(elements.getOrNull(1) as ASN1Primitive?) // Dirección
                    result["cityAddress"] = getStringFromASN1(elements.getOrNull(2) as ASN1Primitive?) // Ciudad
                    result["provinceAddress"] = getStringFromASN1(elements.getOrNull(3) as ASN1Primitive?) // Provincia
                    result["countryAddress"] = getStringFromASN1(elements.getOrNull(4) as ASN1Primitive?) // País
                }
            }
            asn1InputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear DG11: ${e.message}", e)
        }
        return result
    }
    /**
     * Parsea los bytes del Data Group 13 (DG13) que contiene datos personales extendidos.
     *
     * @param bytes Bytes del DG13.
     * @return Mapa con los campos personales extraídos.
     */
    private fun parseDG13(bytes: ByteArray): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        try {
            Log.d(TAG, "Datos crudos DG13: ${bytes.joinToString("") { "%02x".format(it) }}")
            val asn1InputStream = ASN1InputStream(ByteArrayInputStream(bytes))
            val root = asn1InputStream.readObject()
            Log.d(TAG, "Objeto ASN1 raíz DG13: $root")

            val content = (root as? ASN1ApplicationSpecific)?.contents ?: bytes
            val contentStream = ASN1InputStream(ByteArrayInputStream(content))
            val sequence = contentStream.readObject() as? ASN1Sequence
                ?: throw IllegalStateException("DG13 root is not a SEQUENCE")

            sequence.objects.toList().forEachIndexed { index, obj ->
                val value = when (obj) {
                    is DEROctetString -> String(obj.octets, Charset.forName("UTF-8")).trim()
                    is ASN1TaggedObject -> getStringFromASN1(obj.`object`)
                    else -> obj.toString().trim()
                }
                Log.d(TAG, "Elemento $index: $value")
                when (index) {
                    0 -> result["surname1"] = value  // Primer apellido
                    1 -> result["surname2"] = value  // Segundo apellido
                    2 -> result["name"] = value      // Nombre completo
                    3 -> result["personalNumber"] = value
                    4 -> result["birthDate"] = value
                    5 -> result["nationality"] = value
                    6 -> result["expirationDate"] = value
                    7 -> result["docNumber"] = value
                    8 -> result["sex"] = value
                    9 -> result["birthPopulation"] = value
                    10 -> result["birthProvince"] = value
                    11 -> {
                        val parents = value?.split(" / ")
                        result["fatherName"] = parents?.getOrNull(0)
                        result["motherName"] = parents?.getOrNull(1)
                    }
                    12 -> result["streetAddress"] = value
                    13 -> result["cityAddress"] = value
                    14 -> result["cityAddress2"] = value
                    15 -> result["provinceAddress"] = value
                }
            }

            result["birthPlace"] = listOfNotNull(
                result["birthPopulation"],
                result["birthProvince"]
            ).joinToString(", ")
            result["actualAddress"] = listOfNotNull(
                result["streetAddress"],
                result["cityAddress"],
                result["cityAddress2"],
                result["provinceAddress"]
            ).joinToString(", ")

            contentStream.close()
            asn1InputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear DG13: ${e.message}", e)
        }
        Log.d(TAG, "Resultado DG13 parseado: $result")
        return result
    }
    /**
     * Extrae una cadena de texto de un objeto ASN.1.
     *
     * @param element Objeto ASN.1 a procesar.
     * @return Cadena de texto extraída o null si no se pudo obtener.
     */
    private fun getStringFromASN1(element: ASN1Primitive?): String? {
        return when (element) {
            is DEROctetString -> String(element.octets, Charset.forName("UTF-8")).trim()
            is ASN1Sequence -> element.toString().trim()
            is ASN1TaggedObject -> getStringFromASN1(element.`object`)
            else -> element?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }.also { Log.d(TAG, "Extraído de ASN1 ($element): $it") }
    }
    /**
     * Formatea una fecha cruda en un formato legible.
     * Soporta múltiples formatos de entrada (YYMMDD, YYYYMMDD, DD MM YYYY).
     *
     * @param dateRaw Fecha en formato crudo.
     * @return Fecha formateada como "día de mes de año" o null si no se pudo formatear.
     */
    private fun formatDate(dateRaw: String?): String? {
        if (dateRaw == null) {
            Log.w(TAG, "Fecha cruda es nula")
            return null
        }

        // Normalizar espacios: eliminar espacios extra y asegurar un solo espacio entre componentes
        val trimmedDate = dateRaw.trim().replace("\\s+".toRegex(), " ")
        Log.d(TAG, "Fecha cruda normalizada: '$trimmedDate' (longitud=${trimmedDate.length})")

        // Si la fecha tiene espacios, asumir formato "DD MM YYYY"
        return if (trimmedDate.contains(" ")) {
            try {
                val parts = trimmedDate.split(" ")
                if (parts.size != 3) {
                    Log.w(TAG, "Formato de fecha inválido, esperaba 3 partes: $trimmedDate")
                    return null
                }
                val day = parts[0]
                val month = parts[1].toInt()
                val year = parts[2]

                val monthName = when (month) {
                    1 -> "enero"
                    2 -> "febrero"
                    3 -> "marzo"
                    4 -> "abril"
                    5 -> "mayo"
                    6 -> "junio"
                    7 -> "julio"
                    8 -> "agosto"
                    9 -> "septiembre"
                    10 -> "octubre"
                    11 -> "noviembre"
                    12 -> "diciembre"
                    else -> throw IllegalArgumentException("Mes inválido: $month")
                }

                val formattedDate = "$day de $monthName de $year"
                Log.d(TAG, "Fecha formateada: $formattedDate")
                formattedDate
            } catch (e: Exception) {
                Log.e(TAG, "Error al formatear fecha con espacios: $trimmedDate, ${e.message}", e)
                null
            }
        } else {
            try {
                val year: String
                val month: Int
                val day: String

                when (trimmedDate.length) {
                    6 -> { // Formato YYMMDD
                        val yearShort = trimmedDate.substring(0, 2).toInt()
                        year = if (yearShort > 50) "19$yearShort" else "20$yearShort"
                        month = trimmedDate.substring(2, 4).toInt()
                        day = trimmedDate.substring(4, 6)
                    }
                    8 -> { // Formato YYYYMMDD
                        year = trimmedDate.substring(0, 4)
                        month = trimmedDate.substring(4, 6).toInt()
                        day = trimmedDate.substring(6, 8)
                    }
                    else -> {
                        Log.w(TAG, "Longitud de fecha sin espacios inválida: ${trimmedDate.length}, fecha: $trimmedDate")
                        return null
                    }
                }

                val monthName = when (month) {
                    1 -> "enero"
                    2 -> "febrero"
                    3 -> "marzo"
                    4 -> "abril"
                    5 -> "mayo"
                    6 -> "junio"
                    7 -> "julio"
                    8 -> "agosto"
                    9 -> "septiembre"
                    10 -> "octubre"
                    11 -> "noviembre"
                    12 -> "diciembre"
                    else -> throw IllegalArgumentException("Mes inválido: $month")
                }

                val formattedDate = "$day de $monthName de $year"
                Log.d(TAG, "Fecha formateada: $formattedDate")
                formattedDate
            } catch (e: Exception) {
                Log.e(TAG, "Error al formatear fecha sin espacios: $trimmedDate, ${e.message}", e)
                null
            }
        }
    }
}