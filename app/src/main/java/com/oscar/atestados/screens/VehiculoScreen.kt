package com.oscar.atestados.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.datastore.preferences.preferencesDataStore
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoTerciarios
import com.oscar.atestados.viewModel.VehiculoViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Configuración de DataStore para almacenar preferencias de la aplicación.
 * Se utiliza para guardar y recuperar datos persistentes relacionados con la entidad "Persona".
 *
 * @property name Nombre del DataStore, utilizado para identificar el archivo de preferencias.
 */
val Context.dataStoreVeh by preferencesDataStore(name = "VEHICULO_PREFERENCES_Nme")


/**
 * Pantalla principal de la entidad "Vehículos".
 * Muestra un formulario para ingresar datos relacionados con un vehículo.
 *
 * @param navigateToScreen Función de navegación para cambiar a otras pantallas.
 * @param vehiculoViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 */
@Composable
fun VehiculoScreen(
    navigateToScreen: (String) -> Unit,
    vehiculoViewModel: VehiculoViewModel
) {
    var showDatePickerMatriculacion by remember { mutableStateOf(false) }
    var showDatePickerITV by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Cargar datos al iniciar la pantalla
    LaunchedEffect(Unit) {
        vehiculoViewModel.loadData(context)
    }

    VehiculoScreenContent(
        navigateToScreen = navigateToScreen,
        vehiculoViewModel = vehiculoViewModel,
        onTextFieldChanged = { text -> vehiculoViewModel.updateMatricula(text) },
        onDatePickerMatriculacionClicked = { showDatePickerMatriculacion = true },
        onDatePickerITVClicked = { showDatePickerITV = true },
        showDatePickerMatriculacion = showDatePickerMatriculacion,
        showDatePickerITV = showDatePickerITV
    )

    if (showDatePickerMatriculacion) {
        getDateDialogVeh(
            onDateSelected = { fechaSeleccionada ->
                vehiculoViewModel.updateFechaMatriculacion(fechaSeleccionada) // Actualiza el ViewModel con la fecha seleccionada
                showDatePickerMatriculacion = false // Cierra el DatePicker
            },
            onDismiss = { showDatePickerMatriculacion = false } // Cierra el DatePicker si se cancela
        )
    }

    if (showDatePickerITV) {
        getDateDialogVeh(
            onDateSelected = { fechaSeleccionada ->
                vehiculoViewModel.updateFechaITV(fechaSeleccionada) // Actualiza el ViewModel con la fecha seleccionada
                showDatePickerITV = false // Cierra el DatePicker
            },
            onDismiss = { showDatePickerITV = false } // Cierra el DatePicker si se cancela
        )
    }
}
/**
 * Contenido principal de la pantalla de vehículos.
 * Incluye campos de texto y selectores de fecha para gestionar los datos del vehículo.
 *
 * @param navigateToScreen Función de navegación para cambiar entre pantallas.
 * @param vehiculoViewModel ViewModel asociado que maneja la lógica y estado de los datos.
 * @param onTextFieldChanged Callback para actualizar el valor de los campos de texto.
 * @param onDatePickerMatriculacionClicked Callback para mostrar el selector de fecha de matriculación.
 * @param onDatePickerITVClicked Callback para mostrar el selector de fecha de ITV.
 * @param showDatePickerMatriculacion Estado que controla la visibilidad del selector de fecha de matriculación.
 * @param showDatePickerITV Estado que controla la visibilidad del selector de fecha de ITV.
 */
