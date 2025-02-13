package com.oscar.atestados.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import com.oscar.atestados.R
import com.oscar.atestados.data.BluetoothDeviceDB
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.viewModel.ImpresoraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configuración de DataStore para almacenar preferencias de impresoras.
 */
val Context.dataStoreImp by preferencesDataStore(name = "IMPRESORA_PREFERENCES")

/**
 * Pantalla principal para la gestión de impresoras.
 * Muestra dispositivos guardados y permite buscar nuevos dispositivos Bluetooth.
 *
 * @param impresoraViewModel ViewModel con la lógica de negocio.
 */
@Composable
fun ImpresoraScreen(
    navigateToScreen: (String) -> Unit,
    impresoraViewModel: ImpresoraViewModel
) {
    val context = LocalContext.current
    val uiState by impresoraViewModel.uiState.collectAsState()
    val savedDevices by impresoraViewModel.savedDevices.collectAsState(emptyList())
    val foundDevices by impresoraViewModel.foundDevices.collectAsState(emptyList())

    var selectedOption by remember { mutableStateOf("Dispositivos guardados") }

    LaunchedEffect(true) {
        impresoraViewModel.loadSavedDevicesDB()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ImpresoraTopBar() },
        floatingActionButton = {
            if (selectedOption == "Buscar dispositivos" && !uiState.isScanning) {
                FloatingActionButton(
                    onClick = {
                        impresoraViewModel.startDiscovery()
                    },
                    containerColor = BotonesNormales
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Buscar dispositivos"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)) {

            // DropdownMenu para seleccionar opción
            DropdownMenuSelector(selectedOption) { newSelection ->
                selectedOption = newSelection
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Mostrar lista según la selección
            LazyColumn {
                val devicesToShow = if (selectedOption == "Dispositivos guardados")
                    savedDevices else foundDevices

                items(
                    items = devicesToShow,
                    key = { device -> device.mac }
                ) { device ->
                    DeviceCard(
                        device = device,
                        isPaired = savedDevices.any { it.mac == device.mac },
                        onActionClick = {
                            when (impresoraViewModel.handleDeviceAction(device)){
                                is ImpresoraViewModel.DeviceActionResult.AlreadyPaired -> {
                                    // Si ya está emparejado, mostrar mensaje
                                    Toast.makeText(context, "El dispositivo ya está emparejado", Toast.LENGTH_SHORT).show()

                                }
                                is ImpresoraViewModel.DeviceActionResult.SuccessfullyPaired -> {
                                    // Si se ha guardado correctamente, mostrar mensaje
                                    Toast.makeText(context, "Dispositivo guardado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Componente que muestra el menú desplegable de selección.
 *
 * @param selectedOption Opción seleccionada actualmente.
 * @param onOptionSelected Callback para cambiar la opción seleccionada.
 */
@Composable
fun DropdownMenuSelector(selectedOption: String, onOptionSelected: (String) -> Unit) {
    val options = listOf("Dispositivos guardados", "Buscar dispositivos")
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedOption)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpresoraTopBar(onCameraButtonClicked: () -> Unit = {}) {
    TopAppBar(
        title = {
            Text(
                text = "Gestión de Impresoras",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onCameraButtonClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "Buscar dispositivos",
                    tint = BotonesNormales
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

/**
 * Componente que representa una tarjeta de dispositivo Bluetooth.
 *
 * @param device Dispositivo Bluetooth.
 * @param isPaired Indica si el dispositivo ya está guardado.
 * @param onActionClick Acción al pulsar el botón de emparejamiento.
 */
@Composable
fun DeviceCard(
    device: BluetoothDeviceDB,
    isPaired: Boolean,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.nombre ?: "Dispositivo desconocido", fontWeight = FontWeight.Bold)
                Text(device.mac, color = Color.Gray, fontSize = 12.sp)
            }

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = if (isPaired) Color.Gray else BotonesNormales)
            ) {
                Text(if (isPaired) "Emparejado" else "Guardar")
            }
        }
    }
}