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

/**
 * Pantalla para la gestión de los datos iniciales de diligencias de alcoholemia.
 *
 * Esta pantalla permite al usuario introducir información relacionada con el inicio de diligencias
 * de alcoholemia, incluyendo fecha y hora, lugar de la investigación, opciones de firma y
 * datos sobre la inmovilización del vehículo y segundo conductor.
 *
 * @param navigateToScreen Función lambda que permite la navegación entre pantallas de la aplicación.
 * @param alcoholemiaDosViewModel ViewModel que gestiona los estados y datos relacionados con esta pantalla.
 */
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

/**
 * Contenido principal de la pantalla de alcoholemia.
 *
 * Muestra todos los campos para introducir la información relacionada con
 * el inicio de diligencias de alcoholemia, organizados en secciones:
 * - Fecha y hora de inicio
 * - Lugar de la investigación
 * - Opciones adicionales (firma, inmovilización, segundo conductor)
 * - Botones para captura de firmas
 *
 * @param modifier Modificador para ajustar la apariencia y comportamiento del componente.
 * @param alcoholemiaDosViewModel ViewModel que gestiona los estados y datos relacionados con esta pantalla.
 * @param onDatePickerFechaDiligenciasClicked Función lambda que se ejecuta al hacer clic para seleccionar la fecha.
 * @param showDatePickerFechaDiligencias Estado booleano que controla la visibilidad del selector de fecha.
 */
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
        TimePickerDialogAlcoholemia(
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

/**
 * Componente que muestra la barra superior de la pantalla de alcoholemia.
 *
 * Contiene el título principal "Alcoholemia" y el subtítulo "Datos inicio diligencias"
 * centrados en la parte superior de la pantalla.
 */
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

/**
 * Componente que muestra la barra inferior de la pantalla de alcoholemia.
 *
 * Contiene los botones de "GUARDAR" y "LIMPIAR" para gestionar los datos del formulario.
 * El botón "GUARDAR" almacena los datos en el sistema y navega a la pantalla principal.
 * El botón "LIMPIAR" restaura todos los campos a sus valores por defecto.
 *
 * @param viewModel ViewModel que contiene los métodos para guardar y limpiar los datos.
 * @param navigateToScreen Función lambda que permite la navegación entre pantallas de la aplicación.
 */
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

/**
 * Campo de texto personalizado para introducir datos en el formulario.
 *
 * Proporciona un TextField estilizado con etiqueta y placeholder para
 * facilitar la entrada de datos en el formulario.
 *
 * @param value Valor actual del campo de texto.
 * @param onValueChange Función lambda que se ejecuta cuando cambia el valor del campo.
 * @param label Etiqueta descriptiva que se muestra sobre el campo.
 * @param modifier Modificador opcional para ajustar la apariencia del componente.
 * @param enabled Estado que determina si el campo está habilitado o no.
 */
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

/**
 * Campo de texto personalizado específico para la pantalla de alcoholemia.
 *
 * Proporciona un TextField estilizado con soporte para iconos, etiquetas y
 * configuración de teclado específica para los campos de la pantalla de alcoholemia.
 *
 * @param value Valor actual del campo de texto.
 * @param onValueChange Función lambda que se ejecuta cuando cambia el valor del campo.
 * @param label Etiqueta descriptiva que se muestra sobre el campo.
 * @param placeholder Texto sugerido que se muestra cuando el campo está vacío.
 * @param keyboardType Tipo de teclado que se mostrará al editar el campo.
 * @param modifier Modificador opcional para ajustar la apariencia del componente.
 * @param leadingIcon Icono opcional que se muestra al inicio del campo.
 */
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

/**
 * Componente que muestra una opción de casilla de verificación con texto.
 *
 * Crea una fila con una casilla de verificación (checkbox) y un texto descriptivo
 * que permite seleccionar opciones binarias en el formulario.
 *
 * @param text Texto descriptivo que se muestra junto a la casilla.
 * @param checked Estado actual de la casilla (marcada o desmarcada).
 * @param onCheckedChange Función lambda que se ejecuta cuando cambia el estado de la casilla.
 */
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

/**
 * Diálogo para seleccionar una fecha para las diligencias.
 *
 * Muestra un selector de fecha en formato calendario que permite al usuario
 * elegir una fecha para las diligencias de alcoholemia.
 *
 * @param onDateSelected Función lambda que recibe la fecha seleccionada formateada.
 * @param onDismiss Función lambda que se ejecuta cuando se cierra el diálogo sin seleccionar fecha.
 */
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