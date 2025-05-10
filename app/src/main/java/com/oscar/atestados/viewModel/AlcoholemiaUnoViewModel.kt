package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
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
 * Configuración de DataStore para almacenar preferencias relacionadas con la pantalla de alcoholemia uno.
 */
val Context.dataStoreAlcoholemia: DataStore<Preferences> by preferencesDataStore(name = "alcoholemia_uno_settings")

/**
 * ViewModel para gestionar los datos relacionados con la pantalla de alcoholemia uno.
 * Utiliza LiveData para observar cambios en los datos y DataStore para persistirlos.
 *
 * @property application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class AlcoholemiaUnoViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     */
    private object PreferencesKeys {
        /** Clave para la opción seleccionada como motivo de las diligencias. */
        val OPCION_MOTIVO = stringPreferencesKey("opcion_motivo")
        /** Clave para la opción seleccionada sobre errores permitidos. */
        val OPCION_ERRORES = stringPreferencesKey("opcion_errores")
        /** Clave para la opción seleccionada sobre deseo de pruebas. */
        val OPCION_PRUEBAS = stringPreferencesKey("opcion_pruebas")
        /** Clave para la marca del dispositivo de medición. */
        val MARCA = stringPreferencesKey("marca")
        /** Clave para el modelo del dispositivo de medición. */
        val MODELO = stringPreferencesKey("modelo")
        /** Clave para el número de serie del dispositivo de medición. */
        val SERIE = stringPreferencesKey("serie")
        /** Clave para la fecha de caducidad del dispositivo de medición. */
        val CADUCIDAD = stringPreferencesKey("caducidad")
        /** Clave para el resultado de la primera tasa de alcoholemia. */
        val PRIMERA_TASA = stringPreferencesKey("primera_tasa")
        /** Clave para la hora de la primera prueba de alcoholemia. */
        val PRIMERA_HORA = stringPreferencesKey("primera_hora")
        /** Clave para el resultado de la segunda tasa de alcoholemia. */
        val SEGUNDA_TASA = stringPreferencesKey("segunda_tasa")
        /** Clave para la hora de la segunda prueba de alcoholemia. */
        val SEGUNDA_HORA = stringPreferencesKey("segunda_hora")
    }

    // Opciones del motivo por el que se instruyen diligencias
    /** LiveData privado para la opción de motivo. */
    private val _opcionMotivo = MutableLiveData<String>()
    /** LiveData público para observar la opción de motivo. */
    val opcionMotivo: LiveData<String> = _opcionMotivo

    // Opciones de errores permitidos
    /** LiveData privado para la opción de errores. */
    private val _opcionErrores = MutableLiveData<String>()
    /** LiveData público para observar la opción de errores. */
    val opcionErrores: LiveData<String> = _opcionErrores

    // Opciones de si desea pruebas
    /** LiveData privado para la opción de deseo de pruebas. */
    private val _opcionDeseaPruebas = MutableLiveData<String>()
    /** LiveData público para observar la opción de deseo de pruebas. */
    val opcionDeseaPruebas: LiveData<String> = _opcionDeseaPruebas

    // Campos de texto
    /** LiveData privado para la marca del dispositivo. */
    private val _marca = MutableLiveData<String>()
    /** LiveData público para observar la marca del dispositivo. */
    val marca: LiveData<String> = _marca

    /** LiveData privado para el modelo del dispositivo. */
    private val _modelo = MutableLiveData<String>()
    /** LiveData público para observar el modelo del dispositivo. */
    val modelo: LiveData<String> = _modelo

    /** LiveData privado para el número de serie del dispositivo. */
    private val _serie = MutableLiveData<String>()
    /** LiveData público para observar el número de serie del dispositivo. */
    val serie: LiveData<String> = _serie

    /** LiveData privado para la fecha de caducidad del dispositivo. */
    private val _caducidad = MutableLiveData<String>()
    /** LiveData público para observar la fecha de caducidad del dispositivo. */
    val caducidad: LiveData<String> = _caducidad

    // Primera prueba
    /** LiveData privado para la primera tasa de alcoholemia. */
    private val _primeraTasa = MutableLiveData<String>()
    /** LiveData público para observar la primera tasa de alcoholemia. */
    val primeraTasa: LiveData<String> = _primeraTasa

    /** LiveData privado para la hora de la primera prueba. */
    private val _primeraHora = MutableLiveData<String>()
    /** LiveData público para observar la hora de la primera prueba. */
    val primeraHora: LiveData<String> = _primeraHora

    // Segunda prueba
    /** LiveData privado para la segunda tasa de alcoholemia. */
    private val _segundaTasa = MutableLiveData<String>()
    /** LiveData público para observar la segunda tasa de alcoholemia. */
    val segundaTasa: LiveData<String> = _segundaTasa

    /** LiveData privado para la hora de la segunda prueba. */
    private val _segundaHora = MutableLiveData<String>()
    /** LiveData público para observar la hora de la segunda prueba. */
    val segundaHora: LiveData<String> = _segundaHora

    init {
        loadSavedData()
    }

    // Funciones para actualizar opciones
    /**
     * Establece la opción seleccionada como motivo de las diligencias.
     *
     * @param opcionMotivo Opción seleccionada para el motivo.
     */
    fun setOpcionMotivo(opcionMotivo: String) {
        _opcionMotivo.value = opcionMotivo
    }

    /**
     * Establece la opción seleccionada sobre errores permitidos.
     *
     * @param opcionErrores Opción seleccionada para los errores.
     */
    fun setOpcionErrores(opcionErrores: String) {
        _opcionErrores.value = opcionErrores
    }

    /**
     * Establece la opción seleccionada sobre el deseo de realizar pruebas.
     *
     * @param opcionDeseaPruebas Opción seleccionada para el deseo de pruebas.
     */
    fun setOpcionDeseaPruebas(opcionDeseaPruebas: String) {
        _opcionDeseaPruebas.value = opcionDeseaPruebas
    }

    // Actualizadores de campos
    /**
     * Actualiza la marca del dispositivo de medición.
     *
     * @param value Nueva marca del dispositivo.
     */
    fun updateMarca(value: String) {
        _marca.value = value
    }

    /**
     * Actualiza el modelo del dispositivo de medición.
     *
     * @param value Nuevo modelo del dispositivo.
     */
    fun updateModelo(value: String) {
        _modelo.value = value
    }

    /**
     * Actualiza el número de serie del dispositivo de medición.
     *
     * @param value Nuevo número de serie del dispositivo.
     */
    fun updateSerie(value: String) {
        _serie.value = value
    }

    /**
     * Actualiza la fecha de caducidad del dispositivo de medición.
     *
     * @param value Nueva fecha de caducidad del dispositivo.
     */
    fun updateCaducidad(value: String) {
        _caducidad.value = value
    }

    /**
     * Actualiza el resultado de la primera tasa de alcoholemia.
     *
     * @param value Nueva primera tasa de alcoholemia.
     */
    fun updatePrimeraTasa(value: String) {
        _primeraTasa.value = value
    }

    /**
     * Actualiza la hora de la primera prueba de alcoholemia.
     *
     * @param value Nueva hora de la primera prueba.
     */
    fun updatePrimeraHora(value: String) {
        _primeraHora.value = value
    }

    /**
     * Actualiza el resultado de la segunda tasa de alcoholemia.
     *
     * @param value Nueva segunda tasa de alcoholemia.
     */
    fun updateSegundaTasa(value: String) {
        _segundaTasa.value = value
    }

    /**
     * Actualiza la hora de la segunda prueba de alcoholemia.
     *
     * @param value Nueva hora de la segunda prueba.
     */
    fun updateSegundaHora(value: String) {
        _segundaHora.value = value
    }

    // Operaciones de guardado y limpieza
    /**
     * Guarda los datos actuales en DataStore de manera asíncrona.
     *
     * @param context Contexto necesario para acceder a DataStore.
     */
    fun guardarDatos(context: Context) {
        viewModelScope.launch {
            context.dataStoreAlcoholemia.edit { preferences ->
                _opcionMotivo.value?.let { preferences[PreferencesKeys.OPCION_MOTIVO] = it }
                _opcionErrores.value?.let { preferences[PreferencesKeys.OPCION_ERRORES] = it }
                _opcionDeseaPruebas.value?.let { preferences[PreferencesKeys.OPCION_PRUEBAS] = it }
                _marca.value?.let { preferences[PreferencesKeys.MARCA] = it }
                _modelo.value?.let { preferences[PreferencesKeys.MODELO] = it }
                _serie.value?.let { preferences[PreferencesKeys.SERIE] = it }
                _caducidad.value?.let { preferences[PreferencesKeys.CADUCIDAD] = it }
                _primeraTasa.value?.let { preferences[PreferencesKeys.PRIMERA_TASA] = it }
                _primeraHora.value?.let { preferences[PreferencesKeys.PRIMERA_HORA] = it }
                _segundaTasa.value?.let { preferences[PreferencesKeys.SEGUNDA_TASA] = it }
                _segundaHora.value?.let { preferences[PreferencesKeys.SEGUNDA_HORA] = it }
            }
        }
    }

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores en el ViewModel.
     */
    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreAlcoholemia.edit { preferences ->
                preferences.clear()
            }

            _opcionMotivo.value = ""
            _opcionErrores.value = ""
            _opcionDeseaPruebas.value = ""
            _marca.value = ""
            _modelo.value = ""
            _serie.value = ""
            _caducidad.value = ""
            _primeraTasa.value = ""
            _primeraHora.value = ""
            _segundaTasa.value = ""
            _segundaHora.value = ""
        }
    }

    /**
     * Carga los datos guardados en DataStore al inicializar el ViewModel.
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStoreAlcoholemia.data.first()

            _opcionMotivo.value = preferences[PreferencesKeys.OPCION_MOTIVO] ?: ""
            _opcionErrores.value = preferences[PreferencesKeys.OPCION_ERRORES] ?: ""
            _opcionDeseaPruebas.value = preferences[PreferencesKeys.OPCION_PRUEBAS] ?: ""
            _marca.value = preferences[PreferencesKeys.MARCA] ?: ""
            _modelo.value = preferences[PreferencesKeys.MODELO] ?: ""
            _serie.value = preferences[PreferencesKeys.SERIE] ?: ""
            _caducidad.value = preferences[PreferencesKeys.CADUCIDAD] ?: ""
            _primeraTasa.value = preferences[PreferencesKeys.PRIMERA_TASA] ?: ""
            _primeraHora.value = preferences[PreferencesKeys.PRIMERA_HORA] ?: ""
            _segundaTasa.value = preferences[PreferencesKeys.SEGUNDA_TASA] ?: ""
            _segundaHora.value = preferences[PreferencesKeys.SEGUNDA_HORA] ?: ""
            Log.d("AlcoholemiaUnoViewModel", "Datos cargados - opcionMotivo: ${_opcionMotivo.value}")
        }
    }
}