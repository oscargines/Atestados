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
    /** LiveData privado para el género del individuo. */
    private val _genero = MutableLiveData<String>()
    /** LiveData público para observar el género del individuo. */
    val genero: LiveData<String> = _genero

    /** LiveData privado para la nacionalidad del individuo. */
    private val _nacionalidad = MutableLiveData<String>()
    /** LiveData público para observar la nacionalidad del individuo. */
    val nacionalidad: LiveData<String> = _nacionalidad

    /** LiveData privado para el tipo de documento del individuo. */
    private val _tipoDocumento = MutableLiveData<String>()
    /** LiveData público para observar el tipo de documento del individuo. */
    val tipoDocumento: LiveData<String> = _tipoDocumento

    /** LiveData privado para el número de documento del individuo. */
    private val _numeroDocumento = MutableLiveData<String>()
    /** LiveData público para observar el número de documento del individuo. */
    val numeroDocumento: LiveData<String> = _numeroDocumento

    /** LiveData privado para el nombre del individuo. */
    private val _nombre = MutableLiveData<String>()
    /** LiveData público para observar el nombre del individuo. */
    val nombre: LiveData<String> = _nombre

    /** LiveData privado para los apellidos del individuo. */
    private val _apellidos = MutableLiveData<String>()
    /** LiveData público para observar los apellidos del individuo. */
    val apellidos: LiveData<String> = _apellidos

    /** LiveData privado para el nombre del padre del individuo. */
    private val _nombrePadre = MutableLiveData<String>()
    /** LiveData público para observar el nombre del padre del individuo. */
    val nombrePadre: LiveData<String> = _nombrePadre

    /** LiveData privado para el nombre de la madre del individuo. */
    private val _nombreMadre = MutableLiveData<String>()
    /** LiveData público para observar el nombre de la madre del individuo. */
    val nombreMadre: LiveData<String> = _nombreMadre

    /** LiveData privado para la fecha de nacimiento del individuo. */
    private val _fechaNacimiento = MutableLiveData<String>()
    /** LiveData público para observar la fecha de nacimiento del individuo. */
    val fechaNacimiento: LiveData<String> = _fechaNacimiento

    /** LiveData privado para el lugar de nacimiento del individuo. */
    private val _lugarNacimiento = MutableLiveData<String>()
    /** LiveData público para observar el lugar de nacimiento del individuo. */
    val lugarNacimiento: LiveData<String> = _lugarNacimiento

    /** LiveData privado para el domicilio del individuo. */
    private val _domicilio = MutableLiveData<String>()
    /** LiveData público para observar el domicilio del individuo. */
    val domicilio: LiveData<String> = _domicilio

    /** LiveData privado para el código postal del individuo. */
    private val _codigoPostal = MutableLiveData<String>()
    /** LiveData público para observar el código postal del individuo. */
    val codigoPostal: LiveData<String> = _codigoPostal

    /** LiveData privado para el teléfono del individuo. */
    private val _telefono = MutableLiveData<String>()
    /** LiveData público para observar el teléfono del individuo. */
    val telefono: LiveData<String> = _telefono

    /** LiveData privado para el email del individuo. */
    private val _email = MutableLiveData<String>()
    /** LiveData público para observar el email del individuo. */
    val email: LiveData<String> = _email

    /** LiveData privado para el código CAN del individuo, puede ser nulo. */
    private val _codigoCan = MutableLiveData<String?>(null)
    /** LiveData público para observar el código CAN del individuo. */
    val codigoCan: LiveData<String?> = _codigoCan

    /**
     * Objeto que contiene las claves para almacenar los datos en DataStore.
     * Cada clave corresponde a un campo específico de información personal.
     */
    object PersonaKeys {
        /** Clave para el género del individuo. */
        val GENERO = stringPreferencesKey("genero")
        /** Clave para la nacionalidad del individuo. */
        val NACIONALIDAD = stringPreferencesKey("nacionalidad")
        /** Clave para el tipo de documento del individuo. */
        val TIPO_DOCUMENTO = stringPreferencesKey("tipo_documento")
        /** Clave para el número de documento del individuo. */
        val NUMERO_DOCUMENTO = stringPreferencesKey("numero_documento")
        /** Clave para el nombre del individuo. */
        val NOMBRE = stringPreferencesKey("nombre")
        /** Clave para los apellidos del individuo. */
        val APELLIDOS = stringPreferencesKey("apellidos")
        /** Clave para el nombre del padre del individuo. */
        val NOMBRE_PADRE = stringPreferencesKey("nombre_padre")
        /** Clave para el nombre de la madre del individuo. */
        val NOMBRE_MADRE = stringPreferencesKey("nombre_madre")
        /** Clave para la fecha de nacimiento del individuo. */
        val FECHA_NACIMIENTO = stringPreferencesKey("fecha_nacimiento")
        /** Clave para el lugar de nacimiento del individuo. */
        val LUGAR_NACIMIENTO = stringPreferencesKey("lugar_nacimiento")
        /** Clave para el domicilio del individuo. */
        val DOMICILIO = stringPreferencesKey("domicilio")
        /** Clave para el código postal del individuo. */
        val CODIGO_POSTAL = stringPreferencesKey("codigo_postal")
        /** Clave para el teléfono del individuo. */
        val TELEFONO = stringPreferencesKey("telefono")
        /** Clave para el email del individuo. */
        val EMAIL = stringPreferencesKey("email")
        /** Clave para el código CAN del individuo. */
        val CODIGO_CAN = stringPreferencesKey("codigo_can")
    }

    // Funciones para actualizar valores
    /**
     * Actualiza el género del individuo.
     *
     * @param valor Nuevo género.
     */
    fun updateGenero(valor: String) {
        _genero.value = valor
    }

    /**
     * Actualiza la nacionalidad del individuo.
     *
     * @param valor Nueva nacionalidad.
     */
    fun updateNacionalidad(valor: String) {
        _nacionalidad.value = valor
    }

    /**
     * Actualiza el tipo de documento del individuo.
     *
     * @param valor Nuevo tipo de documento.
     */
    fun updateTipoDocumento(valor: String) {
        _tipoDocumento.value = valor
    }

    /**
     * Actualiza el número de documento del individuo.
     *
     * @param valor Nuevo número de documento.
     */
    fun updateNumeroDocumento(valor: String) {
        _numeroDocumento.value = valor
    }

    /**
     * Actualiza el nombre del individuo.
     *
     * @param valor Nuevo nombre.
     */
    fun updateNombre(valor: String) {
        _nombre.value = valor
    }

    /**
     * Actualiza los apellidos del individuo.
     *
     * @param valor Nuevos apellidos.
     */
    fun updateApellidos(valor: String) {
        _apellidos.value = valor
    }

    /**
     * Actualiza el nombre del padre del individuo.
     *
     * @param valor Nuevo nombre del padre.
     */
    fun updateNombrePadre(valor: String) {
        _nombrePadre.value = valor
    }

    /**
     * Actualiza el nombre de la madre del individuo.
     *
     * @param valor Nuevo nombre de la madre.
     */
    fun updateNombreMadre(valor: String) {
        _nombreMadre.value = valor
    }

    /**
     * Actualiza la fecha de nacimiento del individuo.
     *
     * @param valor Nueva fecha de nacimiento.
     */
    fun updateFechaNacimiento(valor: String) {
        _fechaNacimiento.value = valor
    }

    /**
     * Actualiza el lugar de nacimiento del individuo.
     *
     * @param valor Nuevo lugar de nacimiento.
     */
    fun updateLugarNacimiento(valor: String) {
        _lugarNacimiento.value = valor
    }

    /**
     * Actualiza el domicilio del individuo.
     *
     * @param valor Nuevo domicilio.
     */
    fun updateDomicilio(valor: String) {
        _domicilio.value = valor
    }

    /**
     * Actualiza el código postal del individuo.
     *
     * @param valor Nuevo código postal.
     */
    fun updateCodigoPostal(valor: String) {
        _codigoPostal.value = valor
    }

    /**
     * Actualiza el teléfono del individuo.
     *
     * @param valor Nuevo teléfono.
     */
    fun updateTelefono(valor: String) {
        _telefono.value = valor
    }

    /**
     * Actualiza el email del individuo.
     *
     * @param valor Nuevo email.
     */
    fun updateEmail(valor: String) {
        _email.value = valor
    }

    /**
     * Actualiza el código CAN del individuo y registra el cambio en el log.
     *
     * @param valor Nuevo código CAN, puede ser nulo.
     */
    fun updateCodigoCan(valor: String?) {
        _codigoCan.value = valor
        Log.d("PersonaViewModel", "Código CAN actualizado: $valor")
    }

    /**
     * Limpia el código CAN del individuo y registra el cambio en el log.
     */
    fun clearCodigoCan() {
        _codigoCan.value = null
        Log.d("PersonaViewModel", "Código CAN limpiado")
    }

    /**
     * Actualiza todos los datos personales desde un objeto [DniData] obtenido de un DNI.
     *
     * @param dniData Datos del DNI a cargar en el ViewModel.
     */
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
     * @param context Contexto de la aplicación necesario para acceder al DataStore y mostrar UI.
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

    /**
     * Guarda el código CAN en el DataStore y actualiza el valor local.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore.
     * @param codigoCan Código CAN a guardar.
     */
    fun saveCodigoCan(context: Context, codigoCan: String) {
        viewModelScope.launch {
            context.dataStorePer.edit { preferences ->
                preferences[PersonaKeys.CODIGO_CAN] = codigoCan
            }
            _codigoCan.value = codigoCan
            Log.d("PersonaViewModel", "Código CAN guardado en DataStore: $codigoCan")
        }
    }

    /**
     * Limpia todos los datos almacenados en el DataStore y restablece los valores locales a vacíos.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore.
     */
    fun clearData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStorePer
            dataStore.edit { preferences ->
                preferences.clear()
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
    }

    /**
     * Limpia el código CAN del DataStore y del valor local, registrando el cambio en el log.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore.
     */
    fun clearCodigoCan(context: Context) {
        viewModelScope.launch {
            context.dataStorePer.edit { preferences ->
                preferences.remove(PersonaKeys.CODIGO_CAN)
            }
            _codigoCan.value = null
            Log.d("PersonaViewModel", "Código CAN limpiado de DataStore")
        }
    }

    /**
     * Carga todos los datos personales desde el DataStore y actualiza los valores locales.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore.
     */
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

    /**
     * Carga únicamente el código CAN desde el DataStore y actualiza el valor local.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore.
     */
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