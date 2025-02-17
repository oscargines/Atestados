package com.oscar.atestados.viewModel


import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map

class AlcoholemiaUnoViewModel(application: Application) : AndroidViewModel(application) {
    //Opciones del motivo por el que se intruyen diligencias
    private val _opcionMotivo = MutableLiveData<String>()
    val opcionMotivo: LiveData<String> = _opcionMotivo

    // Campos de texto
    private val _marca = MutableLiveData<String>()
    val marca: LiveData<String> = _marca

    private val _modelo = MutableLiveData<String>()
    val modelo: LiveData<String> = _modelo

    private val _serie = MutableLiveData<String>()
    val serie: LiveData<String> = _serie

    private val _caducidad = MutableLiveData<String>()
    val caducidad: LiveData<String> = _caducidad

    private val _primeraTasa = MutableLiveData<String>()
    val primeraTasa: LiveData<String> = _primeraTasa

    private val _primeraHora = MutableLiveData<String>()
    val primeraHora: LiveData<String> = _primeraHora

    // Estados de los checkboxes
    private val _checkboxesState = MutableLiveData<MutableMap<String, Boolean>>(mutableMapOf())

    // Tasa y hora de segunda prueba
    private val _segundaTasa = MutableLiveData<String>()
    val segundaTasa: LiveData<String> = _segundaTasa

    private val _segundaHora = MutableLiveData<String>()
    val segundaHora: LiveData<String> = _segundaHora

    // Estado de segunda prueba habilitada
    private val _segundaPruebaHabilitada = MutableLiveData<Boolean>(false)
    val segundaPruebaHabilitada: LiveData<Boolean> = _segundaPruebaHabilitada

    fun setOpcionMotivo(opcionMotivo: String) {
        _opcionMotivo.value = opcionMotivo
    }
    // Actualizadores de campos
    fun updateMarca(value: String) { _marca.value = value }
    fun updateModelo(value: String) { _modelo.value = value }
    fun updateSerie(value: String) { _serie.value = value }
    fun updateCaducidad(value: String) { _caducidad.value = value }
    fun updatePrimeraTasa(value: String) { _primeraTasa.value = value }
    fun updatePrimeraHora(value: String) { _primeraHora.value = value }
    fun updateSegundaTasa(value: String) { _segundaTasa.value = value }
    fun updateSegundaHora(value: String) { _segundaHora.value = value }

    // Manejo de checkboxes
    fun getCheckboxState(key: String): LiveData<Boolean> {
        return _checkboxesState.map { states ->
            states?.get(key) ?: false
        }
    }

    fun updateCheckboxState(key: String, checked: Boolean) {
        val currentStates = _checkboxesState.value ?: mutableMapOf()
        currentStates[key] = checked
        _checkboxesState.value = currentStates

        // Lógica para habilitar segunda prueba si se selecciona "Sí"
        if (key == "Sí") {
            _segundaPruebaHabilitada.value = checked
        }
    }

    // Operaciones de guardado y limpieza
    fun guardarDatos(context: Context) {
        // Lógica para guardar en DataStore/Base de Datos
    }

    fun limpiarDatos() {
        _marca.value = ""
        _modelo.value = ""
        _serie.value = ""
        _caducidad.value = ""
        _primeraTasa.value = ""
        _primeraHora.value = ""
        _segundaTasa.value = ""
        _segundaHora.value = ""
        _checkboxesState.value?.clear()
        _segundaPruebaHabilitada.value = false
    }
}