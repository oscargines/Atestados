package com.oscar.atestados.data

/**
 * Clase de datos que representa un dispositivo Bluetooth almacenado en la base de datos.
 *
 * @property nombre Nombre del dispositivo Bluetooth.
 * @property mac Dirección MAC única del dispositivo Bluetooth.
 */
data class BluetoothDeviceDB(
    val nombre: String,
    val mac: String
)