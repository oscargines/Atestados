package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Configuración de DataStore para almacenar preferencias relacionadas con la citación.
 */
val Context.dataStoreCitacion: DataStore<Preferences> by preferencesDataStore(name = "citacion_settings")

/**
 * ViewModel para gestionar los datos relacionados con la citación.
 * Utiliza LiveData para observar cambios en los datos y DataStore para persistirlos.
 *
 * @property application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class CitacionViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     */
    private object PreferencesKeys {
        /** Clave para el tipo de juzgado. */
        val TIPO_JUZGADO = stringPreferencesKey("tipo_juzgado")
        /** Clave para la provincia. */
        val PROVINCIA = stringPreferencesKey("provincia")
        /** Clave para la localidad. */
        val LOCALIDAD = stringPreferencesKey("localidad")
        /** Clave para el juzgado. */
        val JUZGADO = stringPreferencesKey("juzgado")
        /** Clave para la fecha de inicio. */
        val FECHA_INICIO = stringPreferencesKey("fecha_inicio")
        /** Clave para la hora. */
        val HORA = stringPreferencesKey("hora")
        /** Clave para el número de diligencias. */
        val NUMERO_DILIGENCIAS = stringPreferencesKey("numero_diligencias")
    }

    /** LiveData privado para el tipo de juzgado. */
    private val _tipoJuzgado = MutableLiveData("")
    /** LiveData público para observar el tipo de juzgado. */
    val tipoJuzgado: LiveData<String> = _tipoJuzgado

    /** LiveData privado para la provincia. */
    private val _provincia = MutableLiveData("")
    /** LiveData público para observar la provincia. */
    val provincia: LiveData<String> = _provincia

    /** LiveData privado para la localidad. */
    private val _localidad = MutableLiveData("")
    /** LiveData público para observar la localidad. */
    val localidad: LiveData<String> = _localidad

    /** LiveData privado para el juzgado. */
    private val _juzgado = MutableLiveData("")
    /** LiveData público para observar el juzgado. */
    val juzgado: LiveData<String> = _juzgado

    /** LiveData privado para la fecha de inicio. */
    private val _fechaInicio = MutableLiveData("")
    /** LiveData público para observar la fecha de inicio. */
    val fechaInicio: LiveData<String> = _fechaInicio

    /** LiveData privado para la hora. */
    private val _hora = MutableLiveData("")
    /** LiveData público para observar la hora. */
    val hora: LiveData<String> = _hora

    /** LiveData privado para el número de diligencias. */
    private val _numeroDiligencias = MutableLiveData("")
    /** LiveData público para observar el número de diligencias. */
    val numeroDiligencias: LiveData<String> = _numeroDiligencias

    /**
     * Inicializa el ViewModel cargando los datos guardados desde DataStore.
     */
    init {
        loadSavedData()
    }

    /**
     * Actualiza el tipo de juzgado.
     *
     * @param tipo Nuevo tipo de juzgado.
     */
    fun updateTipoJuzgado(tipo: String) {
        _tipoJuzgado.value = tipo
    }

    /**
     * Actualiza la provincia.
     *
     * @param provincia Nueva provincia.
     */
    fun updateProvincia(provincia: String) {
        _provincia.value = provincia
    }

    /**
     * Actualiza la localidad.
     *
     * @param localidad Nueva localidad.
     */
    fun updateLocalidad(localidad: String) {
        _localidad.value = localidad
    }

    /**
     * Actualiza el juzgado.
     *
     * @param juzgado Nuevo juzgado.
     */
    fun updateJuzgado(juzgado: String) {
        _juzgado.value = juzgado
    }

    /**
     * Actualiza la fecha de inicio.
     *
     * @param fecha Nueva fecha de inicio.
     */
    fun updateFechaInicio(fecha: String) {
        _fechaInicio.value = fecha
    }

    /**
     * Actualiza la hora.
     *
     * @param hora Nueva hora.
     */
    fun updateHora(hora: String) {
        _hora.value = hora
    }

    /**
     * Actualiza el número de diligencias.
     *
     * @param numero Nuevo número de diligencias.
     */
    fun updateNumeroDiligencias(numero: String) { // Nueva función
        _numeroDiligencias.value = numero
    }

    /**
     * Guarda los datos actuales en DataStore de manera asíncrona.
     */
    fun guardarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreCitacion.edit { preferences ->
                _tipoJuzgado.value?.let { preferences[PreferencesKeys.TIPO_JUZGADO] = it }
                _provincia.value?.let { preferences[PreferencesKeys.PROVINCIA] = it }
                _localidad.value?.let { preferences[PreferencesKeys.LOCALIDAD] = it }
                _juzgado.value?.let { preferences[PreferencesKeys.JUZGADO] = it }
                _fechaInicio.value?.let { preferences[PreferencesKeys.FECHA_INICIO] = it }
                _hora.value?.let { preferences[PreferencesKeys.HORA] = it }
                _numeroDiligencias.value?.let { preferences[PreferencesKeys.NUMERO_DILIGENCIAS] = it }
            }
        }
    }

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores en el ViewModel.
     */
    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreCitacion.edit { preferences ->
                preferences.clear()
            }

            _tipoJuzgado.value = ""
            _provincia.value = ""
            _localidad.value = ""
            _juzgado.value = ""
            _fechaInicio.value = ""
            _hora.value = ""
            _numeroDiligencias.value = ""
        }
    }

    /**
     * Carga los datos guardados en DataStore al inicializar el ViewModel.
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStoreCitacion.data.first()

            _tipoJuzgado.value = preferences[PreferencesKeys.TIPO_JUZGADO] ?: ""
            _provincia.value = preferences[PreferencesKeys.PROVINCIA] ?: ""
            _localidad.value = preferences[PreferencesKeys.LOCALIDAD] ?: ""
            _juzgado.value = preferences[PreferencesKeys.JUZGADO] ?: ""
            _fechaInicio.value = preferences[PreferencesKeys.FECHA_INICIO] ?: ""
            _hora.value = preferences[PreferencesKeys.HORA] ?: ""
            _numeroDiligencias.value = preferences[PreferencesKeys.NUMERO_DILIGENCIAS] ?: ""
        }
    }
}