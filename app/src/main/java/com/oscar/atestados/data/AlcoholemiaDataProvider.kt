package com.oscar.atestados.data

import android.graphics.Bitmap
import android.util.Log
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel
import com.oscar.atestados.viewModel.AlcoholemiaUnoViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.LecturaDerechosViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.viewModel.TomaDerechosViewModel
import com.oscar.atestados.viewModel.TomaManifestacionAlcoholViewModel
import com.oscar.atestados.viewModel.VehiculoViewModel

/**
 * Proveedor de datos para el atestado de alcoholemia.
 * Recopila y valida datos de múltiples ViewModels y la base de datos para generar el documento.
 *
 * @param alcoholemiaDosViewModel ViewModel con datos de la pantalla Alcoholemia02.
 * @param alcoholemiaUnoViewModel ViewModel con datos de la pantalla Alcoholemia01.
 * @param personaViewModel ViewModel con datos de la persona investigada.
 * @param vehiculoViewModel ViewModel con datos del vehículo.
 * @param tomaDerechosViewModel ViewModel con datos de la toma de derechos.
 * @param tomaManifestacionViewModel ViewModel con datos de la toma de manifestación.
 * @param lecturaDerechosViewModel ViewModel con datos de la lectura de derechos.
 * @param guardiasViewModel ViewModel con datos de los guardias intervinientes.
 * @param db Instancia de AccesoBaseDatos para consultas a la base de datos.
 */
