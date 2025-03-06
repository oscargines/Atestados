package com.oscar.atestados.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.viewModel.TomaDerechosViewModel
import java.io.InputStream

/**
 * Modelo de datos que contiene una lista de derechos.
 *
 * @property derechos Lista de mensajes con los derechos a mostrar.
 */
data class Mensajes(
    val derechos: List<String>
)

/**
 * Modelo de datos que contiene elementos esenciales y sus mensajes asociados.
 *
 * @property item Lista de elementos esenciales.
 * @property mensaje Lista de mensajes correspondientes a los elementos esenciales.
 */
data class ElementosEsenciales(
    val item: List<String>,
    val mensaje: List<String>
)

/**
 * Pantalla para la toma de derechos en un atestado.
 *
 * Permite al usuario registrar opciones relacionadas con los derechos del investigado,
 * como prestar declaración, asistencia letrada y acceso a elementos esenciales.
 *
 * @param navigateToScreen Función lambda para navegar a otra pantalla, recibe una [String] con el nombre de la pantalla destino.
 * @param tomaDerechosViewModel ViewModel que gestiona los datos de la toma de derechos.
 */
@Composable
fun TomaDerechosScreen(
    navigateToScreen: (String) -> Unit,
    tomaDerechosViewModel: TomaDerechosViewModel
) {
    val mensajes = ObtenerDerechos(context = LocalContext.current)

    // Solo lanza el alert si hay mensajes cargados
    if (mensajes.isNotEmpty() && mensajes != listOf("Cargando mensajes...")) {
        LanzaAlert(mensajes = mensajes)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        topBar = { TomaDerechosTopBar() },
        bottomBar = { LecturaDerechosDosBottomBar(tomaDerechosViewModel, navigateToScreen) }
    ) { paddingValues ->
        TomaDerechosContent(
            modifier = Modifier.padding(paddingValues),
            tomaDerechosViewModel = tomaDerechosViewModel
        )
    }
}

