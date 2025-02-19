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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alcoholemia_uno_settings")

class AlcoholemiaUnoViewModel(application: Application) : AndroidViewModel(application) {
    // Claves para DataStore
    private object PreferencesKeys {
        val OPCION_MOTIVO = stringPreferencesKey("opcion_motivo")
        val OPCION_ERRORES = stringPreferencesKey("opcion_errores")
        val OPCION_PRUEBAS = stringPreferencesKey("opcion_pruebas")
        val MARCA = stringPreferencesKey("marca")
        val MODELO = stringPreferencesKey("modelo")
        val SERIE = stringPreferencesKey("serie")
        val CADUCIDAD = stringPreferencesKey("caducidad")
        val PRIMERA_TASA = stringPreferencesKey("primera_tasa")
        val PRIMERA_HORA = stringPreferencesKey("primera_hora")
        val SEGUNDA_TASA = stringPreferencesKey("segunda_tasa")
        val SEGUNDA_HORA = stringPreferencesKey("segunda_hora")
    }
    // Opciones del motivo por el que se instruyen diligencias
    private val _opcionMotivo = MutableLiveData<String>()
    val opcionMotivo: LiveData<String> = _opcionMotivo

    // Opciones de errores permitidos
    private val _opcionErrores = MutableLiveData<String>()
    val opcionErrores: LiveData<String> = _opcionErrores

    // Opciones de si desea pruebas
    private val _opcionDeseaPruebas = MutableLiveData<String>()
    val opcionDeseaPruebas: LiveData<String> = _opcionDeseaPruebas

    // Campos de texto
    private val _marca = MutableLiveData<String>()
    val marca: LiveData<String> = _marca

    private val _modelo = MutableLiveData<String>()
    val modelo: LiveData<String> = _modelo

    private val _serie = MutableLiveData<String>()
    val serie: LiveData<String> = _serie

    private val _caducidad = MutableLiveData<String>()
    val caducidad: LiveData<String> = _caducidad

    // Primera prueba
    private val _primeraTasa = MutableLiveData<String>()
    val primeraTasa: LiveData<String> = _primeraTasa

    private val _primeraHora = MutableLiveData<String>()
    val primeraHora: LiveData<String> = _primeraHora

    // Segunda prueba
    private val _segundaTasa = MutableLiveData<String>()
    val segundaTasa: LiveData<String> = _segundaTasa

    private val _segundaHora = MutableLiveData<String>()
    val segundaHora: LiveData<String> = _segundaHora

    init {
        loadSavedData()
    }

    // Funciones para actualizar opciones
    fun setOpcionMotivo(opcionMotivo: String) {
        _opcionMotivo.value = opcionMotivo
    }

    fun setOpcionErrores(opcionErrores: String) {
        _opcionErrores.value = opcionErrores
    }

    fun setOpcionDeseaPruebas(opcionDeseaPruebas: String) {
        _opcionDeseaPruebas.value = opcionDeseaPruebas
    }

    // Actualizadores de campos
    fun updateMarca(value: String) {
        _marca.value = value
    }

    fun updateModelo(value: String) {
        _modelo.value = value
    }

    fun updateSerie(value: String) {
        _serie.value = value
    }

    fun updateCaducidad(value: String) {
        _caducidad.value = value
    }

    fun updatePrimeraTasa(value: String) {
        _primeraTasa.value = value
    }

    fun updatePrimeraHora(value: String) {
        _primeraHora.value = value
    }

    fun updateSegundaTasa(value: String) {
        _segundaTasa.value = value
    }

    fun updateSegundaHora(value: String) {
        _segundaHora.value = value
    }

    // Operaciones de guardado y limpieza
    fun guardarDatos(context: Context) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
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

    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
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
    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStore.data.first()

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
        }
    }
}