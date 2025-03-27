package com.oscar.atestados.utils
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel

/**
 * Fábrica para crear instancias de [AlcoholemiaDosViewModel].
 *
 * @param application Aplicación Android necesaria para inicializar el ViewModel.
 */
class AlcoholemiaDosViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlcoholemiaDosViewModel::class.java)) {
            return AlcoholemiaDosViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}