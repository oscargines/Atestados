package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

val Context.dataStoreAlcoholemiaDos: DataStore<Preferences> by preferencesDataStore(name = "alcoholemia_dos_settings")

class AlcoholemiaDosViewModel(application: Application) : AndroidViewModel(application) {
    // Claves para DataStore
    private object PreferencesKeys {
        val FECHA_INICIO = stringPreferencesKey("fecha_inicio")
        val HORA_INICIO = stringPreferencesKey("hora_inicio")
        val LUGAR_COINCIDE = booleanPreferencesKey("lugar_coincide")
        val LUGAR_DILIGENCIAS = stringPreferencesKey("lugar_diligencias")
        val DESEA_FIRMAR = booleanPreferencesKey("desea_firmar")
        val INMOVILIZA_VEHICULO = booleanPreferencesKey("inmoviliza_vehiculo")
        val HAY_SEGUNDO_CONDUCTOR = booleanPreferencesKey("hay_segundo_conductor")
        val NOMBRE_SEGUNDO_CONDUCTOR = stringPreferencesKey("nombre_segundo_conductor")
    }

    // Fecha y hora de inicio
    private val _fechaInicio = MutableLiveData<String>("")
    val fechaInicio: LiveData<String> = _fechaInicio

    private val _horaInicio = MutableLiveData<String>("")
    val horaInicio: LiveData<String> = _horaInicio

    // Lugar de la investigación
    private val _lugarCoincide = MutableLiveData<Boolean>(false)
    val lugarCoincide: LiveData<Boolean> = _lugarCoincide

    private val _lugarDiligencias = MutableLiveData<String>("")
    val lugarDiligencias: LiveData<String> = _lugarDiligencias

    // Opciones de firma y vehículo
    private val _deseaFirmar = MutableLiveData<Boolean>(false)
    val deseaFirmar: LiveData<Boolean> = _deseaFirmar

    private val _inmovilizaVehiculo = MutableLiveData<Boolean>(false)
    val inmovilizaVehiculo: LiveData<Boolean> = _inmovilizaVehiculo

    private val _haySegundoConductor = MutableLiveData<Boolean>(false)
    val haySegundoConductor: LiveData<Boolean> = _haySegundoConductor

    private val _nombreSegundoConductor = MutableLiveData<String>("")
    val nombreSegundoConductor: LiveData<String> = _nombreSegundoConductor

    // Firmas de intervinientes
    private val _firmaInvestigado = MutableLiveData<Bitmap?>()
    val firmaInvestigado: LiveData<Bitmap?> = _firmaInvestigado

    private val _firmaSegundoConductor = MutableLiveData<Bitmap?>()
    val firmaSegundoConductor: LiveData<Bitmap?> = _firmaSegundoConductor

    private val _firmaInstructor = MutableLiveData<Bitmap?>()
    val firmaInstructor: LiveData<Bitmap?> = _firmaInstructor

    private val _firmaSecretario = MutableLiveData<Bitmap?>()
    val firmaSecretario: LiveData<Bitmap?> = _firmaSecretario

    init {
        loadSavedData()
    }

    // Funciones para actualizar datos
    fun updateFechaInicio(value: String) {
        if (value.isNotEmpty()) {
            _fechaInicio.value = value
        }
    }

    fun updateHoraInicio(value: String) {
        if (value.isNotEmpty()) {
            _horaInicio.value = value
        }
    }

    fun updateLugarCoincide(value: Boolean) {
        _lugarCoincide.value = value
    }

    fun updateLugarDiligencias(value: String) {
        if (value.isNotEmpty()) {
            _lugarDiligencias.value = value
        }
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
        if (value.isNotEmpty()) {
            _nombreSegundoConductor.value = value
        }
    }
    fun updateFirmaInvestigado(bitmap: Bitmap) {
        _firmaInvestigado.value = bitmap
    }

    fun updateFirmaSegundoConductor(bitmap: Bitmap) {
        _firmaSegundoConductor.value = bitmap
    }

    fun updateFirmaInstructor(bitmap: Bitmap) {
        _firmaInstructor.value = bitmap
    }

    fun updateFirmaSecretario(bitmap: Bitmap) {
        _firmaSecretario.value = bitmap
    }

    // Guardar y limpiar datos
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
        }
    }

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
    private val FIRMA_INVESTIGADO = stringPreferencesKey("firma_investigado")
    private val FIRMA_SEGUNDO_CONDUCTOR = stringPreferencesKey("firma_segundo_conductor")
    private val FIRMA_INSTRUCTOR = stringPreferencesKey("firma_instructor")
    private val FIRMA_SECRETARIO = stringPreferencesKey("firma_secretario")

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