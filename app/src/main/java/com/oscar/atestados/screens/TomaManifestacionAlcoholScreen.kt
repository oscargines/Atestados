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
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios
import com.oscar.atestados.viewModel.TomaManifestacionAlcoholViewModel

/**
 * Pantalla para la toma de manifestación en un atestado por alcoholemia.
 *
 * Permite al usuario registrar la declaración del investigado respecto al consumo de alcohol
 * y otros detalles relevantes, con opciones para guardar o limpiar los datos.
 *
 * @param navigateToScreen Función lambda para navegar a otra pantalla, recibe una [String] con el nombre de la pantalla destino.
 * @param tomaManifestacionAlcoholViewModel ViewModel que gestiona los datos de la toma de manifestación.
 */
@Composable
fun TomaManifestacionAlcoholScreen(
    navigateToScreen: (String) -> Unit,
    tomaManifestacionAlcoholViewModel: TomaManifestacionAlcoholViewModel
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TomaManifestacionAlcoholTopBar() },
        bottomBar = { TomaManifestacionAlcoholBottomBar(tomaManifestacionAlcoholViewModel, navigateToScreen) }
    ) { paddingValues ->
        TomaManifestacionAlcoholContent(
            modifier = Modifier.padding(paddingValues),
            viewModel = tomaManifestacionAlcoholViewModel
        )
    }
}

/**
 * Barra superior de la pantalla TomaManifestacionAlcoholScreen.
 *
 * Muestra el título "Toma de manifestación" y el subtítulo "Alcoholemia".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TomaManifestacionAlcoholTopBar() {
    CenterAlignedTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Toma de manifestación",
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alcoholemia",
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
 * Contenido principal de la pantalla TomaManifestacionAlcoholScreen.
 *
 * Incluye interruptores y campos de texto para registrar la declaración del investigado
 * relacionada con un control de alcoholemia.
 *
 * @param modifier Modificador para personalizar el diseño del contenido.
 * @param viewModel ViewModel que contiene los datos de la toma de manifestación.
 */
