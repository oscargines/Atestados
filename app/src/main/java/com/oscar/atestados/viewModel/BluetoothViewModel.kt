package com.oscar.atestados.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    /** Administrador de Bluetooth para acceder a las funciones del sistema. */
    private val bluetoothManager: BluetoothManager =
        application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    /** Adaptador Bluetooth del dispositivo. Puede ser nulo si el Bluetooth no es compatible. */
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    /** Flujo de estado que contiene el dispositivo Bluetooth seleccionado actualmente. */
    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice.asStateFlow()

    /** Flujo de estado que contiene la lista de dispositivos Bluetooth encontrados. */
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    /** Flujo de estado que indica si se está realizando una búsqueda de dispositivos. */
    private val _isScanning = MutableStateFlow(false)
    val isScanningState: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** Indica si el dispositivo soporta Bluetooth. */
    val isBluetoothSupported: Boolean = bluetoothAdapter != null

    /** Indica si el Bluetooth está habilitado en el dispositivo. */
    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled ?: false

    /** Receptor de broadcast para recibir eventos de dispositivos Bluetooth encontrados. */
    private val _receiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                    device?.let {
                        _devices.update { currentDevices ->
                            if (!currentDevices.contains(it)) currentDevices + it else currentDevices
                        }
                    }
                }
            }
        }
    }

    init {
        registerReceiver()
    }

    /** Selecciona un dispositivo Bluetooth como el activo. */
    @SuppressLint("MissingPermission")
    fun selectDevice(device: BluetoothDevice?) {
        _selectedDevice.value = device
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isBluetoothEnabled) {
            _isScanning.value = true
            bluetoothAdapter?.startDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isBluetoothEnabled) {
            _isScanning.value = false
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        getApplication<Application>().registerReceiver(_receiver, filter)
    }

    private fun unregisterReceiver() {
        getApplication<Application>().unregisterReceiver(_receiver)
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
        unregisterReceiver()
    }
}

class BluetoothViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
            return BluetoothViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}