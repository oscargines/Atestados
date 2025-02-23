package com.oscar.atestados.screens


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.oscar.atestados.R
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoTerciarios
import com.oscar.atestados.viewModel.PersonaViewModel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

/**
 * Configuración de DataStore para almacenar preferencias de la aplicación.
 * Se utiliza para guardar y recuperar datos persistentes relacionados con la entidad "Persona".
 *
 * @property name Nombre del DataStore, utilizado para identificar el archivo de preferencias.
 */
val Context.dataStorePer by preferencesDataStore(name = "PERSONA_PREFERENCES_Nme")

/**
 * Clase de datos para almacenar la información extraída del DNI electrónico.
 *
 * @param numeroDocumento Número del documento de identidad
 * @param nombre Nombre del titular
 * @param apellidos Apellidos del titular
 * @param fechaNacimiento Fecha de nacimiento formateada
 * @param sexo Género del titular ("Masculino" o "Femenino")
 * @param nacionalidad Nacionalidad del titular (por defecto "España")
 */
data class DniData(
    val numeroDocumento: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val fechaNacimiento: String = "",
    val sexo: String = "",
    val nacionalidad: String = "España"
)
/**
 * Convierte un String hexadecimal a un array de bytes.
 *
 * @return ByteArray con los valores convertidos
 */
fun String.hexStringToByteArray(): ByteArray {
    val len = length
    val data = ByteArray(len / 2)
    for (i in 0 until len step 2) {
        data[i / 2] = ((Character.digit(this[i], 16) shl 4) +
                Character.digit(this[i + 1], 16)).toByte()
    }
    return data
}
/**
 * Pantalla principal de la entidad "Persona".
 * Muestra un formulario para ingresar datos personales y permite la captura de imágenes mediante la cámara.
 *
 * @param navigateToScreen Función de navegación para cambiar a otras pantallas.
 * @param personaViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 */
@Composable
fun PersonaScreen(
    navigateToScreen: (String) -> Unit,
    personaViewModel: PersonaViewModel,
    nfcTag: Tag? = null // Parámetro opcional para recibir el Tag NFC
) {
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }
    var showCanDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    var isReadingNfc by remember { mutableStateOf(false) }

    // Función para manejar el clic en el botón de la cámara
    val onCameraButtonClicked = {
        showCamera = true
    }
    // Función para manejar el botón del NFC
    val onNfcButtonClicked = {
        if (nfcAdapter?.isEnabled == true) {
            showCanDialog = true
        } else {
            Toast.makeText(context, "Por favor, active el NFC", Toast.LENGTH_SHORT).show()
        }
    }

    // Cargar datos al iniciar la pantalla
    LaunchedEffect(Unit) {
        personaViewModel.loadData(context)
    }
    // Manejar Tag NFC cuando esté disponible
    LaunchedEffect(nfcTag) {
        if (nfcTag != null && isReadingNfc) {
            val canCode = personaViewModel.codigoCan.value ?: return@LaunchedEffect
            coroutineScope.launch {
                try {
                    val dniData = readDniData(nfcTag, canCode, context)
                    updatePersonaViewModel(personaViewModel, dniData)
                    Toast.makeText(context, "Datos del DNI leídos correctamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al leer el DNI: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isReadingNfc = false
                }
            }
        }
    }

    // Mostrar la cámara si showCamera es true
    if (showCamera) {
        CameraPreview(
            onImageCaptured = { bitmap ->
                showCamera = false
                processImage(bitmap,
                    { text -> personaViewModel.updateNombre(text) },
                    { error ->
                        Toast.makeText(
                            context,
                            "Se ha producido un error al procesar la imagen",
                            LENGTH_SHORT
                        ).show()
                    })
            },
            onDismiss = { showCamera = false }
        )
    }
    if (showCanDialog) {
        CanDialog(
            onConfirm = { canCode ->
                personaViewModel.updateCodigoCan(canCode)
                showCanDialog = false
                isReadingNfc = true
                Toast.makeText(context, "Acerque el DNI al lector NFC", Toast.LENGTH_LONG).show()
                // La lectura real ocurrirá cuando el sistema detecte el Tag
            },
            onDismiss = { showCanDialog = false }
        )
    }
    // Contenido principal de la pantalla.
    PersonaScreenContent(
        navigateToScreen = navigateToScreen,
        personaViewModel = personaViewModel,
        onCameraButtonClicked = onCameraButtonClicked,
        onNfcButtonClicked = onNfcButtonClicked,
        onTextFieldChanged = { text -> personaViewModel.updateNombre(text) }
    )

}
/**
 * Lee los datos del DNI electrónico usando el protocolo NFC.
 *
 * @param tag Tag NFC detectado
 * @param canCode Código CAN del DNI
 * @param context Contexto de la aplicación
 * @return DniData con los datos extraídos del DNI
 * @throws IOException si falla la comunicación NFC o la autenticación
 */
