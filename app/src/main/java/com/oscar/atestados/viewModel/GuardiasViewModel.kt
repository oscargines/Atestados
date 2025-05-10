package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configuración de DataStore para almacenar preferencias relacionadas con los guardias intervinientes.
 */
val Context.dataStoreGua by preferencesDataStore(name = "GUARDIAS_PREFERENCES")

/**
 * ViewModel para gestionar los datos de los guardias intervinientes (primer y segundo interviniente).
 * Utiliza LiveData para observar cambios y DataStore para persistir los datos.
 */
class GuardiasViewModel(application: Application) : AndroidViewModel(application) {

    // Estados para el Primer Interviniente
    /** LiveData privado para el rol del primer interviniente. */
    private val _rolPrimerInterviniente = MutableLiveData<String>()
    /** LiveData público para observar el rol del primer interviniente. */
    val rolPrimerInterviniente: LiveData<String> = _rolPrimerInterviniente

    /** LiveData privado para el TIP del primer interviniente. */
    private val _primerTip = MutableLiveData<String>()
    /** LiveData público para observar el TIP del primer interviniente. */
    val primerTip: LiveData<String> = _primerTip

    /** LiveData privado para el empleo del primer interviniente. */
    private val _empleoPrimerInterviniente = MutableLiveData<String>()
    /** LiveData público para observar el empleo del primer interviniente. */
    val empleoPrimerInterviniente: LiveData<String> = _empleoPrimerInterviniente

    /** LiveData privado para la unidad del primer interviniente. */
    private val _primerUnidad = MutableLiveData<String>()
    /** LiveData público para observar la unidad del primer interviniente. */
    val primerUnidad: LiveData<String> = _primerUnidad

    // Estados para el Segundo Interviniente
    /** LiveData privado para el rol del segundo interviniente. */
    private val _rolSegundoInterviniente = MutableLiveData<String>()
    /** LiveData público para observar el rol del segundo interviniente. */
    val rolSegundoInterviniente: LiveData<String> = _rolSegundoInterviniente

    /** LiveData privado para el TIP del segundo interviniente. */
    private val _segundoTip = MutableLiveData<String>()
    /** LiveData público para observar el TIP del segundo interviniente. */
    val segundoTip: LiveData<String> = _segundoTip

    /** LiveData privado para el empleo del segundo interviniente. */
    private val _empleoSegundoInterviniente = MutableLiveData<String>()
    /** LiveData público para observar el empleo del segundo interviniente. */
    val empleoSegundoInterviniente: LiveData<String> = _empleoSegundoInterviniente

    /** LiveData privado para la unidad del segundo interviniente. */
    private val _segundoUnidad = MutableLiveData<String>()
    /** LiveData público para observar la unidad del segundo interviniente. */
    val segundoUnidad: LiveData<String> = _segundoUnidad

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     */
    object PreferencesKeys {
        /** Clave para el rol del primer interviniente. */
        val ROL_PRIMER_INSTERVINIENTE = stringPreferencesKey("rol_primer_interviniente")
        /** Clave para el TIP del primer interviniente. */
        val PRIMER_TIP = stringPreferencesKey("primer_tip")
        /** Clave para el empleo del primer interviniente. */
        val EMPLEO_PRIMER_INSTERVINIENTE = stringPreferencesKey("empleo_primer_interviniente")
        /** Clave para la unidad del primer interviniente. */
        val UNIDAD_PRIMER_INSTERVINIENTE = stringPreferencesKey("unidad_primer_interviniente")

        /** Clave para el rol del segundo interviniente. */
        val ROL_SEGUNDO_INSTERVINIENTE = stringPreferencesKey("rol_segundo_interviniente")
        /** Clave para el TIP del segundo interviniente. */
        val SEGUNDO_TIP = stringPreferencesKey("segundo_tip")
        /** Clave para el empleo del segundo interviniente. */
        val SEGUNDO_GUARDIA = stringPreferencesKey("empleo_segundo_interviniente")
        /** Clave para la unidad del segundo interviniente. */
        val SEGUNDO_UNIDAD = stringPreferencesKey("unidad_segundo_interviniente")
    }

