package com.oscar.atestados.viewModel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStoreTomaDerechos: DataStore<Preferences> by preferencesDataStore(name = "toma_derechos_settings")

class TomaDerechosViewModel(application: Application) : AndroidViewModel(application) {

    // Claves para DataStore
    private object PreferencesKeys {
        val PRESTAR_DECLARACION = booleanPreferencesKey("prestar_declaracion")
        val RENUNCIA_ASISTENCIA_LETRADA = booleanPreferencesKey("renuncia_asistencia_letrada")
        val ASISTENCIA_LETRADO_PARTICULAR = booleanPreferencesKey("asistencia_letrado_particular")
        val NOMBRE_LETRADO = stringPreferencesKey("nombre_letrado")
        val ASISTENCIA_LETRADO_OFICIO = booleanPreferencesKey("asistencia_letrado_oficio")
        val ACCESO_ELEMENTOS = booleanPreferencesKey("acceso_elementos")
        val INTERPRETE = booleanPreferencesKey("interprete")
        val TEXTO_ELEMENTOS_ESENCIALES = stringPreferencesKey("texto_elementos_esenciales")
        val SELECTED_OPTION = stringPreferencesKey("selected_option") // Nueva clave para la opción seleccionada
    }

    // Estados para los switches y campos de texto
    private val _prestarDeclaracion = MutableLiveData(true)
    val prestarDeclaracion: LiveData<Boolean> = _prestarDeclaracion

    private val _renunciaAsistenciaLetrada = MutableLiveData(true)
    val renunciaAsistenciaLetrada: LiveData<Boolean> = _renunciaAsistenciaLetrada

    private val _asistenciaLetradoParticular = MutableLiveData(false)
    val asistenciaLetradoParticular: LiveData<Boolean> = _asistenciaLetradoParticular

    private val _nombreLetrado = MutableLiveData("")
    val nombreLetrado: LiveData<String> = _nombreLetrado

    private val _asistenciaLetradoOficio = MutableLiveData(true)
    val asistenciaLetradoOficio: LiveData<Boolean> = _asistenciaLetradoOficio

    private val _accesoElementos = MutableLiveData(false)
    val accesoElementos: LiveData<Boolean> = _accesoElementos

    private val _interprete = MutableLiveData(false)
    val interprete: LiveData<Boolean> = _interprete

    // Nuevo estado para el texto del CustomTextField
    private val _textoElementosEsenciales = MutableLiveData("")
    val textoElementosEsenciales: LiveData<String> = _textoElementosEsenciales

    // Nuevo estado para la opción seleccionada en el DropdownMenu
    private val _selectedOption = MutableLiveData("")
    val selectedOption: LiveData<String> = _selectedOption

    init {
        loadSavedData()
    }

    // Funciones para actualizar los estados
    fun setPrestarDeclaracion(value: Boolean) {
        _prestarDeclaracion.value = value
    }

    fun setRenunciaAsistenciaLetrada(value: Boolean) {
        _renunciaAsistenciaLetrada.value = value
    }

    fun setAsistenciaLetradoParticular(value: Boolean) {
        if (value) {
            _asistenciaLetradoOficio.value = false // Desactivar el otro switch
        }
        _asistenciaLetradoParticular.value = value
    }

    fun setNombreLetrado(value: String) {
        _nombreLetrado.value = value
    }

    fun setAsistenciaLetradoOficio(value: Boolean) {
        if (value) {
            _asistenciaLetradoParticular.value = false // Desactivar el otro switch
        }
        _asistenciaLetradoOficio.value = value
    }

    fun setAccesoElementos(value: Boolean) {
        _accesoElementos.value = value
    }

    fun setInterprete(value: Boolean) {
        _interprete.value = value
    }

    // Nueva función para actualizar el texto del CustomTextField
    fun setTextoElementosEsenciales(value: String) {
        _textoElementosEsenciales.value = value
    }

    // Nueva función para actualizar la opción seleccionada en el DropdownMenu
    fun setSelectedOption(option: String) {
        _selectedOption.value = option
    }

    // Guardar datos en DataStore
    fun guardarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreTomaDerechos.edit { preferences ->
                _prestarDeclaracion.value?.let { preferences[PreferencesKeys.PRESTAR_DECLARACION] = it }
                _renunciaAsistenciaLetrada.value?.let { preferences[PreferencesKeys.RENUNCIA_ASISTENCIA_LETRADA] = it }
                _asistenciaLetradoParticular.value?.let { preferences[PreferencesKeys.ASISTENCIA_LETRADO_PARTICULAR] = it }
                _nombreLetrado.value?.let { preferences[PreferencesKeys.NOMBRE_LETRADO] = it }
                _asistenciaLetradoOficio.value?.let { preferences[PreferencesKeys.ASISTENCIA_LETRADO_OFICIO] = it }
                _accesoElementos.value?.let { preferences[PreferencesKeys.ACCESO_ELEMENTOS] = it }
                _interprete.value?.let { preferences[PreferencesKeys.INTERPRETE] = it }
                _textoElementosEsenciales.value?.let { preferences[PreferencesKeys.TEXTO_ELEMENTOS_ESENCIALES] = it }
                _selectedOption.value?.let { preferences[PreferencesKeys.SELECTED_OPTION] = it }
            }
        }
    }

    // Limpiar datos del DataStore
    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreTomaDerechos.edit { preferences ->
                preferences.clear()
            }

            // Restablecer los valores por defecto
            _prestarDeclaracion.value = true
            _renunciaAsistenciaLetrada.value = true
            _asistenciaLetradoParticular.value = false
            _nombreLetrado.value = ""
            _asistenciaLetradoOficio.value = true
            _accesoElementos.value = false
            _interprete.value = false
            _textoElementosEsenciales.value = ""
            _selectedOption.value = ""
        }
    }

    // Cargar datos guardados desde DataStore
    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStoreTomaDerechos.data.first()

            _prestarDeclaracion.value = preferences[PreferencesKeys.PRESTAR_DECLARACION] ?: true
            _renunciaAsistenciaLetrada.value = preferences[PreferencesKeys.RENUNCIA_ASISTENCIA_LETRADA] ?: true
            _asistenciaLetradoParticular.value = preferences[PreferencesKeys.ASISTENCIA_LETRADO_PARTICULAR] ?: false
            _nombreLetrado.value = preferences[PreferencesKeys.NOMBRE_LETRADO] ?: ""
            _asistenciaLetradoOficio.value = preferences[PreferencesKeys.ASISTENCIA_LETRADO_OFICIO] ?: true
            _accesoElementos.value = preferences[PreferencesKeys.ACCESO_ELEMENTOS] ?: false
            _interprete.value = preferences[PreferencesKeys.INTERPRETE] ?: false
            _textoElementosEsenciales.value = preferences[PreferencesKeys.TEXTO_ELEMENTOS_ESENCIALES] ?: ""
            _selectedOption.value = preferences[PreferencesKeys.SELECTED_OPTION] ?: ""
        }
    }
}