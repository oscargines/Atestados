package com.oscar.atestados.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Fábrica para crear instancias de [OtrosDocumentosViewModel].
 * Proporciona una forma de inyectar el contexto en el ViewModel al momento de su creación.
 *
 * @property context Contexto de la aplicación necesario para inicializar el ViewModel.
 */
class OtrosDocumentosViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    /**
     * Crea una nueva instancia del ViewModel solicitado.
     * Verifica si el modelo solicitado es [OtrosDocumentosViewModel] y lo instancia con el contexto proporcionado.
     *
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Una instancia del ViewModel solicitado.
     * @throws IllegalArgumentException Si la clase del ViewModel no es reconocida.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtrosDocumentosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtrosDocumentosViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}