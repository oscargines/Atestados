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
 * Configuración de DataStore para almacenar preferencias relacionadas con la lectura de derechos.
 */
val Context.dataStoreDerechos: DataStore<Preferences> by preferencesDataStore(name = "lectura_derechos_settings")

/**
 * ViewModel para gestionar los datos relacionados con la lectura de derechos.
 * Utiliza LiveData para observar cambios en los datos y DataStore para persistirlos.
 *
 * @property application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class LecturaDerechosViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     */
    private object PreferencesKeys {
        /** Clave para el momento de la lectura de derechos. */
        val MOMENTO_LECTURA = stringPreferencesKey("momento_lectura")
        /** Clave para el lugar de la investigación. */
        val LUGAR_INVESTIGACION = stringPreferencesKey("lugar_investigacion")
        /** Clave para el lugar del delito. */
        val LUGAR_DELITO = stringPreferencesKey("lugar_delito")
        /** Clave para el resumen de los hechos. */
        val RESUMEN_HECHOS = stringPreferencesKey("resumen_hechos")
        /** Clave para la calificación de los hechos. */
        val CALIFICACION_HECHOS = stringPreferencesKey("calificacion_hechos")
        /** Clave para la relación de indicios. */
        val RELACION_INDICIOS = stringPreferencesKey("relacion_indicios")
    }

    /** LiveData privado para el momento de la lectura de derechos, con valor por defecto "Tomada en el momento". */
    private val _momentoLectura = MutableLiveData("Tomada en el momento")
    /** LiveData público para observar el momento de la lectura de derechos. */
    val momentoLectura: LiveData<String> = _momentoLectura

    /** LiveData privado para el lugar de la investigación. */
    private val _lugarInvestigacion = MutableLiveData("")
    /** LiveData público para observar el lugar de la investigación. */
    val lugarInvestigacion: LiveData<String> = _lugarInvestigacion

    /** LiveData privado para el lugar del delito. */
    private val _lugarDelito = MutableLiveData("")
    /** LiveData público para observar el lugar del delito. */
    val lugarDelito: LiveData<String> = _lugarDelito

    /** LiveData privado para el resumen de los hechos. */
    private val _resumenHechos = MutableLiveData("")
    /** LiveData público para observar el resumen de los hechos. */
    val resumenHechos: LiveData<String> = _resumenHechos

    /** LiveData privado para la calificación de los hechos. */
    private val _calificacionHechos = MutableLiveData("")
    /** LiveData público para observar la calificación de los hechos. */
    val calificacionHechos: LiveData<String> = _calificacionHechos

    /** LiveData privado para la relación de indicios. */
    private val _relacionIndicios = MutableLiveData("")
    /** LiveData público para observar la relación de indicios. */
    val relacionIndicios: LiveData<String> = _relacionIndicios

    /**
     * Inicializa el ViewModel cargando los datos guardados desde DataStore.
     */
    init {
        loadSavedData()
    }

    /**
     * Establece el momento de la lectura de derechos.
     *
     * @param momento Nuevo momento de la lectura.
     */
    fun setMomentoLectura(momento: String) {
        _momentoLectura.value = momento
    }

    /**
     * Actualiza el lugar de la investigación.
     *
     * @param lugar Nuevo lugar de la investigación.
     */
    fun updateLugarInvestigacion(lugar: String) {
        _lugarInvestigacion.value = lugar
    }

    /**
     * Actualiza el lugar del delito.
     *
     * @param lugar Nuevo lugar del delito.
     */
    fun updateLugarDelito(lugar: String) {
        _lugarDelito.value = lugar
    }

    /**
     * Actualiza el resumen de los hechos.
     *
     * @param resumen Nuevo resumen de los hechos.
     */
    fun updateResumenHechos(resumen: String) {
        _resumenHechos.value = resumen
    }

    /**
     * Actualiza la calificación de los hechos.
     *
     * @param calificacion Nueva calificación de los hechos.
     */
    fun updateCalificacionHechos(calificacion: String) {
        _calificacionHechos.value = calificacion
    }

    /**
     * Actualiza la relación de indicios.
     *
     * @param indicios Nueva relación de indicios.
     */
    fun updateRelacionIndicios(indicios: String) {
        _relacionIndicios.value = indicios
    }

    /**
     * Guarda los datos actuales en DataStore de manera asíncrona.
     */
    fun guardarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreDerechos.edit { preferences ->
                _momentoLectura.value?.let { preferences[PreferencesKeys.MOMENTO_LECTURA] = it }
                _lugarInvestigacion.value?.let { preferences[PreferencesKeys.LUGAR_INVESTIGACION] = it }
                _lugarDelito.value?.let { preferences[PreferencesKeys.LUGAR_DELITO] = it }
                _resumenHechos.value?.let { preferences[PreferencesKeys.RESUMEN_HECHOS] = it }
                _calificacionHechos.value?.let { preferences[PreferencesKeys.CALIFICACION_HECHOS] = it }
                _relacionIndicios.value?.let { preferences[PreferencesKeys.RELACION_INDICIOS] = it }
            }
        }
    }

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores en el ViewModel.
     */
    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreDerechos.edit { preferences ->
                preferences.clear()
            }

            _momentoLectura.value = "Tomada en el momento"
            _lugarInvestigacion.value = ""
            _lugarDelito.value = ""
            _resumenHechos.value = ""
            _calificacionHechos.value = ""
            _relacionIndicios.value = ""
        }
    }

    /**
     * Carga los datos guardados en DataStore al inicializar el ViewModel.
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStoreDerechos.data.first()

            _momentoLectura.value = preferences[PreferencesKeys.MOMENTO_LECTURA] ?: "Tomada en el momento"
            _lugarInvestigacion.value = preferences[PreferencesKeys.LUGAR_INVESTIGACION] ?: ""
            _lugarDelito.value = preferences[PreferencesKeys.LUGAR_DELITO] ?: ""
            _resumenHechos.value = preferences[PreferencesKeys.RESUMEN_HECHOS] ?: ""
            _calificacionHechos.value = preferences[PreferencesKeys.CALIFICACION_HECHOS] ?: ""
            _relacionIndicios.value = preferences[PreferencesKeys.RELACION_INDICIOS] ?: ""
        }
    }
}