private fun readDniData(tag: Tag, canCode: String, context: Context): DniData {
    val isoDep = IsoDep.get(tag)
    try {
        isoDep.connect()
        isoDep.timeout = 5000

        val selectAID = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0x4D, 0x61, 0x73, 0x74, 0x65, 0x72, 0x2E
        )
        val canCommand = "00A4020C0F$canCode".hexStringToByteArray()

        isoDep.transceive(selectAID)
        val response = isoDep.transceive(canCommand)

        if (response[response.size - 2] != 0x90.toByte() || response[response.size - 1] != 0x00.toByte()) {
            throw IOException("Autenticación CAN fallida")
        }

        val readDG1 = byteArrayOf(0x00, 0xB0.toByte(), 0x81.toByte(), 0x00, 0xFF.toByte())
        val dg1Data = isoDep.transceive(readDG1)

        return parseDniData(dg1Data)
    } finally {
        isoDep.close()
    }
}

/**
 * Parsea los datos brutos del DNI en un objeto DniData.
 *
 * @param data Datos brutos obtenidos del NFC
 * @return DniData con la información extraída
 * @throws IOException si el formato de los datos es inválido
 */
private fun parseDniData(data: ByteArray): DniData {
    val mrzString = String(data, StandardCharsets.UTF_8)
    val lines = mrzString.split("\n").filter { it.isNotEmpty() }

    if (lines.size >= 2) {
        val documentNumber = lines[0].substring(0, 9)
        val birthDate = lines[1].substring(13, 19) // YYMMDD
        val formattedDate = SimpleDateFormat("yyMMdd").parse(birthDate)?.let { date ->
            SimpleDateFormat("d 'de' MMMM 'de' yyyy'", Locale("es", "ES")).format(date)
        } ?: ""

        val nameFields = lines[1].substring(30).split("<<")
        val apellidos = nameFields[0].replace("<", " ").trim()
        val nombre = nameFields.getOrNull(1)?.replace("<", " ")?.trim() ?: ""
        val sexo = if (lines[1][20] == 'M') "Masculino" else "Femenino"

        return DniData(
            numeroDocumento = documentNumber,
            nombre = nombre,
            apellidos = apellidos,
            fechaNacimiento = formattedDate,
            sexo = sexo,
            nacionalidad = "España"
        )
    }
    throw IOException("Formato de datos inválido")
}
/**
 * Actualiza el ViewModel con los datos extraídos del DNI.
 *
 * @param viewModel ViewModel a actualizar
 * @param data Datos del DNI a aplicar
 */
private fun updatePersonaViewModel(viewModel: PersonaViewModel, data: DniData) {
    viewModel.updateTipoDocumento("DNI")
    viewModel.updateNumeroDocumento(data.numeroDocumento)
    viewModel.updateNombre(data.nombre)
    viewModel.updateApellidos(data.apellidos)
    viewModel.updateFechaNacimiento(data.fechaNacimiento)
    viewModel.updateGenero(data.sexo)
    viewModel.updateNacionalidad(data.nacionalidad)
}

/**
 * Contenido principal de la pantalla "Persona".
 * Organiza la interfaz de usuario en un [Scaffold] con una barra superior, un contenido central y una barra inferior.
 *
 * @param navigateToScreen Función de navegación para cambiar a otras pantallas.
 * @param personaViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 * @param onCameraButtonClicked Función para manejar el clic en el botón de la cámara.
 */
