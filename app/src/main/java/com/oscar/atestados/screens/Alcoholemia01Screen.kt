package com.oscar.atestados.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.viewModel.AlcoholemiaUnoViewModel

@Composable
fun Alcoholemia01Screen(
    navigateToScreen: (String) -> Unit,
    alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AlcoholemiaTopBar() },
        bottomBar = { AlcoholemiaBottomBar(alcoholemiaUnoViewModel, navigateToScreen) }
    ) { paddingValues ->
        AlcoholemiaContent(
            modifier = Modifier.padding(paddingValues),
            alcoholemiaUnoViewModel = alcoholemiaUnoViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlcoholemiaTopBar() {
    CenterAlignedTopAppBar(
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
                    text = "Datos etilómetro y alcoholemia",
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
    viewModel: AlcoholemiaUnoViewModel,
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
                navigateToScreen("LecturaDerechosScreen")
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlcoholemiaContent(
    modifier: Modifier = Modifier,
    alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel
) {
    val context = LocalContext.current

    var showTimePicker1 by remember { mutableStateOf(false) }
    var showTimePicker2 by remember { mutableStateOf(false) }
    val timePickerState1 = rememberTimePickerState()
    val timePickerState2 = rememberTimePickerState()

    // Estados del ViewModel
    val marca by alcoholemiaUnoViewModel.marca.observeAsState("")
    val modelo by alcoholemiaUnoViewModel.modelo.observeAsState("")
    val serie by alcoholemiaUnoViewModel.serie.observeAsState("")
    val caducidad by alcoholemiaUnoViewModel.caducidad.observeAsState("")
    val primeraTasa by alcoholemiaUnoViewModel.primeraTasa.observeAsState("")
    val primeraHora by alcoholemiaUnoViewModel.primeraHora.observeAsState("")
    val segundaTasa by alcoholemiaUnoViewModel.segundaTasa.observeAsState("")
    val segundaHora by alcoholemiaUnoViewModel.segundaHora.observeAsState("")

    // TimePicker para primera hora
    if (showTimePicker1) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker1 = false },
            confirmButton = {
                Button(
                    onClick = {
                        alcoholemiaUnoViewModel.updatePrimeraHora(
                            "${timePickerState1.hour}:${
                                timePickerState1.minute.toString().padStart(2, '0')
                            }"
                        )
                        showTimePicker1 = false
                    }
                ) { Text("OK") }
            }
        ) { TimePicker(state = timePickerState1) }
    }

    // TimePicker para segunda hora
    if (showTimePicker2) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker2 = false },
            confirmButton = {
                Button(
                    onClick = {
                        alcoholemiaUnoViewModel.updateSegundaHora(
                            "${timePickerState2.hour}:${
                                timePickerState2.minute.toString().padStart(2, '0')
                            }"
                        )
                        showTimePicker2 = false
                    }
                ) { Text("OK") }
            }
        ) { TimePicker(state = timePickerState2) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        val opcionSeleccionada by alcoholemiaUnoViewModel.opcionMotivo.observeAsState()

        Column {
            listOf(
                "Implicado en siniestro vial",
                "Síntomas evidentes o bajo influencia",
                "Infracción contra Seguridad vial",
                "Control preventivo"
            ).forEach { opcion ->
                RadioOption(
                    text = opcion,
                    selected = opcionSeleccionada == opcion,
                    onSelect = { alcoholemiaUnoViewModel.setOpcionMotivo(opcion) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        CustomTextField(
            value = marca,
            onValueChange = { alcoholemiaUnoViewModel.updateMarca(it) },
            label = "Marca del etilómetro"
        )
        CustomTextField(
            value = modelo,
            onValueChange = { alcoholemiaUnoViewModel.updateModelo(it) },
            label = "Modelo del etilómetro"
        )
        CustomTextField(
            value = serie,
            onValueChange = { alcoholemiaUnoViewModel.updateSerie(it) },
            label = "Número de Serie"
        )
        CustomTextField(
            value = caducidad,
            onValueChange = { alcoholemiaUnoViewModel.updateCaducidad(it) },
            label = "Fecha de caducidad"
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Sección Errores permitidos
        Text(
            text = "Máximos errores permitidos",
            style = MaterialTheme.typography.titleSmall,
            color = TextoTerciarios,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )
        // Para errores permitidos
        val opcionErroresSeleccionada by alcoholemiaUnoViewModel.opcionErrores.observeAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "Nuevo",
                "Mas de un año",
                "Habiendo sido reparado"
            ).forEach { opcion ->
                RadioOptionHorizontal(
                    text = opcion,
                    selected = opcionErroresSeleccionada == opcion,
                    onSelect = { alcoholemiaUnoViewModel.setOpcionErrores(opcion) }
                )
            }
        }
        // Sección Pruebas
        Text(
            text = "¿Desea realizar las pruebas?",
            style = MaterialTheme.typography.titleSmall,
            color = TextoTerciarios,
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
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Para deseo de realizar pruebas
            val opcionDeseoSeleccionada by alcoholemiaUnoViewModel.opcionDeseaPruebas.observeAsState()

            listOf(
                "Si",
                "No"
            ).forEach { opcion ->
                RadioOptionHorizontal(
                    text = opcion,
                    selected = opcionDeseoSeleccionada == opcion,
                    onSelect = { alcoholemiaUnoViewModel.setOpcionDeseaPruebas(opcion) }
                )
            }
        }

        // Sección Tasas
        Text(
            text = "Tasas de alcoholemia",
            style = MaterialTheme.typography.titleSmall,
            color = TextoTerciarios,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )
        //Primera prueba
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Primera prueba",
                modifier = Modifier
                    .weight(1.2f)
                    .padding(end = 8.dp),
                color = TextoNormales,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                modifier = Modifier.weight(0.8f),
                value = primeraTasa,
                onValueChange = { alcoholemiaUnoViewModel.updatePrimeraTasa(it) },
                label = { Text("Tasa", color = TextoTerciarios) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = primeraHora,
                onValueChange = { alcoholemiaUnoViewModel.updatePrimeraHora(it) },
                label = { Text("Hora", color = TextoTerciarios) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker1 = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.reloj_ico),
                            contentDescription = "Seleccionar hora"
                        )
                    }
                }
            )

        }
        //Segunda prueba
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Segunda prueba",
                modifier = Modifier
                    .weight(1.2f)
                    .padding(end = 8.dp),
                color = TextoNormales,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                modifier = Modifier.weight(0.8f),
                value = segundaTasa,
                onValueChange = { alcoholemiaUnoViewModel.updateSegundaTasa(it) },
                label = { Text("Tasa", color = TextoTerciarios) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = segundaHora,
                onValueChange = { alcoholemiaUnoViewModel.updateSegundaHora(it) },
                label = { Text("Hora", color = TextoTerciarios) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker2 = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.reloj_ico),
                            contentDescription = "Seleccionar hora"
                        )
                    }
                }
            )

        }

    }
}

@Composable
fun RadioOptionHorizontal(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
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
            modifier = Modifier.padding(start = 4.dp)
        )
    }

}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraLarge
                ),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()

                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text("Cancelar")
                    }
                    confirmButton()
                }
            }
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