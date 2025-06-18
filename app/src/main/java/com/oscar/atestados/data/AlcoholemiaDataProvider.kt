package com.oscar.atestados.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.itextpdf.io.source.ByteArrayOutputStream
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
    private val context: Context
) : DocumentDataProvider {

    companion object {
        private const val TAG = "AlcoholemiaDataProvider"
    }

    private fun encodeBitmapToBase64(filePath: String?): String {
        if (filePath.isNullOrEmpty()) {
            Log.w(TAG, "Ruta de archivo nula o vacía")
            return ""
        }

        val cleanPath = filePath.removePrefix("file://")
        val file = File(cleanPath)

        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "Archivo no encontrado o no legible: $cleanPath")
            return ""
        }

        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                bitmap.recycle()
                val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                "data:image/png;base64,$base64String"
            } else {
                Log.e(TAG, "No se pudo decodificar el bitmap desde: $cleanPath")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al codificar imagen a Base64: ${e.message}", e)
            ""
        }
    }

    override fun getData(): Map<String, String> {
        // Nombre completo de la persona
        val nombreCompleto = "${personaViewModel.nombre.value ?: ""} ${personaViewModel.apellidos.value ?: ""}".trim()
        Log.d(TAG, "Nombre completo: $nombreCompleto")

        // Lugar de diligencias
        val lugarDiligencias = alcoholemiaDosViewModel.lugarDiligencias.value ?: ""
        Log.d(TAG, "Lugar diligencias: $lugarDiligencias")

        // Término municipal (extraído de lugarDiligencias)
        val terminoMunicipal = if (lugarDiligencias.isNotBlank()) {
            val partes = lugarDiligencias.split(", ")
            partes.getOrNull(2) ?: ""
        } else ""
        Log.d(TAG, "Término municipal: $terminoMunicipal")

        // Partido judicial
        val partidoJudicial = alcoholemiaDosViewModel.partidoJudicial.value ?: ""
        Log.d(TAG, "Partido judicial: $partidoJudicial")

        // Letra de investigación basada en el motivo
        val opcionMotivo = alcoholemiaUnoViewModel.opcionMotivo.value?.lowercase() ?: ""
        val letraInvestigacion = when (opcionMotivo) {
            "accidente" -> "a"
            "sintomas" -> "b"
            "infraccion" -> "c"
            "control", "control preventivo" -> "d"
            else -> ""
        }
        Log.d(TAG, "Letra investigación: $letraInvestigacion")

        // Datos de los guardias
        val tipInstructor = guardiasViewModel.primerTip.value ?: ""
        val empleoInstructor = guardiasViewModel.empleoPrimerInterviniente.value ?: "Guardia Civil"
        val tipSecretario = guardiasViewModel.segundoTip.value ?: ""
        val empleoSecretario = guardiasViewModel.empleoSegundoInterviniente.value ?: "Guardia Civil"

        // Matrícula del vehículo
        val matriculaVehiculo = vehiculoViewModel.matricula.value ?: ""

        // Firmas
        val firmaInvestigado = if (!alcoholemiaDosViewModel.deseaFirmar.value!! && alcoholemiaDosViewModel.firmaInvestigado.value.isNullOrEmpty()) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, com.oscar.atestados.R.drawable.no_desea_firmar)
                if (bitmap != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    bitmap.recycle()
                    val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                    "data:image/png;base64,$base64String"
                } else {
                    Log.e(TAG, "No se pudo cargar no_desea_firmar.png desde recursos")
                    ""
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al codificar no_desea_firmar.png: ${e.message}", e)
                ""
            }
        } else {
            encodeBitmapToBase64(alcoholemiaDosViewModel.firmaInvestigado.value)
        }

        val firmaSegundoConductor = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaSegundoConductor.value)
        val firmaInstructor = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaInstructor.value)
        val firmaSecretario = encodeBitmapToBase64(alcoholemiaDosViewModel.firmaSecretario.value)

        return mapOf(
            // Datos de AlcoholemiaUnoViewModel
            "opcionMotivo" to (alcoholemiaUnoViewModel.opcionMotivo.value ?: ""),
            "desea_realizar_pruebas" to (alcoholemiaUnoViewModel.opcionDeseaPruebas.value ?: ""),
            "marca_etilometro" to (alcoholemiaUnoViewModel.marca.value ?: ""),
            "modelo_etilometro" to (alcoholemiaUnoViewModel.modelo.value ?: ""),
            "num_serie_eti" to (alcoholemiaUnoViewModel.serie.value ?: ""),
            "fecha_servicio_etilometro" to (alcoholemiaUnoViewModel.caducidad.value ?: ""),
            "hora_prueba_uno" to (alcoholemiaUnoViewModel.primeraHora.value ?: ""),
            "tasa_uno" to (alcoholemiaUnoViewModel.primeraTasa.value ?: ""),
            "hora_prueba_dos" to (alcoholemiaUnoViewModel.segundaHora.value ?: ""),
            "tasa_dos" to (alcoholemiaUnoViewModel.segundaTasa.value ?: ""),

            // Datos de AlcoholemiaDosViewModel
            "lugar" to lugarDiligencias,
            "termino_municipal" to terminoMunicipal,
            "partido_judicial" to partidoJudicial,
            "hora_diligencia" to (alcoholemiaDosViewModel.horaInicio.value ?: ""),
            "fecha_diligencia" to (alcoholemiaDosViewModel.fechaInicio.value ?: ""),

            // Datos de PersonaViewModel
            "nombre_completo_persona" to nombreCompleto,
            "documento" to (personaViewModel.numeroDocumento.value ?: ""),
            "fecha_nacimiento" to (personaViewModel.fechaNacimiento.value ?: ""),
            "lugar_nacimiento" to (personaViewModel.lugarNacimiento.value ?: ""),
            "nombre_padre" to (personaViewModel.nombrePadre.value ?: ""),
            "nombre_madre" to (personaViewModel.nombreMadre.value ?: ""),
            "domicilio" to (personaViewModel.domicilio.value ?: ""),
            "telefono" to (personaViewModel.telefono.value ?: ""),
            "correo_electronico" to (personaViewModel.email.value ?: ""),

            // Datos de VehiculoViewModel
            "tipo_vehiculo" to (vehiculoViewModel.tipoVehiculo.value ?: ""),
            "marca_vehiculo" to (vehiculoViewModel.marca.value ?: ""),
            "modelo_vehiculo" to (vehiculoViewModel.modelo.value ?: ""),
            "matricula_vehiculo" to matriculaVehiculo,

            // Datos de GuardiasViewModel
            "empleo_instructor" to empleoInstructor,
            "tip_instructor" to tipInstructor,
            "empleo_secretario" to empleoSecretario,
            "tip_secretario" to tipSecretario,

            // Datos de TomaDerechosViewModel
            "prestar_declaracion" to (tomaDerechosViewModel.prestarDeclaracion.value?.toString() ?: "true"),
            "renuncia_asistencia_letrada" to (tomaDerechosViewModel.renunciaAsistenciaLetrada.value?.toString() ?: "true"),
            "asistencia_letrado_particular" to (tomaDerechosViewModel.asistenciaLetradoParticular.value?.toString() ?: "false"),
            "datos_letrado_particular" to (tomaDerechosViewModel.nombreLetrado.value ?: ""),
            "asistencia_letrado_oficio" to (tomaDerechosViewModel.asistenciaLetradoOficio.value?.toString() ?: "true"),
            "acceso_elementos" to (tomaDerechosViewModel.accesoElementos.value?.toString() ?: "false"),
            "interprete" to (tomaDerechosViewModel.interprete.value?.toString() ?: "false"),
            "elementos_esenciales" to (tomaDerechosViewModel.textoElementosEsenciales.value ?: ""),

            // Datos de TomaManifestacionAlcoholViewModel
            "manifestacion_desea_declarar" to (tomaManifestacionViewModel.deseaDeclarar.value?.toString() ?: "false"),
            "manifestacion_renuncia_letrado" to (tomaManifestacionViewModel.renunciaExpresaLletrado.value?.toString() ?: "false"),
            "pregunta_1" to (tomaManifestacionViewModel.condicionesParaManifestacion.value ?: ""),
            "pregunta_2" to (tomaManifestacionViewModel.procedencia.value ?: ""),
            "pregunta_3" to (tomaManifestacionViewModel.consumoAlcohol.value ?: ""),
            "pregunta_4" to (tomaManifestacionViewModel.ultimaVezAlcohol.value ?: ""),
            "pregunta_5" to (tomaManifestacionViewModel.enfermedadMedicamentos.value ?: ""),
            "pregunta_6" to (tomaManifestacionViewModel.tiempoUltimoTrago.value ?: ""),
            "pregunta_7" to (tomaManifestacionViewModel.conscientePeligros.value ?: ""),
            "pregunta_8" to (tomaManifestacionViewModel.declaracionAdicional.value ?: ""),

            // Datos de LecturaDerechosViewModel
            "lugar_investigacion" to (lecturaDerechosViewModel.lugarInvestigacion.value ?: ""),
            "momento_lectura" to (lecturaDerechosViewModel.momentoLectura.value ?: "Tomada en el momento"),
            "lugar_delito" to (lecturaDerechosViewModel.lugarDelito.value ?: ""),
            "resumen_hechos" to (lecturaDerechosViewModel.resumenHechos.value ?: ""),
            "calificacion_penal" to (lecturaDerechosViewModel.calificacionHechos.value ?: ""),
            "relacion_indicios" to (lecturaDerechosViewModel.relacionIndicios.value ?: ""),

            // Firmas
            "firma_instructor" to firmaInstructor,
            "firma_secretario" to firmaSecretario,
            "firma_investigado" to firmaInvestigado,
            "firma_segundo_conductor" to firmaSegundoConductor,

            // Datos adicionales
            "letra_investigacion" to letraInvestigacion,
            "contraste" to "NO",
            "alegaciones_alcoholemia" to "Ninguna"
        ).also { data ->
            Log.d(TAG, "Mapa de datos: ${data.entries.joinToString("\n") { "${it.key}=${it.value}" }}")
        }
    }

    override fun validateData(): Pair<Boolean, List<String>> {
        val data = getData()
        val requiredFields = mutableMapOf(
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
            "desea_realizar_pruebas" to "Deseo de realizar pruebas",
            "firma_investigado" to "Firma del investigado",
            "momento_lectura" to "Momento de la lectura de derechos",
            "lugar_investigacion" to "Lugar de la investigación",
            "lugar_delito" to "Lugar del delito",
            "resumen_hechos" to "Resumen de los hechos",
            "calificacion_penal" to "Calificación penal",
            "relacion_indicios" to "Relación de indicios",
            "prestar_declaracion" to "Prestar declaración",
            "renuncia_asistencia_letrada" to "Renuncia a asistencia letrada",
            "asistencia_letrado_particular" to "Asistencia de letrado particular",
            "asistencia_letrado_oficio" to "Asistencia de letrado de oficio",
            "acceso_elementos" to "Acceso a elementos esenciales",
            "interprete" to "Solicitud de intérprete",
            "manifestacion_desea_declarar" to "Deseo de declarar en manifestación",
            "manifestacion_renuncia_letrado" to "Renuncia expresa a letrado en manifestación"
        )

        // Agregar firma del segundo conductor como requerida si haySegundoConductor es true
        if (alcoholemiaDosViewModel.haySegundoConductor.value == true) {
            requiredFields["firma_segundo_conductor"] = "Firma del segundo conductor"
            requiredFields["nombre_segundo_conductor"] = "Nombre del segundo conductor"
        }

        // Agregar nombre del letrado como requerido si asistenciaLetradoParticular es true
        if (tomaDerechosViewModel.asistenciaLetradoParticular.value == true) {
            requiredFields["datos_letrado_particular"] = "Nombre del letrado particular"
        }

        // Agregar campos de manifestación como requeridos si deseaDeclarar es true
        if (tomaManifestacionViewModel.deseaDeclarar.value == true) {
            requiredFields["pregunta_1"] = "Condiciones para la manifestación"
            requiredFields["pregunta_2"] = "Procedencia del investigado"
            requiredFields["pregunta_3"] = "Consumo de alcohol declarado"
            requiredFields["pregunta_4"] = "Última vez que consumió alcohol"
            requiredFields["pregunta_5"] = "Enfermedades o medicamentos"
            requiredFields["pregunta_6"] = "Tiempo desde el último trago"
            requiredFields["pregunta_7"] = "Consciente de los peligros"
            requiredFields["pregunta_8"] = "Declaración adicional"
        }

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