@Composable
fun PersonaScreenContent(
    navigateToScreen: (String) -> Unit,
    personaViewModel: PersonaViewModel,
    onCameraButtonClicked: () -> Unit,
    onNfcButtonClicked: () -> Unit,
    onTextFieldChanged: (String) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ToolbarPersona(
            onCameraButtonClicked = onCameraButtonClicked,
            onNFCClicked = onNfcButtonClicked
        ) },
        bottomBar = { BottomAppBarPersona(personaViewModel, navigateToScreen) }
    ) { paddingValues ->
        PersonaContent(
            modifier = Modifier.padding(paddingValues),
            onTextFieldChanged = { text -> personaViewModel.updateNombre(text) },
            personaViewModel = personaViewModel
        )

    }

}

/**
 * Barra inferior de la pantalla "Persona".
 * Contiene botones para guardar y limpiar los datos ingresados.
 *
 * @param personaViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarPersona(
    personaViewModel: PersonaViewModel,
    navigateToScreen: (String) -> Unit
) {

    val context = LocalContext.current
    val plainTooltipState = rememberTooltipState()

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
                        personaViewModel.saveData(context)
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
                        personaViewModel.clearData(context)
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
 * Barra superior de la pantalla "Persona".
 * Muestra el título de la pantalla y un botón para acceder a la cámara.
 *
 * @param onCameraButtonClicked Función para manejar el clic en el botón de la cámara.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarPersona(
    onCameraButtonClicked: () -> Unit,
    onNFCClicked: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Persona",
                fontSize = 30.sp,
                color = TextoNormales,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            Row(
                modifier = Modifier.padding(start = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onCameraButtonClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.camera_50),
                        contentDescription = "Botón para capturar información mediante el uso de la cámara",
                        tint = BotonesNormales
                    )
                }
                IconButton(onClick = onNFCClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.nfc_ico),
                        contentDescription = "Botón para capturar información mediante el uso del sistema NFC",
                        tint = BotonesNormales
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * Contenido central de la pantalla "Persona".
 * Muestra un formulario con campos de texto para ingresar datos personales.
 *
 * @param modifier Modificador para personalizar el diseño.
 * @param onTextFieldChanged Función para manejar cambios en los campos de texto.
 * @param personaViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaContent(
    modifier: Modifier = Modifier,
    onTextFieldChanged: (String) -> Unit,
    personaViewModel: PersonaViewModel,

    ) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val paises by remember { mutableStateOf(getPaises(context)) }

    //Estados para los campos de texto
    val nombre by personaViewModel.nombre.observeAsState(initial = "")
    val apellidos by personaViewModel.apellidos.observeAsState(initial = "")
    val nombrePadre by personaViewModel.nombrePadre.observeAsState(initial = "")
    val nombreMadre by personaViewModel.nombreMadre.observeAsState(initial = "")
    val fechaNacimiento by personaViewModel.fechaNacimiento.observeAsState(initial = "")
    val lugarNacimiento by personaViewModel.lugarNacimiento.observeAsState(initial = "")
    val domicilio by personaViewModel.domicilio.observeAsState(initial = "")
    val codigoPostal by personaViewModel.codigoPostal.observeAsState(initial = "")
    val telefono by personaViewModel.telefono.observeAsState(initial = "")
    val correoElectronico by personaViewModel.email.observeAsState(initial = "")


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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DropDownSexo(personaViewModel)
                Spacer(modifier = Modifier.width(8.dp))
                DropDownNacionalidad(personaViewModel, paises)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DropDownDocumento(
                    personaViewModel = personaViewModel,
                    onTextFieldChanged = onTextFieldChanged
                )
            }
            /**
             * Campo de texto personalizado con un diseño de borde y opciones adicionales.
             *
             * @param value Valor actual del campo de texto.
             * @param onValueChange Función para manejar cambios en el valor del campo.
             * @param label Etiqueta del campo.
             * @param placeholder Texto de marcador de posición.
             * @param keyboardType Tipo de teclado para la entrada de texto.
             * @param modifier Modificador para personalizar el diseño.
             * @param leadingIcon Icono opcional para mostrar a la izquierda del campo.
             */
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
            CustomOutlinedTextField(
                value = nombre,
                onValueChange = { personaViewModel.updateNombre(it) },
                label = "Nombre",
                placeholder = "Introduzca el nombre completo"
            )

            CustomOutlinedTextField(
                value = apellidos,
                onValueChange = { personaViewModel.updateApellidos(it) },
                label = "Apellidos",
                placeholder = "Introduzca los apellidos completos"
            )

            CustomOutlinedTextField(
                value = nombrePadre,
                onValueChange = { personaViewModel.updateNombrePadre(it) },
                label = "Nombre del Padre",
                placeholder = "Introduzca el nombre del padre"
            )

            CustomOutlinedTextField(
                value = nombreMadre,
                onValueChange = { personaViewModel.updateNombreMadre(it) },
                label = "Nombre de la Madre",
                placeholder = "Introduzca el nombre de la madre"
            )

            CustomOutlinedTextField(
                value = fechaNacimiento,
                onValueChange = { personaViewModel.updateFechaNacimiento(it) },
                label = "Fecha de nacimiento",
                placeholder = "Seleccione la fecha",
                keyboardType = KeyboardType.Number,
                leadingIcon = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.size(24.dp)
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
                value = lugarNacimiento,
                onValueChange = { personaViewModel.updateLugarNacimiento(it) },
                label = "Lugar de nacimiento",
                placeholder = "Introduzca el lugar de nacimiento"
            )

            CustomOutlinedTextField(
                value = domicilio,
                onValueChange = { personaViewModel.updateDomicilio(it) },
                label = "Domicilio",
                placeholder = "Introduzca el domicilio completo"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CustomOutlinedTextField(
                    value = codigoPostal,
                    onValueChange = { personaViewModel.updateCodigoPostal(it) },
                    label = "Código Postal",
                    placeholder = "C.P.",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(150.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                CustomOutlinedTextField(
                    value = telefono,
                    onValueChange = { personaViewModel.updateTelefono(it) },
                    label = "Teléfono",
                    placeholder = "Teléfono",
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f)
                )
            }

            CustomOutlinedTextField(
                value = correoElectronico,
                onValueChange = { personaViewModel.updateEmail(it) },
                label = "Correo electrónico",
                placeholder = "Introduzca el correo electrónico, si lo tiene",
                keyboardType = KeyboardType.Email
            )
        }
    }

    if (showDatePicker) {
        getDateDialog(
            onDateSelected = { fechaSeleccionada ->
                personaViewModel.updateFechaNacimiento(fechaSeleccionada)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Diálogo para seleccionar una fecha.
 *
 * @param onDateSelected Función para manejar la fecha seleccionada.
 * @param onDismiss Función para cerrar el diálogo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getDateDialog(
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
                        .ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
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

/**
 * Selector desplegable para elegir el género.
 *
 * @param personaViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownSexo(personaViewModel: PersonaViewModel) {
    var isExpandedSexo by remember { mutableStateOf(false) }
    val listSexo = listOf("Masculino", "Femenino")
    val selectedText by personaViewModel.genero.observeAsState(initial = listSexo[0])

    ExposedDropdownMenuBox(
        expanded = isExpandedSexo,
        onExpandedChange = { isExpandedSexo = !isExpandedSexo },
        modifier = Modifier.width(150.dp)
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedSexo) },
            modifier = Modifier.menuAnchor()

        )
        ExposedDropdownMenu(
            expanded = isExpandedSexo,
            onDismissRequest = { isExpandedSexo = false }
        ) {
            listSexo.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        personaViewModel.updateGenero(item)
                        isExpandedSexo = false
                    }
                )
            }
        }
    }
}

/**
 * Selector desplegable para elegir la nacionalidad.
 *
 * @param personaViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownNacionalidad(
    personaViewModel: PersonaViewModel,
    paises: List<String>
) {
    var isExpandedNacionalidad by remember { mutableStateOf(false) }
    val selectedText by personaViewModel.nacionalidad.observeAsState(
        initial = paises.firstOrNull() ?: ""
    )

    ExposedDropdownMenuBox(
        expanded = isExpandedNacionalidad,
        onExpandedChange = { isExpandedNacionalidad = !isExpandedNacionalidad }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedNacionalidad) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = isExpandedNacionalidad,
            onDismissRequest = { isExpandedNacionalidad = false }
        ) {
            paises.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        personaViewModel.updateNacionalidad(item)
                        isExpandedNacionalidad = false
                    }
                )
            }
        }
    }
}

/**
 * Selector desplegable para elegir el tipo de documento de identidad.
 *
 * @param personaViewModel ViewModel que gestiona el estado y la lógica de la pantalla.
 * @param onTextFieldChanged Función para manejar cambios en el campo de texto del documento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownDocumento(personaViewModel: PersonaViewModel, onTextFieldChanged: (String) -> Unit) {
    var isExpandedDocumento by remember { mutableStateOf(false) }
    val listDocumento = listOf("DNI", "NIE", "Pasaporte", "Per. Nacional Cond.", "Otros")
    val tipoDocumento by personaViewModel.tipoDocumento.observeAsState(initial = "DNI")
    val documento by personaViewModel.numeroDocumento.observeAsState(initial = "")
    var selectedText by remember { mutableStateOf(listDocumento[0]) }


    ExposedDropdownMenuBox(
        expanded = isExpandedDocumento,
        onExpandedChange = { isExpandedDocumento = !isExpandedDocumento },
        modifier = Modifier
            .width(150.dp)
            .padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = tipoDocumento,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedDocumento) },
            modifier = Modifier.menuAnchor()

        )
        ExposedDropdownMenu(
            expanded = isExpandedDocumento,
            onDismissRequest = { isExpandedDocumento = false }
        ) {
            listDocumento.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        personaViewModel.updateTipoDocumento(item)
                        isExpandedDocumento = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }

        }

    }
    Spacer(modifier = Modifier.width(8.dp))

    OutlinedTextField(
        value = documento,
        onValueChange = { personaViewModel.updateNumeroDocumento(it) },
        label = { Text("Documento de identidad", color = TextoTerciarios) },
        placeholder = {
            Text(
                "Introduzca el documento de identidad completo",
                color = TextoTerciarios,
                textDecoration = TextDecoration.Underline
            )
        },
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(end = 5.dp),
        singleLine = true,
    )
}

fun getPaises(context: Context): List<String> {
    val myDB = AccesoBaseDatos(context, "paises.db", 1)

    return myDB.query("SELECT nombre FROM paises").map { it["nombre"] as String }
}

@Composable
fun DniScannerScreen(onScanButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onScanButtonClicked) {
            Text("Escanear DNI")
        }
    }
}

@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Crear ImageCapture fuera de AndroidView para garantizar su inicialización
    val imageCapture = remember { ImageCapture.Builder().build() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraExecutor = ContextCompat.getMainExecutor(ctx)

                // Crear el objeto Preview
                val preview = Preview.Builder().build()

                // Vincular el SurfaceProvider del PreviewView al Preview
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                    }
                }, cameraExecutor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botón para capturar la imagen
        Button(
            onClick = {
                val file = File(context.externalCacheDir, "dni_photo.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            onImageCaptured(bitmap)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraCapture", "Error capturing image", exc)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("Capturar Imagen")
        }

        // Botón para cerrar la cámara
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Cerrar cámara",
                tint = Color.White
            )
        }
    }
}

fun processImage(bitmap: Bitmap, onTextRecognized: (String) -> Unit, onError: (Exception) -> Unit) {
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isNotBlank()) {
                    onTextRecognized(visionText.text)
                } else {
                    onError(Exception("No se pudo detectar texto en la imagen"))
                }
            }
            .addOnFailureListener { e ->
                Log.e("Reconocimiento de texto", "Error: ", e)
                onError(e)
            }
    } catch (e: Exception) {
        Log.e("Procesamiento de imagen", "Error: ", e)
        onError(e)
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var canCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Código CAN del DNI") },
        text = {
            Column {
                Text("Por favor, introduzca el código CAN de 6 dígitos del DNI")
                OutlinedTextField(
                    value = canCode,
                    onValueChange = {
                        if (it.length <= 6) canCode = it.filter { char -> char.isDigit() }
                    },
                    label = { Text("Código CAN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canCode.length == 6) {
                        onConfirm(canCode)
                    } else {
                        Toast.makeText(context, "El código CAN debe tener 6 dígitos", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Aceptar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}