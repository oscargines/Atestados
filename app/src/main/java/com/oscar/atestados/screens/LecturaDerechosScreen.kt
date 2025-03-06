package com.oscar.atestados.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.viewModel.LecturaDerechosViewModel

/**
 * Pantalla para registrar la lectura de derechos en una investigación.
 *
 * Esta pantalla muestra un formulario con campos para registrar información relevante sobre
 * la investigación y un diálogo inicial con instrucciones. Permite guardar o limpiar los datos.
 *
 * @param navigateToScreen Función lambda que recibe una [String] para navegar a otra pantalla.
 * @param lecturaDerechosViewModel ViewModel que gestiona el estado y la lógica de los datos.
 */
@Composable
fun LecturaDerechosScreen(
    navigateToScreen: (String) -> Unit,
    lecturaDerechosViewModel: LecturaDerechosViewModel
) {
    var showInitialDialog by remember { mutableStateOf(true) }

    // Diálogo inicial con instrucciones
    if (showInitialDialog) {
        AlertDialog(
            onDismissRequest = { showInitialDialog = false },
            title = {
                Text(
                    "ATENCIÓN",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Rellene todos los campos y antes de que guarde la información, " +
                            "informe al investigado sobre la información que ha introducido.\n\n " +
                            "Hágalo de forma clara y entendible para el usuario." +
                            "\n\nHágase entender.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextoNormales
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showInitialDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BotonesNormales
                    )
                ) {
                    Text("ENTENDIDO")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
            .padding(top = 16.dp),
        topBar = { LecturaDerechosTopBar() },
        bottomBar = { LecturaDerechosBottomBar(lecturaDerechosViewModel, navigateToScreen) }
    ) { paddingValues ->
        LecturaDerechosContent(
            modifier = Modifier.padding(paddingValues),
            lecturaDerechosViewModel = lecturaDerechosViewModel
        )
    }
}

/**
 * Barra superior de la pantalla de lectura de derechos.
 *
 * Muestra un título principal "Lectura de derechos" y un subtítulo con información adicional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LecturaDerechosTopBar() {
    CenterAlignedTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        title = {
            Column {
                Text(
                    text = "Lectura de derechos",
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Datos relativos a la investigación",
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoSecundarios
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * Contenido principal de la pantalla de lectura de derechos.
 *
 * Incluye opciones de radio para el momento de la lectura y campos de texto para detalles de
 * la investigación, como lugar, hechos e indicios.
 *
 * @param modifier Modificador para personalizar el diseño del contenido.
 * @param lecturaDerechosViewModel ViewModel que gestiona el estado y la lógica de los datos.
 */
@Composable
private fun LecturaDerechosContent(
    modifier: Modifier = Modifier,
    lecturaDerechosViewModel: LecturaDerechosViewModel
) {
    val momentoLectura by lecturaDerechosViewModel.momentoLectura.observeAsState("Tomada en el momento")
    val lugarInvestigacion by lecturaDerechosViewModel.lugarInvestigacion.observeAsState("")
    val lugarDelito by lecturaDerechosViewModel.lugarDelito.observeAsState("")
    val resumenHechos by lecturaDerechosViewModel.resumenHechos.observeAsState("")
    val calificacionHechos by lecturaDerechosViewModel.calificacionHechos.observeAsState("")
    val relacionIndicios by lecturaDerechosViewModel.relacionIndicios.observeAsState("")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Radio buttons para el momento de la lectura
        Column {
            listOf(
                "Tomada en el momento",
                "En otro momento"
            ).forEach { opcion ->
                RadioOption(
                    text = opcion,
                    selected = momentoLectura == opcion,
                    onSelect = { lecturaDerechosViewModel.setMomentoLectura(opcion) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campos de texto
        CustomTextField(
            value = lugarInvestigacion,
            onValueChange = { lecturaDerechosViewModel.updateLugarInvestigacion(it) },
            label = "Lugar, hora y fecha de la investigación",
            singleLine = false,
            maxLines = 1
        )

        CustomTextField(
            value = lugarDelito,
            onValueChange = { lecturaDerechosViewModel.updateLugarDelito(it) },
            label = "Lugar, hora y fecha de la comisión del delito",
            singleLine = false,
            maxLines = 1
        )

        CustomTextField(
            value = resumenHechos,
            onValueChange = { lecturaDerechosViewModel.updateResumenHechos(it) },
            label = "Breve resumen de los hechos",
            modifier = Modifier.height(150.dp),
            singleLine = false,
            maxLines = 5
        )

        CustomTextField(
            value = calificacionHechos,
            onValueChange = { lecturaDerechosViewModel.updateCalificacionHechos(it) },
            label = "Calificación provisional de los hechos",
            modifier = Modifier.height(100.dp),
            singleLine = false,
            maxLines = 3
        )

        CustomTextField(
            value = relacionIndicios,
            onValueChange = { lecturaDerechosViewModel.updateRelacionIndicios(it) },
            label = "Relación de indicios de los que se deduce la investigación",
            modifier = Modifier.height(250.dp),
            singleLine = false,
            maxLines = 10
        )
    }
}

/**
 * Barra inferior con botones para guardar o limpiar los datos.
 *
 * Contiene un botón "GUARDAR" para salvar la información y navegar a otra pantalla, y un botón
 * "LIMPIAR" para borrar los datos ingresados.
 *
 * @param viewModel ViewModel que gestiona el estado y la lógica de los datos.
 * @param navigateToScreen Función lambda para navegar a otra pantalla.
 */
@Composable
private fun LecturaDerechosBottomBar(
    viewModel: LecturaDerechosViewModel,
    navigateToScreen: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = {
                viewModel.guardarDatos()
                navigateToScreen("TomaDerechosScreen")
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
 * Componente de opción de radio para seleccionar el momento de la lectura.
 *
 * @param text Texto de la opción (por ejemplo, "Tomada en el momento").
 * @param selected Indica si la opción está seleccionada.
 * @param onSelect Callback que se ejecuta al seleccionar la opción.
 */
@Composable
private fun RadioOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = BotonesNormales,
                unselectedColor = TextoSecundarios
            )
        )
        Text(
            text = text,
            color = TextoNormales,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Campo de texto personalizado para la entrada de datos.
 *
 * Permite introducir texto con una etiqueta y personalizar el número de líneas y el tamaño.
 *
 * @param value Valor actual del campo de texto.
 * @param onValueChange Callback que se ejecuta al cambiar el valor.
 * @param label Etiqueta descriptiva del campo.
 * @param modifier Modificador para personalizar el diseño del campo.
 * @param singleLine Indica si el campo debe ser de una sola línea.
 * @param maxLines Número máximo de líneas permitidas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoTerciarios) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = singleLine,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = TextoSecundarios,
            focusedBorderColor = BotonesNormales
        )
    )
}