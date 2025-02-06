package com.oscar.atestados.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.atestados.ui.theme.BlueGray200
import com.oscar.atestados.ui.theme.BlueGray900
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.PrimerComponenteColor
import com.oscar.atestados.ui.theme.SegundoComponenteColor
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios

/**
 * Pantalla principal de Guardias Civiles instructores.
 * Muestra un formulario para gestionar los datos de los instructores intervinientes.
 */
@Composable
fun GuardiasScreen(
    navigateToScreen: (String) -> Unit,
    guardiasViewModel: GuardiasViewModel
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ToolbarGuardias() },
        bottomBar = { BottomAppBarGuardias(guardiasViewModel, navigateToScreen) }
    ) { paddingValues ->
        GuardiasContent(
            modifier = Modifier.padding(paddingValues),
            guardiasViewModel = guardiasViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarGuardias() {
    TopAppBar(
        title = {
            Text(
                text = "Instructores",
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

@Composable
fun GuardiasContent(
    modifier: Modifier = Modifier,
    guardiasViewModel: GuardiasViewModel
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Primer Interviniente Section
            Text(
                text = "Primer Interviniente",
                style = MaterialTheme.typography.titleMedium,
                color = TextoSecundarios,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.size(32.dp))

            DropdownRol(guardiasViewModel, 1, PrimerComponenteColor)

            guardiasViewModel.primerTip.value?.let {
                CustomOutlinedTextField(
                    value = it,
                    onValueChange = { guardiasViewModel.updatePrimerTip(it) },
                    label = "TIP",
                    placeholder = "Introduzca el TIP"
                )
            }
            Spacer(modifier = Modifier.size(10.dp))

            DropdownEmpleo(guardiasViewModel, 1, PrimerComponenteColor)

            Spacer(modifier = Modifier.height(10.dp))

            guardiasViewModel.primerUnidad.value?.let {
                CustomOutlinedTextField(
                    value = it,
                    onValueChange = { guardiasViewModel.updatePrimerUnidad(it) },
                    label = "Unidad",
                    placeholder = "Introduzca la unidad"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Segundo Interviniente Section
            Text(
                text = "Segundo Interviniente",
                style = MaterialTheme.typography.titleMedium,
                color = TextoSecundarios,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.size(32.dp))

            DropdownRol(guardiasViewModel, 2, SegundoComponenteColor)

            guardiasViewModel.primerTip.value?.let {
                CustomOutlinedTextField(
                    value = it,
                    onValueChange = { guardiasViewModel.updatePrimerTip(it) },
                    label = "TIP",
                    placeholder = "Introduzca el TIP"
                )
            }
            Spacer(modifier = Modifier.size(10.dp))

            DropdownEmpleo(guardiasViewModel, 2, SegundoComponenteColor)

            Spacer(modifier = Modifier.height(10.dp))

            guardiasViewModel.primerUnidad.value?.let {
                CustomOutlinedTextField(
                    value = it,
                    onValueChange = { guardiasViewModel.updatePrimerUnidad(it) },
                    label = "Unidad",
                    placeholder = "Introduzca la unidad"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownRol(guardiasViewModel: GuardiasViewModel,
                int: Int,
                containerColor: Color) {
    var isExpandedRol by remember { mutableStateOf(false) }
    val listRol = listOf("Instructor", "Secretario", "Otro rol")

    val selectedTextRol = when (int) {
        1 -> guardiasViewModel.rolPrimerInterviniente.observeAsState(initial = listRol[0])
        2 -> guardiasViewModel.rolSegundoInterviniente.observeAsState(initial = listRol[0])
        else -> guardiasViewModel.rolPrimerInterviniente.observeAsState(initial = listRol[0])
    }.value

    ExposedDropdownMenuBox(
        expanded = isExpandedRol,
        onExpandedChange = { isExpandedRol = !isExpandedRol }
    ) {
        OutlinedTextField(
            value = selectedTextRol,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedRol) },
            modifier = Modifier.menuAnchor()
                .background(containerColor),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                focusedTextColor = BlueGray900,
                unfocusedTextColor = BlueGray900
            )
        )

        ExposedDropdownMenu(
            expanded = isExpandedRol,
            onDismissRequest = { isExpandedRol = false }
        ) {
            listRol.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        when (int) {
                            1 -> guardiasViewModel.updateRolPrimerInterviniente(item)
                            2 -> guardiasViewModel.updateRolSegundoInterviniente(item)
                        }
                        isExpandedRol = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownEmpleo(guardiasViewModel: GuardiasViewModel,
                   int: Int,
                   containerColor: Color) {
    var isExpandedEmpleo by remember { mutableStateOf(false) }
    val listRol = listOf(
        "Guardia Civil",
        "Guardia Civil 1ª",
        "Cabo",
        "Cabo 1º",
        "Sargento",
        "Sargento 1º",
        "Brigada",
        "Subteniente",
        "Teniente",
        "Capitán",
        "Comandante"
    )
    val selectedTextEmpleo = when (int) {
        1 -> guardiasViewModel.empleoPrimerInterviniente.observeAsState(initial = listRol[0])
        2 -> guardiasViewModel.empleoSegundoInterviniente.observeAsState(initial = listRol[0])
        else -> guardiasViewModel.empleoPrimerInterviniente.observeAsState(initial = listRol[0])
    }.value

    ExposedDropdownMenuBox(
        expanded = isExpandedEmpleo,
        onExpandedChange = { isExpandedEmpleo = !isExpandedEmpleo }
    ) {
        OutlinedTextField(
            value = selectedTextEmpleo,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedEmpleo) },
            modifier = Modifier
                .menuAnchor()
                .background(containerColor),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                focusedTextColor = BlueGray900,
                unfocusedTextColor = BlueGray900
            )

        )

        ExposedDropdownMenu(
            expanded = isExpandedEmpleo,
            onDismissRequest = { isExpandedEmpleo = false }
        ) {
            listRol.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        when (int) {
                            1 -> guardiasViewModel.updateEmpleoPrimerInterviniente(item)
                            2 -> guardiasViewModel.updateEmpleoSegundoInterviniente(item)
                        }
                        isExpandedEmpleo = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarGuardias(
    guardiasViewModel: GuardiasViewModel,
    navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.wrapContentHeight(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    guardiasViewModel.saveData(context)
                    navigateToScreen("MainScreen")
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 5.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotonesNormales,
                    contentColor = TextoBotonesNormales
                )
            ) {
                Text(
                    "GUARDAR",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    guardiasViewModel.clearData(context)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 5.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotonesNormales,
                    contentColor = TextoBotonesNormales
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