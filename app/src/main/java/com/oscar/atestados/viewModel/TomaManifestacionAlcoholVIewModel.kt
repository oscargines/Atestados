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
 * Configuración de DataStore para almacenar preferencias relacionadas con la toma de manifestación en casos de alcohol.
 */
val Context.dataStoreManifestacionAlcohol: DataStore<Preferences> by preferencesDataStore(name = "manifestacion_alcohol_settings")

/**
 * ViewModel para gestionar los datos relacionados con la toma de manifestación del investigado en casos de alcohol.
 * Utiliza LiveData para observar cambios en los datos y DataStore para persistirlos.
 *
 * @property application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class TomaManifestacionAlcoholViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     */
    private object PreferencesKeys {
        /** Clave para indicar si el investigado desea declarar. */
        val DESEA_DECLARAR = booleanPreferencesKey("desea_declarar")
        /** Clave para indicar si el investigado renuncia expresamente a un letrado. */
        val RENUNCIA_EXPRESA_LETRADO = booleanPreferencesKey("renuncia_expresa_letrado")
        /** Clave para las condiciones en las que se realiza la manifestación. */
        val CONDICIONES_PARA_MANIFESTACION = stringPreferencesKey("condiciones_para_manifestacion")
        /** Clave para el consumo de alcohol declarado por el investigado. */
        val CONSUMO_ALCOHOL = stringPreferencesKey("consumo_alcohol")
        /** Clave para la procedencia del investigado. */
        val PROCEDENCIA = stringPreferencesKey("procedencia")
        /** Clave para enfermedades o medicamentos que puedan afectar al investigado. */
        val ENFERMEDAD_MEDICAMENTOS = stringPreferencesKey("enfermedad_medicamentos")
        /** Clave para el tiempo transcurrido desde el último trago de alcohol. */
        val TIEMPO_ULTIMO_TRAGO = stringPreferencesKey("tiempo_ultimo_trago")
        /** Clave para la última vez que el investigado consumió alcohol. */
        val ULTIMA_VEZ_ALCOHOL = stringPreferencesKey("ultima_vez_alcohol")
        /** Clave para si el investigado es consciente de los peligros de conducir bajo los efectos del alcohol. */
        val CONSCIENTE_PELIGROS = stringPreferencesKey("consciente_peligros")
        /** Clave para una declaración adicional del investigado. */
        val DECLARACION_ADICIONAL = stringPreferencesKey("declaracion_adicional")
    }

    // Estados para los switches y campos de texto
    /** LiveData privado para indicar si el investigado desea declarar, por defecto false. */
    private val _deseaDeclarar = MutableLiveData(false)
    /** LiveData público para observar si el investigado desea declarar. */
    val deseaDeclarar: LiveData<Boolean> = _deseaDeclarar

    /** LiveData privado para indicar si el investigado renuncia expresamente a un letrado, por defecto false. */
    private val _renunciaExpresaLletrado = MutableLiveData(false)
    /** LiveData público para observar si el investigado renuncia expresamente a un letrado. */
    val renunciaExpresaLletrado: LiveData<Boolean> = _renunciaExpresaLletrado

    /** LiveData privado para las condiciones de la manifestación, por defecto vacío. */
    private val _condicionesParaManifestacion = MutableLiveData("")
    /** LiveData público para observar las condiciones de la manifestación. */
    val condicionesParaManifestacion: LiveData<String> = _condicionesParaManifestacion

    /** LiveData privado para el consumo de alcohol declarado, por defecto vacío. */
    private val _consumoAlcohol = MutableLiveData("")
    /** LiveData público para observar el consumo de alcohol declarado. */
    val consumoAlcohol: LiveData<String> = _consumoAlcohol

    /** LiveData privado para la procedencia del investigado, por defecto vacío. */
    private val _procedencia = MutableLiveData("")
    /** LiveData público para observar la procedencia del investigado. */
    val procedencia: LiveData<String> = _procedencia

    /** LiveData privado para enfermedades o medicamentos, por defecto vacío. */
    private val _enfermedadMedicamentos = MutableLiveData("")
    /** LiveData público para observar enfermedades o medicamentos. */
    val enfermedadMedicamentos: LiveData<String> = _enfermedadMedicamentos

    /** LiveData privado para el tiempo desde el último trago, por defecto vacío. */
    private val _tiempoUltimoTrago = MutableLiveData("")
    /** LiveData público para observar el tiempo desde el último trago. */
    val tiempoUltimoTrago: LiveData<String> = _tiempoUltimoTrago

    /** LiveData privado para la última vez que se consumió alcohol, por defecto vacío. */
    private val _ultimaVezAlcohol = MutableLiveData("")
    /** LiveData público para observar la última vez que se consumió alcohol. */
    val ultimaVezAlcohol: LiveData<String> = _ultimaVezAlcohol

    /** LiveData privado para la conciencia de los peligros, por defecto vacío. */
    private val _conscientePeligros = MutableLiveData("")
    /** LiveData público para observar la conciencia de los peligros. */
    val conscientePeligros: LiveData<String> = _conscientePeligros

    /** LiveData privado para una declaración adicional, por defecto vacío. */
    private val _declaracionAdicional = MutableLiveData("")
    /** LiveData público para observar una declaración adicional. */
    val declaracionAdicional: LiveData<String> = _declaracionAdicional

    /**
     * Inicializa el ViewModel cargando los datos guardados desde DataStore.
     */
    init {
        loadSavedData()
    }

    // Funciones para actualizar los estados
    /**
     * Establece si el investigado desea declarar.
     *
     * @param value Nuevo valor para el deseo de declarar.
     */
    fun setDeseaDeclarar(value: Boolean) {
        _deseaDeclarar.value = value
    }

    /**
     * Establece si el investigado renuncia expresamente a un letrado.
     *
     * @param value Nuevo valor para la renuncia expresa a un letrado.
     */
    fun setRenunciaExpresaLletrado(value: Boolean) {
        _renunciaExpresaLletrado.value = value
    }

    /**
     * Establece las condiciones en las que se realiza la manifestación.
     *
     * @param value Nuevas condiciones para la manifestación.
     */
    fun setCondicionesParaManifestacion(value: String) {
        _condicionesParaManifestacion.value = value
    }

    /**
     * Establece el consumo de alcohol declarado por el investigado.
     *
     * @param value Nuevo consumo de alcohol declarado.
     */
    fun setConsumoAlcohol(value: String) {
        _consumoAlcohol.value = value
    }

    /**
     * Establece la procedencia del investigado.
     *
     * @param value Nueva procedencia del investigado.
     */
    fun setProcedencia(value: String) {
        _procedencia.value = value
    }

    /**
     * Establece información sobre enfermedades o medicamentos que puedan afectar al investigado.
     *
     * @param value Nueva información sobre enfermedades o medicamentos.
     */
    fun setEnfermedadMedicamentos(value: String) {
        _enfermedadMedicamentos.value = value
    }

    /**
     * Establece el tiempo transcurrido desde el último trago de alcohol.
     *
     * @param value Nuevo tiempo desde el último trago.
     */
    fun setTiempoUltimoTrago(value: String) {
        _tiempoUltimoTrago.value = value
    }

    /**
     * Establece la última vez que el investigado consumió alcohol.
     *
     * @param value Nueva última vez de consumo de alcohol.
     */
    fun setUltimaVezAlcohol(value: String) {
        _ultimaVezAlcohol.value = value
    }

    /**
     * Establece si el investigado es consciente de los peligros de conducir bajo los efectos del alcohol.
     *
     * @param value Nueva declaración sobre la conciencia de los peligros.
     */
    fun setConscientePeligros(value: String) {
        _conscientePeligros.value = value
    }

    /**
     * Establece una declaración adicional del investigado.
     *
     * @param value Nueva declaración adicional.
     */
    fun setDeclaracionAdicional(value: String) {
        _declaracionAdicional.value = value
    }

    /**
     * Guarda los datos actuales en DataStore de manera asíncrona.
     */
    fun guardarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreManifestacionAlcohol.edit { preferences ->
                _deseaDeclarar.value?.let { preferences[PreferencesKeys.DESEA_DECLARAR] = it }
                _renunciaExpresaLletrado.value?.let {
                    preferences[PreferencesKeys.RENUNCIA_EXPRESA_LETRADO] = it
                }
                _condicionesParaManifestacion.value?.let {
                    preferences[PreferencesKeys.CONDICIONES_PARA_MANIFESTACION] = it
                }
                _consumoAlcohol.value?.let { preferences[PreferencesKeys.CONSUMO_ALCOHOL] = it }
                _procedencia.value?.let { preferences[PreferencesKeys.PROCEDENCIA] = it }
                _enfermedadMedicamentos.value?.let {
                    preferences[PreferencesKeys.ENFERMEDAD_MEDICAMENTOS] = it
                }
                _tiempoUltimoTrago.value?.let {
                    preferences[PreferencesKeys.TIEMPO_ULTIMO_TRAGO] = it
                }
                _ultimaVezAlcohol.value?.let {
                    preferences[PreferencesKeys.ULTIMA_VEZ_ALCOHOL] = it
                }
                _conscientePeligros.value?.let {
                    preferences[PreferencesKeys.CONSCIENTE_PELIGROS] = it
                }
                _declaracionAdicional.value?.let {
                    preferences[PreferencesKeys.DECLARACION_ADICIONAL] = it
                }
            }
        }
    }

    /**
     * Limpia todos los datos almacenados en DataStore y restablece los valores por defecto en el ViewModel.
     */
    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreManifestacionAlcohol.edit { preferences ->
                preferences.clear()
            }

            // Restablecer los valores por defecto
            _deseaDeclarar.value = false
            _renunciaExpresaLletrado.value = false
            _condicionesParaManifestacion.value = ""
            _consumoAlcohol.value = ""
            _procedencia.value = ""
            _enfermedadMedicamentos.value = ""
            _tiempoUltimoTrago.value = ""
            _ultimaVezAlcohol.value = ""
            _conscientePeligros.value = ""
            _declaracionAdicional.value = ""
        }
    }

    /**
     * Carga los datos guardados en DataStore al inicializar el ViewModel.
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences =
                getApplication<Application>().dataStoreManifestacionAlcohol.data.first()

            // Leer valores Boolean
            _deseaDeclarar.value = preferences[PreferencesKeys.DESEA_DECLARAR] ?: false
            _renunciaExpresaLletrado.value =
                preferences[PreferencesKeys.RENUNCIA_EXPRESA_LETRADO] ?: false

            // Leer valores String
            _condicionesParaManifestacion.value =
                preferences[PreferencesKeys.CONDICIONES_PARA_MANIFESTACION] ?: ""
            _consumoAlcohol.value = preferences[PreferencesKeys.CONSUMO_ALCOHOL] ?: ""
            _procedencia.value = preferences[PreferencesKeys.PROCEDENCIA] ?: ""
            _enfermedadMedicamentos.value =
                preferences[PreferencesKeys.ENFERMEDAD_MEDICAMENTOS] ?: ""
            _tiempoUltimoTrago.value = preferences[PreferencesKeys.TIEMPO_ULTIMO_TRAGO] ?: ""
            _ultimaVezAlcohol.value = preferences[PreferencesKeys.ULTIMA_VEZ_ALCOHOL] ?: ""
            _conscientePeligros.value = preferences[PreferencesKeys.CONSCIENTE_PELIGROS] ?: ""
            _declaracionAdicional.value = preferences[PreferencesKeys.DECLARACION_ADICIONAL] ?: ""
        }
    }
}