package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Configuración de DataStore para almacenar preferencias relacionadas con la pantalla de alcoholemia dos.
 * Define un DataStore con el nombre "alcoholemia_dos_settings" accesible desde el contexto de la aplicación.
 */
val Context.dataStoreAlcoholemiaDos: DataStore<Preferences> by preferencesDataStore(name = "alcoholemia_dos_settings")

/**
 * ViewModel para gestionar los datos relacionados con la pantalla de alcoholemia dos.
 * Utiliza LiveData para observar cambios en los datos y DataStore para persistirlos de manera asíncrona.
 *
 * @param application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class AlcoholemiaDosViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     * Contiene claves para cada campo del formulario de alcoholemia.
     */
    private object PreferencesKeys {
        /** Clave para la fecha de inicio de las diligencias. */
        val FECHA_INICIO = stringPreferencesKey("fecha_inicio")
        /** Clave para la hora de inicio de las diligencias. */
        val HORA_INICIO = stringPreferencesKey("hora_inicio")
        /** Clave para indicar si el lugar coincide con otro evento. */
        val LUGAR_COINCIDE = booleanPreferencesKey("lugar_coincide")
        /** Clave para el lugar donde se realizan las diligencias, incluyendo localidad y provincia. */
        val LUGAR_DILIGENCIAS = stringPreferencesKey("lugar_diligencias")
        /** Clave para indicar si el investigado desea firmar. */
        val DESEA_FIRMAR = booleanPreferencesKey("desea_firmar")
        /** Clave para indicar si se inmoviliza el vehículo. */
        val INMOVILIZA_VEHICULO = booleanPreferencesKey("inmoviliza_vehiculo")
        /** Clave para indicar si hay un segundo conductor. */
        val HAY_SEGUNDO_CONDUCTOR = booleanPreferencesKey("hay_segundo_conductor")
        /** Clave para el nombre del segundo conductor. */
        val NOMBRE_SEGUNDO_CONDUCTOR = stringPreferencesKey("nombre_segundo_conductor")
    }

    // Fecha y hora de inicio
    /** LiveData privado para la fecha de inicio de las diligencias. */
    private val _fechaInicio = MutableLiveData<String>("")
    /** LiveData público para observar la fecha de inicio de las diligencias. */
    val fechaInicio: LiveData<String> = _fechaInicio

    /** LiveData privado para la hora de inicio de las diligencias. */
    private val _horaInicio = MutableLiveData<String>("")
    /** LiveData público para observar la hora de inicio de las diligencias. */
    val horaInicio: LiveData<String> = _horaInicio

    // Lugar de la investigación
    /** LiveData privado para indicar si el lugar de investigación coincide con el de instrucción. */
    private val _lugarCoincide = MutableLiveData<Boolean>(false)
    /** LiveData público para observar si el lugar coincide. */
    val lugarCoincide: LiveData<Boolean> = _lugarCoincide

    /** LiveData privado para el lugar donde se realizan las diligencias, incluyendo localidad y provincia. */
    private val _lugarDiligencias = MutableLiveData<String>("")
    /** LiveData público para observar el lugar de las diligencias. */
    val lugarDiligencias: LiveData<String> = _lugarDiligencias

    // Opciones de firma y vehículo
    /** LiveData privado para indicar si el investigado desea firmar. */
    private val _deseaFirmar = MutableLiveData<Boolean>(false)
    /** LiveData público para observar si el investigado desea firmar. */
    val deseaFirmar: LiveData<Boolean> = _deseaFirmar

    /** LiveData privado para indicar si se inmoviliza el vehículo. */
    private val _inmovilizaVehiculo = MutableLiveData<Boolean>(false)
    /** LiveData público para observar si se inmoviliza el vehículo. */
    val inmovilizaVehiculo: LiveData<Boolean> = _inmovilizaVehiculo

    /** LiveData privado para indicar si hay un segundo conductor. */
    private val _haySegundoConductor = MutableLiveData<Boolean>(false)
    /** LiveData público para observar si hay un segundo conductor. */
    val haySegundoConductor: LiveData<Boolean> = _haySegundoConductor

    /** LiveData privado para el nombre del segundo conductor. */
    private val _nombreSegundoConductor = MutableLiveData<String>("")
    /** LiveData público para observar el nombre del segundo conductor. */
    val nombreSegundoConductor: LiveData<String> = _nombreSegundoConductor

    // Firmas de intervinientes
    /** LiveData privado para la firma del investigado en formato Bitmap. */
    private val _firmaInvestigado = MutableLiveData<Bitmap?>()
    /** LiveData público para observar la firma del investigado. */
    val firmaInvestigado: LiveData<Bitmap?> = _firmaInvestigado

    /** LiveData privado para la firma del segundo conductor en formato Bitmap. */
    private val _firmaSegundoConductor = MutableLiveData<Bitmap?>()
    /** LiveData público para observar la firma del segundo conductor. */
    val firmaSegundoConductor: LiveData<Bitmap?> = _firmaSegundoConductor

    /** LiveData privado para la firma del instructor en formato Bitmap. */
    private val _firmaInstructor = MutableLiveData<Bitmap?>()
    /** LiveData público para observar la firma del instructor. */
    val firmaInstructor: LiveData<Bitmap?> = _firmaInstructor

    /** LiveData privado para la firma del secretario en formato Bitmap. */
    private val _firmaSecretario = MutableLiveData<Bitmap?>()
    /** LiveData público para observar la firma del secretario. */
    val firmaSecretario: LiveData<Bitmap?> = _firmaSecretario

    /**
     * Inicializa el ViewModel cargando los datos y firmas guardados previamente.
     */
    init {
        loadSavedData()
        loadSavedFirmas()
    }

    // Funciones para actualizar datos
    /**
     * Actualiza la fecha de inicio si el valor proporcionado no está vacío.
     *
     * @param value Nueva fecha de inicio en formato de cadena.
     */
    fun updateFechaInicio(value: String) {
        if (value.isNotEmpty()) {
            _fechaInicio.value = value
        }
    }

    /**
     * Actualiza la hora de inicio si el valor proporcionado no está vacío.
     *
     * @param value Nueva hora de inicio en formato de cadena (ej. "HH:mm").
     */
    fun updateHoraInicio(value: String) {
        if (value.isNotEmpty()) {
            _horaInicio.value = value
        }
    }

    /**
     * Actualiza el estado de coincidencia del lugar de investigación.
     *
     * @param value Indicador booleano de si el lugar coincide con el de instrucción.
     */
    fun updateLugarCoincide(value: Boolean) {
        _lugarCoincide.value = value
    }

    /**
     * Actualiza el lugar donde se realizan las diligencias si el valor no está vacío.
     * Puede incluir localidad y provincia obtenidas del GPS (ej. "Madrid, Madrid").
     *
     * @param value Nuevo lugar de las diligencias en formato de cadena.
     */
    fun updateLugarDiligencias(value: String) {
        if (value.isNotEmpty()) {
            _lugarDiligencias.value = value
        }
    }

    /**
     * Actualiza si el investigado desea firmar el documento.
     *
     * @param value Indicador booleano de si el investigado desea firmar.
     */
    fun updateDeseaFirmar(value: Boolean) {
        _deseaFirmar.value = value
    }

    /**
     * Actualiza si el vehículo se inmoviliza durante las diligencias.
     *
     * @param value Indicador booleano de si el vehículo se inmoviliza.
     */
    fun updateInmovilizaVehiculo(value: Boolean) {
        _inmovilizaVehiculo.value = value
    }

    /**
     * Actualiza si existe un segundo conductor involucrado.
     *
     * @param value Indicador booleano de si hay un segundo conductor.
     */
    fun updateHaySegundoConductor(value: Boolean) {
        _haySegundoConductor.value = value
    }

    /**
     * Actualiza el nombre del segundo conductor si el valor no está vacío.
     *
     * @param value Nombre completo del segundo conductor en formato de cadena.
     */
    fun updateNombreSegundoConductor(value: String) {
        if (value.isNotEmpty()) {
            _nombreSegundoConductor.value = value
        }
    }

    /**
     * Actualiza la firma del investigado.
     *
     * @param bitmap Imagen de la firma en formato Bitmap.
     */
    fun updateFirmaInvestigado(bitmap: Bitmap) {
        _firmaInvestigado.value = bitmap
    }

    /**
     * Actualiza la firma del segundo conductor.
     *
     * @param bitmap Imagen de la firma en formato Bitmap.
     */
    fun updateFirmaSegundoConductor(bitmap: Bitmap) {
        _firmaSegundoConductor.value = bitmap
    }

    /**
     * Actualiza la firma del instructor.
     *
     * @param bitmap Imagen de la firma en formato Bitmap.
     */
    fun updateFirmaInstructor(bitmap: Bitmap) {
        _firmaInstructor.value = bitmap
    }

    /**
     * Actualiza la firma del secretario.
     *
     * @param bitmap Imagen de la firma en formato Bitmap.
     */
    fun updateFirmaSecretario(bitmap: Bitmap) {
        _firmaSecretario.value = bitmap
    }

    // Guardar y limpiar datos
    /**
     * Guarda todos los datos actuales del formulario en DataStore de manera asíncrona.
     * Incluye la ubicación (localidad y provincia) en el campo `lugarDiligencias` si está disponible.
     *
     * @param context Contexto necesario para acceder a DataStore.
     */
    fun guardarDatos(context: Context) {
        viewModelScope.launch {
            context.dataStoreAlcoholemiaDos.edit { preferences ->
                _fechaInicio.value?.let { preferences[PreferencesKeys.FECHA_INICIO] = it }
                _horaInicio.value?.let { preferences[PreferencesKeys.HORA_INICIO] = it }
                _lugarCoincide.value?.let { preferences[PreferencesKeys.LUGAR_COINCIDE] = it }
                _lugarDiligencias.value?.let { preferences[PreferencesKeys.LUGAR_DILIGENCIAS] = it }
                _deseaFirmar.value?.let { preferences[PreferencesKeys.DESEA_FIRMAR] = it }
                _inmovilizaVehiculo.value?.let { preferences[PreferencesKeys.INMOVILIZA_VEHICULO] = it }
                _haySegundoConductor.value?.let { preferences[PreferencesKeys.HAY_SEGUNDO_CONDUCTOR] = it }
                _nombreSegundoConductor.value?.let { preferences[PreferencesKeys.NOMBRE_SEGUNDO_CONDUCTOR] = it }
            }
            // Guardar también las firmas al guardar los datos
            guardarFirmas(context)
        }
    }

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores del ViewModel a sus estados iniciales.
     */
    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreAlcoholemiaDos.edit { preferences ->
                preferences.clear()
            }

            _fechaInicio.value = ""
            _horaInicio.value = ""
            _lugarCoincide.value = false
            _lugarDiligencias.value = ""
            _deseaFirmar.value = false
            _inmovilizaVehiculo.value = false
            _haySegundoConductor.value = false
            _nombreSegundoConductor.value = ""
            _firmaInvestigado.value = null
            _firmaSegundoConductor.value = null
            _firmaInstructor.value = null
            _firmaSecretario.value = null
        }
    }

    /**
     * Carga los datos guardados previamente en DataStore al inicializar el ViewModel.
     * Incluye la ubicación (localidad y provincia) almacenada en `lugarDiligencias`.
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStoreAlcoholemiaDos.data.first()

            _fechaInicio.value = preferences[PreferencesKeys.FECHA_INICIO] ?: ""
            _horaInicio.value = preferences[PreferencesKeys.HORA_INICIO] ?: ""
            _lugarCoincide.value = preferences[PreferencesKeys.LUGAR_COINCIDE] ?: false
            _lugarDiligencias.value = preferences[PreferencesKeys.LUGAR_DILIGENCIAS] ?: ""
            _deseaFirmar.value = preferences[PreferencesKeys.DESEA_FIRMAR] ?: false
            _inmovilizaVehiculo.value = preferences[PreferencesKeys.INMOVILIZA_VEHICULO] ?: false
            _haySegundoConductor.value = preferences[PreferencesKeys.HAY_SEGUNDO_CONDUCTOR] ?: false
            _nombreSegundoConductor.value = preferences[PreferencesKeys.NOMBRE_SEGUNDO_CONDUCTOR] ?: ""
        }
    }

    /** Clave para la firma del investigado en DataStore. */
    private val FIRMA_INVESTIGADO = stringPreferencesKey("firma_investigado")
    /** Clave para la firma del segundo conductor en DataStore. */
    private val FIRMA_SEGUNDO_CONDUCTOR = stringPreferencesKey("firma_segundo_conductor")
    /** Clave para la firma del instructor en DataStore. */
    private val FIRMA_INSTRUCTOR = stringPreferencesKey("firma_instructor")
    /** Clave para la firma del secretario en DataStore. */
    private val FIRMA_SECRETARIO = stringPreferencesKey("firma_secretario")

    /**
     * Guarda las firmas de los intervinientes en DataStore convirtiéndolas a cadenas de bytes.
     *
     * @param context Contexto necesario para acceder a DataStore.
     */
    fun guardarFirmas(context: Context) {
        viewModelScope.launch {
            context.dataStoreAlcoholemiaDos.edit { preferences ->
                _firmaInvestigado.value?.let { bitmap ->
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    preferences[FIRMA_INVESTIGADO] = byteArrayOutputStream.toByteArray().toString(Charsets.ISO_8859_1)
                }
                _firmaSegundoConductor.value?.let { bitmap ->
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    preferences[FIRMA_SEGUNDO_CONDUCTOR] = byteArrayOutputStream.toByteArray().toString(Charsets.ISO_8859_1)
                }
                _firmaInstructor.value?.let { bitmap ->
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    preferences[FIRMA_INSTRUCTOR] = byteArrayOutputStream.toByteArray().toString(Charsets.ISO_8859_1)
                }
                _firmaSecretario.value?.let { bitmap ->
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    preferences[FIRMA_SECRETARIO] = byteArrayOutputStream.toByteArray().toString(Charsets.ISO_8859_1)
                }
            }
        }
    }

    /**
     * Carga las firmas guardadas en DataStore y las convierte de nuevo a objetos Bitmap.
     */
    private fun loadSavedFirmas() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStoreAlcoholemiaDos.data.first()

            preferences[FIRMA_INVESTIGADO]?.let { byteArrayString ->
                val byteArray = byteArrayString.toByteArray(Charsets.ISO_8859_1)
                _firmaInvestigado.value = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }
            preferences[FIRMA_SEGUNDO_CONDUCTOR]?.let { byteArrayString ->
                val byteArray = byteArrayString.toByteArray(Charsets.ISO_8859_1)
                _firmaSegundoConductor.value = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }
            preferences[FIRMA_INSTRUCTOR]?.let { byteArrayString ->
                val byteArray = byteArrayString.toByteArray(Charsets.ISO_8859_1)
                _firmaInstructor.value = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }
            preferences[FIRMA_SECRETARIO]?.let { byteArrayString ->
                val byteArray = byteArrayString.toByteArray(Charsets.ISO_8859_1)
                _firmaSecretario.value = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }
        }
    }
}