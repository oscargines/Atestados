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

val Context.dataStoreDerechos: DataStore<Preferences> by preferencesDataStore(name = "lectura_derechos_settings")

class LecturaDerechosViewModel(application: Application) : AndroidViewModel(application) {
    // Claves para DataStore
    private object PreferencesKeys {
        val MOMENTO_LECTURA = stringPreferencesKey("momento_lectura")
        val LUGAR_INVESTIGACION = stringPreferencesKey("lugar_investigacion")
        val LUGAR_DELITO = stringPreferencesKey("lugar_delito")
        val RESUMEN_HECHOS = stringPreferencesKey("resumen_hechos")
        val CALIFICACION_HECHOS = stringPreferencesKey("calificacion_hechos")
        val RELACION_INDICIOS = stringPreferencesKey("relacion_indicios")
    }

    private val _momentoLectura = MutableLiveData("Tomada en el momento")
    val momentoLectura: LiveData<String> = _momentoLectura

    private val _lugarInvestigacion = MutableLiveData("")
    val lugarInvestigacion: LiveData<String> = _lugarInvestigacion

    private val _lugarDelito = MutableLiveData("")
    val lugarDelito: LiveData<String> = _lugarDelito

    private val _resumenHechos = MutableLiveData("")
    val resumenHechos: LiveData<String> = _resumenHechos

    private val _calificacionHechos = MutableLiveData("")
    val calificacionHechos: LiveData<String> = _calificacionHechos

    private val _relacionIndicios = MutableLiveData("")
    val relacionIndicios: LiveData<String> = _relacionIndicios

    init {
        loadSavedData()
    }

    fun setMomentoLectura(momento: String) {
        _momentoLectura.value = momento
    }

    fun updateLugarInvestigacion(lugar: String) {
        _lugarInvestigacion.value = lugar
    }

    fun updateLugarDelito(lugar: String) {
        _lugarDelito.value = lugar
    }

    fun updateResumenHechos(resumen: String) {
        _resumenHechos.value = resumen
    }

    fun updateCalificacionHechos(calificacion: String) {
        _calificacionHechos.value = calificacion
    }

    fun updateRelacionIndicios(indicios: String) {
        _relacionIndicios.value = indicios
    }

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