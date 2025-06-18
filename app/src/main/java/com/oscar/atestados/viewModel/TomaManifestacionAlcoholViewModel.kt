package com.oscar.atestados.viewModel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar los datos relacionados con la toma de manifestación del investigado en casos de alcohol.
 * Utiliza LiveData para observar cambios en los datos y DataStore para persistirlos.
 *
 * @property application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class TomaManifestacionAlcoholViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Objeto interno que define las claves utilizadas para almacenar datos en DataStore.
     * Las claves tienen un prefijo "manifestacion_" para evitar conflictos con otros datos.
     */
    private object PreferencesKeys {
        val DESEA_DECLARAR = booleanPreferencesKey("manifestacion_desea_declarar")
        val RENUNCIA_EXPRESA_LETRADO = booleanPreferencesKey("manifestacion_renuncia_expresa_letrado")
        val CONDICIONES_PARA_MANIFESTACION = stringPreferencesKey("manifestacion_condiciones_para_manifestacion")
        val CONSUMO_ALCOHOL = stringPreferencesKey("manifestacion_consumo_alcohol")
        val PROCEDENCIA = stringPreferencesKey("manifestacion_procedencia")
        val ENFERMEDAD_MEDICAMENTOS = stringPreferencesKey("manifestacion_enfermedad_medicamentos")
        val TIEMPO_ULTIMO_TRAGO = stringPreferencesKey("manifestacion_tiempo_ultimo_trago")
        val ULTIMA_VEZ_ALCOHOL = stringPreferencesKey("manifestacion_ultima_vez_alcohol")
        val CONSCIENTE_PELIGROS = stringPreferencesKey("manifestacion_consciente_peligros")
        val DECLARACION_ADICIONAL = stringPreferencesKey("manifestacion_declaracion_adicional")
    }

    // Estados para los switches y campos de texto
    private val _deseaDeclarar = MutableLiveData(false)
    val deseaDeclarar: LiveData<Boolean> = _deseaDeclarar

    private val _renunciaExpresaLletrado = MutableLiveData(false)
    val renunciaExpresaLletrado: LiveData<Boolean> = _renunciaExpresaLletrado

    private val _condicionesParaManifestacion = MutableLiveData("")
    val condicionesParaManifestacion: LiveData<String> = _condicionesParaManifestacion

    private val _consumoAlcohol = MutableLiveData("")
    val consumoAlcohol: LiveData<String> = _consumoAlcohol

    private val _procedencia = MutableLiveData("")
    val procedencia: LiveData<String> = _procedencia

    private val _enfermedadMedicamentos = MutableLiveData("")
    val enfermedadMedicamentos: LiveData<String> = _enfermedadMedicamentos

    private val _tiempoUltimoTrago = MutableLiveData("")
    val tiempoUltimoTrago: LiveData<String> = _tiempoUltimoTrago

    private val _ultimaVezAlcohol = MutableLiveData("")
    val ultimaVezAlcohol: LiveData<String> = _ultimaVezAlcohol

    private val _conscientePeligros = MutableLiveData("")
    val conscientePeligros: LiveData<String> = _conscientePeligros

    private val _declaracionAdicional = MutableLiveData("")
    val declaracionAdicional: LiveData<String> = _declaracionAdicional

    init {
        loadSavedData()
    }

    fun setDeseaDeclarar(value: Boolean) {
        _deseaDeclarar.value = value
    }

    fun setRenunciaExpresaLletrado(value: Boolean) {
        _renunciaExpresaLletrado.value = value
    }

    fun setCondicionesParaManifestacion(value: String) {
        _condicionesParaManifestacion.value = value
    }

    fun setConsumoAlcohol(value: String) {
        _consumoAlcohol.value = value
    }

    fun setProcedencia(value: String) {
        _procedencia.value = value
    }

    fun setEnfermedadMedicamentos(value: String) {
        _enfermedadMedicamentos.value = value
    }

    fun setTiempoUltimoTrago(value: String) {
        _tiempoUltimoTrago.value = value
    }

    fun setUltimaVezAlcohol(value: String) {
        _ultimaVezAlcohol.value = value
    }

    fun setConscientePeligros(value: String) {
        _conscientePeligros.value = value
    }

    fun setDeclaracionAdicional(value: String) {
        _declaracionAdicional.value = value
    }

    fun guardarDatos(context: android.content.Context) {
        viewModelScope.launch {
            context.dataStoreAlcoholemia.edit { preferences ->
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

    fun limpiarDatos() {
        viewModelScope.launch {
            getApplication<Application>().dataStoreAlcoholemia.edit { preferences ->
                preferences.remove(PreferencesKeys.DESEA_DECLARAR)
                preferences.remove(PreferencesKeys.RENUNCIA_EXPRESA_LETRADO)
                preferences.remove(PreferencesKeys.CONDICIONES_PARA_MANIFESTACION)
                preferences.remove(PreferencesKeys.CONSUMO_ALCOHOL)
                preferences.remove(PreferencesKeys.PROCEDENCIA)
                preferences.remove(PreferencesKeys.ENFERMEDAD_MEDICAMENTOS)
                preferences.remove(PreferencesKeys.TIEMPO_ULTIMO_TRAGO)
                preferences.remove(PreferencesKeys.ULTIMA_VEZ_ALCOHOL)
                preferences.remove(PreferencesKeys.CONSCIENTE_PELIGROS)
                preferences.remove(PreferencesKeys.DECLARACION_ADICIONAL)
            }

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

    private fun loadSavedData() {
        viewModelScope.launch {
            val preferences = getApplication<Application>().dataStoreAlcoholemia.data.first()

            _deseaDeclarar.value = preferences[PreferencesKeys.DESEA_DECLARAR] ?: false
            _renunciaExpresaLletrado.value = preferences[PreferencesKeys.RENUNCIA_EXPRESA_LETRADO] ?: false
            _condicionesParaManifestacion.value = preferences[PreferencesKeys.CONDICIONES_PARA_MANIFESTACION] ?: ""
            _consumoAlcohol.value = preferences[PreferencesKeys.CONSUMO_ALCOHOL] ?: ""
            _procedencia.value = preferences[PreferencesKeys.PROCEDENCIA] ?: ""
            _enfermedadMedicamentos.value = preferences[PreferencesKeys.ENFERMEDAD_MEDICAMENTOS] ?: ""
            _tiempoUltimoTrago.value = preferences[PreferencesKeys.TIEMPO_ULTIMO_TRAGO] ?: ""
            _ultimaVezAlcohol.value = preferences[PreferencesKeys.ULTIMA_VEZ_ALCOHOL] ?: ""
            _conscientePeligros.value = preferences[PreferencesKeys.CONSCIENTE_PELIGROS] ?: ""
            _declaracionAdicional.value = preferences[PreferencesKeys.DECLARACION_ADICIONAL] ?: ""
        }
    }
}