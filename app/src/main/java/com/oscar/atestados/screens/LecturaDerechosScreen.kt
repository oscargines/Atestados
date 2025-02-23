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

@Composable
fun LecturaDerechosScreen(
    navigateToScreen: (String) -> Unit,
    lecturaDerechosViewModel: LecturaDerechosViewModel
) {
    var showInitialDialog by remember { mutableStateOf(true) }

    // Dialog inicial
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