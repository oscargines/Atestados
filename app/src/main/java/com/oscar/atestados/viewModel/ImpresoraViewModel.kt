package com.oscar.atestados.viewModel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.data.BluetoothDeviceDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para la gestión de impresoras Bluetooth.
 *
 * Maneja las operaciones de descubrimiento de dispositivos, almacenamiento en base de datos
 * y sincronización entre el estado de Bluetooth y la interfaz de usuario.
 *
 * @property bluetoothViewModel ViewModel para operaciones Bluetooth de bajo nivel
 * @property context Contexto de la aplicación para operaciones de base de datos
 * @constructor Crea una instancia del ViewModel con las dependencias necesarias
 */
@SuppressLint("MissingPermission")
class ImpresoraViewModel(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModel() {

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceDB?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceDB?> = _selectedDevice.asStateFlow()

    /**
     * Instancia de acceso a la base de datos SQLite para dispositivos guardados
     */
    private val database = AccesoBaseDatos(context, "dispositivos.db", 1)

    /**
     * Flujo de estado con la lista de dispositivos guardados en la base de datos
     */
    private val _savedDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val savedDevices: StateFlow<List<BluetoothDeviceDB>> = _savedDevices.asStateFlow()

    /**
     * Flujo de estado con la lista de dispositivos encontrados durante el escaneo
     */
    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDeviceDB>> = _foundDevices.asStateFlow()

    /**
     * Estado general de la UI relacionado con impresoras
     */
    private val _uiState = MutableStateFlow(ImpresoraUiState())
    val uiState: StateFlow<ImpresoraUiState> = _uiState.asStateFlow()

    init {
        // Inicialización de la base de datos y observadores Bluetooth
        viewModelScope.launch(Dispatchers.IO) {
            database.ensureTableExists()
            loadSavedDevicesDB()
        }
        setupBluetoothObservers()
    }

    fun selectDevice(device: BluetoothDeviceDB) {
        _selectedDevice.value = device
    }

    fun saveSelectedDevice() {
        viewModelScope.launch {
            val device = _selectedDevice.value ?: return@launch

            // Guardar en base de datos si no existe
            if (!isDeviceSaved(device.mac)) {
                saveDeviceToDatabase(device)
                Log.d("ImpresoraViewModel", "Dispositivo guardado: ${device.nombre}")
            }

            // Guardar en DataStore
            context.dataStoreAlcoholemia.edit { preferences ->
                preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")] = device.nombre
                preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")] = device.mac
            }

            // Actualizar lista
            loadSavedDevicesDB()
        }
    }

    /**
     * Configura los observadores para los cambios en dispositivos Bluetooth y estado de escaneo
     */
    private fun setupBluetoothObservers() {
        // Observador de dispositivos Bluetooth encontrados
        viewModelScope.launch {
            bluetoothViewModel.devices.collect { devices ->
                _foundDevices.value = devices.mapToDeviceDB()
                updateScanningState(bluetoothViewModel.isScanningState.value)
            }
        }

        // Observador del estado de escaneo Bluetooth
        viewModelScope.launch {
            bluetoothViewModel.isScanningState.collect { isScanning ->
                updateScanningState(isScanning)
            }
        }
    }

    /**
     * Actualiza el estado de escaneo en el UI State
     * @param isScanning Indica si el escaneo está activo
     */
    private fun updateScanningState(isScanning: Boolean) {
        _uiState.update { it.copy(isScanning = isScanning) }
    }

    /**
     * Convierte una lista de dispositivos Bluetooth nativos a nuestro modelo de datos
     * @receiver Lista de dispositivos Bluetooth del sistema
     * @return Lista de [BluetoothDeviceDB] mapeados
     */
    private fun List<BluetoothDevice>.mapToDeviceDB(): List<BluetoothDeviceDB> {
        return this.map { device ->
            BluetoothDeviceDB(
                nombre = device.name ?: "Dispositivo desconocido",
                mac = device.address
            )
        }.distinctBy { it.mac } // Elimina dispositivos duplicados
    }

    /**
     * Carga los dispositivos guardados desde la base de datos
     * @throws SQLException Si ocurre un error en la operación de base de datos
     */
    fun loadSavedDevicesDB() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dispositivos = database.query("SELECT nombre, mac FROM dispositivos").map { row ->
                    BluetoothDeviceDB(
                        nombre = row["nombre"] as? String ?: "Desconocido",
                        mac = row["mac"] as? String ?: ""
                    )
                }
                _savedDevices.value = dispositivos
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error loading devices: ${e.message}")
                _savedDevices.value = emptyList()
            }
        }
    }

    /**
     * Inicia el proceso de descubrimiento de dispositivos Bluetooth
     */
    fun startDiscovery() {
        viewModelScope.launch {
            stopDiscovery()
            delay(500)
            _foundDevices.value = emptyList()
            bluetoothViewModel.startScan()
        }
    }

    /**
     * Detiene el proceso de descubrimiento de dispositivos Bluetooth
     */
    fun stopDiscovery() {
        bluetoothViewModel.stopScan()
    }

    /**
     * Maneja la acción de usuario sobre un dispositivo (guardar/emparejar)
     * @param device Dispositivo Bluetooth seleccionado
     * @return [DeviceActionResult] con el resultado de la operación
     */
    fun handleDeviceAction(device: BluetoothDeviceDB): DeviceActionResult {
        return if (isDeviceSaved(device.mac)) {
            DeviceActionResult.AlreadyPaired
        } else {
            saveDeviceToDatabase(device)
            DeviceActionResult.SuccessfullyPaired
        }
    }

    /**
     * Guarda un dispositivo en la base de datos
     * @param device Dispositivo a guardar
     * @throws SQLException Si ocurre un error en la operación de inserción
     */
    fun saveDeviceToDatabase(device: BluetoothDeviceDB) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isDeviceSaved(device.mac)) {
                    database.execSQL(
                        "INSERT OR IGNORE INTO dispositivos (nombre, mac) VALUES (?, ?)",
                        arrayOf(device.nombre, device.mac)
                    )
                }
                loadSavedDevicesDB()
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error guardando el dispositivio: ${e.message}")
            }
        }
    }

    /**
     * Verifica si un dispositivo ya está guardado en la base de datos
     * @param mac Dirección MAC del dispositivo a verificar
     * @return `true` si el dispositivo existe, `false` en caso contrario
     */
    private fun isDeviceSaved(mac: String): Boolean {
        return try {
            val result = database.query("SELECT COUNT(*) FROM dispositivos WHERE mac = '$mac'")
            result.isNotEmpty() && (result[0]["COUNT(*)"] as? Long ?: 0) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Elimina todos los dispositivos guardados de la base de datos
     * @throws SQLException Si ocurre un error en la operación de borrado
     */
    fun clearAllDevices(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Limpiar base de datos
                database.execSQL("DELETE FROM dispositivos")

                // Limpiar preferencias
                context.dataStoreAlcoholemia.edit { preferences ->
                    preferences.clear()
                }

                // Resetear estados
                _selectedDevice.value = null
                loadSavedDevicesDB()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Datos borrados correctamente", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error clearing devices: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al borrar los datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Estado de la UI para la gestión de impresoras
     * @property isScanning Indica si está activo el escaneo Bluetooth
     * @property errorMessage Mensaje de error para mostrar al usuario
     */
    data class ImpresoraUiState(
        val isScanning: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Resultados de las acciones sobre dispositivos
     */
    sealed class DeviceActionResult {
        /**
         * Indica que el dispositivo ya estaba emparejado
         */
        object AlreadyPaired : DeviceActionResult()

        /**
         * Indica que el emparejamiento fue exitoso
         */
        object SuccessfullyPaired : DeviceActionResult()
    }
}