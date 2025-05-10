package com.oscar.atestados.utils

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

// Configuración de DataStore para el caché
val Context.geocodingCache: DataStore<Preferences> by preferencesDataStore(name = "geocoding_cache")

object GeocodingUtils {
    private const val TAG = "GeocodingUtils"
    private const val BIGDATACLOUD_URL = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%s&longitude=%s&localityLanguage=es"

    /**
     * Obtiene el municipio a partir de coordenadas usando Geocoder como primera opción y BigDataCloud como respaldo.
     *
     * @param context Contexto de la aplicación para Geocoder y DataStore.
     * @param latitude Latitud de la ubicación.
     * @param longitude Longitud de la ubicación.
     * @return Nombre del municipio si se encuentra, o cadena vacía si no se encuentra.
     */
    suspend fun getMunicipalityFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String {
        // Verificar caché primero
        val cacheKey = stringPreferencesKey("municipality_${latitude}_${longitude}")
        val cachedMunicipality = withContext(Dispatchers.IO) {
            context.geocodingCache.data.first()[cacheKey]
        }
        if (!cachedMunicipality.isNullOrEmpty()) {
            Log.d(TAG, "Municipio obtenido del caché: $cachedMunicipality")
            return cachedMunicipality
        }

        // Intentar con Geocoder primero
        try {
            val geocoder = Geocoder(context, Locale("es", "ES"))
            val addresses = withContext(Dispatchers.IO) {
                geocoder.getFromLocation(latitude, longitude, 1)
            }
            if (addresses?.isNotEmpty() == true) {
                val municipality = addresses[0].locality ?: ""
                if (municipality.isNotEmpty()) {
                    // Guardar en caché
                    withContext(Dispatchers.IO) {
                        context.geocodingCache.edit { preferences ->
                            preferences[cacheKey] = municipality
                        }
                    }
                    Log.d(TAG, "Municipio encontrado con Geocoder: $municipality")
                    return municipality
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en Geocoder: ${e.message}", e)
        }

        // Respaldo con BigDataCloud
        try {
            val municipality = fetchMunicipalityFromBigDataCloud(latitude, longitude)
            if (municipality.isNotEmpty()) {
                // Guardar en caché
                withContext(Dispatchers.IO) {
                    context.geocodingCache.edit { preferences ->
                        preferences[cacheKey] = municipality
                    }
                }
                Log.d(TAG, "Municipio encontrado con BigDataCloud: $municipality")
                return municipality
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error en BigDataCloud: ${e.message}", e)
        }

        Log.w(TAG, "No se encontró municipio para las coordenadas ($latitude, $longitude)")
        return ""
    }

    /**
     * Realiza una solicitud HTTP a BigDataCloud para obtener el municipio.
     *
     * @param latitude Latitud de la ubicación.
     * @param longitude Longitud de la ubicación.
     * @return Nombre del municipio o cadena vacía si no se encuentra.
     */
    private suspend fun fetchMunicipalityFromBigDataCloud(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val url = BIGDATACLOUD_URL.format(latitude, longitude)
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AtestadosApp/1.0 (contacto@ejemplo.com)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Error en respuesta de BigDataCloud: ${response.code}")
                    return@withContext ""
                }

                val jsonString = response.body?.string() ?: return@withContext ""
                val json = JSONObject(jsonString)
                val locality = json.optString("locality", "")
                locality.takeIf { it.isNotEmpty() } ?: ""
            }
        }
    }
}