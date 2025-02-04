package com.oscar.atestados.viewModel

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.screens.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersonaViewModel : ViewModel() {

    private val _genero = MutableLiveData<String>()
    val genero: LiveData<String> = _genero

    private val _nacionalidad = MutableLiveData<String>()
    val nacionalidad: LiveData<String> = _nacionalidad

    private val _tipoDocumento = MutableLiveData<String>()
    val tipoDocumento: LiveData<String> = _tipoDocumento

    private val _numeroDocumento = MutableLiveData<String>()
    val numeroDocumento: LiveData<String> = _numeroDocumento

    private val _nombre = MutableLiveData<String>()
    val nombre: LiveData<String> = _nombre

    private val _apellidos = MutableLiveData<String>()
    val apellidos: LiveData<String> = _apellidos

    private val _nombrePadre = MutableLiveData<String>()
    val nombrePadre: LiveData<String> = _nombrePadre

    private val _nombreMadre = MutableLiveData<String>()
    val nombreMadre: LiveData<String> = _nombreMadre

    private val _fechaNacimiento = MutableLiveData<String>()
    val fechaNacimiento: LiveData<String> = _fechaNacimiento

    private val _lugarNacimiento = MutableLiveData<String>()
    val lugarNacimiento: LiveData<String> = _lugarNacimiento

    private val _domicilio = MutableLiveData<String>()
    val domicilio: LiveData<String> = _domicilio

    private val _codigoPostal = MutableLiveData<String>()
    val codigoPostal: LiveData<String> = _codigoPostal

    private val _telefono = MutableLiveData<String>()
    val telefono: LiveData<String> = _telefono

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

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
    }

    // Funciones para actualizar valores
    fun updateGenero(valor: String) {
        _genero.value = valor
    }

    fun updateNacionalidad(valor: String) {
        _nacionalidad.value = valor
    }

    fun updateTipoDocumento(valor: String) {
        _tipoDocumento.value = valor
    }

    fun updateNumeroDocumento(valor: String) {
        _numeroDocumento.value = valor
    }

    fun updateNombre(valor: String) {
        _nombre.value = valor
    }

    fun updateApellidos(valor: String) {
        _apellidos.value = valor
    }

    fun updateNombrePadre(valor: String) {
        _nombrePadre.value = valor
    }

    fun updateNombreMadre(valor: String) {
        _nombreMadre.value = valor
    }

    fun updateFechaNacimiento(valor: String) {
        _fechaNacimiento.value = valor
    }

    fun updateLugarNacimiento(valor: String) {
        _lugarNacimiento.value = valor
    }

    fun updateDomicilio(valor: String) {
        _domicilio.value = valor
    }

    fun updateCodigoPostal(valor: String) {
        _codigoPostal.value = valor
    }

    fun updateTelefono(valor: String) {
        _telefono.value = valor
    }

    fun updateEmail(valor: String) {
        _email.value = valor
    }

    // Función para guardar los datos en Preferences DataStore
    fun saveData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStore

            // Verificar si hay datos para guardar
            val hasDataToSave =
                _genero.value.isNullOrBlank() &&
                        _nacionalidad.value.isNullOrBlank() &&
                        _tipoDocumento.value.isNullOrBlank() &&
                        _numeroDocumento.value.isNullOrBlank() &&
                        _nombre.value.isNullOrBlank() &&
                        _apellidos.value.isNullOrBlank() &&
                        _nombrePadre.value.isNullOrBlank() &&
                        _nombreMadre.value.isNullOrBlank() &&
                        _fechaNacimiento.value.isNullOrBlank() &&
                        _lugarNacimiento.value.isNullOrBlank() &&
                        _domicilio.value.isNullOrBlank() &&
                        _codigoPostal.value.isNullOrBlank() &&
                        _telefono.value.isNullOrBlank() &&
                        _email.value.isNullOrBlank()

            if (hasDataToSave) {
                // Mostrar alerta si no hay datos
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(context)
                        .setTitle("Advertencia")
                        .setMessage("No se han introducido datos para guardar.")
                        .setPositiveButton("Aceptar", null)
                        .show()
                }
            } else {
                // Guardar los datos en el DataStore
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
                }

                // Mostrar un Toast cuando los datos se guarden correctamente
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Datos guardados correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Función para limpiar los datos
    fun clearData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStore
            dataStore.edit { preferences ->
                preferences.clear() // Limpia todas las preferencias
            }
        }
        // También limpia los LiveData
        _genero.value = ""
        _nacionalidad.value = ""
        _tipoDocumento.value = ""
        _numeroDocumento.value = ""
        _nombre.value = ""
        _apellidos.value = ""
        _nombrePadre.value = ""
        _nombreMadre.value = ""
        _fechaNacimiento.value = ""
        _lugarNacimiento.value = ""
        _domicilio.value = ""
        _codigoPostal.value = ""
        _telefono.value = ""
        _email.value = ""
    }

    // Función para cargar todos los datos
    fun loadData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStore

            // Leer todos los datos del DataStore
            dataStore.data.collect { preferences ->
                _genero.value = preferences[PersonaKeys.GENERO] ?: ""
                _nacionalidad.value = preferences[PersonaKeys.NACIONALIDAD] ?: ""
                _tipoDocumento.value = preferences[PersonaKeys.TIPO_DOCUMENTO] ?: ""
                _numeroDocumento.value = preferences[PersonaKeys.NUMERO_DOCUMENTO] ?: ""
                _nombre.value = preferences[PersonaKeys.NOMBRE] ?: ""
                _apellidos.value = preferences[PersonaKeys.APELLIDOS] ?: ""
                _nombrePadre.value = preferences[PersonaKeys.NOMBRE_PADRE] ?: ""
                _nombreMadre.value = preferences[PersonaKeys.NOMBRE_MADRE] ?: ""
                _fechaNacimiento.value = preferences[PersonaKeys.FECHA_NACIMIENTO] ?: ""
                _lugarNacimiento.value = preferences[PersonaKeys.LUGAR_NACIMIENTO] ?: ""
                _domicilio.value = preferences[PersonaKeys.DOMICILIO] ?: ""
                _codigoPostal.value = preferences[PersonaKeys.CODIGO_POSTAL] ?: ""
                _telefono.value = preferences[PersonaKeys.TELEFONO] ?: ""
                _email.value = preferences[PersonaKeys.EMAIL] ?: ""
            }
        }
    }

}
