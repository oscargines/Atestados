package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
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

val Context.dataStoreAlcoholemiaDos: DataStore<Preferences> by preferencesDataStore(name = "alcoholemia_dos_settings")

class AlcoholemiaDosViewModel(application: Application) : AndroidViewModel(application) {

    private object PreferencesKeys {
        val FECHA_INICIO = stringPreferencesKey("fecha_inicio")
        val HORA_INICIO = stringPreferencesKey("hora_inicio")
        val LUGAR_COINCIDE = booleanPreferencesKey("lugar_coincide")
        val LUGAR_DILIGENCIAS = stringPreferencesKey("lugar_diligencias")
        val DESEA_FIRMAR = booleanPreferencesKey("desea_firmar")
        val INMOVILIZA_VEHICULO = booleanPreferencesKey("inmoviliza_vehiculo")
        val HAY_SEGUNDO_CONDUCTOR = booleanPreferencesKey("hay_segundo_conductor")
        val NOMBRE_SEGUNDO_CONDUCTOR = stringPreferencesKey("nombre_segundo_conductor")
        val PARTIDO_JUDICIAL = stringPreferencesKey("partido_judicial")
        val LATITUD = stringPreferencesKey("latitud")
        val LONGITUD = stringPreferencesKey("longitud")
        val MUNICIPIO = stringPreferencesKey("municipio")
        val FIRMA_INVESTIGADO = stringPreferencesKey("firma_investigado")
        val FIRMA_SEGUNDO_CONDUCTOR = stringPreferencesKey("firma_segundo_conductor")
        val FIRMA_INSTRUCTOR = stringPreferencesKey("firma_instructor")
        val FIRMA_SECRETARIO = stringPreferencesKey("firma_secretario")
    }

    private val _fechaInicio = MutableLiveData<String>("")
    val fechaInicio: LiveData<String> = _fechaInicio

    private val _horaInicio = MutableLiveData<String>("")
    val horaInicio: LiveData<String> = _horaInicio

    private val _lugarCoincide = MutableLiveData<Boolean>(false)
    val lugarCoincide: LiveData<Boolean> = _lugarCoincide

    private val _lugarDiligencias = MutableLiveData<String>("")
    val lugarDiligencias: LiveData<String> = _lugarDiligencias

    private val _deseaFirmar = MutableLiveData<Boolean>(false)
    val deseaFirmar: LiveData<Boolean> = _deseaFirmar

    private val _inmovilizaVehiculo = MutableLiveData<Boolean>(false)
    val inmovilizaVehiculo: LiveData<Boolean> = _inmovilizaVehiculo

    private val _haySegundoConductor = MutableLiveData<Boolean>(false)
    val haySegundoConductor: LiveData<Boolean> = _haySegundoConductor

    private val _nombreSegundoConductor = MutableLiveData<String>("")
    val nombreSegundoConductor: LiveData<String> = _nombreSegundoConductor

    private val _partidoJudicial = MutableLiveData<String>("")
    val partidoJudicial: LiveData<String> = _partidoJudicial

    private val _latitud = MutableLiveData<String>("")
    val latitud: LiveData<String> = _latitud

    private val _longitud = MutableLiveData<String>("")
    val longitud: LiveData<String> = _longitud

    private val _municipio = MutableLiveData<String>("")
    val municipio: LiveData<String> = _municipio

    private val _firmaInvestigado = MutableLiveData<String?>()
    val firmaInvestigado: LiveData<String?> = _firmaInvestigado

    private val _firmaSegundoConductor = MutableLiveData<String?>()
    val firmaSegundoConductor: LiveData<String?> = _firmaSegundoConductor

    private val _firmaInstructor = MutableLiveData<String?>()
    val firmaInstructor: LiveData<String?> = _firmaInstructor

    private val _firmaSecretario = MutableLiveData<String?>()
    val firmaSecretario: LiveData<String?> = _firmaSecretario

    init {
        loadSavedData()
    }

    fun updateFechaInicio(value: String) {
        if (value.isNotEmpty()) _fechaInicio.value = value
    }

