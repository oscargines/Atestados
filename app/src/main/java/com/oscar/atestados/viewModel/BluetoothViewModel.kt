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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar la conectividad Bluetooth.
 * Se encarga de iniciar y detener la búsqueda de dispositivos,
 * así como de mantener un flujo de estado de los dispositivos encontrados.
 *
 * @property application Aplicación Android necesaria para inicializar el ViewModel y acceder al contexto.
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    /** Administrador de Bluetooth para acceder a las funciones del sistema. */
    private val bluetoothManager: BluetoothManager =
        application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    /** Adaptador Bluetooth del dispositivo. Puede ser nulo si el Bluetooth no es compatible. */
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    /** Flujo de estado que contiene la lista de dispositivos Bluetooth encontrados. */
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    /** Flujo de estado público para observar los dispositivos Bluetooth encontrados. */
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    /** Flujo de estado que indica si se está realizando una búsqueda de dispositivos. */
    private val _isScanning = MutableStateFlow(false)
    /** Flujo de estado público para observar si se está escaneando dispositivos. */
    val isScanningState: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** Indica si el dispositivo soporta Bluetooth. */
    val isBluetoothSupported: Boolean = bluetoothAdapter != null

    /** Indica si el Bluetooth está habilitado en el dispositivo. */
    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled ?: false

    /** Receptor de broadcast para recibir eventos de dispositivos Bluetooth encontrados. */
    private val _receiver = object : BroadcastReceiver() {
        /**
         * Maneja los eventos recibidos del sistema relacionados con Bluetooth.
         *
         * @param context Contexto en el que se recibe el evento.
         * @param intent Intent que contiene la información del evento.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    device?.let {
                        _devices.update { currentDevices ->
                            if (!currentDevices.contains(it)) currentDevices + it else currentDevices
                        }
                    }
                }
            }
        }
    }

    /**
     * Inicializa el ViewModel registrando el receptor de eventos Bluetooth.
     */
    init {
        registerReceiver()
    }

    /**
     * Inicia el escaneo de dispositivos Bluetooth cercanos.
     * Requiere que el Bluetooth esté habilitado.
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isBluetoothEnabled) {
            _isScanning.value = true
            bluetoothAdapter?.startDiscovery()
        }
    }

    /**
     * Detiene el escaneo de dispositivos Bluetooth.
     * Requiere que el Bluetooth esté habilitado.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isBluetoothEnabled) {
            _isScanning.value = false
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    /**
     * Registra el receptor de eventos Bluetooth en el sistema para escuchar dispositivos encontrados.
     */
    private fun registerReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        getApplication<Application>().registerReceiver(_receiver, filter)
    }

    /**
     * Desregistra el receptor de eventos Bluetooth para evitar fugas de memoria.
     */
    private fun unregisterReceiver() {
        getApplication<Application>().unregisterReceiver(_receiver)
    }

    /**
     * Se ejecuta cuando el ViewModel es destruido, deteniendo la búsqueda y liberando recursos.
     */
    override fun onCleared() {
        super.onCleared()
        stopScan()
        unregisterReceiver()
    }
}