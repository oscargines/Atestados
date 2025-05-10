package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "LecturaDerechosViewModel"

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
        val MOMENTO_LECTURA = stringPreferencesKey("momento_lectura")
        val LUGAR_INVESTIGACION = stringPreferencesKey("lugar_investigacion")
        val LUGAR_DELITO = stringPreferencesKey("lugar_delito")
        val RESUMEN_HECHOS = stringPreferencesKey("resumen_hechos")
        val CALIFICACION_HECHOS = stringPreferencesKey("calificacion_hechos")
        val RELACION_INDICIOS = stringPreferencesKey("relacion_indicios")
        val LATITUD_INVESTIGACION = doublePreferencesKey("latitud_investigacion")
        val LONGITUD_INVESTIGACION = doublePreferencesKey("longitud_investigacion")
        val LATITUD_DELITO = doublePreferencesKey("latitud_delito")
        val LONGITUD_DELITO = doublePreferencesKey("longitud_delito")
    }

    /** LiveData privado para el momento de la lectura de derechos, con valor por defecto "Tomada en el momento". */
    private val _momentoLectura = MutableLiveData("Tomada en el momento")
    /** LiveData público para observar el momento de la lectura de derechos. */
    val momentoLectura: LiveData<String> = _momentoLectura

    /** LiveData privado para el lugar de la investigación. */
    private val _lugarInvestigacion = MutableLiveData("")
    /** LiveData público para observar el lugar de la investigación. */
    val lugarInvestigacion: LiveData<String> = _lugarInvestigacion

    /** LiveData privado para la latitud de la investigación. */
    private val _latitudInvestigacion = MutableLiveData<Double?>(null)
    /** LiveData público para observar la latitud de la investigación. */
    val latitudInvestigacion: LiveData<Double?> = _latitudInvestigacion

    /** LiveData privado para la longitud de la investigación. */
    private val _longitudInvestigacion = MutableLiveData<Double?>(null)
    /** LiveData público para observar la longitud de la investigación. */
    val longitudInvestigacion: LiveData<Double?> = _longitudInvestigacion

    /** LiveData privado para el lugar del delito. */
    private val _lugarDelito = MutableLiveData("")
    /** LiveData público para observar el lugar del delito. */
    val lugarDelito: LiveData<String> = _lugarDelito

    /** LiveData privado para la latitud del delito. */
    private val _latitudDelito = MutableLiveData<Double?>(null)
    /** LiveData público para observar la latitud del delito. */
    val latitudDelito: LiveData<Double?> = _latitudDelito

    /** LiveData privado para la longitud del delito. */
    private val _longitudDelito = MutableLiveData<Double?>(null)
    /** LiveData público para observar la longitud del delito. */
    val longitudDelito: LiveData<Double?> = _longitudDelito

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
        Log.d(TAG, "Actualizando momentoLectura a: $momento")
        _momentoLectura.value = momento
    }

    /**
     * Actualiza el lugar de la investigación y sus coordenadas.
     *
     * @param lugar Nuevo lugar de la investigación.
     * @param latitud Latitud de la ubicación (puede ser null).
     * @param longitud Longitud de la ubicación (puede ser null).
     */
    fun updateLugarInvestigacion(lugar: String, latitud: Double?, longitud: Double?) {
        Log.d(TAG, "Actualizando lugarInvestigacion a: $lugar, lat=$latitud, lon=$longitud")
        _lugarInvestigacion.value = lugar
        _latitudInvestigacion.value = latitud
        _longitudInvestigacion.value = longitud
    }

    /**
     * Actualiza el lugar del delito y sus coordenadas.
     *
     * @param lugar Nuevo lugar del delito.
     * @param latitud Latitud de la ubicación (puede ser null).
     * @param longitud Longitud de la ubicación (puede ser null).
     */
    fun updateLugarDelito(lugar: String, latitud: Double?, longitud: Double?) {
        Log.d(TAG, "Actualizando lugarDelito a: $lugar, lat=$latitud, lon=$longitud")
        _lugarDelito.value = lugar
        _latitudDelito.value = latitud
        _longitudDelito.value = longitud
    }

    /**
     * Actualiza el resumen de los hechos.
     *
     * @param resumen Nuevo resumen de los hechos.
     */
    fun updateResumenHechos(resumen: String) {
        Log.d(TAG, "Actualizando resumenHechos a: $resumen")
        _resumenHechos.value = resumen
    }

    /**
     * Actualiza la calificación de los hechos.
     *
     * @param calificacion Nueva calificación de los hechos.
     */
    fun updateCalificacionHechos(calificacion: String) {
        Log.d(TAG, "Actualizando calificacionHechos a: $calificacion")
        _calificacionHechos.value = calificacion
    }

    /**
     * Actualiza la relación de indicios.
     *
     * @param indicios Nueva relación de indicios.
     */
    fun updateRelacionIndicios(indicios: String) {
        Log.d(TAG, "Actualizando relacionIndicios a: $indicios")
        _relacionIndicios.value = indicios
    }

    /**
     * Guarda los datos actuales en DataStore de manera asíncrona.
     */
    fun guardarDatos(context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Guardando datos en DataStore")
            try {
                getApplication<Application>().dataStoreDerechos.edit { preferences ->
                    _momentoLectura.value?.let {
                        preferences[PreferencesKeys.MOMENTO_LECTURA] = it
                        Log.d(TAG, "Guardado momentoLectura: $it")
                    }
                    _lugarInvestigacion.value?.let {
                        preferences[PreferencesKeys.LUGAR_INVESTIGACION] = it
                        Log.d(TAG, "Guardado lugarInvestigacion: $it")
                    }
                    _latitudInvestigacion.value?.let {
                        preferences[PreferencesKeys.LATITUD_INVESTIGACION] = it
                        Log.d(TAG, "Guardado latitudInvestigacion: $it")
                    }
                    _longitudInvestigacion.value?.let {
                        preferences[PreferencesKeys.LONGITUD_INVESTIGACION] = it
                        Log.d(TAG, "Guardado longitudInvestigacion: $it")
                    }
                    _lugarDelito.value?.let {
                        preferences[PreferencesKeys.LUGAR_DELITO] = it
                        Log.d(TAG, "Guardado lugarDelito: $it")
                    }
                    _latitudDelito.value?.let {
                        preferences[PreferencesKeys.LATITUD_DELITO] = it
                        Log.d(TAG, "Guardado latitudDelito: $it")
                    }
                    _longitudDelito.value?.let {
                        preferences[PreferencesKeys.LONGITUD_DELITO] = it
                        Log.d(TAG, "Guardado longitudDelito: $it")
                    }
                    _resumenHechos.value?.let {
                        preferences[PreferencesKeys.RESUMEN_HECHOS] = it
                        Log.d(TAG, "Guardado resumenHechos: $it")
                    }
                    _calificacionHechos.value?.let {
                        preferences[PreferencesKeys.CALIFICACION_HECHOS] = it
                        Log.d(TAG, "Guardado calificacionHechos: $it")
                    }
                    _relacionIndicios.value?.let {
                        preferences[PreferencesKeys.RELACION_INDICIOS] = it
                        Log.d(TAG, "Guardado relacionIndicios: $it")
                    }
                }
                Log.d(TAG, "Datos guardados exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar datos: ${e.message}", e)
            }
        }
    }

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores en el ViewModel.
     */
    fun limpiarDatos() {
        viewModelScope.launch {
            Log.d(TAG, "Limpiando datos en DataStore")
            try {
                getApplication<Application>().dataStoreDerechos.edit { preferences ->
                    preferences.clear()
                    Log.d(TAG, "DataStore limpiado")
                }

                _momentoLectura.value = "Tomada en el momento"
                _lugarInvestigacion.value = ""
                _latitudInvestigacion.value = null
                _longitudInvestigacion.value = null
                _lugarDelito.value = ""
                _latitudDelito.value = null
                _longitudDelito.value = null
                _resumenHechos.value = ""
                _calificacionHechos.value = ""
                _relacionIndicios.value = ""
                Log.d(TAG, "Valores del ViewModel restablecidos")
            } catch (e: Exception) {
                Log.e(TAG, "Error al limpiar datos: ${e.message}", e)
            }
        }
    }

    /**
     * Carga los datos guardados en DataStore al inicializar el ViewModel.
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            Log.d(TAG, "Cargando datos desde DataStore")
            try {
                val preferences = getApplication<Application>().dataStoreDerechos.data.first()

                _momentoLectura.value = preferences[PreferencesKeys.MOMENTO_LECTURA] ?: "Tomada en el momento"
                Log.d(TAG, "Cargado momentoLectura: ${_momentoLectura.value}")
                _lugarInvestigacion.value = preferences[PreferencesKeys.LUGAR_INVESTIGACION] ?: ""
                Log.d(TAG, "Cargado lugarInvestigacion: ${_lugarInvestigacion.value}")
                _latitudInvestigacion.value = preferences[PreferencesKeys.LATITUD_INVESTIGACION]
                Log.d(TAG, "Cargado latitudInvestigacion: ${_latitudInvestigacion.value}")
                _longitudInvestigacion.value = preferences[PreferencesKeys.LONGITUD_INVESTIGACION]
                Log.d(TAG, "Cargado longitudInvestigacion: ${_longitudInvestigacion.value}")
                _lugarDelito.value = preferences[PreferencesKeys.LUGAR_DELITO] ?: ""
                Log.d(TAG, "Cargado lugarDelito: ${_lugarDelito.value}")
                _latitudDelito.value = preferences[PreferencesKeys.LATITUD_DELITO]
                Log.d(TAG, "Cargado latitudDelito: ${_latitudDelito.value}")
                _longitudDelito.value = preferences[PreferencesKeys.LONGITUD_DELITO]
                Log.d(TAG, "Cargado longitudDelito: ${_longitudDelito.value}")
                _resumenHechos.value = preferences[PreferencesKeys.RESUMEN_HECHOS] ?: ""
                Log.d(TAG, "Cargado resumenHechos: ${_resumenHechos.value}")
                _calificacionHechos.value = preferences[PreferencesKeys.CALIFICACION_HECHOS] ?: ""
                Log.d(TAG, "Cargado calificacionHechos: ${_calificacionHechos.value}")
                _relacionIndicios.value = preferences[PreferencesKeys.RELACION_INDICIOS] ?: ""
                Log.d(TAG, "Cargado relacionIndicios: ${_relacionIndicios.value}")
                Log.d(TAG, "Datos cargados exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar datos: ${e.message}", e)
            }
        }
    }
}