/**
 * Barra superior de la pantalla TomaDerechosScreen.
 *
 * Muestra el título "Lectura de derechos" y un subtítulo descriptivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TomaDerechosTopBar() {
    CenterAlignedTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lectura de derechos",
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Toma de los derechos",
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
 * Contenido principal de la pantalla TomaDerechosScreen.
 *
 * Incluye opciones de selección (switches), menús desplegables y campos de texto para registrar
 * los datos relacionados con los derechos del investigado.
 *
 * @param modifier Modificador para personalizar el diseño del contenido.
 * @param tomaDerechosViewModel ViewModel que contiene los datos de la toma de derechos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TomaDerechosContent(
    modifier: Modifier = Modifier,
    tomaDerechosViewModel: TomaDerechosViewModel
) {
    val prestarDeclaracion by tomaDerechosViewModel.prestarDeclaracion.observeAsState(true)
    val renunciaAsistenciaLetrada by tomaDerechosViewModel.renunciaAsistenciaLetrada.observeAsState(true)
    val asistenciaLetradoParticular by tomaDerechosViewModel.asistenciaLetradoParticular.observeAsState(false)
    val nombreLetrado by tomaDerechosViewModel.nombreLetrado.observeAsState("")
    val asistenciaLetradoOficio by tomaDerechosViewModel.asistenciaLetradoOficio.observeAsState(true)
    val accesoElementos by tomaDerechosViewModel.accesoElementos.observeAsState(false)
    val interprete by tomaDerechosViewModel.interprete.observeAsState(false)
    val textoElementosEsenciales by tomaDerechosViewModel.textoElementosEsenciales.observeAsState("")
    val selectedOption by tomaDerechosViewModel.selectedOption.observeAsState("Otros")

    var isExpanded by remember { mutableStateOf(false) }

    val options = listOf(
        "Se muestra tickets",
        "Se muestra hoja de síntomas",
        "Recibe explicación por siniestro",
        "Otros"
    )
    val context = LocalContext.current

    var textoOtros by remember { mutableStateOf("") }

    var elementosEsenciales by remember {
        mutableStateOf(ElementosEsenciales(emptyList(), emptyList()))
    }
    var showDialog by remember { mutableStateOf(false) }

    // Cargar los elementos esenciales desde el JSON
    LaunchedEffect(Unit) {
        elementosEsenciales = leerElementosEsenciales(context, "elementos_esenciales.json")
    }

    // Texto que se mostrará en el CustomTextField
    val textoMostrado = if (selectedOption != "Otros") {
        val index = options.indexOf(selectedOption)
        if (index in elementosEsenciales.item.indices) {
            elementosEsenciales.item[index] // Obtener el texto del JSON
        } else {
            "" // Si no hay texto correspondiente, mostrar vacío
        }
    } else {
        textoElementosEsenciales // Si es "Otros", mostrar el texto ingresado por el usuario
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Switch para Prestar declaración
        SwitchOption(
            text = "Prestar declaración",
            checked = prestarDeclaracion,
            onCheckedChange = { tomaDerechosViewModel.setPrestarDeclaracion(it) }
        )

        // Switch para Renuncia a la asistencia letrada in situ
        SwitchOption(
            text = "Renuncia a la asistencia letrada in situ",
            checked = renunciaAsistenciaLetrada,
            onCheckedChange = { tomaDerechosViewModel.setRenunciaAsistenciaLetrada(it) }
        )

        // Switch para Ser asistido por el letrado D./Dª.
        SwitchOption(
            text = "Ser asistido por el letrado D./Dª.",
            checked = asistenciaLetradoParticular,
            onCheckedChange = { tomaDerechosViewModel.setAsistenciaLetradoParticular(it) }
        )

        // Campo de texto para el nombre del letrado
        CustomTextField(
            value = nombreLetrado,
            onValueChange = { tomaDerechosViewModel.setNombreLetrado(it) },
            label = "Nombre completo del letrado",
            singleLine = true,
            maxLines = 1,
            enabled = asistenciaLetradoParticular
        )

        // Switch para Ser asistido por el letrado de oficio
        SwitchOption(
            text = "Ser asistido por el letrado de oficio",
            checked = asistenciaLetradoOficio,
            onCheckedChange = { tomaDerechosViewModel.setAsistenciaLetradoOficio(it) }
        )

        // Switch para Acceder a los elementos esenciales de la investigación
        SwitchOption(
            text = "Acceder a los elementos esenciales de la investigación",
            checked = accesoElementos,
            onCheckedChange = { tomaDerechosViewModel.setAccesoElementos(it) }
        )

        // Switch para Intérprete
        SwitchOption(
            text = "Intérprete",
            checked = interprete,
            onCheckedChange = { tomaDerechosViewModel.setInterprete(it) }
        )
        Spacer(modifier = Modifier.height(26.dp))

        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = it }
        ) {
            TextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            tomaDerechosViewModel.setSelectedOption(option)
                            isExpanded = false
                            if (option == "Se muestra hoja de síntomas" || option == "Recibe explicación por siniestro") {
                                showDialog = true
                            }
                            if (option == "Otros") {
                                val index = options.indexOf(option)
                                if (index in elementosEsenciales.item.indices) {
                                    tomaDerechosViewModel.setTextoElementosEsenciales(elementosEsenciales.item[index])
                                }
                            } else {
                                tomaDerechosViewModel.setTextoElementosEsenciales("")
                            }
                        }
                    )
                }
            }
        }

        // Mostrar contenido no editable en función de la opción seleccionada
        if (selectedOption != "Otros") {
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("INFORMACIÓN IMPORTANTE") },
                    text = {
                        when (selectedOption) {
                            "Se muestra hoja de síntomas" -> {
                                Text("ESTA INFORMACIÓN SOLO SERÁ MOSTRADA EN CASOS MUY EXCEPCIONALES Y DEBE VALORAR LA CONVENIENCIA DE HACERLO Y ANTE QUIEN HACERLO.")
                            }
                            "Recibe explicación por siniestro" -> {
                                Text("Realice una valoración sobre la conveniencia o no de mostrar este tipo de información")
                            }
                            else -> { Text("") }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) { Text("Continuar") }
                    }
                )
            }
        }

        // Campo de texto para "Otros" (siempre visible)
        CustomTextField(
            value = textoMostrado,
            onValueChange = { textoOtros = it },
            modifier = Modifier.height(200.dp),
            label = if (selectedOption == "Otros") "Especifique otros detalles" else "Elementos esenciales",
            singleLine = false,
            maxLines = 5,
            enabled = selectedOption == "Otros",
            readOnly = selectedOption != "Otros"
        )
    }
}

/**
 * Barra inferior de la pantalla TomaDerechosScreen.
 *
 * Contiene botones para guardar los datos y continuar, o limpiar los datos ingresados.
 *
 * @param viewModel ViewModel que gestiona los datos de la toma de derechos.
 * @param navigateToScreen Función lambda para navegar a otra pantalla.
 */
