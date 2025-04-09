package com.oscar.atestados.data

data class Persona(
    val genero: String = "",
    val nacionalidad: String = "",
    val tipoDocumento: String = "",
    val numeroDocumento: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val nombrePadre: String = "",
    val nombreMadre: String = "",
    val fechaNacimiento: String = "",
    val lugarNacimiento: String = "",
    val domicilio: String = "",
    val codigoPostal: String = "",
    val telefono: String = "",
    val email: String = "",
    val photo: ByteArray? = null,
    val signature: ByteArray? = null,
    val uid: String? = null,
    val can: String? = null
)