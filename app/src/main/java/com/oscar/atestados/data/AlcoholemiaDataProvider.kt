package com.oscar.atestados.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.itextpdf.io.source.ByteArrayOutputStream
import com.oscar.atestados.R
import com.oscar.atestados.viewModel.*
import java.io.File

class AlcoholemiaDataProvider(
    private val alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    private val alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel,
    private val personaViewModel: PersonaViewModel,
    private val vehiculoViewModel: VehiculoViewModel,
    private val tomaDerechosViewModel: TomaDerechosViewModel,
    private val tomaManifestacionViewModel: TomaManifestacionAlcoholViewModel,
    private val lecturaDerechosViewModel: LecturaDerechosViewModel,
    private val guardiasViewModel: GuardiasViewModel,
    private val db: AccesoBaseDatos,
    private val context: Context // Añadido para acceder a recursos
) : DocumentDataProvider {

    companion object {
        private const val TAG = "AlcoholemiaDataProvider"
    }

    private fun encodeBitmapToBase64(filePath: String): String {
        if (filePath == "NO DESEA FIRMAR") {
            Log.d(TAG, "No desea firmar, cargando imagen no_desea_firmar.png")
            try {
                // Cargar la imagen desde res/drawable
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.no_desea_firmar)
                if (bitmap == null) {
                    Log.e(TAG, "No se pudo cargar no_desea_firmar.png desde recursos")
                    return ""
                }
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                bitmap.recycle()
                val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                val dataUri = "data:image/png;base64,$base64String"
                Log.d(TAG, "Imagen no_desea_firmar.png convertida a Base64")
                return dataUri
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar no_desea_firmar.png: ${e.message}", e)
                return ""
            }
        }

        if (filePath.isEmpty()) {
            Log.w(TAG, "Ruta de archivo vacía")
            return ""
        }

        val cleanPath = filePath.removePrefix("file://")
        val file = File(cleanPath)
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "Archivo de firma no encontrado o no legible: $cleanPath")
            return ""
        }

        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                Log.e(TAG, "No se pudo decodificar el bitmap desde: $cleanPath")
                return ""
            }
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            bitmap.recycle()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            val dataUri = "data:image/png;base64,$base64String"
            Log.d(TAG, "Firma convertida a Base64: $dataUri")
            return dataUri
        } catch (e: Exception) {
            Log.e(TAG, "Error al codificar firma a Base64: ${e.message}", e)
            return ""
        }
    }

    override fun getData(): Map<String, String> {
        val nombreCompleto = "${personaViewModel.nombre.value ?: ""} ${personaViewModel.apellidos.value ?: ""}".trim()
        Log.d(TAG, "Nombre completo: $nombreCompleto")

        val lugarDiligencias = alcoholemiaDosViewModel.lugarDiligencias.value ?: ""
        Log.d(TAG, "Lugar diligencias: $lugarDiligencias")

        val terminoMunicipal = if (lugarDiligencias.isNotBlank()) {
            val partes = lugarDiligencias.split(", ")
            partes.getOrNull(2) ?: ""
        } else ""
        Log.d(TAG, "Término municipal: $terminoMunicipal")

        val partidoJudicial = alcoholemiaDosViewModel.partidoJudicial.value ?: ""
        Log.d(TAG, "Partido judicial: $partidoJudicial")

        val opcionMotivo = alcoholemiaUnoViewModel.opcionMotivo.value?.lowercase() ?: ""
        val letraInvestigacion = when (opcionMotivo) {
            "accidente" -> "a"
            "sintomas" -> "b"
            "infraccion" -> "c"
            "control", "control preventivo" -> "d"
            else -> ""
        }

        val tipInstructor = guardiasViewModel.primerTip.value ?: ""
        val empleoInstructor = guardiasViewModel.empleoPrimerInterviniente.value ?: "Guardia Civil"
        val tipSecretario = guardiasViewModel.segundoTip.value ?: ""
        val empleoSecretario = guardiasViewModel.empleoSegundoInterviniente.value ?: "Guardia Civil"

        val matriculaVehiculo = vehiculoViewModel.matricula.value ?: ""

        // Usar rutas de archivo para las firmas
        val firmaInvestigado = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaInvestigado.value ?: "")
        val firmaInstructor = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaInstructor.value ?: "")
        val firmaSecretario = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaSecretario.value ?: "")

        return mapOf(
            "lugar" to lugarDiligencias,
            "termino_municipal" to terminoMunicipal,
            "partido_judicial" to partidoJudicial,
            "hora_diligencia" to (alcoholemiaDosViewModel.horaInicio.value ?: ""),
            "fecha_diligencia" to (alcoholemiaDosViewModel.fechaInicio.value ?: ""),
            "empleo_instructor" to empleoInstructor,
            "tip_instructor" to tipInstructor,
            "empleo_secretario" to empleoSecretario,
            "tip_secretario" to tipSecretario,
            "nombre_completo_persona" to nombreCompleto,
            "documento" to (personaViewModel.numeroDocumento.value ?: ""),
            "fecha_nacimiento" to (personaViewModel.fechaNacimiento.value ?: ""),
            "lugar_nacimiento" to (personaViewModel.lugarNacimiento.value ?: ""),
            "nombre_padre" to (personaViewModel.nombrePadre.value ?: ""),
            "nombre_madre" to (personaViewModel.nombreMadre.value ?: ""),
            "domicilio" to (personaViewModel.domicilio.value ?: ""),
            "telefono" to (personaViewModel.telefono.value ?: ""),
            "correo_electronico" to (personaViewModel.email.value ?: ""),
            "tipo_vehiculo" to (vehiculoViewModel.tipoVehiculo.value ?: ""),
            "marca_vehiculo" to (vehiculoViewModel.marca.value ?: ""),
            "modelo_vehiculo" to (vehiculoViewModel.modelo.value ?: ""),
            "matricula_vehiculo" to matriculaVehiculo,
            "marca_etilometro" to (alcoholemiaUnoViewModel.marca.value ?: ""),
            "modelo_etilometro" to (alcoholemiaUnoViewModel.modelo.value ?: ""),
            "num_serie_eti" to (alcoholemiaUnoViewModel.serie.value ?: ""),
            "letra_investigacion" to letraInvestigacion,
            "desea_realizar_pruebas" to (alcoholemiaUnoViewModel.opcionDeseaPruebas.value?.uppercase() ?: "NO"),
            "firma_den" to firmaInvestigado,
            "firma_tip_instructor" to firmaInstructor,
            "firma_tip_secretario" to firmaSecretario
        ).also { data ->
            Log.d(TAG, "Mapa de datos: ${data.entries.joinToString("\n") { "${it.key}=${it.value}" }}")
        }
    }

    override fun validateData(): Pair<Boolean, List<String>> {
        val data = getData()
        val requiredFields = mapOf(
            "nombre_completo_persona" to "Nombre y apellidos",
            "documento" to "Documento de identidad",
            "lugar" to "Lugar de diligencias",
            "fecha_diligencia" to "Fecha de diligencias",
            "hora_diligencia" to "Hora de diligencias",
            "tip_instructor" to "TIP del instructor",
            "matricula_vehiculo" to "Matrícula del vehículo",
            "marca_etilometro" to "Marca del etilómetro",
            "modelo_etilometro" to "Modelo del etilómetro",
            "num_serie_eti" to "Número de serie del etilómetro",
            "letra_investigacion" to "Motivo de la investigación",
            "desea_realizar_pruebas" to "Deseo de realizar pruebas"
        )

        val missingFields = requiredFields.keys.filter { field ->
            val isMissing = data[field]?.isBlank() ?: true
            if (isMissing) {
                Log.e(TAG, "Campo requerido vacío: $field (Valor: ${data[field]})")
            }
            isMissing
        }.map { requiredFields[it] ?: it }

        Log.d(TAG, "Campos faltantes: $missingFields")
        return Pair(missingFields.isEmpty(), missingFields)
    }
}