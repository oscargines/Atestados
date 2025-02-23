package com.oscar.atestados.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.BotonesFirmaAjena
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.BotonesSecundarios
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios
import com.oscar.atestados.ui.theme.TextoTerciarios
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun Alcoholemia02Screen(
    navigateToScreen: (String) -> Unit,
    alcoholemiaDosViewModel: AlcoholemiaDosViewModel
) {
    var showDatePickerFechaDiligencias by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AlcoholemiaTopBar() },
        bottomBar = { AlcoholemiaBottomBar(alcoholemiaDosViewModel, navigateToScreen) }
    ) { paddingValues ->

        Alcoholemia02Content(
            modifier = Modifier.padding(paddingValues),
            alcoholemiaDosViewModel = alcoholemiaDosViewModel,
            onDatePickerFechaDiligenciasClicked = { showDatePickerFechaDiligencias = true },
            showDatePickerFechaDiligencias = showDatePickerFechaDiligencias
        )
        if (showDatePickerFechaDiligencias) {
            getDateDialogVeh(
                onDateSelected = { fechaSeleccionada ->
                    alcoholemiaDosViewModel.updateFechaInicio(fechaSeleccionada) // Actualiza el ViewModel con la fecha seleccionada
                    showDatePickerFechaDiligencias = false // Cierra el DatePicker
                },
                onDismiss = { showDatePickerFechaDiligencias = false } // Cierra el DatePicker si se cancela
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Alcoholemia02Content(
    modifier: Modifier = Modifier,
    alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    onDatePickerFechaDiligenciasClicked: () -> Unit,
    showDatePickerFechaDiligencias: Boolean
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    // Estados del ViewModel
    val fechaInicio by alcoholemiaDosViewModel.fechaInicio.observeAsState("")
    val horaInicio by alcoholemiaDosViewModel.horaInicio.observeAsState("")
    val lugarCoincide by alcoholemiaDosViewModel.lugarCoincide.observeAsState(false)
    val lugarDiligencias by alcoholemiaDosViewModel.lugarDiligencias.observeAsState("")
    val deseaFirmar by alcoholemiaDosViewModel.deseaFirmar.observeAsState(false)
    val inmovilizaVehiculo by alcoholemiaDosViewModel.inmovilizaVehiculo.observeAsState(false)
    val haySegundoConductor by alcoholemiaDosViewModel.haySegundoConductor.observeAsState(false)
    val nombreSegundoConductor by alcoholemiaDosViewModel.nombreSegundoConductor.observeAsState("")

    var showSignatureDialog by remember { mutableStateOf(false) }
    var signatureType by remember { mutableStateOf("") } // Para identificar qué firma se está capturando

    // TimePicker para primera hora
    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        alcoholemiaDosViewModel.updateHoraInicio(
                            "${timePickerState.hour}:${timePickerState.minute.toString().padStart(2, '0')}"
                        )
                        showTimePicker = false
                    }
                ) { Text("OK") }
            }
        ) { TimePicker(state = timePickerState) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Sección Fecha y Hora de Inicio
        Text(
            text = "Fecha y hora de inicio",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomOutlinedTextFieldAlcohol(
                modifier = Modifier.weight(1.2f),
                value = fechaInicio,
                onValueChange = { alcoholemiaDosViewModel.updateFechaInicio(it) },
                label = "Fecha",
                placeholder = "Seleccione la fecha",
                keyboardType = KeyboardType.Decimal,
                leadingIcon = {
                    IconButton(
                        onClick = onDatePickerFechaDiligenciasClicked,
                        modifier = Modifier.size(35.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar_ico),
                            tint = BotonesNormales,
                            contentDescription = "Botón de acceso a calendario"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                modifier = Modifier.weight(0.8f),
                value = horaInicio,
                onValueChange = { alcoholemiaDosViewModel.updateHoraInicio(it) },
                label = { Text("Hora", color = TextoTerciarios) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.reloj_ico),
                            contentDescription = "Seleccionar hora"
                        )
                    }
                }
            )
        }

        // Sección Lugar de la investigación
        Text(
            text = "¿El lugar de la investigación coincide con el de instrucción?",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )

        CheckboxOption(
            text = "Lugar coincide",
            checked = lugarCoincide,
            onCheckedChange = { alcoholemiaDosViewModel.updateLugarCoincide(it) }
        )

        // Mostrar el CustomTextField solo si "Lugar coincide" es false
        if (!lugarCoincide) {
            CustomTextField(
                value = lugarDiligencias,
                onValueChange = { alcoholemiaDosViewModel.updateLugarDiligencias(it) },
                label = "Lugar donde se realiza diligencias",
                enabled = !lugarCoincide // Deshabilitar si "Lugar coincide" es true
            )
        }

        // Sección Opciones de firma y vehículo
        Text(
            text = "Opciones adicionales",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )

        CheckboxOption(
            text = "¿Desea firmar?",
            checked = deseaFirmar,
            onCheckedChange = { alcoholemiaDosViewModel.updateDeseaFirmar(it) }
        )

        CheckboxOption(
            text = "¿Se inmoviliza el vehículo?",
            checked = inmovilizaVehiculo,
            onCheckedChange = { alcoholemiaDosViewModel.updateInmovilizaVehiculo(it) }
        )

        CheckboxOption(
            text = "¿Hay segundo conductor?",
            checked = haySegundoConductor,
            onCheckedChange = { alcoholemiaDosViewModel.updateHaySegundoConductor(it) }
        )

        if (haySegundoConductor) {
            CustomTextField(
                value = nombreSegundoConductor,
                onValueChange = { alcoholemiaDosViewModel.updateNombreSegundoConductor(it) },
                label = "Nombre y Apellidos (DNI) segundo conductor"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showSignatureDialog) {
            SignatureCaptureScreen(
                onSignatureCaptured = { bitmap ->
                    when (signatureType) {
                        "investigado" -> alcoholemiaDosViewModel.updateFirmaInvestigado(bitmap)
                        "segundo_conductor" -> alcoholemiaDosViewModel.updateFirmaSegundoConductor(bitmap)
                        "instructor" -> alcoholemiaDosViewModel.updateFirmaInstructor(bitmap)
                        "secretario" -> alcoholemiaDosViewModel.updateFirmaSecretario(bitmap)
                    }
                    showSignatureDialog = false
                },
                onDismiss = { showSignatureDialog = false }
            )
        }

        // Botones de firma
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mostrar el botón de FIRMA DEL INVESTIGADO solo si "¿Desea firmar?" es true
            if (deseaFirmar) {
                Button(
                    onClick = {
                        signatureType = "investigado"
                        showSignatureDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesFirmaAjena,
                        contentColor = TextoBotonesNormales
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        text = "FIRMA DEL INVESTIGADO",
                        textAlign = TextAlign.Center, // Centrar el texto dentro del botón
                        modifier = Modifier.fillMaxWidth() // Ocupar todo el ancho del botón
                    )
                }
            } else {
                // Espacio vacío si el botón no se muestra
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Mostrar el botón de FIRMA SEGUNDO CONDUCTOR solo si "¿Hay segundo conductor?" es true
            if (haySegundoConductor) {
                Button(
                    onClick = {
                        signatureType = "segundo_conductor"
                        showSignatureDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesFirmaAjena,
                        contentColor = TextoBotonesNormales
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        text = "FIRMA SEGUNDO CONDUCTOR",
                        textAlign = TextAlign.Center, // Centrar el texto dentro del botón
                        modifier = Modifier.fillMaxWidth() // Ocupar todo el ancho del botón
                    )
                }
            } else {
                // Espacio vacío si el botón no se muestra
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    signatureType = "instructor"
                    showSignatureDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotonesNormales,
                    contentColor = TextoBotonesNormales
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(
                    text = "FIRMA DEL INSTRUCTOR",
                    textAlign = TextAlign.Center, // Centrar el texto dentro del botón
                    modifier = Modifier.fillMaxWidth() // Ocupar todo el ancho del botón
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    signatureType = "secretario"
                    showSignatureDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotonesNormales,
                    contentColor = TextoBotonesNormales
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(
                    text = "FIRMA DEL SECRETARIO",
                    textAlign = TextAlign.Center, // Centrar el texto dentro del botón
                    modifier = Modifier.fillMaxWidth() // Ocupar todo el ancho del botón
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlcoholemiaTopBar() {
    CenterAlignedTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Alcoholemia",
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Datos inicio diligencias",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoSecundarios
                )
                Spacer(modifier = Modifier.height(15.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun AlcoholemiaBottomBar(
    viewModel: AlcoholemiaDosViewModel,
    navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = {
                viewModel.guardarDatos(context)
                navigateToScreen("MainScreen")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("GUARDAR")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = { viewModel.limpiarDatos() },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("LIMPIAR")
        }
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoTerciarios) },
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        placeholder = {
            Text(
                "Introduzca $label",
                color = TextoTerciarios,
                textDecoration = TextDecoration.Underline
            )
        }
    )
}
@Composable
private fun CustomOutlinedTextFieldAlcohol(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoTerciarios) },
        placeholder = {
            Text(
                placeholder,
                color = TextoTerciarios,
                textDecoration = TextDecoration.Underline
            )
        },
        shape = MaterialTheme.shapes.extraSmall,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.padding(vertical = 4.dp),
        singleLine = true,
        leadingIcon = leadingIcon
    )
}

@Composable
private fun CheckboxOption(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = BotonesNormales,
                uncheckedColor = BotonesSecundarios
            )
        )
        Text(
            text = text,
            color = Color.Black,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getDateDialogDiligencias(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val selectedDate = state.selectedDateMillis?.let {
                    val localDate = Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    // Formatear la fecha en español
                    val formatter = DateTimeFormatter
                        .ofPattern("d 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
                    localDate.format(formatter)
                } ?: ""
                onDateSelected(selectedDate)
                onDismiss()
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = state)
    }
}