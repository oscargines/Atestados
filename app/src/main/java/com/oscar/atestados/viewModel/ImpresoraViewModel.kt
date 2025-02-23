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
import com.oscar.atestados.screens.dataStoreImp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
class ImpresoraViewModel(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModel() {

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceDB?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceDB?> = _selectedDevice.asStateFlow()

    private val database = AccesoBaseDatos(context, "dispositivos.db", 1)

    private val _savedDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val savedDevices: StateFlow<List<BluetoothDeviceDB>> = _savedDevices.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDeviceDB>> = _foundDevices.asStateFlow()

    private val _uiState = MutableStateFlow(ImpresoraUiState())
    val uiState: StateFlow<ImpresoraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.ensureTableExists()
            loadSavedDevicesDB()
            loadSelectedDeviceFromPreferences() // Cargar dispositivo seleccionado al iniciar
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
                Log.d("ImpresoraViewModel", "Dispositivo guardado en DB: ${device.nombre}")
            }

            // Guardar en DataStore
            context.dataStoreImp.edit { preferences ->
                preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")] = device.nombre
                preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")] = device.mac
            }
            Log.d("ImpresoraViewModel", "Dispositivo guardado en DataStore: ${device.nombre}")

            // Actualizar lista
            loadSavedDevicesDB()
        }
    }

    private suspend fun loadSelectedDeviceFromPreferences() {
        val preferences = context.dataStoreImp.data.first()
        val name = preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")]
        val mac = preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
        if (name != null && mac != null) {
            _selectedDevice.value = BluetoothDeviceDB(nombre = name, mac = mac)
            Log.d("ImpresoraViewModel", "Dispositivo cargado desde preferencias: $name ($mac)")
        }
    }

    private fun setupBluetoothObservers() {
        viewModelScope.launch {
            bluetoothViewModel.devices.collect { devices ->
                _foundDevices.value = devices.mapToDeviceDB()
                updateScanningState(bluetoothViewModel.isScanningState.value)
            }
        }

        viewModelScope.launch {
            bluetoothViewModel.isScanningState.collect { isScanning ->
                updateScanningState(isScanning)
            }
        }
    }

    private fun updateScanningState(isScanning: Boolean) {
        _uiState.update { it.copy(isScanning = isScanning) }
    }

    private fun List<BluetoothDevice>.mapToDeviceDB(): List<BluetoothDeviceDB> {
        return this.map { device ->
            BluetoothDeviceDB(
                nombre = device.name ?: "Dispositivo desconocido",
                mac = device.address
            )
        }.distinctBy { it.mac }
    }

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

    fun startDiscovery() {
        viewModelScope.launch {
            stopDiscovery()
            delay(500)
            _foundDevices.value = emptyList()
            bluetoothViewModel.startScan()
        }
    }

    fun stopDiscovery() {
        bluetoothViewModel.stopScan()
    }

    fun handleDeviceAction(device: BluetoothDeviceDB): DeviceActionResult {
        return if (isDeviceSaved(device.mac)) {
            DeviceActionResult.AlreadyPaired
        } else {
            saveDeviceToDatabase(device)
            DeviceActionResult.SuccessfullyPaired
        }
    }

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
                Log.e("ImpresoraViewModel", "Error guardando el dispositivo: ${e.message}")
            }
        }
    }

    private fun isDeviceSaved(mac: String): Boolean {
        return try {
            val result = database.query("SELECT COUNT(*) FROM dispositivos WHERE mac = '$mac'")
            result.isNotEmpty() && (result[0]["COUNT(*)"] as? Long ?: 0) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllDevices(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.execSQL("DELETE FROM dispositivos")
                context.dataStoreImp.edit { preferences ->
                    preferences.clear()
                }
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

    data class ImpresoraUiState(
        val isScanning: Boolean = false,
        val errorMessage: String? = null
    )

    sealed class DeviceActionResult {
        object AlreadyPaired : DeviceActionResult()
        object SuccessfullyPaired : DeviceActionResult()
    }
}