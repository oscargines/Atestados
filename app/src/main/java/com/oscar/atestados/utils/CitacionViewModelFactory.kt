package com.oscar.atestados.utils

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oscar.atestados.viewModel.CitacionViewModel

/**
 * Fábrica para crear instancias de [CitacionViewModel].
 *
 * @param application Aplicación Android necesaria para inicializar el ViewModel.
 */
class CitacionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CitacionViewModel::class.java)) {
            return CitacionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}