@Composable
private fun LecturaDerechosDosBottomBar(
    viewModel: TomaDerechosViewModel,
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
                navigateToScreen("TomaManifestacionAlcoholScreen")
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = TextoNormales,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BotonesNormales,
                checkedTrackColor = BotonesNormales.copy(alpha = 0.5f),
                uncheckedThumbColor = TextoSecundarios,
                uncheckedTrackColor = TextoSecundarios.copy(alpha = 0.5f)
            )
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
 * @param enabled Indica si el campo está habilitado para edición.
 * @param readOnly Indica si el campo es solo de lectura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoSecundarios) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = singleLine,
        maxLines = maxLines,
        enabled = enabled,
        readOnly = readOnly,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = if (enabled) TextoSecundarios else ItemSelected,
            focusedBorderColor = if (enabled) BotonesNormales else ItemSelected,
            disabledTextColor = TextoSecundarios,
            disabledBorderColor = BlueGray50,
            disabledLabelColor = BlueGray50
        )
    )
}

/**
 * Diálogo que muestra una lista de mensajes secuencialmente.
 *
 * @param mensajes Lista de mensajes a mostrar en el diálogo.
 */
@Composable
fun LanzaAlert(
    mensajes: List<String>
) {
    var showDialog by remember { mutableStateOf(true) }
    var currentIndex by remember { mutableStateOf(0) }

    if (showDialog && currentIndex < mensajes.size) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = {
                Text(
                    mensajes[currentIndex],
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextoNormales
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentIndex < mensajes.size - 1) {
                            currentIndex++
                        } else {
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = BotonesNormales)
                ) {
                    Text(if (currentIndex < mensajes.size - 1) "Siguiente" else "Finalizar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Lee los derechos desde un archivo JSON en los assets.
 *
 * @param context Contexto de la aplicación.
 * @param fileName Nombre del archivo JSON a leer.
 * @return Lista de derechos leídos o un mensaje de error en caso de fallo.
 */
fun LeerDerechos(context: Context, fileName: String): List<String> {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        val json = String(buffer, Charsets.UTF_8)

        val tipo = object : TypeToken<Mensajes>() {}.type
        val mensajes: Mensajes = Gson().fromJson(json, tipo)

        mensajes.derechos
    } catch (e: Exception) {
        listOf("Error al cargar los mensajes.")
    }
}

/**
 * Obtiene los derechos desde un archivo JSON y los carga en un estado.
 *
 * @param context Contexto de la aplicación.
 * @return Lista de derechos cargados.
 */
@Composable
fun ObtenerDerechos(context: Context): List<String> {
    var mensajes by remember { mutableStateOf(listOf("Cargando mensajes...")) }

    LaunchedEffect(Unit) {
        mensajes = LeerDerechos(context, "derechos.json")
    }
    return mensajes
}

/**
 * Lee los elementos esenciales desde un archivo JSON en los assets.
 *
 * @param context Contexto de la aplicación.
 * @param fileName Nombre del archivo JSON a leer.
 * @return Objeto [ElementosEsenciales] con los datos leídos o vacío en caso de error.
 */
fun leerElementosEsenciales(context: Context, fileName: String): ElementosEsenciales {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        val json = String(buffer, Charsets.UTF_8)

        val tipo = object : TypeToken<ElementosEsenciales>() {}.type
        Gson().fromJson(json, tipo)
    } catch (e: Exception) {
        ElementosEsenciales(emptyList(), emptyList())
    }
}