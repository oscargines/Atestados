package com.oscar.atestados.screens

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import com.oscar.atestados.ui.theme.BlueGray200
import com.oscar.atestados.ui.theme.BlueGray900
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.PrimerComponenteColor
import com.oscar.atestados.ui.theme.SegundoComponenteColor
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios
import com.oscar.atestados.ui.theme.TextoTerciarios

/**
 * Delegate para acceder al DataStore de preferencias usado para almacenar datos de guardias.
 */
val Context.dataStoreGua by preferencesDataStore(name = "GUARDIAS_PREFERENCES")

/**
 * Pantalla principal para gestionar datos de Guardias Civiles instructores.
 *
 * Esta pantalla muestra un formulario con campos para registrar información de dos intervinientes,
 * como su TIP, rol, empleo y unidad. Utiliza un [Scaffold] con una barra superior y otra inferior
 * para navegación y acciones.
 *
 * @param navigateToScreen Función lambda que recibe una [String] para navegar a otra pantalla.
 * @param guardiasViewModel ViewModel que gestiona el estado y la lógica de los datos de la pantalla.
 */
@Composable
fun GuardiasScreen(
    navigateToScreen: (String) -> Unit,
    guardiasViewModel: GuardiasViewModel
) {
    val context = LocalContext.current

    // Carga los datos al iniciar la pantalla
    LaunchedEffect(Unit) {
        guardiasViewModel.loadData(context)
    }

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

/**
 * Barra superior de la pantalla de Guardias.
 *
 * Muestra un título centrado con el texto "Instructores" utilizando un estilo definido por el tema.
 */
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
                    .padding(10.dp)
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * Contenido principal de la pantalla de Guardias.
 *
 * Organiza los formularios para registrar información de dos intervinientes en una columna
 * desplazable. Incluye campos para TIP, rol, empleo y unidad.
 *
 * @param modifier Modificador para personalizar el diseño del contenido.
 * @param guardiasViewModel ViewModel que gestiona el estado y la lógica de los datos.
 */
@Composable
fun GuardiasContent(
    modifier: Modifier = Modifier,
    guardiasViewModel: GuardiasViewModel
) {
    val primerTip by guardiasViewModel.primerTip.observeAsState(initial = "")
    val segundoTip by guardiasViewModel.segundoTip.observeAsState(initial = "")
    val primerUnidad by guardiasViewModel.primerUnidad.observeAsState(initial = "")
    val segundoUnidad by guardiasViewModel.segundoUnidad.observeAsState(initial = "")

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primer Interviniente
            Text(
                text = "Primer Interviniente",
                style = MaterialTheme.typography.titleMedium,
                color = TextoSecundarios,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )

            DropdownRol(guardiasViewModel, 1, PrimerComponenteColor)

            OutlinedTextFieldCustom(
                value = primerTip,
                onValueChange = { guardiasViewModel.updatePrimerTip(it) },
                label = "TIP",
                placeholder = "Introduzca el TIP",
                containerColor = PrimerComponenteColor,
                backColor = PrimerComponenteColor
            )

            DropdownEmpleo(guardiasViewModel, 1, PrimerComponenteColor)

            OutlinedTextFieldCustom(
                value = primerUnidad,
                onValueChange = { guardiasViewModel.updatePrimerUnidad(it) },
                label = "Unidad",
                placeholder = "Introduzca la unidad",
                containerColor = PrimerComponenteColor,
                backColor = PrimerComponenteColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Segundo Interviniente
            Text(
                text = "Segundo Interviniente",
                style = MaterialTheme.typography.titleMedium,
                color = TextoSecundarios,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )

            DropdownRol(guardiasViewModel, 2, SegundoComponenteColor)

            OutlinedTextFieldCustom(
                value = segundoTip,
                onValueChange = { guardiasViewModel.updateSegundoTip(it) },
                label = "TIP",
                placeholder = "Introduzca el TIP",
                containerColor = SegundoComponenteColor,
                backColor = SegundoComponenteColor
            )

            DropdownEmpleo(guardiasViewModel, 2, SegundoComponenteColor)

            OutlinedTextFieldCustom(
                value = segundoUnidad,
                onValueChange = { guardiasViewModel.updateSegundoUnidad(it) },
                label = "Unidad",
                placeholder = "Introduzca la unidad",
                containerColor = SegundoComponenteColor,
                backColor = SegundoComponenteColor
            )
        }
    }
}

