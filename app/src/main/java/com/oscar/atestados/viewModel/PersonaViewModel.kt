package com.oscar.atestados.viewModel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.screens.dataStorePer
import com.oscar.atestados.data.DniData
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PersonaViewModel : ViewModel() {

    private val _genero = MutableLiveData<String?>()
    val genero: LiveData<String?> = _genero

    private val _nacionalidad = MutableLiveData<String?>()
    val nacionalidad: LiveData<String?> = _nacionalidad

    private val _tipoDocumento = MutableLiveData<String?>()
    val tipoDocumento: LiveData<String?> = _tipoDocumento

    private val _numeroDocumento = MutableLiveData<String?>()
    val numeroDocumento: LiveData<String?> = _numeroDocumento

    private val _nombre = MutableLiveData<String?>()
    val nombre: LiveData<String?> = _nombre

    private val _apellidos = MutableLiveData<String?>()
    val apellidos: LiveData<String?> = _apellidos

    private val _nombrePadre = MutableLiveData<String?>()
    val nombrePadre: LiveData<String?> = _nombrePadre

    private val _nombreMadre = MutableLiveData<String?>()
    val nombreMadre: LiveData<String?> = _nombreMadre

    private val _fechaNacimiento = MutableLiveData<String?>()
    val fechaNacimiento: LiveData<String?> = _fechaNacimiento

    private val _lugarNacimiento = MutableLiveData<String?>()
    val lugarNacimiento: LiveData<String?> = _lugarNacimiento

    private val _domicilio = MutableLiveData<String?>()
    val domicilio: LiveData<String?> = _domicilio

    private val _codigoPostal = MutableLiveData<String?>()
    val codigoPostal: LiveData<String?> = _codigoPostal

    private val _telefono = MutableLiveData<String?>()
    val telefono: LiveData<String?> = _telefono

    private val _email = MutableLiveData<String?>()
    val email: LiveData<String?> = _email

    private val _codigoCan = MutableLiveData<String?>(null)
    val codigoCan: LiveData<String?> = _codigoCan

    private val _uid = MutableLiveData<String?>()
    val uid: LiveData<String?> = _uid

    object PersonaKeys {
        val GENERO = stringPreferencesKey("genero")
        val NACIONALIDAD = stringPreferencesKey("nacionalidad")
        val TIPO_DOCUMENTO = stringPreferencesKey("tipo_documento")
        val NUMERO_DOCUMENTO = stringPreferencesKey("numero_documento")
        val NOMBRE = stringPreferencesKey("nombre")
        val APELLIDOS = stringPreferencesKey("apellidos")
        val NOMBRE_PADRE = stringPreferencesKey("nombre_padre")
        val NOMBRE_MADRE = stringPreferencesKey("nombre_madre")
        val FECHA_NACIMIENTO = stringPreferencesKey("fecha_nacimiento")
        val LUGAR_NACIMIENTO = stringPreferencesKey("lugar_nacimiento")
        val DOMICILIO = stringPreferencesKey("domicilio")
        val CODIGO_POSTAL = stringPreferencesKey("codigo_postal")
        val TELEFONO = stringPreferencesKey("telefono")
        val EMAIL = stringPreferencesKey("email")
        val CODIGO_CAN = stringPreferencesKey("codigo_can")
        val UID = stringPreferencesKey("uid")
    }

    fun updateGenero(valor: String?) { _genero.value = valor }
    fun updateNacionalidad(valor: String?) { _nacionalidad.value = valor }
    fun updateTipoDocumento(valor: String?) { _tipoDocumento.value = valor }
    fun updateNumeroDocumento(valor: String?) { _numeroDocumento.value = valor }
    fun updateNombre(valor: String?) { _nombre.value = valor }
    fun updateApellidos(valor: String?) { _apellidos.value = valor }
    fun updateNombrePadre(valor: String?) { _nombrePadre.value = valor }
    fun updateNombreMadre(valor: String?) { _nombreMadre.value = valor }
    fun updateFechaNacimiento(valor: String?) { _fechaNacimiento.value = valor }
    fun updateLugarNacimiento(valor: String?) { _lugarNacimiento.value = valor }
    fun updateDomicilio(valor: String?) { _domicilio.value = valor }
    fun updateCodigoPostal(valor: String?) { _codigoPostal.value = valor }
    fun updateTelefono(valor: String?) { _telefono.value = valor }
    fun updateEmail(valor: String?) { _email.value = valor }
    fun updateCodigoCan(valor: String?) {
        _codigoCan.value = valor
        Log.d("PersonaViewModel", "Código CAN actualizado: $valor")
    }
    fun clearCodigoCan() {
        _codigoCan.value = null
        Log.d("PersonaViewModel", "Código CAN limpiado")
    }

    fun updateFromDniData(dniData: DniData) {
        _genero.value = dniData.genero ?: "Masculino"
        _nacionalidad.value = dniData.nacionalidad ?: "España"
        _tipoDocumento.value = dniData.tipoDocumento ?: "DNI"
        _numeroDocumento.value = dniData.numeroDocumento ?: ""
        _nombre.value = dniData.nombre ?: ""
        _apellidos.value = dniData.apellidos ?: ""
        _nombrePadre.value = dniData.nombrePadre ?: ""
        _nombreMadre.value = dniData.nombreMadre ?: ""
        _fechaNacimiento.value = dniData.fechaNacimiento ?: ""
        _lugarNacimiento.value = dniData.lugarNacimiento ?: ""
        _domicilio.value = dniData.domicilio ?: ""
        _uid.value = dniData.uid
        Log.d("PersonaViewModel", "Datos actualizados desde DniData: $dniData")
    }

    fun saveData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStorePer
            dataStore.edit { preferences ->
                preferences[PersonaKeys.GENERO] = _genero.value ?: ""
                preferences[PersonaKeys.NACIONALIDAD] = _nacionalidad.value ?: ""
                preferences[PersonaKeys.TIPO_DOCUMENTO] = _tipoDocumento.value ?: ""
                preferences[PersonaKeys.NUMERO_DOCUMENTO] = _numeroDocumento.value ?: ""
                preferences[PersonaKeys.NOMBRE] = _nombre.value ?: ""
                preferences[PersonaKeys.APELLIDOS] = _apellidos.value ?: ""
                preferences[PersonaKeys.NOMBRE_PADRE] = _nombrePadre.value ?: ""
                preferences[PersonaKeys.NOMBRE_MADRE] = _nombreMadre.value ?: ""
                preferences[PersonaKeys.FECHA_NACIMIENTO] = _fechaNacimiento.value ?: ""
                preferences[PersonaKeys.LUGAR_NACIMIENTO] = _lugarNacimiento.value ?: ""
                preferences[PersonaKeys.DOMICILIO] = _domicilio.value ?: ""
                preferences[PersonaKeys.CODIGO_POSTAL] = _codigoPostal.value ?: ""
                preferences[PersonaKeys.TELEFONO] = _telefono.value ?: ""
                preferences[PersonaKeys.EMAIL] = _email.value ?: ""
                preferences[PersonaKeys.CODIGO_CAN] = _codigoCan.value ?: ""
                preferences[PersonaKeys.UID] = _uid.value ?: ""

                Log.d("PersonaViewModel", "Número de documento guardado: ${_numeroDocumento.value}")
            }
        }
    }

    fun saveCodigoCan(context: Context, codigoCan: String) {
        viewModelScope.launch {
            context.dataStorePer.edit { preferences ->
                preferences[PersonaKeys.CODIGO_CAN] = codigoCan
            }
            _codigoCan.value = codigoCan
            Log.d("PersonaViewModel", "Código CAN guardado: $codigoCan")
        }
    }

    fun clearData(context: Context) {
        viewModelScope.launch {
            context.dataStorePer.edit { preferences ->
                preferences.clear()
            }
            _genero.value = null
            _nacionalidad.value = null
            _tipoDocumento.value = null
            _numeroDocumento.value = null
            _nombre.value = null
            _apellidos.value = null
            _nombrePadre.value = null
            _nombreMadre.value = null
            _fechaNacimiento.value = null
            _lugarNacimiento.value = null
            _domicilio.value = null
            _codigoPostal.value = null
            _telefono.value = null
            _email.value = null
            _codigoCan.value = null
            _uid.value = null
        }
    }

    fun clearCodigoCan(context: Context) {
        viewModelScope.launch {
            context.dataStorePer.edit { preferences ->
                preferences.remove(PersonaKeys.CODIGO_CAN)
            }
            _codigoCan.value = null
            Log.d("PersonaViewModel", "Código CAN limpiado de DataStore")
        }
    }

    fun loadData(context: Context) {
        viewModelScope.launch {
            try {
                val preferences = context.dataStorePer.data.firstOrNull()
                if (preferences == null) {
                    Log.e("PersonaViewModel", "No se encontraron datos en DataStore")
                    return@launch
                }
                _genero.value = preferences[PersonaKeys.GENERO]
                _nacionalidad.value = preferences[PersonaKeys.NACIONALIDAD]
                _tipoDocumento.value = preferences[PersonaKeys.TIPO_DOCUMENTO]
                _numeroDocumento.value = preferences[PersonaKeys.NUMERO_DOCUMENTO]
                _nombre.value = preferences[PersonaKeys.NOMBRE]
                _apellidos.value = preferences[PersonaKeys.APELLIDOS]
                _nombrePadre.value = preferences[PersonaKeys.NOMBRE_PADRE]
                _nombreMadre.value = preferences[PersonaKeys.NOMBRE_MADRE]
                _fechaNacimiento.value = preferences[PersonaKeys.FECHA_NACIMIENTO]
                _lugarNacimiento.value = preferences[PersonaKeys.LUGAR_NACIMIENTO]
                _domicilio.value = preferences[PersonaKeys.DOMICILIO]
                _codigoPostal.value = preferences[PersonaKeys.CODIGO_POSTAL]
                _telefono.value = preferences[PersonaKeys.TELEFONO]
                _email.value = preferences[PersonaKeys.EMAIL]
                _codigoCan.value = preferences[PersonaKeys.CODIGO_CAN]
                _uid.value = preferences[PersonaKeys.UID]
                Log.d("PersonaViewModel", "Datos cargados - numeroDocumento: ${_numeroDocumento.value}, canCode: ${_codigoCan.value}")
            } catch (e: Exception) {
                Log.e("PersonaViewModel", "Error cargando datos de DataStore: ${e.message}", e)
            }
        }
    }

    fun loadCodigoCan(context: Context) {
        viewModelScope.launch {
            val codigoCan = context.dataStorePer.data
                .map { preferences -> preferences[PersonaKeys.CODIGO_CAN] }
                .firstOrNull()
            _codigoCan.value = codigoCan
            Log.d("PersonaViewModel", "Código CAN cargado: $codigoCan")
        }
    }
}