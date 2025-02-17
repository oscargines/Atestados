package com.oscar.atestados.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Text(
                text = "Alcoholemia",
                fontSize = 30.sp,
                style = MaterialTheme.typography.titleLarge,
                color = TextoNormales,
                fontWeight = FontWeight.Bold
            )
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
                navigateToScreen("MainScreen")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier.weight(1f)
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
            modifier = Modifier.weight(1f)
        ) {
            Text("LIMPIAR")
        }
    }
}

@Composable
private fun AlcoholemiaContent(
    modifier: Modifier = Modifier,
    alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel
) {
    val context = LocalContext.current

    // Estados del ViewModel
    val marca by alcoholemiaUnoViewModel.marca.observeAsState("")
    val modelo by alcoholemiaUnoViewModel.modelo.observeAsState("")
    val serie by alcoholemiaUnoViewModel.serie.observeAsState("")
    val caducidad by alcoholemiaUnoViewModel.caducidad.observeAsState("")
    val primeraTasa by alcoholemiaUnoViewModel.primeraTasa.observeAsState("")
    val primeraHora by alcoholemiaUnoViewModel.primeraHora.observeAsState("")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Sección Datos del etilómetro
        Text(
            text = "Datos etilómetro y alcoholemia",
            style = MaterialTheme.typography.titleSmall,
            color = TextoTerciarios,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )
        val opcionSeleccionada by alcoholemiaUnoViewModel.opcionMotivo.observeAsState()

        Column {
            listOf(
                "Implicado en siniestro vial",
                "Síntomas evidentes o bajo influencia",
                "Infracción contra Seguridad vial",
                "Control preventivo"
            ).forEach { opcion ->
                RadioOptionMotivo(
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

        Text(
            text = "Caducidad del etilómetro",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        CustomTextField(
            value = caducidad,
            onValueChange = { alcoholemiaUnoViewModel.updateCaducidad(it) },
            label = "Fecha de caducidad"
        )

        // Sección Errores permitidos
        Text(
            text = "Máximos errores permitidos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        CheckboxOption(text = "Nuevo", viewModel = alcoholemiaUnoViewModel)
        CheckboxOption(text = "Más de un año", viewModel = alcoholemiaUnoViewModel)
        CheckboxOption(text = "Habiendo sido reparado", viewModel = alcoholemiaUnoViewModel)

        // Sección Pruebas
        Text(
            text = "¿Desea realizar las pruebas?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CheckboxOption(text = "Sí", viewModel = alcoholemiaUnoViewModel)
            CheckboxOption(text = "No", viewModel = alcoholemiaUnoViewModel)
        }

        // Sección Tasas
        Text(
            text = "Tasas de alcoholemia",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Primera prueba", modifier = Modifier.weight(1f))
            CustomTextField(
                value = primeraTasa,
                onValueChange = { alcoholemiaUnoViewModel.updatePrimeraTasa(it) },
                label = "Tasa",
                modifier = Modifier.width(80.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            CustomTextField(
                value = primeraHora,
                onValueChange = { alcoholemiaUnoViewModel.updatePrimeraHora(it) },
                label = "Hora",
                modifier = Modifier.width(80.dp)
            )
        }

        // Segunda prueba (opcional)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { /* Implementar lógica */ }
            )
            Text("Segunda prueba", modifier = Modifier.weight(1f))
            CustomTextField(
                value = "",
                onValueChange = {},
                label = "Tasa",
                modifier = Modifier.width(80.dp),
                enabled = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            CustomTextField(
                value = "",
                onValueChange = {},
                label = "Hora",
                modifier = Modifier.width(80.dp),
                enabled = false
            )
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
private fun RadioOptionMotivo(
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
        Text(text = text,
            color = TextoNormales,
            modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CheckboxOption(
    text: String,
    viewModel: AlcoholemiaUnoViewModel
) {
    val isChecked = viewModel.getCheckboxState(text).observeAsState(false)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = isChecked.value,
            onCheckedChange = { viewModel.updateCheckboxState(text, it) }
        )
        Text(text = text)
    }
}