    fun updateHoraInicio(value: String) {
        if (value.isNotEmpty()) _horaInicio.value = value
    }

    fun updateLugarCoincide(value: Boolean) {
        _lugarCoincide.value = value
    }

    fun updateLugarDiligencias(value: String) {
        if (value.isNotEmpty()) _lugarDiligencias.value = value
    }

    fun updateDeseaFirmar(value: Boolean) {
        _deseaFirmar.value = value
    }

    fun updateInmovilizaVehiculo(value: Boolean) {
        _inmovilizaVehiculo.value = value
    }

    fun updateHaySegundoConductor(value: Boolean) {
        _haySegundoConductor.value = value
    }

    fun updateNombreSegundoConductor(value: String) {
        if (value.isNotEmpty()) _nombreSegundoConductor.value = value
    }

    fun updatePartidoJudicial(value: String) {
        if (value.isNotEmpty()) _partidoJudicial.value = value
    }

    fun updateLatitud(value: String) {
        _latitud.value = value
    }

    fun updateLongitud(value: String) {
        _longitud.value = value
    }

    fun updateMunicipio(value: String) {
        if (value.isNotEmpty()) _municipio.value = value
    }

    // Métodos actualizados para manejar rutas de archivo
    fun updateFirmaInvestigado(filePath: String?) {
        _firmaInvestigado.value = filePath
        Log.d("AlcoholemiaDosViewModel", "Firma investigado actualizada: $filePath")
    }

    fun updateFirmaSegundoConductor(filePath: String?) {
        _firmaSegundoConductor.value = filePath
        Log.d("AlcoholemiaDosViewModel", "Firma Segundo Conductor actualizada: $filePath")
    }

    fun updateFirmaInstructor(filePath: String?) {
        _firmaInstructor.value = filePath
        Log.d("AlcoholemiaDosViewModel", "Firma instructor actualizada: $filePath")
    }

    fun updateFirmaSecretario(filePath: String?) {
        _firmaSecretario.value = filePath
        Log.d("AlcoholemiaDosViewModel", "Firma secretario actualizada: $filePath")
    }

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
                _partidoJudicial.value?.let { preferences[PreferencesKeys.PARTIDO_JUDICIAL] = it }
                _latitud.value?.let { preferences[PreferencesKeys.LATITUD] = it }
                _longitud.value?.let { preferences[PreferencesKeys.LONGITUD] = it }
                _municipio.value?.let { preferences[PreferencesKeys.MUNICIPIO] = it }
                _firmaInvestigado.value?.let { preferences[PreferencesKeys.FIRMA_INVESTIGADO] = it }
                _firmaSegundoConductor.value?.let { preferences[PreferencesKeys.FIRMA_SEGUNDO_CONDUCTOR] = it }
                _firmaInstructor.value?.let { preferences[PreferencesKeys.FIRMA_INSTRUCTOR] = it }
                _firmaSecretario.value?.let { preferences[PreferencesKeys.FIRMA_SECRETARIO] = it }
            }
        }
    }

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
            _partidoJudicial.value = ""
            _latitud.value = ""
            _longitud.value = ""
            _municipio.value = ""
        }
    }

    fun loadSavedData() {
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
            _partidoJudicial.value = preferences[PreferencesKeys.PARTIDO_JUDICIAL] ?: "Partido judicial no disponible"
            _latitud.value = preferences[PreferencesKeys.LATITUD] ?: ""
            _longitud.value = preferences[PreferencesKeys.LONGITUD] ?: ""
            _municipio.value = preferences[PreferencesKeys.MUNICIPIO] ?: ""
            _firmaInvestigado.value = preferences[PreferencesKeys.FIRMA_INVESTIGADO]
            _firmaSegundoConductor.value = preferences[PreferencesKeys.FIRMA_SEGUNDO_CONDUCTOR]
            _firmaInstructor.value = preferences[PreferencesKeys.FIRMA_INSTRUCTOR]
            _firmaSecretario.value = preferences[PreferencesKeys.FIRMA_SECRETARIO]
        }
    }
}