@Composable
fun VehiculoScreenContent(
    navigateToScreen: (String) -> Unit,
    vehiculoViewModel: VehiculoViewModel,
    onTextFieldChanged: (String) -> Unit,
    onDatePickerMatriculacionClicked: () -> Unit,
    onDatePickerITVClicked: () -> Unit,
    showDatePickerMatriculacion: Boolean,
    showDatePickerITV: Boolean
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ToolbarVehiculo() },
        bottomBar = { BottomAppBarVehiculo(vehiculoViewModel, navigateToScreen) }
    ) { paddingValues ->
        VehiculoContent(
            modifier = Modifier.padding(paddingValues),
            onTextFieldChanged = onTextFieldChanged,
            vehiculoViewModel = vehiculoViewModel,
            onDatePickerMatriculacionClicked = onDatePickerMatriculacionClicked,
            onDatePickerITVClicked = onDatePickerITVClicked,
            showDatePickerMatriculacion = showDatePickerMatriculacion,
            showDatePickerITV = showDatePickerITV
        )
    }
}
/**
 * Barra inferior de la pantalla de vehículos.
 * Incluye botones con tooltips para guardar y limpiar los datos ingresados.
 *
 * @param vehiculoViewModel ViewModel asociado que maneja la lógica y estado de los datos.
 * @param navigateToScreen Función de navegación para cambiar entre pantallas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarVehiculo(
    vehiculoViewModel: VehiculoViewModel,
    navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val plainTooltipState = rememberTooltipState()
    Surface(
        modifier = Modifier.wrapContentHeight()
            .padding(bottom = 30.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Botón "GUARDAR" con tooltip.
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                modifier = Modifier.width(175.dp),
                tooltip = {
                    PlainTooltip {
                        Text("Pulse aquí para guardar los datos introducidos")
                    }
                }
            ) {
                Button(
                    onClick = {
                        vehiculoViewModel.saveData(context)
                        Toast.makeText(
                            context,
                            "Datos guardados correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToScreen("MainScreen")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 5.dp),
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
                        "GUARDAR",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Botón "LIMPIAR" con tooltip.
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                modifier = Modifier.width(175.dp),
                tooltip = {
                    PlainTooltip {
                        Text(
                            "Pulse aquí para limpiar todos los datos almacenados en la aplicación"
                        )
                    }
                }
            ) {
                Button(
                    onClick = {
                        vehiculoViewModel.clearData(context)
                    },
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
}
/**
 * Barra superior de la pantalla de vehículos.
 * Muestra el título "Vehículo" con un estilo centrado y personalizado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarVehiculo() {
    TopAppBar(
        title = {
            Text(
                text = "Vehículo",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                color = TextoNormales,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}
/**
 * Contenido principal del formulario de vehículos.
 * Incluye campos de texto y selectores de fecha para gestionar los datos del vehículo.
 *
 * @param modifier Modificador de Compose para estilizar el contenedor.
 * @param onTextFieldChanged Callback para actualizar el valor de los campos de texto.
 * @param vehiculoViewModel ViewModel asociado que maneja la lógica y estado de los datos.
 * @param onDatePickerMatriculacionClicked Callback para mostrar el selector de fecha de matriculación.
 * @param onDatePickerITVClicked Callback para mostrar el selector de fecha de ITV.
 * @param showDatePickerMatriculacion Estado que controla la visibilidad del selector de fecha de matriculación.
 * @param showDatePickerITV Estado que controla la visibilidad del selector de fecha de ITV.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculoContent(
    modifier: Modifier = Modifier,
    onTextFieldChanged: (String) -> Unit,
    vehiculoViewModel: VehiculoViewModel,
    onDatePickerMatriculacionClicked: () -> Unit,
    onDatePickerITVClicked: () -> Unit,
    showDatePickerMatriculacion: Boolean,
    showDatePickerITV: Boolean
) {
    // Estados para los campos de texto
    val matricula by vehiculoViewModel.matricula.observeAsState(initial = "")
    val fechaMatriculacion by vehiculoViewModel.fechaMatriculacion.observeAsState(initial = "")
    val marca by vehiculoViewModel.marca.observeAsState(initial = "")
    val modelo by vehiculoViewModel.modelo.observeAsState(initial = "")
    val color by vehiculoViewModel.color.observeAsState(initial = "")
    val tipoVehiculo by vehiculoViewModel.tipoVehiculo.observeAsState(initial = "")
    val asegurados by vehiculoViewModel.aseguradora.observeAsState(initial = "")
    val numeroPoliza by vehiculoViewModel.numeroPoliza.observeAsState(initial = "")
    val fechaITV by vehiculoViewModel.fechaITV.observeAsState(initial = "")

    Spacer(modifier = Modifier.height(80.dp))
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 40.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomOutlinedTextField(
                value = matricula,
                onValueChange = { vehiculoViewModel.updateMatricula(it) },
                label = "Matrícula",
                placeholder = "Introduzca la matrícula"
            )
            CustomOutlinedTextField(
                value = fechaMatriculacion,
                onValueChange = { vehiculoViewModel.updateFechaMatriculacion(it) },
                label = "Fecha de matriculación",
                placeholder = "Seleccione la fecha",
                keyboardType = KeyboardType.Text,
                leadingIcon = {
                    IconButton(
                        onClick = onDatePickerMatriculacionClicked, // Muestra el DatePicker cuando se hace clic en el ícono
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
            CustomOutlinedTextField(
                value = marca,
                onValueChange = { vehiculoViewModel.updateMarca(it) },
                label = "Marca",
                placeholder = "Introduzca la marca"
            )
            CustomOutlinedTextField(
                value = modelo,
                onValueChange = { vehiculoViewModel.updateModelo(it) },
                label = "Modelo",
                placeholder = "Introduzca el modelo"
            )
            CustomOutlinedTextField(
                value = color,
                onValueChange = { vehiculoViewModel.updateColor(it) },
                label = "Color",
                placeholder = "Introduzca el color"
            )
            CustomOutlinedTextField(
                value = tipoVehiculo,
                onValueChange = { vehiculoViewModel.updateTipoVehiculo(it) },
                label = "Tipo de vehículo",
                placeholder = "Introduzca el tipo de vehículo"
            )
            CustomOutlinedTextField(
                value = asegurados,
                onValueChange = { vehiculoViewModel.updateAsegurados(it) },
                label = "Asegurados",
                placeholder = "Introduzca los asegurados"
            )
            CustomOutlinedTextField(
                value = numeroPoliza,
                onValueChange = { vehiculoViewModel.updateNumeroPoliza(it) },
                label = "Número de póliza",
                placeholder = "Introduzca el número de póliza"
            )
            CustomOutlinedTextField(
                value = fechaITV,
                onValueChange = { vehiculoViewModel.updateFechaITV(it) },
                label = "Fecha de ITV",
                placeholder = "Seleccione la fecha",
                keyboardType = KeyboardType.Text,
                leadingIcon = {
                    IconButton(
                        onClick = onDatePickerITVClicked, // Muestra el DatePicker cuando se hace clic en el ícono
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
        }
    }
}

/**
 * Campo de texto personalizado con soporte para etiquetas, marcadores de posición e íconos.
 *
 * @param value Valor actual del campo de texto.
 * @param onValueChange Callback para actualizar el valor del campo de texto.
 * @param label Etiqueta del campo de texto.
 * @param placeholder Marcador de posición del campo de texto.
 * @param keyboardType Tipo de teclado utilizado.
 * @param modifier Modificador de Compose para estilizar el campo de texto.
 * @param leadingIcon Ícono opcional que aparece al inicio del campo de texto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomOutlinedTextField(
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
 * Selector de fecha personalizado para gestionar fechas relacionadas con el vehículo.
 *
 * @param onDateSelected Callback para manejar la fecha seleccionada.
 * @param onDismiss Callback para manejar el cierre del selector de fecha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getDateDialogVeh(
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