class AlcoholemiaDataProvider(
    private val alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    private val alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel,
    private val personaViewModel: PersonaViewModel,
    private val vehiculoViewModel: VehiculoViewModel,
    private val tomaDerechosViewModel: TomaDerechosViewModel,
    private val tomaManifestacionViewModel: TomaManifestacionAlcoholViewModel,
    private val lecturaDerechosViewModel: LecturaDerechosViewModel,
    private val guardiasViewModel: GuardiasViewModel,
    private val db: AccesoBaseDatos
) : DocumentDataProvider {

    /**
     * Obtiene un mapa de datos para completar el atestado de alcoholemia.
     * Incluye información de diligencias, persona investigada, vehículo, etilómetro, firmas y más.
     * Deriva el término municipal desde lugarDiligencias.
     *
     * @return Mapa con claves y valores para rellenar la plantilla del atestado.
     */
    override fun getData(): Map<String, String> {
        val nombreCompleto = "${personaViewModel.nombre.value ?: ""} ${personaViewModel.apellidos.value ?: ""}".trim()
        Log.d("AlcoholemiaDataProvider", "Nombre completo: $nombreCompleto")

        // Derivar termino_municipal desde lugarDiligencias
        val lugarDiligencias = alcoholemiaDosViewModel.lugarDiligencias.value ?: ""
        Log.d("AlcoholemiaDataProvider", "Lugar diligencias: $lugarDiligencias")

        val terminoMunicipal = if (lugarDiligencias.isNotBlank()) {
            val partes = lugarDiligencias.split(", ")
            partes.getOrNull(2) ?: ""
        } else ""
        Log.d("AlcoholemiaDataProvider", "Término municipal: $terminoMunicipal")

        // Usar partido_judicial del ViewModel
        val partidoJudicial = alcoholemiaDosViewModel.partidoJudicial.value ?: ""
        Log.d("AlcoholemiaDataProvider", "Partido judicial: $partidoJudicial")

        val carretera = alcoholemiaDosViewModel.carretera.value ?: "Carretera desconocida"
        Log.d("AlcoholemiaDataProvider", "Carretera: $carretera")

        val puntoKilometrico = alcoholemiaDosViewModel.puntoKilometrico.value ?: "PK no disponible"
        Log.d("AlcoholemiaDataProvider", "Punto kilométrico: $puntoKilometrico")

        // Usar lugar_nacimiento directamente
        val lugarNacimiento = personaViewModel.lugarNacimiento.value ?: ""
        Log.d("AlcoholemiaDataProvider", "Lugar nacimiento: $lugarNacimiento")

        // Mapear motivo_diligencias a letra_investigacion
        val opcionMotivo = alcoholemiaUnoViewModel.opcionMotivo.value?.lowercase() ?: ""
        Log.d("AlcoholemiaDataProvider", "Opción motivo: $opcionMotivo")
        val letraInvestigacion = when (opcionMotivo) {
            "accidente" -> "a"
            "sintomas" -> "b"
            "infraccion" -> "c"
            "control", "control preventivo" -> "d"
            else -> ""
        }
        Log.d("AlcoholemiaDataProvider", "Letra investigación: $letraInvestigacion")

        // Datos de guardias
        val tipInstructor = guardiasViewModel.primerTip.value ?: ""
        val empleoInstructor = guardiasViewModel.empleoPrimerInterviniente.value ?: "Guardia Civil"
        val tipSecretario = guardiasViewModel.segundoTip.value ?: ""
        val empleoSecretario = guardiasViewModel.empleoSegundoInterviniente.value ?: "Guardia Civil"
        Log.d("AlcoholemiaDataProvider", "TIP Instructor: $tipInstructor")
        Log.d("AlcoholemiaDataProvider", "Empleo Instructor: $empleoInstructor")
        Log.d("AlcoholemiaDataProvider", "TIP Secretario: $tipSecretario")
        Log.d("AlcoholemiaDataProvider", "Empleo Secretario: $empleoSecretario")

        // Datos del vehículo
        val matriculaVehiculo = vehiculoViewModel.matricula.value ?: ""
        Log.d("AlcoholemiaDataProvider", "Matrícula vehículo: $matriculaVehiculo")

        // Convertir firmas a Base64
        val firmaInvestigado = alcoholemiaDosViewModel.firmaInvestigado.value?.let { encodeBitmapToBase64(it) } ?: ""
        val firmaInstructor = alcoholemiaDosViewModel.firmaInstructor.value?.let { encodeBitmapToBase64(it) } ?: ""
        val firmaSecretario = alcoholemiaDosViewModel.firmaSecretario.value?.let { encodeBitmapToBase64(it) } ?: ""
        Log.d("AlcoholemiaDataProvider", "Firma investigado: ${if (firmaInvestigado.isNotEmpty()) "Presente" else "Vacía"}")
        Log.d("AlcoholemiaDataProvider", "Firma instructor: ${if (firmaInstructor.isNotEmpty()) "Presente" else "Vacía"}")
        Log.d("AlcoholemiaDataProvider", "Firma secretario: ${if (firmaSecretario.isNotEmpty()) "Presente" else "Vacía"}")

        return mapOf(
            // Datos de la diligencia
            "lugar" to lugarDiligencias,
            "termino_municipal" to terminoMunicipal,
            "partido_judicial" to partidoJudicial,
            "hora_diligencia" to (alcoholemiaDosViewModel.horaInicio.value ?: ""),
            "fecha_diligencia" to (alcoholemiaDosViewModel.fechaInicio.value ?: ""),

            // Datos del instructor y secretario
            "empleo_instructor" to empleoInstructor,
            "tip_instructor" to tipInstructor,
            "empleo_secretario" to empleoSecretario,
            "tip_secretario" to tipSecretario,

            // Datos de la persona investigada
            "nombre_completo_persona" to nombreCompleto,
            "documento" to (personaViewModel.numeroDocumento.value ?: ""),
            "fecha_nacimiento" to (personaViewModel.fechaNacimiento.value ?: ""),
            "lugar_nacimiento" to lugarNacimiento,
            "nombre_padre" to (personaViewModel.nombrePadre.value ?: ""),
            "nombre_madre" to (personaViewModel.nombreMadre.value ?: ""),
            "domicilio" to (personaViewModel.domicilio.value ?: ""),
            "telefono" to (personaViewModel.telefono.value ?: ""),
            "correo_electronico" to (personaViewModel.email.value ?: ""),

            // Datos del vehículo
            "tipo_vehiculo" to (vehiculoViewModel.tipoVehiculo.value ?: ""),
            "marca_vehiculo" to (vehiculoViewModel.marca.value ?: ""),
            "modelo_vehiculo" to (vehiculoViewModel.modelo.value ?: ""),
            "matricula_vehiculo" to matriculaVehiculo,

            // Datos del etilómetro
            "marca_etilometro" to (alcoholemiaUnoViewModel.marca.value ?: ""),
            "modelo_etilometro" to (alcoholemiaUnoViewModel.modelo.value ?: ""),
            "num_serie_eti" to (alcoholemiaUnoViewModel.serie.value ?: ""),

            // Motivo de la investigación
            "letra_investigacion" to letraInvestigacion,

            // Deseo de realizar pruebas
            "desea_realizar_pruebas" to (alcoholemiaUnoViewModel.opcionDeseaPruebas.value?.uppercase() ?: "NO"),

            // Firmas
            "firma_investigado" to firmaInvestigado,
            "firma_instructor" to firmaInstructor,
            "firma_secretario" to firmaSecretario
        ).also { data ->
            Log.d("AlcoholemiaDataProvider", "Mapa de datos: ${data.entries.joinToString("\n") { "${it.key}=${it.value}" }}")
        }
    }

    /**
     * Valida los datos requeridos para el atestado.
     * Verifica que los campos obligatorios no estén vacíos y devuelve una lista de los faltantes.
     *
     * @return Par con un booleano que indica si todos los datos son válidos y una lista de campos faltantes.
     */
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

        // Recopilar campos vacíos
        val missingFields = requiredFields.keys.filter { field ->
            val isMissing = data[field]?.isBlank() ?: true
            if (isMissing) {
                Log.e("AlcoholemiaDataProvider", "Campo requerido vacío: $field (Valor: ${data[field]})")
            }
            isMissing
        }.map { requiredFields[it] ?: it }

        Log.d("AlcoholemiaDataProvider", "Campos faltantes: $missingFields")
        return Pair(missingFields.isEmpty(), missingFields)
    }

    /**
     * Codifica un bitmap a una cadena Base64 para su inclusión en el documento.
     *
     * @param bitmap Imagen a codificar.
     * @return Cadena Base64 que representa la imagen.
     */
    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }
}