/**
 * Menú desplegable para seleccionar el rol de un interviniente.
 *
 * Permite al usuario elegir entre "Instructor", "Secretario" u "Otro rol" y actualiza el estado
 * en el ViewModel según el interviniente seleccionado.
 *
 * @param guardiasViewModel ViewModel que gestiona el estado y la lógica.
 * @param int Identificador del interviniente (1 para el primero, 2 para el segundo).
 * @param containerColor Color de fondo del componente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownRol(
    guardiasViewModel: GuardiasViewModel,
    int: Int,
    containerColor: Color
) {
    var isExpandedRol by remember { mutableStateOf(false) }
    val listRol = listOf("Instructor", "Secretario", "Otro rol")

    val selectedTextRol = when (int) {
        1 -> guardiasViewModel.rolPrimerInterviniente.observeAsState(initial = listRol[0])
        2 -> guardiasViewModel.rolSegundoInterviniente.observeAsState(initial = listRol[0])
        else -> guardiasViewModel.rolPrimerInterviniente.observeAsState(initial = listRol[0])
    }.value

    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
        expanded = isExpandedRol,
        onExpandedChange = { isExpandedRol = !isExpandedRol }
    ) {
        OutlinedTextField(
            value = selectedTextRol,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedRol) },
            modifier = Modifier
                .menuAnchor()
                .background(containerColor)
                .fillMaxWidth(),
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

/**
 * Menú desplegable para seleccionar el empleo de un interviniente.
 *
 * Ofrece una lista de empleos militares (Guardia Civil, Cabo, Sargento, etc.) y actualiza el estado
 * en el ViewModel según el interviniente seleccionado.
 *
 * @param guardiasViewModel ViewModel que gestiona el estado y la lógica.
 * @param int Identificador del interviniente (1 para el primero, 2 para el segundo).
 * @param containerColor Color de fondo del componente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownEmpleo(
    guardiasViewModel: GuardiasViewModel,
    int: Int,
    containerColor: Color
) {
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
        modifier = Modifier.fillMaxWidth(),
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
                .background(containerColor)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                focusedTextColor = BlueGray900,
                unfocusedTextColor = BlueGray900
            ),
            maxLines = 1
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

/**
 * Campo de texto personalizado con estilo específico para la pantalla de Guardias.
 *
 * Permite al usuario introducir texto con una etiqueta y un placeholder personalizados, aplicando
 * colores definidos por el tema.
 *
 * @param value Valor actual del campo de texto.
 * @param onValueChange Callback que se ejecuta cuando el valor del campo cambia.
 * @param label Etiqueta descriptiva del campo.
 * @param placeholder Texto de ayuda que aparece cuando el campo está vacío.
 * @param modifier Modificador para personalizar el diseño del campo.
 * @param containerColor Color del contenedor del campo.
 * @param backColor Color de fondo del campo.
 */
@Composable
fun OutlinedTextFieldCustom(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    containerColor: Color,
    backColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = TextoSecundarios
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = TextoTerciarios,
                textDecoration = TextDecoration.Underline
            )
        },
        shape = MaterialTheme.shapes.extraSmall,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = backColor,
            unfocusedContainerColor = backColor,
            focusedTextColor = BlueGray900,
            unfocusedTextColor = BlueGray900,
            focusedLabelColor = TextoSecundarios,
            unfocusedLabelColor = TextoSecundarios
        )
    )
}

/**
 * Barra inferior de la pantalla de Guardias con botones de acción.
 *
 * Contiene dos botones: "GUARDAR" para salvar los datos y navegar a la pantalla principal, y
 * "LIMPIAR" para borrar los datos del formulario.
 *
 * @param guardiasViewModel ViewModel que gestiona el estado y la lógica.
 * @param navigateToScreen Función lambda para navegar a otra pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarGuardias(
    guardiasViewModel: GuardiasViewModel,
    navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .wrapContentHeight()
            .padding(bottom = 20.dp),
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