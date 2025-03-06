package com.oscar.atestados.viewModel

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.screens.dataStoreVeh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestionar los datos de un vehículo.
 * Maneja la persistencia de datos utilizando DataStore y expone los datos a través de LiveData.
 * Incluye información sobre matrícula, fechas, características del vehículo y datos del seguro.
 */
class VehiculoViewModel : ViewModel() {

    /** LiveData privado para la matrícula del vehículo. */
    private val _matricula = MutableLiveData<String>()
    /** LiveData público para observar la matrícula del vehículo. */
    val matricula: LiveData<String> = _matricula

    /** LiveData privado para la fecha de matriculación del vehículo. */
    private val _fechaMatriculacion = MutableLiveData<String>()
    /** LiveData público para observar la fecha de matriculación del vehículo. */
    val fechaMatriculacion: LiveData<String> = _fechaMatriculacion

    /** LiveData privado para la marca del vehículo. */
    private val _marca = MutableLiveData<String>()
    /** LiveData público para observar la marca del vehículo. */
    val marca: LiveData<String> = _marca

    /** LiveData privado para el modelo del vehículo. */
    private val _modelo = MutableLiveData<String>()
    /** LiveData público para observar el modelo del vehículo. */
    val modelo: LiveData<String> = _modelo

    /** LiveData privado para el color del vehículo. */
    private val _color = MutableLiveData<String>()
    /** LiveData público para observar el color del vehículo. */
    val color: LiveData<String> = _color

    /** LiveData privado para el tipo de vehículo. */
    private val _tipoVehiculo = MutableLiveData<String>()
    /** LiveData público para observar el tipo de vehículo. */
    val tipoVehiculo: LiveData<String> = _tipoVehiculo

    /** LiveData privado para la aseguradora del vehículo. */
    private val _aseguradora = MutableLiveData<String>()
    /** LiveData público para observar la aseguradora del vehículo. */
    val aseguradora: LiveData<String> = _aseguradora

    /** LiveData privado para el número de póliza del seguro. */
    private val _numeroPoliza = MutableLiveData<String>()
    /** LiveData público para observar el número de póliza del seguro. */
    val numeroPoliza: LiveData<String> = _numeroPoliza

    /** LiveData privado para la fecha de la ITV del vehículo. */
    private val _fechaITV = MutableLiveData<String>()
    /** LiveData público para observar la fecha de la ITV del vehículo. */
    val fechaITV: LiveData<String> = _fechaITV

    /**
     * Objeto que contiene las claves para almacenar los datos del vehículo en DataStore.
     * Define las claves para cada propiedad del vehículo que se almacenará en las preferencias.
     */
    object VehiculoKeys {
        /** Clave para la matrícula del vehículo. */
        val MATRICULA = stringPreferencesKey("matricula")
        /** Clave para la fecha de matriculación del vehículo. */
        val FECHA_MATRICULACION = stringPreferencesKey("fecha_matriculacion")
        /** Clave para la marca del vehículo. */
        val MARCA = stringPreferencesKey("marca")
        /** Clave para el modelo del vehículo. */
        val MODELO = stringPreferencesKey("modelo")
        /** Clave para el color del vehículo. */
        val COLOR = stringPreferencesKey("color")
        /** Clave para el tipo de vehículo. */
        val TIPO_VEHICULO = stringPreferencesKey("tipo_vehiculo")
        /** Clave para la aseguradora del vehículo. */
        val ASEGURADORA = stringPreferencesKey("aseguradora")
        /** Clave para el número de póliza del seguro. */
        val NUMERO_POLIZA = stringPreferencesKey("numero_poliza")
        /** Clave para la fecha de la ITV del vehículo. */
        val FECHA_ITV = stringPreferencesKey("fecha_itv")
    }

    /**
     * Actualiza la matrícula del vehículo.
     *
     * @param matricula Nueva matrícula del vehículo.
     */
    fun updateMatricula(matricula: String) {
        _matricula.value = matricula
    }

    /**
     * Actualiza la fecha de matriculación del vehículo.
     *
     * @param fechaMatriculacion Nueva fecha de matriculación.
     */
    fun updateFechaMatriculacion(fechaMatriculacion: String) {
        _fechaMatriculacion.value = fechaMatriculacion
    }

    /**
     * Actualiza la marca del vehículo.
     *
     * @param marca Nueva marca del vehículo.
     */
    fun updateMarca(marca: String) {
        _marca.value = marca
    }

    /**
     * Actualiza el modelo del vehículo.
     *
     * @param modelo Nuevo modelo del vehículo.
     */
    fun updateModelo(modelo: String) {
        _modelo.value = modelo
    }