@Composable
private fun TomaManifestacionAlcoholContent(
    modifier: Modifier = Modifier,
    viewModel: TomaManifestacionAlcoholViewModel
) {
    val deseaDeclarar by viewModel.deseaDeclarar.observeAsState(false)
    val renunciaExpresaLletrado by viewModel.renunciaExpresaLletrado.observeAsState(false)
    val condicionesParaManifestacion by viewModel.condicionesParaManifestacion.observeAsState("")
    val consumoAlcohol by viewModel.consumoAlcohol.observeAsState("")
    val procedencia by viewModel.procedencia.observeAsState("")
    val enfermedadMedicamentos by viewModel.enfermedadMedicamentos.observeAsState("")
    val ultimaVezAlcohol by viewModel.ultimaVezAlcohol.observeAsState("")
    val conscientePeligros by viewModel.conscientePeligros.observeAsState("")
    val declaracionAdicional by viewModel.declaracionAdicional.observeAsState("")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Switch para Desea declarar
        SwitchOption(
            text = "Desea declarar",
            checked = deseaDeclarar,
            onCheckedChange = {
                viewModel.setDeseaDeclarar(it)
                if (it) viewModel.setRenunciaExpresaLletrado(false)
            }
        )

        // Switch para Renuncia expresa a letrado
        SwitchOption(
            text = "Renuncia expresa a letrado",
            checked = renunciaExpresaLletrado,
            onCheckedChange = {
                viewModel.setRenunciaExpresaLletrado(it)
                if (it) viewModel.setDeseaDeclarar(false)
            }
        )
        Text(
            text = "¿Se encuentra en condiciones adecuadas para prestar manifestación?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Campo de texto para Condiciones para manifestación
        CustomTextField(
            value = condicionesParaManifestacion,
            onValueChange = { viewModel.setCondicionesParaManifestacion(it) },
            label = "",
            singleLine = true,
            maxLines = 3
        )
        Text(
            text = "¿Ha consumido ALCOHOL con anterioridad a conducir el vehículo?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Campo de texto para Consumo de alcohol
        CustomTextField(
            value = consumoAlcohol,
            onValueChange = { viewModel.setConsumoAlcohol(it) },
            label = "Especificar la cantidad",
            modifier = Modifier.height(150.dp),
            singleLine = false,
            maxLines = 3
        )
        Text(
            text = "¿De qué lugar procede, a dónde se dirigía y a qué hora tenía previsto llegar?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Campo de texto para la procedencia
        CustomTextField(
            value = procedencia,
            onValueChange = { viewModel.setProcedencia(it) },
            label = "",
            modifier = Modifier.height(150.dp),
            singleLine = false,
            maxLines = 5
        )
        Text(
            text = "¿Padece algún tipo de enfermedad o ha tomado medicamentos?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Campo de texto para Enfermedad o medicamentos
        CustomTextField(
            value = enfermedadMedicamentos,
            onValueChange = { viewModel.setEnfermedadMedicamentos(it) },
            label = "Especificar el tipo de medicamento",
            modifier = Modifier.height(150.dp),
            singleLine = false,
            maxLines = 3
        )
        Text(
            text = "¿Cuánto hace de la última vez que tomó alcohol, anterior al control de alcoholemia?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Campo de texto para la última vez que tomó alcohol
        CustomTextField(
            value = ultimaVezAlcohol,
            onValueChange = { viewModel.setUltimaVezAlcohol(it) },
            label = "Especificar la cantidad",
            modifier = Modifier.height(150.dp),
            singleLine = true,
            maxLines = 1
        )
        Text(
            text = "¿Es consciente de los peligros y de la falta de facultades que entraña conducir habiendo consumido bebidas alcohólicas?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Campo de texto para Consciente de los peligros
        CustomTextField(
            value = conscientePeligros,
            onValueChange = { viewModel.setConscientePeligros(it) },
            label = "",
            modifier = Modifier.height(150.dp),
            singleLine = false,
            maxLines = 3
        )
        Text(
            text = "¿Tiene algo más que declarar o decir alguna ampliación o aclaración?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Campo de texto para declaración adicional
        CustomTextField(
            value = declaracionAdicional,
            onValueChange = { viewModel.setDeclaracionAdicional(it) },
            label = "",
            modifier = Modifier.height(250.dp),
            singleLine = false,
            maxLines = 10
        )
    }
}

/**
 * Barra inferior de la pantalla TomaManifestacionAlcoholScreen.
 *
 * Contiene botones para guardar los datos y continuar, o limpiar los datos ingresados.
 *
 * @param viewModel ViewModel que gestiona los datos de la toma de manifestación.
 * @param navigateToScreen Función lambda para navegar a otra pantalla.
 */
@Composable
private fun TomaManifestacionAlcoholBottomBar(
    viewModel: TomaManifestacionAlcoholViewModel,
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
                navigateToScreen("Alcoholemia02Screen")
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("GUARDAR")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = { viewModel.limpiarDatos() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("LIMPIAR")
        }
    }
}

/**
 * Componente de interruptor (switch) con texto descriptivo.
 *
 * @param text Texto que describe la opción del interruptor.
 * @param checked Estado actual del interruptor (activado/desactivado).
 * @param onCheckedChange Callback que se ejecuta al cambiar el estado del interruptor.
 */
@Composable
private fun SwitchOption(
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
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Campo de texto personalizado con borde.
 *
 * @param value Valor actual del campo.
 * @param onValueChange Callback para actualizar el valor del campo.
 * @param label Etiqueta del campo.
 * @param modifier Modificador para personalizar el diseño.
 * @param singleLine Indica si el campo es de una sola línea.
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
        label = { Text(label, fontWeight = FontWeight.Bold) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = singleLine,
        maxLines = maxLines
    )
}