package com.oscar.atestados.utils

/**
 * Objeto singleton que almacena la configuración estática de la aplicación,
 * como la versión y el código de versión.
 *
 * Este objeto proporciona acceso a constantes que definen la versión de la
 * aplicación, eliminando la dependencia de archivos generados automáticamente
 * como `BuildConfig`.
 */
object AppConfig {
    /**
     * Nombre de la versión de la aplicación.
     *
     * Representa la versión actual de la aplicación en formato de texto,
     * por ejemplo, "beta 0.1.0". Este valor se usa en la interfaz de usuario
     * y en la lógica de comparación de versiones.
     */
    const val VERSION_NAME = "beta 0.1.0"

    /**
     * Código de versión de la aplicación.
     *
     * Representa un número entero que identifica la versión de la aplicación.
     * Se incluye para mantener consistencia con la estructura de versiones,
     * aunque no se utiliza directamente en la lógica actual.
     */
    const val VERSION_CODE = 1
}