package com.oscar.atestados.viewModel

import com.oscar.atestados.utils.ZebraPrinterHelper
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

/**
 * ViewModel para gestionar la funcionalidad de impresoras Bluetooth.
 * Administra la selección, almacenamiento y escaneo de dispositivos Bluetooth, así como la impresión de archivos ZPL.
 *
 * @property bluetoothViewModel ViewModel de Bluetooth para manejar el escaneo de dispositivos.
 * @property context Contexto de la aplicación necesario para acceder a DataStore, base de datos y UI.
 */
@SuppressLint("MissingPermission")
class ImpresoraViewModel(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModel() {

    /** Flujo de estado que contiene el dispositivo Bluetooth seleccionado actualmente. */
    private val _selectedDevice = MutableStateFlow<BluetoothDeviceDB?>(null)
    /** Flujo de estado público para observar el dispositivo seleccionado. */
    val selectedDevice: StateFlow<BluetoothDeviceDB?> = _selectedDevice.asStateFlow()

    /** Instancia de la base de datos para almacenar dispositivos Bluetooth. */
    private val database = AccesoBaseDatos(context, "dispositivos.db", 1)

    /** Flujo de estado que contiene la lista de dispositivos Bluetooth guardados en la base de datos. */
    private val _savedDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    /** Flujo de estado público para observar los dispositivos guardados. */
    val savedDevices: StateFlow<List<BluetoothDeviceDB>> = _savedDevices.asStateFlow()

    /** Flujo de estado que contiene la lista de dispositivos Bluetooth encontrados durante el escaneo. */
    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    /** Flujo de estado público para observar los dispositivos encontrados. */
    val foundDevices: StateFlow<List<BluetoothDeviceDB>> = _foundDevices.asStateFlow()

    /** Flujo de estado que contiene el estado de la interfaz de usuario (escaneo, errores, etc.). */
    private val _uiState = MutableStateFlow(ImpresoraUiState())
    /** Flujo de estado público para observar el estado de la UI. */
    val uiState: StateFlow<ImpresoraUiState> = _uiState.asStateFlow()

    /**
     * Inicializa el ViewModel, asegurando la existencia de la tabla en la base de datos,
     * cargando dispositivos guardados y configurando observadores Bluetooth.
     */
    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.ensureTableExists()
            loadSavedDevicesDB()
            loadSelectedDeviceFromPreferences() // Cargar dispositivo seleccionado al iniciar
        }
        setupBluetoothObservers()
    }

    /**
     * Selecciona un dispositivo Bluetooth como el dispositivo activo.
     *
     * @param device Dispositivo Bluetooth a seleccionar.
     */
    fun selectDevice(device: BluetoothDeviceDB) {
        _selectedDevice.value = device
    }

    /**
     * Guarda el dispositivo seleccionado en la base de datos y en DataStore.
     */
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

    /**
     * Carga el dispositivo seleccionado previamente desde DataStore.
     */
    private suspend fun loadSelectedDeviceFromPreferences() {
        val preferences = context.dataStoreImp.data.first()
        val name = preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")]
        val mac = preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
        if (name != null && mac != null) {
            _selectedDevice.value = BluetoothDeviceDB(nombre = name, mac = mac)
            Log.d("ImpresoraViewModel", "Dispositivo cargado desde preferencias: $name ($mac)")
        }
    }

    /**
     * Configura observadores para los dispositivos Bluetooth encontrados y el estado de escaneo.
     */
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

    /**
     * Actualiza el estado de escaneo en la interfaz de usuario.
     *
     * @param isScanning Indica si el escaneo está activo.
     */
    private fun updateScanningState(isScanning: Boolean) {
        _uiState.update { it.copy(isScanning = isScanning) }
    }

    /**
     * Convierte una lista de [BluetoothDevice] a una lista de [BluetoothDeviceDB].
     *
     * @return Lista de dispositivos en formato [BluetoothDeviceDB] sin duplicados por MAC.
     */
    private fun List<BluetoothDevice>.mapToDeviceDB(): List<BluetoothDeviceDB> {
        return this.map { device ->
            BluetoothDeviceDB(
                nombre = device.name ?: "Dispositivo desconocido",
                mac = device.address
            )
        }.distinctBy { it.mac }
    }

    /**
     * Carga los dispositivos guardados desde la base de datos.
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
     * Inicia el descubrimiento de dispositivos Bluetooth cercanos.
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
     * Detiene el descubrimiento de dispositivos Bluetooth.
     */
    fun stopDiscovery() {
        bluetoothViewModel.stopScan()
    }

    /**
     * Maneja la acción de emparejar un dispositivo Bluetooth.
     *
     * @param device Dispositivo Bluetooth a procesar.
     * @return Resultado de la acción ([DeviceActionResult]).
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
     * Guarda un dispositivo Bluetooth en la base de datos si no existe previamente.
     *
     * @param device Dispositivo Bluetooth a guardar.
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
                Log.e("ImpresoraViewModel", "Error guardando el dispositivo: ${e.message}")
            }
        }
    }

    /**
     * Verifica si un dispositivo ya está guardado en la base de datos por su dirección MAC.
     *
     * @param mac Dirección MAC del dispositivo a verificar.
     * @return `true` si el dispositivo está guardado, `false` en caso contrario o si hay un error.
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
     * Limpia todos los dispositivos guardados en la base de datos y DataStore.
     *
     * @param context Contexto necesario para mostrar notificaciones y acceder a DataStore.
     */
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

    /**
     * Imprime un archivo ZPL desde los assets utilizando el dispositivo seleccionado.
     *
     * @param assetFileName Nombre del archivo ZPL en los assets.
     * @return Resultado de la operación de impresión ([Result]).
     */
    suspend fun printZplFile(assetFileName: String): Result<String> {
        val printerHelper = ZebraPrinterHelper(context)
        return printerHelper.printFromAsset(assetFileName)
    }

    /**
     * Estado de la interfaz de usuario para la gestión de impresoras.
     *
     * @property isScanning Indica si se está escaneando dispositivos Bluetooth.
     * @property errorMessage Mensaje de error, si lo hay.
     */
    data class ImpresoraUiState(
        val isScanning: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Resultados posibles de las acciones sobre dispositivos Bluetooth.
     */
    sealed class DeviceActionResult {
        /** Indica que el dispositivo ya estaba emparejado. */
        object AlreadyPaired : DeviceActionResult()
        /** Indica que el dispositivo se emparejó con éxito. */
        object SuccessfullyPaired : DeviceActionResult()
    }
}