    /**
     * Actualiza el color del vehículo.
     *
     * @param color Nuevo color del vehículo.
     */
    fun updateColor(color: String) {
        _color.value = color
    }

    /**
     * Actualiza el tipo de vehículo.
     *
     * @param tipoVehiculo Nuevo tipo de vehículo.
     */
    fun updateTipoVehiculo(tipoVehiculo: String) {
        _tipoVehiculo.value = tipoVehiculo
    }

    /**
     * Actualiza la aseguradora del vehículo.
     *
     * @param aseguradora Nueva aseguradora del vehículo.
     */
    fun updateAsegurados(aseguradora: String) {
        _aseguradora.value = aseguradora
    }

    /**
     * Actualiza el número de póliza del seguro del vehículo.
     *
     * @param numeroPoliza Nuevo número de póliza.
     */
    fun updateNumeroPoliza(numeroPoliza: String) {
        _numeroPoliza.value = numeroPoliza
    }

    /**
     * Actualiza la fecha de la ITV del vehículo.
     *
     * @param fechaITV Nueva fecha de la ITV.
     */
    fun updateFechaITV(fechaITV: String) {
        _fechaITV.value = fechaITV
    }

    /**
     * Guarda los datos del vehículo en el DataStore de manera asíncrona.
     * Muestra una alerta si no hay datos para guardar.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore y mostrar UI.
     */
    fun saveData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStoreVeh

            val hasDataToSave =
                _matricula.value.isNullOrBlank() &&
                        _fechaMatriculacion.value.isNullOrBlank() &&
                        _marca.value.isNullOrBlank() &&
                        _modelo.value.isNullOrBlank() &&
                        _color.value.isNullOrBlank() &&
                        _tipoVehiculo.value.isNullOrBlank() &&
                        _aseguradora.value.isNullOrBlank() &&
                        _numeroPoliza.value.isNullOrBlank() &&
                        _fechaITV.value.isNullOrBlank()

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
                dataStore.edit { preferences ->
                    preferences[VehiculoKeys.MATRICULA] = _matricula.value ?: ""
                    preferences[VehiculoKeys.FECHA_MATRICULACION] = _fechaMatriculacion.value ?: ""
                    preferences[VehiculoKeys.MARCA] = _marca.value ?: ""
                    preferences[VehiculoKeys.MODELO] = _modelo.value ?: ""
                    preferences[VehiculoKeys.COLOR] = _color.value ?: ""
                    preferences[VehiculoKeys.TIPO_VEHICULO] = _tipoVehiculo.value ?: ""
                    preferences[VehiculoKeys.ASEGURADORA] = _aseguradora.value ?: ""
                    preferences[VehiculoKeys.NUMERO_POLIZA] = _numeroPoliza.value ?: ""
                    preferences[VehiculoKeys.FECHA_ITV] = _fechaITV.value ?: ""
                }
            }
        }
    }

    /**
     * Limpia todos los datos del vehículo almacenados en el DataStore y restablece los valores en los LiveData a cadenas vacías.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore.
     */
    fun clearData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStoreVeh
            dataStore.edit { preferences ->
                preferences.clear()
            }
            _matricula.value = ""
            _fechaMatriculacion.value = ""
            _marca.value = ""
            _modelo.value = ""
            _color.value = ""
            _tipoVehiculo.value = ""
            _aseguradora.value = ""
            _numeroPoliza.value = ""
            _fechaITV.value = ""
        }
    }

    /**
     * Carga los datos del vehículo almacenados en el DataStore y los asigna a los LiveData correspondientes.
     * Si no hay datos almacenados, utiliza cadenas vacías como valores predeterminados.
     *
     * @param context Contexto de la aplicación necesario para acceder al DataStore.
     */
    fun loadData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStoreVeh

            dataStore.data.collect { preferences ->
                _matricula.value = preferences[VehiculoKeys.MATRICULA] ?: ""
                _fechaMatriculacion.value = preferences[VehiculoKeys.FECHA_MATRICULACION] ?: ""
                _marca.value = preferences[VehiculoKeys.MARCA] ?: ""
                _modelo.value = preferences[VehiculoKeys.MODELO] ?: ""
                _color.value = preferences[VehiculoKeys.COLOR] ?: ""
                _tipoVehiculo.value = preferences[VehiculoKeys.TIPO_VEHICULO] ?: ""
                _aseguradora.value = preferences[VehiculoKeys.ASEGURADORA] ?: ""
                _numeroPoliza.value = preferences[VehiculoKeys.NUMERO_POLIZA] ?: ""
                _fechaITV.value = preferences[VehiculoKeys.FECHA_ITV] ?: ""
            }
        }
    }
}