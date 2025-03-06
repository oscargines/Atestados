package com.oscar.atestados.viewModel

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.screens.dataStoreImp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar datos relacionados con otros documentos, específicamente la selección de impresoras.
 * Utiliza StateFlow para observar cambios en la impresora seleccionada y DataStore para cargar los datos persistidos.
 *
 * @property context Contexto de la aplicación necesario para acceder a DataStore.
 */
class OtrosDocumentosViewModel(private val context: Context) : ViewModel() {

    /** Flujo de estado privado que contiene el nombre y MAC de la impresora seleccionada como una cadena. */
    private val _selectedPrinter = MutableStateFlow<String?>(null)
    /** Flujo de estado público para observar la impresora seleccionada. */
    val selectedPrinter: StateFlow<String?> = _selectedPrinter.asStateFlow()

    /**
     * Inicializa el ViewModel cargando la impresora seleccionada desde DataStore.
     */
    init {
        loadSelectedPrinter()
    }

    /**
     * Carga la impresora seleccionada desde DataStore de manera asíncrona.
     * Combina el nombre y la dirección MAC en una sola cadena para su presentación.
     */
    private fun loadSelectedPrinter() {
        viewModelScope.launch {
            val preferences = context.dataStoreImp.data.first()
            val name = preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")]
            val mac = preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
            if (name != null && mac != null) {
                _selectedPrinter.value = "$name ($mac)"
            } else {
                _selectedPrinter.value = "Ninguna impresora seleccionada"
            }
        }
    }
}