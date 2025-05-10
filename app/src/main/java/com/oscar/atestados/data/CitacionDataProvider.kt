package com.oscar.atestados.data

import com.oscar.atestados.viewModel.CitacionViewModel

/**
 * Proveedor de datos para el documento de citación.
 * Implementa [DocumentDataProvider] para proporcionar un mapa de datos que se usarán
 * para reemplazar los placeholders en una plantilla HTML de citación.
 *
 * @param viewModel ViewModel que contiene los datos de la citación.
 */
class CitacionDataProvider(private val viewModel: CitacionViewModel) : DocumentDataProvider {

    /**
     * Obtiene los datos de la citación en forma de un mapa donde las claves son los IDs
     * de los elementos `<span>` en la plantilla HTML y los valores son los datos a insertar.
     *
     * @return Mapa con los datos de la citación, usando valores vacíos como respaldo si no hay datos.
     */
    override fun getData(): Map<String, String> {
        return mapOf(
            "provincia" to (viewModel.provincia.value ?: ""),
            "localidad" to (viewModel.localidad.value ?: ""),
            "juzgado" to (viewModel.juzgado.value ?: ""),
            "fechaInicio" to (viewModel.fechaInicio.value ?: ""),
            "hora" to (viewModel.hora.value ?: ""),
            "numeroDiligencias" to (viewModel.numeroDiligencias.value ?: "")
        )
    }

    /**
     * Valida si los datos requeridos están completos.
     * Este método puede ser usado antes de generar el documento para asegurar que los datos mínimos
     * necesarios están presentes.
     *
     * @return Un Pair donde el primer elemento es `true` si todos los datos requeridos están presentes
     *         y no vacíos, y el segundo elemento es una lista de los campos faltantes (vacía si no hay errores).
     */
    override fun validateData(): Pair<Boolean, List<String>> {
        val data = getData()
        val requiredFields = mapOf(
            "provincia" to "Provincia",
            "localidad" to "Localidad",
            "juzgado" to "Juzgado",
            "fechaInicio" to "Fecha de inicio",
            "hora" to "Hora",
            "numeroDiligencias" to "Número de diligencias"
        )

        // Recopilar campos vacíos
        val missingFields = requiredFields.keys.filter { field ->
            data[field]?.isBlank() != false
        }.map { requiredFields[it] ?: it }

        // Retornar resultado de validación y lista de campos faltantes
        return Pair(missingFields.isEmpty(), missingFields)
    }
}