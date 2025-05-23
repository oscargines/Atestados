package com.oscar.atestados.screens

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import com.oscar.atestados.R
import com.oscar.atestados.data.BluetoothDeviceDB
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.viewModel.ImpresoraViewModel

val Context.dataStoreImp by preferencesDataStore(name = "IMPRESORA_PREFERENCES")

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ImpresoraScreen(
    navigateToScreen: (String) -> Unit,
    impresoraViewModel: ImpresoraViewModel
) {
    val context = LocalContext.current
    val uiState by impresoraViewModel.uiState.collectAsState()
    val savedDevices by impresoraViewModel.savedDevices.collectAsState(emptyList())
    val foundDevices by impresoraViewModel.foundDevices.collectAsState(emptyList())
    val defaultDevice by impresoraViewModel.defaultDevice.collectAsState()
    var selectedOption by remember { mutableStateOf("Dispositivos guardados") }

    LaunchedEffect(impresoraViewModel.uiState.value.isScanning) {
        if (!impresoraViewModel.uiState.value.isScanning) {
            impresoraViewModel.loadSavedDevicesDB()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ImpresoraTopBar(impresoraViewModel) },
        bottomBar = {
            BottomAppBarImpresora(
                impresoraViewModel = impresoraViewModel,
                navigateToScreen = navigateToScreen
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            DropdownMenuSelector(
                selectedOption = selectedOption,
                onOptionSelected = { newSelection ->
                    selectedOption = newSelection
                    if (newSelection == "Buscar dispositivos") {
                        impresoraViewModel.startDiscovery()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = BlueGray100,
                border = BorderStroke(1.dp, BlueGray300)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bluetooth),
                        contentDescription = "Icono de Bluetooth",
                        modifier = Modifier.size(24.dp),
                        tint = BotonesNormales
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Dispositivo Predeterminado",
                            fontSize = 12.sp,
                            color = TextoSecundarios,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = defaultDevice?.let { "${it.nombre} (${it.mac})" }
                                ?: "Ningún dispositivo seleccionado",
                            fontSize = 16.sp,
                            color = if (defaultDevice != null) TextoNormales else TextoTerciarios,
                            fontWeight = if (defaultDevice != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = BotonesNormales
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                val devicesToShow = when (selectedOption) {
                    "Dispositivos guardados" -> savedDevices.distinctBy { it.mac }
                    "Buscar dispositivos" -> foundDevices.distinctBy { it.mac }
                    else -> emptyList()
                }

                items(items = devicesToShow, key = { device -> device.mac }) { device ->
                    DeviceCard(
                        device = device,
                        isSearchMode = selectedOption == "Buscar dispositivos",
                        savedDevices = savedDevices,
                        impresoraViewModel = impresoraViewModel,
                        onActionClick = {
                            impresoraViewModel.selectDevice(device)
                            Toast.makeText(
                                context,
                                "Dispositivo seleccionado: ${device.nombre}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onLongPress = {
                            if (selectedOption == "Dispositivos guardados") {
                                impresoraViewModel.showDeleteDialog(device, context)
                            }
                        }
                    )
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
                        containerColor = BotonesNormales,
                        contentColor = TextoBotonesNormales
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
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
        impresoraViewModel.DeleteDeviceDialog() // Añadido aquí para mostrar el diálogo
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuSelector(selectedOption: String, onOptionSelected: (String) -> Unit) {
    val options = listOf("Dispositivos guardados", "Buscar dispositivos")
    var isExpandedBusquedaImpresora by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpandedBusquedaImpresora,
        onExpandedChange = { isExpandedBusquedaImpresora = !isExpandedBusquedaImpresora }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedBusquedaImpresora) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraSmall
        )

        ExposedDropdownMenu(
            expanded = isExpandedBusquedaImpresora,
            onDismissRequest = { isExpandedBusquedaImpresora = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        isExpandedBusquedaImpresora = false
                    }
                )
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
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarImpresora(
    impresoraViewModel: ImpresoraViewModel,
    navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedDevice by impresoraViewModel.bluetoothViewModel.selectedDevice.collectAsState()
    val plainTooltipState = rememberTooltipState()
    var showClearDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .wrapContentHeight()
            .padding(vertical = 30.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
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
                            impresoraViewModel.saveSelectedDevice()
                            Toast.makeText(
                                context,
                                "Dispositivo guardado: ${selectedDevice!!.name ?: "Desconocido"}",
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
                        containerColor = BotonesNormales,
                        contentColor = TextoBotonesNormales
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        "GUARDAR",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                modifier = Modifier.width(175.dp),
                tooltip = {
                    PlainTooltip {
                        Text("Pulse aquí para limpiar todos los datos almacenados")
                    }
                }
            ) {
                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    enabled = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesNormales,
                        contentColor = TextoBotonesNormales
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        "LIMPIAR",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
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

@Composable
fun DeviceCard(
    device: BluetoothDeviceDB,
    isSearchMode: Boolean,
    savedDevices: List<BluetoothDeviceDB>,
    impresoraViewModel: ImpresoraViewModel,
    onActionClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val selectedDevice by impresoraViewModel.bluetoothViewModel.selectedDevice.collectAsState()
    val isSaved = savedDevices.any { it.mac == device.mac }
    val isSelected = selectedDevice?.address == device.mac
    val buttonText = when {
        isSearchMode && isSaved -> "Guardado"
        !isSearchMode && isSelected -> "Pulse Guardar"
        !isSearchMode -> "Enlazado"
        else -> "Enlazar"
    }
    val cardColor = if (isSelected) ItemSelected else Color.White
    val buttonColor = if (isSaved || isSelected) Color.Gray else BotonesNormales

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { impresoraViewModel.selectDevice(device) },
                    onLongPress = { onLongPress() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.nombre ?: "Dispositivo desconocido",
                    fontWeight = FontWeight.Bold,
                    color = TextoNormales
                )
                Text(
                    device.mac,
                    color = TextoSecundarios,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    if (!isSaved) {
                        onActionClick()
                    }
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = TextoBotonesNormales
                ),
                enabled = !isSaved,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun SelectedDeviceInfo(impresoraViewModel: ImpresoraViewModel) {
    val selectedDevice by impresoraViewModel.bluetoothViewModel.selectedDevice.collectAsState()

    selectedDevice?.let { device ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dispositivo Seleccionado: ${device.name ?: "Desconocido"}",
                color = TextoInformacion,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "MAC: ${device.address}",
                color = TextoInformacion
            )
        }
    }
}