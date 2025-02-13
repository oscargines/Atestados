package com.oscar.atestados.viewModel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.data.BluetoothDeviceDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/**
 * ViewModel para la gestión de impresoras.
 * Se encarga de manejar los dispositivos Bluetooth y su almacenamiento en la base de datos.
 *
 * @param bluetoothViewModel ViewModel encargado del escaneo de dispositivos Bluetooth.
 * @param context Contexto de la aplicación para acceder a la base de datos.
 */
@SuppressLint("MissingPermission")
class ImpresoraViewModel(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModel() {

    /** Acceso a la base de datos SQLite. */
    private val database = AccesoBaseDatos(context, "dispositivos.db", 1)

    // Flujos convertidos a BluetoothDeviceDB
    private val _savedDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val savedDevices: StateFlow<List<BluetoothDeviceDB>> = _savedDevices.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDeviceDB>> = _foundDevices.asStateFlow()

    private val _uiState = MutableStateFlow(ImpresoraUiState())
    val uiState: StateFlow<ImpresoraUiState> = _uiState.asStateFlow()

    val isScanning = bluetoothViewModel.isScanningState.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        false
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.ensureTableExists()
            loadSavedDevicesDB()
        }
        setupFoundDevicesConversion()
    }
    private fun observeScanningState() {
        viewModelScope.launch {
            bluetoothViewModel.isScanningState.collect { isScanning ->
                _uiState.update { it.copy(isScanning = isScanning) }
            }
        }
    }

    private fun setupFoundDevicesConversion() {
        viewModelScope.launch {
            bluetoothViewModel.devices.collect { nativeDevices ->
                _foundDevices.value = nativeDevices.map { device ->
                    BluetoothDeviceDB(
                        nombre = device.name ?: "Desconocido",
                        mac = device.address
                    )
                }
            }
        }
    }


    /**
     * Carga los dispositivos guardados en la base de datos.
     */
    fun loadSavedDevicesDB() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Primero verificamos si la tabla existe
                val tableCheck = database.query("""
                SELECT name FROM sqlite_master 
                WHERE type='table' AND name='dispositivos'
            """)

                if (tableCheck.isEmpty()) {
                    // La tabla no existe, la creamos
                    database.query("""
                    CREATE TABLE IF NOT EXISTS dispositivos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nombre TEXT NOT NULL,
                        mac TEXT NOT NULL UNIQUE
                    )
                """)
                }

                // Ahora intentamos obtener los dispositivos
                val dispositivos = database.query("SELECT nombre, mac FROM dispositivos").map { row ->
                    BluetoothDeviceDB(
                        nombre = row["nombre"] as? String ?: "Desconocido",
                        mac = row["mac"] as? String ?: "00:00:00:00:00:00"
                    )
                }
                _savedDevices.value = dispositivos

            } catch (e: Exception) {
                // Manejo del error
                withContext(Dispatchers.Main) {
                    // Aquí podrías mostrar un mensaje al usuario si lo deseas
                    Log.e("ImpresoraViewModel", "Error al cargar dispositivos", e)
                    _savedDevices.value = emptyList() // Aseguramos un estado válido
                }
            }
        }
    }

    /**
     * Inicia el escaneo de dispositivos Bluetooth.
     */
    fun startDiscovery() {
        viewModelScope.launch {
            _foundDevices.value = emptyList()
            bluetoothViewModel.startScan()
            delay(10000)
            stopDiscovery()
        }
    }
    fun handleDeviceAction(device: BluetoothDeviceDB): DeviceActionResult {
        return if (isDeviceStored(device.mac)) {
            DeviceActionResult.AlreadyPaired
        } else {
            saveDeviceDB(device)
            DeviceActionResult.SuccessfullyPaired
        }
    }

    /**
     * Detiene el escaneo de dispositivos Bluetooth.
     */
    fun stopDiscovery() {
        bluetoothViewModel.stopScan()
    }

    /**
     * Guarda un dispositivo en la base de datos si no está guardado previamente.
     *
     * @param device Dispositivo Bluetooth a guardar.
     */
    @SuppressLint("MissingPermission")
    private fun saveDeviceDB(device: BluetoothDeviceDB) {
        viewModelScope.launch(Dispatchers.IO) {
            val query = "INSERT INTO dispositivos (nombre, mac) VALUES ('${device.nombre}', '${device.mac}')"
            database.query(query)
            loadSavedDevicesDB()
        }
    }

    /**
     * Verifica si un dispositivo ya está guardado en la base de datos.
     *
     * @param mac Dirección MAC del dispositivo.
     * @return `true` si el dispositivo está guardado, `false` en caso contrario.
     */
    private fun isDeviceStored(mac: String): Boolean {
        val result = database.query("SELECT COUNT(*) FROM dispositivos WHERE mac = '$mac'")
        return result.isNotEmpty() && result[0]["COUNT(*)"] != "0"
    }

    /**
     * Verifica si un dispositivo ya está guardado antes de enlazarlo.
     * Si no está guardado, lo almacena en la base de datos.
     *
     * @param context Contexto de la aplicación.
     * @param device Dispositivo Bluetooth a emparejar.
     */
    @SuppressLint("MissingPermission")
    fun pairDevice(context: Context, device: BluetoothDeviceDB) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSaved = isDeviceStored(device.mac)

            if (!isSaved) {
                saveDeviceDB(device)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Dispositivo guardado y emparejado", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "El dispositivo ya está emparejado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    data class ImpresoraUiState(
        val isScanning: Boolean = false,
        val selectedOption: String = "Dispositivos guardados"
    )

    sealed class DeviceActionResult {
        object AlreadyPaired : DeviceActionResult()
        object SuccessfullyPaired : DeviceActionResult()
    }
}

