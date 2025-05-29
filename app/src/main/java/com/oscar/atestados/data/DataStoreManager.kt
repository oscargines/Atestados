package com.oscar.atestados.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Define el DataStore como un singleton
object DataStoreManager {
    private const val TOMA_DERECHOS_DATASTORE_NAME = "toma_derechos_settings"

    // Mapa para almacenar instancias de DataStore por contexto
    private val dataStoreCache = mutableMapOf<Context, DataStore<Preferences>>()

    // Método para obtener el DataStore
    fun getTomaDerechosDataStore(context: Context): DataStore<Preferences> {
        return dataStoreCache.getOrPut(context.applicationContext) {
            context.applicationContext.dataStore
        }
    }

    // Propiedad delegada para el DataStore
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = TOMA_DERECHOS_DATASTORE_NAME
    )
}