    // Funciones para actualizar el Primer Interviniente
    /**
     * Actualiza el rol del primer interviniente.
     *
     * @param valor Nuevo rol del primer interviniente.
     */
    fun updateRolPrimerInterviniente(valor: String) {
        _rolPrimerInterviniente.value = valor
    }

    /**
     * Actualiza el TIP del primer interviniente.
     *
     * @param valor Nuevo TIP del primer interviniente.
     */
    fun updatePrimerTip(valor: String) {
        _primerTip.value = valor
    }

    /**
     * Actualiza el empleo del primer interviniente.
     *
     * @param valor Nuevo empleo del primer interviniente.
     */
    fun updateEmpleoPrimerInterviniente(valor: String) {
        _empleoPrimerInterviniente.value = valor
    }

    /**
     * Actualiza la unidad del primer interviniente.
     *
     * @param valor Nueva unidad del primer interviniente.
     */
    fun updatePrimerUnidad(valor: String) {
        _primerUnidad.value = valor
    }

    // Funciones para actualizar el Segundo Interviniente
    /**
     * Actualiza el rol del segundo interviniente.
     *
     * @param valor Nuevo rol del segundo interviniente.
     */
    fun updateRolSegundoInterviniente(valor: String) {
        _rolSegundoInterviniente.value = valor
    }

    /**
     * Actualiza el TIP del segundo interviniente.
     *
     * @param valor Nuevo TIP del segundo interviniente.
     */
    fun updateSegundoTip(valor: String) {
        _segundoTip.value = valor
    }

    /**
     * Actualiza el empleo del segundo interviniente.
     *
     * @param valor Nuevo empleo del segundo interviniente.
     */
    fun updateEmpleoSegundoInterviniente(valor: String) {
        _empleoSegundoInterviniente.value = valor
    }

    /**
     * Actualiza la unidad del segundo interviniente.
     *
     * @param valor Nueva unidad del segundo interviniente.
     */
    fun updateSegundoUnidad(valor: String) {
        _segundoUnidad.value = valor
    }

    init {
        loadData(getApplication()) // Cargar datos al inicializar
    }

    /**
     * Carga los datos de los intervinientes desde DataStore de manera asíncrona.
     *
     * @param context Contexto necesario para acceder a DataStore.
     */
    fun loadData(context: Context) {
        viewModelScope.launch {
            context.dataStoreGua.data.collect { preferences ->
                // Primer Interviniente
                _rolPrimerInterviniente.value = preferences[PreferencesKeys.ROL_PRIMER_INSTERVINIENTE] ?: "Instructor"
                _primerTip.value = preferences[PreferencesKeys.PRIMER_TIP] ?: ""
                _empleoPrimerInterviniente.value = preferences[PreferencesKeys.EMPLEO_PRIMER_INSTERVINIENTE] ?: "Guardia Civil"
                _primerUnidad.value = preferences[PreferencesKeys.UNIDAD_PRIMER_INSTERVINIENTE] ?: ""

                // Segundo Interviniente
                _rolSegundoInterviniente.value = preferences[PreferencesKeys.ROL_SEGUNDO_INSTERVINIENTE] ?: "Secretario"
                _segundoTip.value = preferences[PreferencesKeys.SEGUNDO_TIP] ?: ""
                _empleoSegundoInterviniente.value = preferences[PreferencesKeys.SEGUNDO_GUARDIA] ?: "Guardia Civil"
                _segundoUnidad.value = preferences[PreferencesKeys.SEGUNDO_UNIDAD] ?: ""
                Log.d("GuardiasViewModel", "Datos cargados - primerTip: ${_primerTip.value}, segundoTip: ${_segundoTip.value}")
            }
        }
    }

    /**
     * Guarda los datos de los intervinientes en DataStore de manera asíncrona.
     * Muestra una alerta si no hay datos del instructor y un Toast al guardar correctamente.
     *
     * @param context Contexto necesario para acceder a DataStore y mostrar UI.
     */
    fun saveData(context: Context) {
        viewModelScope.launch {
            val dataStore = context.dataStoreGua

            // Verificar si hay datos para guardar (al menos del primer interviniente)
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
                        .setMessage(
                            "No se han introducido datos necesarios del instructor." +
                                    "\nRecuerde que al menos debe de haber un instructor."
                        )
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

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores en el ViewModel.
     *
     * @param context Contexto necesario para acceder a DataStore.
     */
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