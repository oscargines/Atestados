package com.oscar.atestados.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.itextpdf.io.source.ByteArrayOutputStream
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel
import com.oscar.atestados.viewModel.CitacionViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import java.io.File

private const val TAG = "CitacionDataProvider"

class CitacionDataProvider(
    private val citacionViewModel: CitacionViewModel,
    private val personaViewModel: PersonaViewModel,
    private val guardiasViewModel: GuardiasViewModel,
    private val alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    private val info: Map<String, String> = emptyMap()
) : DocumentDataProvider {

    private fun encodeBitmapToBase64(filePath: String): String {
        if (filePath.isEmpty()) {
            Log.w(TAG, "Ruta de archivo vacía")
            return ""
        }
        val file = File(filePath.removePrefix("file://"))
        if (!file.exists()) {
            Log.w(TAG, "Archivo no existe: ${file.absolutePath}")
            return ""
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            Log.w(TAG, "No se pudo decodificar el bitmap desde: ${file.absolutePath}")
            return ""
        }
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            bitmap.recycle()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.d(TAG, "Firma codificada a Base64, longitud: ${base64String.length}")
            return "data:image/png;base64,$base64String"
        }
    }

    override fun getData(): Map<String, String> {
        val nombreCompleto = "${personaViewModel.nombre.value ?: ""} ${personaViewModel.apellidos.value ?: ""}".trim()
        Log.d(TAG, "Nombre completo: $nombreCompleto")

        val numeroDocumento = personaViewModel.numeroDocumento.value ?: ""
        Log.d(TAG, "Número documento: $numeroDocumento")

        val lugar = alcoholemiaDosViewModel.lugarDiligencias.value ?: ""
        Log.d(TAG, "Lugar: $lugar")

        val terminoMunicipal = if (lugar.isNotBlank()) {
            val partes = lugar.split(", ")
            partes.getOrNull(2) ?: ""
        } else ""
        Log.d(TAG, "Término municipal: $terminoMunicipal")

        val partidoJudicial = alcoholemiaDosViewModel.partidoJudicial.value ?: ""
        Log.d(TAG, "Partido judicial: $partidoJudicial")

        val fechaDiligencia = citacionViewModel.fechaInicio.value ?: ""
        Log.d(TAG, "Fecha diligencia: $fechaDiligencia")
        val horaDiligencia = citacionViewModel.hora.value ?: ""
        Log.d(TAG, "Hora diligencia: $horaDiligencia")

        val tipInstructor = guardiasViewModel.primerTip.value ?: ""
        val empleoInstructor = guardiasViewModel.empleoPrimerInterviniente.value ?: "Guardia Civil"
        val tipSecretario = guardiasViewModel.segundoTip.value ?: ""
        val empleoSecretario = guardiasViewModel.empleoSegundoInterviniente.value ?: "Guardia Civil"
        Log.d(TAG, "TIP Instructor: $tipInstructor")
        Log.d(TAG, "Empleo Instructor: $empleoInstructor")
        Log.d(TAG, "TIP Secretario: $tipSecretario")
        Log.d(TAG, "Empleo Secretario: $empleoSecretario")

        val juzgado = citacionViewModel.juzgado.value ?: ""
        val localidad = citacionViewModel.localidad.value ?: ""
        val provincia = citacionViewModel.provincia.value ?: ""
        val datosJuzgado = buildString {
            append(juzgado)
            if (localidad.isNotBlank()) append(", $localidad")
            if (provincia.isNotBlank()) append(", $provincia")
            if (info["direccion"]?.isNotBlank() == true) append(", ${info["direccion"]}")
            if (info["telefono"]?.isNotBlank() == true) append(", Tel: ${info["telefono"]}")
            if (info["codigo_postal"]?.isNotBlank() == true) append(", CP: ${info["codigo_postal"]}")
        }.trim()
        Log.d(TAG, "Datos juzgado: $datosJuzgado")

        val diaJuicio = citacionViewModel.fechaInicio.value ?: ""
        Log.d(TAG, "Día juicio: $diaJuicio")

        val horaJuicio = citacionViewModel.hora.value ?: ""
        Log.d(TAG, "Hora juicio: $horaJuicio")

        val unidad = guardiasViewModel.primerUnidad.value ?: ""
        Log.d(TAG, "Unidad: $unidad")

        val abogadoDesignado = citacionViewModel.abogadoDesignado.value ?: false
        val abogadoOficio = citacionViewModel.abogadoOficio.value ?: false
        Log.d(TAG, "Abogado designado: $abogadoDesignado")
        Log.d(TAG, "Abogado de oficio: $abogadoOficio")

        val abogado: String
        val numColegiado: String
        val colegioAbogados: String
        val telefonema: String
        val op1Checked: String
        val op2Checked: String

        // Obtener firmas
        val firmaInstructor = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaInstructor.value ?: "")
        val firmaSecretario = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaSecretario.value ?: "")
        val firmaInvestigado = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaInvestigado.value ?: "")

        Log.d(TAG, "Firma instructor: ${if (firmaInstructor.isNotEmpty()) "Base64 (longitud=${firmaInstructor.length})" else "Vacía"}")
        Log.d(TAG, "Firma secretario: ${if (firmaSecretario.isNotEmpty()) "Base64 (longitud=${firmaSecretario.length})" else "Vacía"}")
        Log.d(TAG, "Firma investigado: ${if (firmaInvestigado.isNotEmpty()) "Base64 (longitud=${firmaInvestigado.length})" else "Vacía"}")

        when {
            abogadoDesignado -> {
                op1Checked = "true"
                op2Checked = "false"
                abogado = citacionViewModel.abogadoNombre.value ?: ""
                numColegiado = citacionViewModel.abogadoColegiado.value ?: ""
                colegioAbogados = citacionViewModel.abogadoColegio.value ?: ""
                telefonema = ""
                Log.d(TAG, "op_1 seleccionado - Nombre abogado: $abogado, Número colegiado: $numColegiado, Colegio abogados: $colegioAbogados, Número comunicación: $telefonema")
            }
            abogadoOficio -> {
                op1Checked = "false"
                op2Checked = "true"
                abogado = ""
                numColegiado = ""
                colegioAbogados = ""
                telefonema = citacionViewModel.comunicacionNumero.value ?: ""
                Log.d(TAG, "op_2 seleccionado - Nombre abogado: $abogado, Número colegiado: $numColegiado, Colegio abogados: $colegioAbogados, Número comunicación: $telefonema")
            }
            else -> {
                op1Checked = "false"
                op2Checked = "false"
                abogado = ""
                numColegiado = ""
                colegioAbogados = ""
                telefonema = ""
                Log.d(TAG, "Ningún checkbox seleccionado - Todos los datos de abogado en blanco")
            }
        }

        val data = mapOf(
            "lugar" to lugar,
            "termino_municipal" to terminoMunicipal,
            "partido_judicial" to partidoJudicial,
            "hora_diligencia" to horaDiligencia,
            "fecha_diligencia" to fechaDiligencia,
            "tip_instructor" to tipInstructor,
            "tip_secretario" to tipSecretario,
            "nombre_completo" to nombreCompleto,
            "num_documento" to numeroDocumento,
            "unidad" to unidad,
            "hora_juicio" to horaJuicio,
            "dia_juicio" to diaJuicio,
            "datos_juzgado" to datosJuzgado,
            "abogado" to abogado,
            "num_colegiado" to numColegiado,
            "colegio_abogados" to colegioAbogados,
            "telefonema" to telefonema,
            "op_1_checked" to op1Checked,
            "op_2_checked" to op2Checked,
            "firma_instructor" to firmaInstructor,
            "firma_secretario" to firmaSecretario,
            "firma_investigado" to firmaInvestigado // Corregido: clave correcta
        )
        Log.d(TAG, "Mapa de datos generados: ${data.entries.joinToString("\n") { "${it.key}=${it.value}" }}")
        return data
    }

    override fun validateData(): Pair<Boolean, List<String>> {
        Log.d(TAG, "Iniciando validación de datos")
        val data = getData()
        val requiredFields = mutableMapOf(
            "nombre_completo" to "Nombre y apellidos",
            "num_documento" to "Documento de identidad",
            "fecha_diligencia" to "Fecha de diligencia",
            "hora_diligencia" to "Hora de diligencia",
            "datos_juzgado" to "Nombre del juzgado",
            "tip_instructor" to "TIP del instructor",
            "tip_secretario" to "TIP del secretario",
            "lugar" to "Lugar de la diligencia",
            "unidad" to "Unidad"
        )

        if (citacionViewModel.abogadoDesignado.value == true) {
            requiredFields["abogado"] = "Nombre del abogado"
            requiredFields["num_colegiado"] = "Número de colegiado"
            requiredFields["colegio_abogados"] = "Colegio de abogados"
        }

        if (citacionViewModel.abogadoOficio.value == true) {
            requiredFields["telefonema"] = "Número de comunicación"
        }

        val missingFields = requiredFields.keys.filter { field ->
            val value = data[field]
            val isMissing = value?.isBlank() ?: true
            if (isMissing) {
                Log.e(TAG, "Campo requerido vacío: $field (Valor: $value)")
            } else {
                Log.d(TAG, "Campo validado: $field (Valor: $value)")
            }
            isMissing
        }.map { requiredFields[it] ?: it }

        Log.d(TAG, "Validación completada. Campos faltantes: $missingFields")
        Log.d(TAG, "Validando datos: ${data.entries.joinToString("\n") { "${it.key}=${it.value}" }}")
        return Pair(missingFields.isEmpty(), missingFields)
    }
}