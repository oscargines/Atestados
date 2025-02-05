package com.oscar.atestados.viewModel

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Context.dataStoreGua by preferencesDataStore(name = "GUARDIAS_PREFERENCES")

class GuardiasViewModel : ViewModel() {

    // Estados para el Primer Interviniente
    private val _rolPrimerInterviniente = MutableLiveData<String>()
    val rolPrimerInterviniente : LiveData<String> = _rolPrimerInterviniente

    private val _primerTip = MutableLiveData<String>()
    val primerTip : LiveData<String> = _primerTip

    private val _empleoPrimerInterviniente = MutableLiveData<String>()
    val empleoPrimerInterviniente : LiveData<String> = _empleoPrimerInterviniente

    private val _primerUnidad = MutableLiveData<String>()
    val primerUnidad : LiveData<String> = _primerUnidad

    // Estados para el Segundo Interviniente
    private val _rolSegundoInterviniente = MutableLiveData<String>()
    val segundoInterviniente : LiveData<String> = _rolSegundoInterviniente

    private val _segundoTip = MutableLiveData<String>()
    val segundoTip : LiveData<String> = _segundoTip

    private val _empleoSegundoInterviniente = MutableLiveData<String>()
    val empleoSegundoInterviniente : LiveData<String> = _empleoSegundoInterviniente

    private val _segundoUnidad = MutableLiveData<String>()
    val segundoUnidad : LiveData<String> = _segundoUnidad

    // Object para las claves de preferencias
    object PreferencesKeys {
        val ROL_PRIMER_INSTERVINIENTE = stringPreferencesKey("rol_primer_interviniente")
        val PRIMER_TIP = stringPreferencesKey("primer_tip")
        val EMPLEO_PRIMER_INSTERVINIENTE = stringPreferencesKey("empleo_primer_interviniente")
        val UNIDAD_PRIMER_INSTERVINIENTE = stringPreferencesKey("unidad_primer_interviniente")

        val ROL_SEGUNDO_INSTERVINIENTE = stringPreferencesKey("rol_segundo_interviniente")
        val SEGUNDO_TIP = stringPreferencesKey("segundo_tip")
        val SEGUNDO_GUARDIA = stringPreferencesKey("empleo_segundo_interviniente")
        val SEGUNDO_UNIDAD = stringPreferencesKey("unidad_segundo_interviniente")
    }

    // Funciones para actualizar el Primer Interviniente
    fun updateRolPrimerInterviniente(valor: String) {
        _rolPrimerInterviniente.value = valor
    }

    fun updatePrimerTip(valor: String) {
        _primerTip.value = valor
    }

    fun updateEmpleoPrimerInterviniente(valor: String) {
        _empleoPrimerInterviniente.value = valor
    }

    fun updatePrimerUnidad(valor: String) {
        _primerUnidad.value = valor
    }

    // Funciones para actualizar el Segundo Interviniente
    fun updateRolSegundoInterviniente(valor: String) {
        _rolSegundoInterviniente.value = valor
    }

    fun updateSegundoTip(valor: String) {
        _segundoTip.value = valor
    }

    fun updateEmpleoSegundoInterviniente(valor: String) {
        _empleoSegundoInterviniente.value = valor
    }

    fun updateSegundoUnidad(valor: String) {
        _segundoUnidad.value = valor
    }

    // Cargar datos desde DataStore
    fun loadData(context: Context) {
        viewModelScope.launch {
            context.dataStoreGua.data.collect { preferences ->
                // Primer Interviniente
                _rolPrimerInterviniente.value = preferences[PreferencesKeys.ROL_PRIMER_INSTERVINIENTE] ?: ""
                _primerTip.value = preferences[PreferencesKeys.PRIMER_TIP] ?: ""
                _empleoPrimerInterviniente.value = preferences[PreferencesKeys.EMPLEO_PRIMER_INSTERVINIENTE] ?: ""
                _primerUnidad.value = preferences[PreferencesKeys.UNIDAD_PRIMER_INSTERVINIENTE] ?: ""

                // Segundo Interviniente
                _rolSegundoInterviniente.value = preferences[PreferencesKeys.ROL_SEGUNDO_INSTERVINIENTE] ?: ""
                _segundoTip.value = preferences[PreferencesKeys.SEGUNDO_TIP] ?: ""
                _empleoSegundoInterviniente.value = preferences[PreferencesKeys.SEGUNDO_GUARDIA] ?: ""
                _segundoUnidad.value = preferences[PreferencesKeys.SEGUNDO_UNIDAD] ?: ""
            }
        }
    }

    // Guardar datos en DataStore
    fun saveData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStoreGua

            // Verificar si hay datos para guardar
            val hasDataToSave =
                _rolPrimerInterviniente.value.isNullOrBlank() &&
                        _primerTip.value.isNullOrBlank() &&
                        _empleoPrimerInterviniente.value.isNullOrBlank() &&
                        _primerUnidad.value.isNullOrBlank()

            if (hasDataToSave) {
                // Mostrar alerta si no hay datos
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(context)
                        .setTitle("Advertencia")
                        .setMessage("No se han introducido datos necesarios del instructor." +
                                "\nRecuerde que al menos debe de haber un instructor.")
                        .setPositiveButton("Aceptar", null)
                        .show()
                }
            } else {
                // Guardar los datos en el DataStore
                dataStore.edit { preferences ->
                    // Primer Interviniente
                    preferences[PreferencesKeys.ROL_PRIMER_INSTERVINIENTE] = _rolPrimerInterviniente.value ?: ""
                    preferences[PreferencesKeys.PRIMER_TIP] = _primerTip.value ?: ""
                    preferences[PreferencesKeys.EMPLEO_PRIMER_INSTERVINIENTE] = _empleoPrimerInterviniente.value ?: ""
                    preferences[PreferencesKeys.UNIDAD_PRIMER_INSTERVINIENTE] = _primerUnidad.value ?: ""

                    // Segundo Interviniente
                    preferences[PreferencesKeys.ROL_SEGUNDO_INSTERVINIENTE] = _rolSegundoInterviniente.value ?: ""
                    preferences[PreferencesKeys.SEGUNDO_TIP] = _segundoTip.value ?: ""
                    preferences[PreferencesKeys.SEGUNDO_GUARDIA] = _empleoSegundoInterviniente.value ?: ""
                    preferences[PreferencesKeys.SEGUNDO_UNIDAD] = _segundoUnidad.value ?: ""
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

    // Limpiar todos los datos
    fun clearData(context: Context) {
        viewModelScope.launch {
            // Limpiar DataStore
            context.dataStoreGua.edit { preferences ->
                preferences.clear()
            }

            // Limpiar estados locales
            // Primer Interviniente
            _rolPrimerInterviniente.value = ""
            _primerTip.value = ""
            _empleoPrimerInterviniente.value = ""
            _primerUnidad.value = ""

            // Segundo Interviniente
            _rolSegundoInterviniente.value = ""
            _segundoTip.value = ""
            _empleoSegundoInterviniente.value = ""
            _segundoUnidad.value = ""
        }
    }




}