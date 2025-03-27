package com.oscar.atestados.utils

import android.content.Context
import android.graphics.Bitmap
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import java.io.File

class GenericMarkdownGenerator(
    private val personaViewModel: PersonaViewModel,
    private val alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    private val context: Context
) {

    fun generarMarkdownDinamico(): String {
        // Obtener datos de PersonaViewModel
        val datosPersona = mapOf(
            "genero" to (personaViewModel.genero.value ?: ""),
            "nacionalidad" to (personaViewModel.nacionalidad.value ?: ""),
            "tipoDocumento" to (personaViewModel.tipoDocumento.value ?: ""),
            "numeroDocumento" to (personaViewModel.numeroDocumento.value ?: ""),
            "nombre" to (personaViewModel.nombre.value ?: ""),
            "apellidos" to (personaViewModel.apellidos.value ?: ""),
            "nombrePadre" to (personaViewModel.nombrePadre.value ?: ""),
            "nombreMadre" to (personaViewModel.nombreMadre.value ?: ""),
            "fechaNacimiento" to (personaViewModel.fechaNacimiento.value ?: ""),
            "lugarNacimiento" to (personaViewModel.lugarNacimiento.value ?: ""),
            "domicilio" to (personaViewModel.domicilio.value ?: ""),
            "codigoPostal" to (personaViewModel.codigoPostal.value ?: ""),
            "telefono" to (personaViewModel.telefono.value ?: ""),
            "email" to (personaViewModel.email.value ?: ""),
            "codigoCan" to (personaViewModel.codigoCan.value ?: "")
        )

        // Guardar la firma como archivo temporal
        val firmaInvestigado = alcoholemiaDosViewModel.firmaInvestigado.value
        val firmaPath = if (firmaInvestigado != null) {
            val file = File(context.getExternalFilesDir(null), "firma_investigado.png")
            file.outputStream().use { out ->
                firmaInvestigado.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } else {
            ""
        }

        // Plantilla base del Markdown
        val plantilla = """
            # INFORMACIÓN SOBRE EL DERECHO DE ASISTENCIA JURÍDICA GRATUITA

            Este derecho se encuentra regulado en la Ley 1/1996, de 10 de enero, de Asistencia Jurídica Gratuita y en el Real Decreto 141/2021, de 9 de marzo, por el que se aprueba el Reglamento de Asistencia Jurídica Gratuita, que tiene por objeto determinar el contenido y alcance del derecho a la asistencia jurídica gratuita al que se refiere el artículo 119 de la Constitución y regular el procedimiento para su reconocimiento y efectividad.

            ## Datos del Informado

            {{nombre}} {{apellidos}}
            {{tipoDocumento}}: {{numeroDocumento}}
            {{genero}}
            {{nacionalidad}}
            {{fechaNacimiento}}
            {{lugarNacimiento}}
            {{domicilio}}
            {{codigoPostal}}
            {{telefono}}
            {{email}}
            {{nombrePadre}}
            {{nombreMadre}}
            {{codigoCan}}

            ## Firma del Investigado
            {{firmaInvestigado}}

        """.trimIndent()

        // Sustituir los marcadores con los datos disponibles
        var markdownResultado = plantilla
        datosPersona.forEach { (clave, valor) ->
            if (valor.isNotBlank()) {
                markdownResultado = markdownResultado.replace("{{$clave}}", valor)
            } else {
                // Si el dato no está presente, eliminamos la línea completa que contiene el marcador
                markdownResultado = markdownResultado.replace("\n{{$clave}}", "")
                markdownResultado = markdownResultado.replace("{{$clave}}", "")
            }
        }

        // Manejar la firma
        if (firmaPath.isNotBlank()) {
            markdownResultado = markdownResultado.replace("{{firmaInvestigado}}", "![Firma Investigado]($firmaPath)")
        } else {
            markdownResultado = markdownResultado.replace("## Firma del Investigado\n{{firmaInvestigado}}", "")
        }

        // Limpiar líneas vacías adicionales
        markdownResultado = markdownResultado.lines()
            .filter { it.trim().isNotEmpty() }
            .joinToString("\n")

        return markdownResultado
    }

    // Método opcional para limpiar el archivo temporal de la firma después de usarlo
    fun limpiarFirmaTemporal() {
        val firmaFile = File(context.getExternalFilesDir(null), "firma_investigado.png")
        if (firmaFile.exists()) {
            firmaFile.delete()
        }
    }
}