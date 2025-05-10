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

/**
 * Configuración de DataStore para almacenar preferencias relacionadas con la toma de derechos.
 */
val Context.dataStoreTomaDerechos: DataStore<Preferences> by preferencesDataStore(name = "toma_derechos_settings")

/**
 * ViewModel para gestionar los datos relacionados con la toma de derechos del investigado.
 * Utiliza LiveData para observar cambios en los datos y DataStore para persistirlos.
 *
 * @property application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class TomaDerechosViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     */
    private object PreferencesKeys {
        /** Clave para indicar si el investigado desea prestar declaración. */
        val PRESTAR_DECLARACION = booleanPreferencesKey("prestar_declaracion")
        /** Clave para indicar si el investigado renuncia a la asistencia letrada. */
        val RENUNCIA_ASISTENCIA_LETRADA = booleanPreferencesKey("renuncia_asistencia_letrada")
        /** Clave para indicar si el investigado solicita asistencia de un letrado particular. */
        val ASISTENCIA_LETRADO_PARTICULAR = booleanPreferencesKey("asistencia_letrado_particular")
        /** Clave para el nombre del letrado particular. */
        val NOMBRE_LETRADO = stringPreferencesKey("nombre_letrado")
        /** Clave para indicar si el investigado solicita asistencia de un letrado de oficio. */
        val ASISTENCIA_LETRADO_OFICIO = booleanPreferencesKey("asistencia_letrado_oficio")
        /** Clave para indicar si el investigado solicita acceso a elementos esenciales. */
        val ACCESO_ELEMENTOS = booleanPreferencesKey("acceso_elementos")
        /** Clave para indicar si el investigado solicita un intérprete. */
        val INTERPRETE = booleanPreferencesKey("interprete")
        /** Clave para el texto descriptivo de los elementos esenciales. */
        val TEXTO_ELEMENTOS_ESENCIALES = stringPreferencesKey("texto_elementos_esenciales")
        /** Clave para la opción seleccionada en un menú desplegable. */
        val SELECTED_OPTION = stringPreferencesKey("selected_option")
    }

    // Estados para los switches y campos de texto
    /** LiveData privado para indicar si el investigado desea prestar declaración, por defecto true. */
    private val _prestarDeclaracion = MutableLiveData(true)
    /** LiveData público para observar si el investigado desea prestar declaración. */
    val prestarDeclaracion: LiveData<Boolean> = _prestarDeclaracion

    /** LiveData privado para indicar si el investigado renuncia a la asistencia letrada, por defecto true. */
    private val _renunciaAsistenciaLetrada = MutableLiveData(true)
    /** LiveData público para observar si el investigado renuncia a la asistencia letrada. */
    val renunciaAsistenciaLetrada: LiveData<Boolean> = _renunciaAsistenciaLetrada

    /** LiveData privado para indicar si el investigado solicita asistencia de un letrado particular, por defecto false. */
    private val _asistenciaLetradoParticular = MutableLiveData(false)
    /** LiveData público para observar si el investigado solicita asistencia de un letrado particular. */
    val asistenciaLetradoParticular: LiveData<Boolean> = _asistenciaLetradoParticular

    /** LiveData privado para el nombre del letrado particular, por defecto vacío. */
    private val _nombreLetrado = MutableLiveData("")
    /** LiveData público para observar el nombre del letrado particular. */
    val nombreLetrado: LiveData<String> = _nombreLetrado

    /** LiveData privado para indicar si el investigado solicita asistencia de un letrado de oficio, por defecto true. */
    private val _asistenciaLetradoOficio = MutableLiveData(true)
    /** LiveData público para observar si el investigado solicita asistencia de un letrado de oficio. */
    val asistenciaLetradoOficio: LiveData<Boolean> = _asistenciaLetradoOficio

    /** LiveData privado para indicar si el investigado solicita acceso a elementos esenciales, por defecto false. */
    private val _accesoElementos = MutableLiveData(false)
    /** LiveData público para observar si el investigado solicita acceso a elementos esenciales. */
    val accesoElementos: LiveData<Boolean> = _accesoElementos

    /** LiveData privado para indicar si el investigado solicita un intérprete, por defecto false. */
    private val _interprete = MutableLiveData(false)
    /** LiveData público para observar si el investigado solicita un intérprete. */
    val interprete: LiveData<Boolean> = _interprete

    /** LiveData privado para el texto descriptivo de los elementos esenciales, por defecto vacío. */
    private val _textoElementosEsenciales = MutableLiveData("")
    /** LiveData público para observar el texto descriptivo de los elementos esenciales. */
    val textoElementosEsenciales: LiveData<String> = _textoElementosEsenciales

    /** LiveData privado para la opción seleccionada en un menú desplegable, por defecto vacío. */
    private val _selectedOption = MutableLiveData("")
    /** LiveData público para observar la opción seleccionada en un menú desplegable. */
    val selectedOption: LiveData<String> = _selectedOption

    /**
     * Inicializa el ViewModel cargando los datos guardados desde DataStore.
     */
    init {
        loadSavedData()
    }

    // Funciones para actualizar los estados
    /**
     * Establece si el investigado desea prestar declaración.
     *
     * @param value Nuevo valor para prestar declaración.
     */
    fun setPrestarDeclaracion(value: Boolean) {
        _prestarDeclaracion.value = value
    }

    /**
     * Establece si el investigado renuncia a la asistencia letrada.
     *
     * @param value Nuevo valor para la renuncia a la asistencia letrada.
     */
    fun setRenunciaAsistenciaLetrada(value: Boolean) {
        _renunciaAsistenciaLetrada.value = value
    }

    /**
     * Establece si el investigado solicita asistencia de un letrado particular.
     * Si se activa, desactiva la asistencia de letrado de oficio.
     *
     * @param value Nuevo valor para la asistencia de letrado particular.
     */
    fun setAsistenciaLetradoParticular(value: Boolean) {
        if (value) {
            _asistenciaLetradoOficio.value = false // Desactivar el otro switch
        }
        _asistenciaLetradoParticular.value = value
    }

    /**
     * Establece el nombre del letrado particular.
     *
     * @param value Nuevo nombre del letrado.
     */
    fun setNombreLetrado(value: String) {
        _nombreLetrado.value = value
    }

    /**
     * Establece si el investigado solicita asistencia de un letrado de oficio.
     * Si se activa, desactiva la asistencia de letrado particular.
     *
     * @param value Nuevo valor para la asistencia de letrado de oficio.
     */
    fun setAsistenciaLetradoOficio(value: Boolean) {
        if (value) {
            _asistenciaLetradoParticular.value = false // Desactivar el otro switch
        }
        _asistenciaLetradoOficio.value = value
    }

    /**
     * Establece si el investigado solicita acceso a elementos esenciales.
     *
     * @param value Nuevo valor para el acceso a elementos esenciales.
     */
    fun setAccesoElementos(value: Boolean) {
        _accesoElementos.value = value
    }

    /**
     * Establece si el investigado solicita un intérprete.
     *
     * @param value Nuevo valor para la solicitud de intérprete.
     */
    fun setInterprete(value: Boolean) {
        _interprete.value = value
    }

    /**
     * Establece el texto descriptivo de los elementos esenciales.
     *
     * @param value Nuevo texto para los elementos esenciales.
     */
    fun setTextoElementosEsenciales(value: String) {
        _textoElementosEsenciales.value = value
    }

    /**
     * Establece la opción seleccionada en un menú desplegable.
     *
     * @param option Nueva opción seleccionada.
     */
    fun setSelectedOption(option: String) {
        _selectedOption.value = option
    }

    /**
     * Guarda los datos actuales en DataStore de manera asíncrona.
     */
    fun guardarDatos(context: Context) {
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

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores por defecto en el ViewModel.
     */
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

    /**
     * Carga los datos guardados en DataStore al inicializar el ViewModel.
     */
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