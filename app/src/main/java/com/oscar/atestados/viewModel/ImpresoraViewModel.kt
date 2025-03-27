package com.oscar.atestados.viewModel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class ImpresoraViewModel(
    val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModel() {

    private val printerHelper = ZebraPrinterHelper(context)
    private val database = AccesoBaseDatos(context, "dispositivos.db", 1)
    private val _savedDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val savedDevices: StateFlow<List<BluetoothDeviceDB>> = _savedDevices.asStateFlow()
    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceDB>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDeviceDB>> = _foundDevices.asStateFlow()
    private val _uiState = MutableStateFlow(ImpresoraUiState())
    val uiState: StateFlow<ImpresoraUiState> = _uiState.asStateFlow()
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val defaultDevice: StateFlow<BluetoothDeviceDB?> = context.dataStoreImp.data
        .map { preferences ->
            val name = preferences[stringPreferencesKey("DEFAULT_PRINTER_NAME")]
            val mac = preferences[stringPreferencesKey("DEFAULT_PRINTER_MAC")]
            if (name != null && mac != null) BluetoothDeviceDB(name, mac) else null
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Nuevo: Estado para controlar el diálogo de borrado
    private val _showDeleteDialog = MutableStateFlow<BluetoothDeviceDB?>(null)
    val showDeleteDialog: StateFlow<BluetoothDeviceDB?> = _showDeleteDialog.asStateFlow()

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

    /** Selecciona un dispositivo Bluetooth como el dispositivo activo (temporal). */
    fun selectDevice(device: BluetoothDeviceDB) {
        viewModelScope.launch {
            var btDevice = bluetoothViewModel.devices.value.find { it.address == device.mac }
            if (btDevice == null) {
                btDevice = bluetoothAdapter?.getRemoteDevice(device.mac)
            }
            if (btDevice != null) {
                Log.d("ImpresoraViewModel", "Seleccionando dispositivo: ${device.nombre} (${device.mac})")
                bluetoothViewModel.selectDevice(btDevice)
            } else {
                Log.w("ImpresoraViewModel", "Dispositivo no disponible: ${device.mac}")
                _uiState.update { it.copy(errorMessage = "Dispositivo no disponible: ${device.mac}") }
                Toast.makeText(context, "Dispositivo no disponible", Toast.LENGTH_SHORT).show()
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

    /** Muestra el diálogo para confirmar el borrado de un dispositivo. */
    fun showDeleteDialog(device: BluetoothDeviceDB, context: Context) {
        _showDeleteDialog.value = device
    }

    /** Borra un dispositivo de la base de datos. */
    fun deleteDevice(device: BluetoothDeviceDB) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.execSQL("DELETE FROM dispositivos WHERE mac = ?", arrayOf(device.mac))
                Log.d("ImpresoraViewModel", "Dispositivo borrado: ${device.nombre} (${device.mac})")

                // Si el dispositivo borrado era el predeterminado, limpiamos DataStore
                val currentDefault = defaultDevice.value
                if (currentDefault?.mac == device.mac) {
                    context.dataStoreImp.edit { preferences ->
                        preferences.remove(stringPreferencesKey("DEFAULT_PRINTER_NAME"))
                        preferences.remove(stringPreferencesKey("DEFAULT_PRINTER_MAC"))
                    }
                    bluetoothViewModel.selectDevice(null)
                }

                loadSavedDevicesDB()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Dispositivo borrado: ${device.nombre}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ImpresoraViewModel", "Error borrando dispositivo: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error borrando dispositivo: ${e.message}") }
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Error al borrar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _showDeleteDialog.value = null // Cerramos el diálogo
            }
        }
    }

    /** Composable para mostrar el diálogo de borrado. */
    @Composable
    fun DeleteDeviceDialog() {
        val deviceToDelete by showDeleteDialog.collectAsState()
        deviceToDelete?.let { device ->
            AlertDialog(
                onDismissRequest = { _showDeleteDialog.value = null },
                title = { Text("Borrar dispositivo") },
                text = { Text("¿Estás seguro de que quieres borrar ${device.nombre} (${device.mac})?") },
                confirmButton = {
                    TextButton(onClick = { deleteDevice(device) }) {
                        Text("Borrar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { _showDeleteDialog.value = null }) {
                        Text("Cancelar")
                    }
                }
            )
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

    /** Imprime un archivo usando la impresora seleccionada. */
    fun printFile(fileName: String) {
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

    /** Devuelve la dirección MAC de la impresora seleccionada. */
    fun getSelectedPrinterMac(): String? {
        val selectedDevice = bluetoothViewModel.selectedDevice.value
        return selectedDevice?.address.also {
            Log.d("ImpresoraViewModel", "Obteniendo MAC de impresora seleccionada: $it")
        } ?: run {
            Log.w("ImpresoraViewModel", "No hay impresora seleccionada para obtener MAC")
            null
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