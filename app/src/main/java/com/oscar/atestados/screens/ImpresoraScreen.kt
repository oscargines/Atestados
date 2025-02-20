package com.oscar.atestados.screens

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.oscar.atestados.ui.theme.ItemSelected
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoInformacion
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios
import com.oscar.atestados.viewModel.ImpresoraViewModel

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
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ImpresoraScreen(
    navigateToScreen: (String) -> Unit, impresoraViewModel: ImpresoraViewModel
) {
    val context = LocalContext.current
    val uiState by impresoraViewModel.uiState.collectAsState()
    val savedDevices by impresoraViewModel.savedDevices.collectAsState(emptyList())
    val foundDevices by impresoraViewModel.foundDevices.collectAsState(emptyList())
    var selectedOption by remember { mutableStateOf("Dispositivos guardados") }

    LaunchedEffect(impresoraViewModel.uiState.value.isScanning) {
        if (!impresoraViewModel.uiState.value.isScanning) {
            impresoraViewModel.loadSavedDevicesDB()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = { ImpresoraTopBar(impresoraViewModel) },
        bottomBar = {
            BottomAppBarImpresora(
                impresoraViewModel = impresoraViewModel, navigateToScreen = navigateToScreen
            )
        }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .padding(bottom = 8.dp)
        ) {
            DropdownMenuSelector(selectedOption = selectedOption,
                onOptionSelected = { newSelection ->
                    selectedOption = newSelection
                    if (newSelection == "Buscar dispositivos") {
                        //impresoraViewModel.stopDiscovery()
                        impresoraViewModel.startDiscovery()
                    }
                })

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(), color = BotonesNormales
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                val devicesToShow = when (selectedOption) {
                    "Dispositivos guardados" -> savedDevices.distinctBy { it.mac }
                    "Buscar dispositivos" -> foundDevices.distinctBy { it.mac }
                    else -> emptyList()
                }

                items(items = devicesToShow, key = { device -> device.mac }) { device ->
                    DeviceCard(device = device,
                        isPaired = savedDevices.any { it.mac == device.mac },
                        impresoraViewModel = impresoraViewModel,
                        onActionClick = {
                            val result = impresoraViewModel.handleDeviceAction(device)
                            when (result) {
                                is ImpresoraViewModel.DeviceActionResult.AlreadyPaired -> {
                                    Toast.makeText(
                                        context, "Dispositivo ya emparejado", Toast.LENGTH_SHORT
                                    ).show()
                                }

                                is ImpresoraViewModel.DeviceActionResult.SuccessfullyPaired -> {
                                    impresoraViewModel.selectDevice(device)
                                    impresoraViewModel.saveSelectedDevice()
                                    Toast.makeText(
                                        context,
                                        "Dispositivo guardado y configurado como predeterminado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        })
                }
            }

            if (selectedOption == "Buscar dispositivos") {
                Button(
                    onClick = {
                        if (uiState.isScanning) {
                            impresoraViewModel.stopDiscovery()
                        } else {
                            impresoraViewModel.startDiscovery()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesNormales, contentColor = TextoBotonesNormales
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp, pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Buscar dispositivos",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (uiState.isScanning) "Detener búsqueda" else "Buscar dispositivos")
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuSelector(selectedOption: String, onOptionSelected: (String) -> Unit) {
    val options = listOf("Dispositivos guardados", "Buscar dispositivos")
    var isExpandedBusquedaImpresora by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = isExpandedBusquedaImpresora,
        onExpandedChange = { isExpandedBusquedaImpresora = !isExpandedBusquedaImpresora }) {
        OutlinedTextField(value = selectedOption,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedBusquedaImpresora) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraSmall
        )

        ExposedDropdownMenu(expanded = isExpandedBusquedaImpresora,
            onDismissRequest = { isExpandedBusquedaImpresora = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onOptionSelected(option)
                    isExpandedBusquedaImpresora = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpresoraTopBar(impresoraViewModel: ImpresoraViewModel) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Gestión de Impresoras",
                fontSize = 20.sp,
                color = TextoNormales,
                fontWeight = FontWeight.Bold
            )
        }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarImpresora(
    impresoraViewModel: ImpresoraViewModel, navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current

    val guardarTooltipState = rememberTooltipState()
    val limpiarTooltipState = rememberTooltipState()
    var showClearDialog by remember { mutableStateOf(false) }
    val selectedDevice by impresoraViewModel.selectedDevice.collectAsState()
    val plainTooltipState = rememberTooltipState()

    Surface(
        modifier = Modifier.wrapContentHeight(), color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            //Botón "GUARDAR" con tooltip.
            TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                modifier = Modifier.width(175.dp),
                tooltip = {
                    PlainTooltip {
                        Text("Pulse aquí para guardar el dispositivo seleccionado")
                    }
                }

            ) {
                Button(
                    onClick = {
                        if (selectedDevice != null) {
                            impresoraViewModel.stopDiscovery()
                            impresoraViewModel.saveSelectedDevice()
                            Toast.makeText(
                                context,
                                "Dispositivo guardado\n" + selectedDevice!!.nombre,
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToScreen("MainScreen")
                        } else {
                            Toast.makeText(context, "Seleccione un dispositivo", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 5.dp),
                    enabled = selectedDevice != null,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesNormales, contentColor = TextoBotonesNormales
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp, pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        "GUARDAR", textAlign = TextAlign.Center, fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Botón "LIMPIAR" con tooltip.
            TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                modifier = Modifier.width(175.dp),
                tooltip = {
                    PlainTooltip {
                        Text(
                            "Pulse aquí para limpiar todos los datos almacenados en la aplicación"
                        )
                    }
                }) {
                Button(
                    onClick = {
                        showClearDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    enabled = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesNormales, contentColor = TextoBotonesNormales
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp, pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        "LIMPIAR", textAlign = TextAlign.Center, fontSize = 12.sp
                    )
                }
            }
        }
    }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpiar datos") },
            text = { Text("¿Estás seguro de que quieres limpiar todos los datos?") },
            confirmButton = {
                TextButton(onClick = {
                    // Llamar al método actualizado del ViewModel
                    impresoraViewModel.clearAllDevices(context)
                    showClearDialog = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

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
    impresoraViewModel: ImpresoraViewModel,
    onActionClick: () -> Unit
) {
    var pairedState by remember { mutableStateOf(isPaired) }
    var isSelected by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                impresoraViewModel.selectDevice(device)
                isSelected = true
                impresoraViewModel.saveSelectedDevice() // Guardar automáticamente al seleccionar
            }, colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ItemSelected else Color.White
        ), border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.nombre ?: "Dispositivo desconocido",
                    fontWeight = FontWeight.Bold,
                    color = TextoNormales
                )
                Text(
                    device.mac, color = TextoSecundarios, fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    val result = impresoraViewModel.handleDeviceAction(device)
                    when (result) {
                        is ImpresoraViewModel.DeviceActionResult.AlreadyPaired -> {
                            pairedState = true
                        }

                        is ImpresoraViewModel.DeviceActionResult.SuccessfullyPaired -> {
                            pairedState = true
                            impresoraViewModel.stopDiscovery() // Detener la búsqueda al emparejar
                            impresoraViewModel.saveSelectedDevice() // Guardar después de emparejar
                        }
                    }
                }, shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(
                    containerColor = if (pairedState) Color.Gray else BotonesNormales,
                    contentColor = TextoBotonesNormales
                ), elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(if (pairedState) "Emparejado" else "Enlazar")
            }
        }
    }
}

@Composable
fun SelectedDeviceInfo(impresoraViewModel: ImpresoraViewModel) {
    val selectedDevice by impresoraViewModel.selectedDevice.collectAsState()

    selectedDevice?.let { device ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dispositivo Seleccionado: ${device.nombre}",
                color = TextoInformacion,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "MAC: ${device.mac}", color = TextoInformacion
            )
        }
    }
}