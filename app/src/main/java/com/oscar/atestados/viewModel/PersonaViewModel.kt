package com.oscar.atestados.viewModel

import android.app.AlertDialog
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
import com.oscar.atestados.utils.DniData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestionar los datos personales de un individuo.
 * Maneja la persistencia de datos utilizando DataStore y expone los datos a través de LiveData.
 */
class PersonaViewModel : ViewModel() {

    // LiveData declarations...
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

    private val _codigoCan = MutableLiveData<String?>(null)
    val codigoCan: LiveData<String?> = _codigoCan

    /**
     * Objeto que contiene las claves para almacenar los datos en DataStore.
     * Cada clave corresponde a un campo específico de información personal.
     */
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

    fun updateCodigoCan(valor: String?) {
        _codigoCan.value = valor
        Log.d("PersonaViewModel", "Código CAN actualizado: $valor")
    }

    fun clearCodigoCan() {
        _codigoCan.value = null
        Log.d("PersonaViewModel", "Código CAN limpiado")
    }

    // Nuevo método para actualizar todos los datos desde DniData
    fun updateFromDniData(dniData: DniData) {
        _tipoDocumento.value = "DNI"
        _numeroDocumento.value = dniData.numeroDocumento
        _nombre.value = dniData.nombre
        _apellidos.value = dniData.apellidos
        _fechaNacimiento.value = dniData.fechaNacimiento
        _genero.value = dniData.sexo
        _nacionalidad.value = dniData.nacionalidad
        _nombrePadre.value = dniData.nombrePadre
        _nombreMadre.value = dniData.nombreMadre
        _lugarNacimiento.value = dniData.lugarNacimiento
        _domicilio.value = dniData.domicilio
        Log.d("PersonaViewModel", "Datos actualizados desde DniData: $dniData")
    }

    /**
     * Guarda todos los datos personales en el DataStore.
     * Muestra una alerta si no hay datos para guardar o un Toast de confirmación si el guardado es exitoso.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore
     */
    fun saveData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStorePer

            // Verificar si hay datos para guardar
            val hasEmptyRequiredField =
                _genero.value.isNullOrBlank() ||
                        _nacionalidad.value.isNullOrBlank() ||
                        _tipoDocumento.value.isNullOrBlank() ||
                        _numeroDocumento.value.isNullOrBlank() ||
                        _nombre.value.isNullOrBlank() ||
                        _apellidos.value.isNullOrBlank() ||
                        _nombrePadre.value.isNullOrBlank() ||
                        _nombreMadre.value.isNullOrBlank() ||
                        _fechaNacimiento.value.isNullOrBlank() ||
                        _lugarNacimiento.value.isNullOrBlank() ||
                        _domicilio.value.isNullOrBlank()

            if (hasEmptyRequiredField) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(context)
                        .setTitle("Datos de la Persona")
                        .setMessage(
                            "No se han introducido los datos necesarios para guardar " +
                                    "y poder confeccionar el atestado. Vuelva a acceder a la pantalla" +
                                    " e introduzca lo necesario para su correcta confección.\n" +
                                    "No olvide guardar los datos."
                        )
                        .setPositiveButton("Aceptar", null)
                        .show()
                }
            } else {
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
                }

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

    fun saveCodigoCan(context: Context, codigoCan: String) {
        viewModelScope.launch {
            context.dataStorePer.edit { preferences ->
                preferences[PersonaKeys.CODIGO_CAN] = codigoCan
            }
            _codigoCan.value = codigoCan
            Log.d("PersonaViewModel", "Código CAN guardado en DataStore: $codigoCan")
        }
    }

    fun clearData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStorePer
            dataStore.edit { preferences ->
                preferences.clear()
            }
        }
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
        _codigoCan.value = ""
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
            val dataStore = context.dataStorePer
            val preferences = dataStore.data.firstOrNull()
            _genero.value = preferences?.get(PersonaKeys.GENERO) ?: ""
            _nacionalidad.value = preferences?.get(PersonaKeys.NACIONALIDAD) ?: ""
            _tipoDocumento.value = preferences?.get(PersonaKeys.TIPO_DOCUMENTO) ?: ""
            _numeroDocumento.value = preferences?.get(PersonaKeys.NUMERO_DOCUMENTO) ?: ""
            _nombre.value = preferences?.get(PersonaKeys.NOMBRE) ?: ""
            _apellidos.value = preferences?.get(PersonaKeys.APELLIDOS) ?: ""
            _nombrePadre.value = preferences?.get(PersonaKeys.NOMBRE_PADRE) ?: ""
            _nombreMadre.value = preferences?.get(PersonaKeys.NOMBRE_MADRE) ?: ""
            _fechaNacimiento.value = preferences?.get(PersonaKeys.FECHA_NACIMIENTO) ?: ""
            _lugarNacimiento.value = preferences?.get(PersonaKeys.LUGAR_NACIMIENTO) ?: ""
            _domicilio.value = preferences?.get(PersonaKeys.DOMICILIO) ?: ""
            _codigoPostal.value = preferences?.get(PersonaKeys.CODIGO_POSTAL) ?: ""
            _telefono.value = preferences?.get(PersonaKeys.TELEFONO) ?: ""
            _email.value = preferences?.get(PersonaKeys.EMAIL) ?: ""
            _codigoCan.value = preferences?.get(PersonaKeys.CODIGO_CAN) ?: ""
            Log.d("PersonaViewModel", "Datos cargados desde DataStore - canCode: ${_codigoCan.value}")
        }
    }

    fun loadCodigoCan(context: Context) {
        viewModelScope.launch {
            val codigoCan = context.dataStorePer.data
                .map { preferences -> preferences[PersonaKeys.CODIGO_CAN] }
                .firstOrNull()
            _codigoCan.value = codigoCan
            Log.d("PersonaViewModel", "Código CAN cargado desde DataStore: $codigoCan")
        }
    }
}