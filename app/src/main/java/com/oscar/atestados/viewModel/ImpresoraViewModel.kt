package com.oscar.atestados.viewModel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.data.BluetoothDeviceDB
import com.oscar.atestados.screens.dataStoreImp
import com.oscar.atestados.utils.ZebraPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class ImpresoraViewModel(
    val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModel() {

    private val printerHelper = ZebraPrinterHelper(context)

    /** Instancia de la base de datos para almacenar dispositivos Bluetooth. */
    private val database = AccesoBaseDatos(context, "dispositivos.db", 1)

    /** Flujo de estado que contiene la lista de dispositivos Bluetooth guardados en la base de datos. */
    private val _savedDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val savedDevices: StateFlow<List<BluetoothDeviceDB>> = _savedDevices.asStateFlow()

    /** Flujo de estado que contiene la lista de dispositivos Bluetooth encontrados durante el escaneo. */
    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDeviceDB>> = _foundDevices.asStateFlow()

    /** Flujo de estado que contiene el estado de la interfaz de usuario (escaneo, errores, etc.). */
    private val _uiState = MutableStateFlow(ImpresoraUiState())
    val uiState: StateFlow<ImpresoraUiState> = _uiState.asStateFlow()

    /** Adaptador Bluetooth para obtener dispositivos reales. */
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.v("ImpresoraViewModel", "Inicializando ViewModel")
                database.ensureTableExists()
                loadSavedDevicesDB()
                loadSelectedDeviceFromPreferences()
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error en inicialización: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error al inicializar: ${e.message}") }
            }
        }
        setupBluetoothObservers()
    }

    /** Selecciona un dispositivo Bluetooth como el dispositivo activo. */
    fun selectDevice(device: BluetoothDeviceDB) {
        viewModelScope.launch {
            val btDevice = bluetoothViewModel.devices.value.find { it.address == device.mac }
            if (btDevice != null) {
                Log.d("ImpresoraViewModel", "Seleccionando dispositivo: ${device.nombre} (${device.mac})")
                bluetoothViewModel.selectDevice(btDevice)
                saveSelectedDevice()
            } else {
                Log.w("ImpresoraViewModel", "Dispositivo no encontrado en la lista: ${device.mac}")
                _uiState.update { it.copy(errorMessage = "Dispositivo no encontrado: ${device.mac}") }
            }
        }
    }

    /** Guarda el dispositivo seleccionado en la base de datos y en DataStore. */
    fun saveSelectedDevice() {
        viewModelScope.launch {
            val device = bluetoothViewModel.selectedDevice.value ?: run {
                Log.w("ImpresoraViewModel", "No hay dispositivo seleccionado para guardar")
                return@launch
            }
            val deviceDB = BluetoothDeviceDB(nombre = device.name ?: "Desconocido", mac = device.address)
            Log.d("ImpresoraViewModel", "Guardando dispositivo seleccionado: ${deviceDB.nombre} (${deviceDB.mac})")

            if (!isDeviceSaved(deviceDB.mac)) {
                saveDeviceToDatabase(deviceDB)
                Log.d("ImpresoraViewModel", "Dispositivo guardado en DB: ${deviceDB.nombre}")
            } else {
                Log.d("ImpresoraViewModel", "Dispositivo ya estaba guardado: ${deviceDB.mac}")
            }

            context.dataStoreImp.edit { preferences ->
                preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")] = deviceDB.nombre
                preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")] = deviceDB.mac
            }
            Log.d("ImpresoraViewModel", "Dispositivo guardado en DataStore: ${deviceDB.nombre}")

            loadSavedDevicesDB()
        }
    }

    private suspend fun loadSelectedDeviceFromPreferences() {
        try {
            val preferences = context.dataStoreImp.data.first()
            val name = preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")]
            val mac = preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
            if (name != null && mac != null) {
                val device = bluetoothViewModel.devices.value.find { it.address == mac }
                    ?: bluetoothAdapter?.getRemoteDevice(mac)
                if (device != null) {
                    bluetoothViewModel.selectDevice(device)
                    Log.d("ImpresoraViewModel", "Dispositivo cargado desde preferencias: $name ($mac)")
                } else {
                    Log.w("ImpresoraViewModel", "No se pudo cargar el dispositivo desde preferencias: $mac")
                }
            } else {
                Log.v("ImpresoraViewModel", "No hay dispositivo guardado en preferencias")
            }
        } catch (e: Exception) {
            Log.e("ImpresoraViewModel", "Error cargando dispositivo desde preferencias: ${e.message}", e)
        }
    }

    private fun setupBluetoothObservers() {
        viewModelScope.launch {
            bluetoothViewModel.devices.collect { devices ->
                _foundDevices.value = devices.mapToDeviceDB()
                updateScanningState(bluetoothViewModel.isScanningState.value)
                Log.v("ImpresoraViewModel", "Dispositivos encontrados actualizados: ${devices.size}")
            }
        }

        viewModelScope.launch {
            bluetoothViewModel.isScanningState.collect { isScanning ->
                updateScanningState(isScanning)
                Log.d("ImpresoraViewModel", "Estado de escaneo actualizado: $isScanning")
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
                Log.d("ImpresoraViewModel", "Dispositivos guardados cargados: ${dispositivos.size}")
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error cargando dispositivos: ${e.message}", e)
                _savedDevices.value = emptyList()
                _uiState.update { it.copy(errorMessage = "Error cargando dispositivos: ${e.message}") }
            }
        }
    }

    fun startDiscovery() {
        viewModelScope.launch {
            Log.d("ImpresoraViewModel", "Iniciando descubrimiento de dispositivos")
            stopDiscovery()
            delay(500)
            _foundDevices.value = emptyList()
            bluetoothViewModel.startScan()
        }
    }

    fun stopDiscovery() {
        bluetoothViewModel.stopScan()
        Log.d("ImpresoraViewModel", "Descubrimiento detenido")
    }

    fun handleDeviceAction(device: BluetoothDeviceDB): DeviceActionResult {
        return if (isDeviceSaved(device.mac)) {
            Log.d("ImpresoraViewModel", "Dispositivo ya está emparejado: ${device.mac}")
            DeviceActionResult.AlreadyPaired
        } else {
            saveDeviceToDatabase(device)
            Log.d("ImpresoraViewModel", "Dispositivo emparejado con éxito: ${device.mac}")
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
                    Log.i("ImpresoraViewModel", "Dispositivo insertado en DB: ${device.nombre} (${device.mac})")
                }
                loadSavedDevicesDB()
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error guardando dispositivo: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error guardando dispositivo: ${e.message}") }
            }
        }
    }

    private fun isDeviceSaved(mac: String): Boolean {
        return try {
            val result = database.query("SELECT COUNT(*) FROM dispositivos WHERE mac = '$mac'")
            val count = result.takeIf { it.isNotEmpty() }?.get(0)?.get("COUNT(*)") as? Long ?: 0
            count > 0
        } catch (e: Exception) {
            Log.e("ImpresoraViewModel", "Error verificando dispositivo guardado: ${e.message}", e)
            false
        }
    }

    fun clearAllDevices(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ImpresoraViewModel", "Limpiando todos los dispositivos")
                database.execSQL("DELETE FROM dispositivos")
                context.dataStoreImp.edit { preferences ->
                    preferences.clear()
                }
                bluetoothViewModel.selectDevice(null)
                loadSavedDevicesDB()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Datos borrados correctamente", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error limpiando dispositivos: ${e.message}", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Error al borrar los datos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                _uiState.update { it.copy(errorMessage = "Error al borrar datos: ${e.message}") }
            }
        }
    }

    /** Imprime un archivo ZPL usando la impresora seleccionada. */
    fun printZplFile(fileName: String) {
        viewModelScope.launch {
            val selectedDevice = bluetoothViewModel.selectedDevice.value
            if (selectedDevice == null) {
                Log.w("ImpresoraViewModel", "No hay impresora seleccionada")
                Toast.makeText(context, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show()
                _uiState.update { it.copy(errorMessage = "No hay impresora seleccionada") }
                return@launch
            }

            Log.d("ImpresoraViewModel", "Iniciando impresión de $fileName en ${selectedDevice.name}")
            val result = printerHelper.printFromAsset(fileName, selectedDevice.address) { status ->
                // Ya estamos en el hilo principal porque printFromAsset usa Dispatchers.Main internamente
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                Log.i("ImpresoraViewModel", "Estado de impresión: $status")
            }

            result.onSuccess { successMsg ->
                Log.i("ImpresoraViewModel", "Impresión exitosa: $successMsg")
                _uiState.update { it.copy(errorMessage = null) }
            }.onFailure { e ->
                Log.e("ImpresoraViewModel", "Fallo al imprimir: ${e.message}", e)
                val errorMsg = "Error al imprimir: ${e.message}"
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                _uiState.update { it.copy(errorMessage = errorMsg) }
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

class ImpresoraViewModelFactory(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImpresoraViewModel::class.java)) {
            return ImpresoraViewModel(bluetoothViewModel, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}