package com.oscar.atestados.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.oscar.atestados.utils.NfcReader
import com.oscar.atestados.utils.DniData
import com.oscar.atestados.utils.QrScanner
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoTerciarios
import com.oscar.atestados.viewModel.PersonaViewModel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** Delegate para el DataStore de preferencias de persona. */
val Context.dataStorePer by preferencesDataStore(name = "PERSONA_PREFERENCES_Nme")
private const val TAG = "PersonaScreen"

/**
 * Pantalla para gestionar los datos de una persona en la aplicación.
 *
 * Esta pantalla permite introducir datos manualmente, capturarlos mediante NFC o cámara,
 * y guardarlos o limpiarlos. Incluye diálogos para manejo de errores y confirmaciones.
 *
 * @param navigateToScreen Función lambda para navegar a otra pantalla, recibe una [String] con el nombre de la pantalla destino.
 * @param personaViewModel ViewModel que gestiona los datos de la persona.
 * @param nfcTag Etiqueta NFC detectada, si existe, para leer datos del DNI.
 * @param onTagProcessed Callback que se ejecuta al finalizar el procesamiento del tag NFC.
 * @param onCameraButtonClicked Callback que se ejecuta al hacer clic en el botón de la cámara.
 */
@Composable
fun PersonaScreen(
    navigateToScreen: (String) -> Unit,
    personaViewModel: PersonaViewModel,
    nfcTag: Tag? = null,
    onTagProcessed: () -> Unit = {},
    onCameraButtonClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    var showQrScanner by remember { mutableStateOf(false) }
    var showCanDialog by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    var showReadingNfcDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var dniData by remember { mutableStateOf<DniData?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    val canCode by personaViewModel.codigoCan.observeAsState()
    var isDataLoaded by remember { mutableStateOf(false) }
    var hasAttemptedRead by remember(nfcTag) { mutableStateOf(false) } // Reinicia por nuevo tag

    LaunchedEffect(Unit) {
        personaViewModel.loadData(context)
        withContext(Dispatchers.IO) {
            context.dataStorePer.data.firstOrNull()
        }
        isDataLoaded = true
        Log.d(TAG, "Cargando datos iniciales en PersonaScreen - canCode: ${personaViewModel.codigoCan.value}")
    }

    LaunchedEffect(nfcTag, isDataLoaded) {
        if (isDataLoaded && nfcTag != null && canCode.isNullOrEmpty()) {
            showCanDialog = true
            Log.d(TAG, "Tag detectado sin CAN, mostrando diálogo")
        }
    }

    LaunchedEffect(canCode, nfcTag, isDataLoaded) {
        Log.d(TAG, "LaunchedEffect ejecutado - nfcTag: $nfcTag, canCode: $canCode")
        if (isDataLoaded && nfcTag != null && !canCode.isNullOrEmpty() && !hasAttemptedRead) {
            Log.d(TAG, "Procesando lectura NFC con tag: $nfcTag")
            showReadingNfcDialog = true
            hasAttemptedRead = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val nfcReader = NfcReader(context, nfcTag)
                    val readDniData = nfcReader.readDni(canCode!!)
                    withContext(Dispatchers.Main) {
                        Log.i(TAG, "Datos del DNI leídos: $readDniData")
                        dniData = readDniData
                        personaViewModel.updateFromDniData(readDniData)
                        showSuccessAlert = true
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error al leer DNI: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        errorMessage = when {
                            e.message?.contains("CAN incorrecto") == true -> "CAN incorrecto. Verifique el número de 6 dígitos del DNI."
                            e.message?.contains("Only one TagTechnology") == true -> "Error de conexión NFC. Intente de nuevo."
                            e.message?.contains("Se ha perdido la conexión") == true -> "Se perdió la conexión con el DNIe. Mantenga el DNI cerca del lector e intente de nuevo."
                            else -> "Error al leer el DNI: ${e.message}"
                        }
                        showErrorAlert = true
                    }
                } catch (e: NoClassDefFoundError) {
                    Log.e(TAG, "Falta una clase crítica: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        errorMessage = "Error crítico: falta una dependencia necesaria. Contacte al soporte."
                        showErrorAlert = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inesperado al leer DNI: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        errorMessage = "Error inesperado: ${e.message}"
                        showErrorAlert = true
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        showReadingNfcDialog = false
                        onTagProcessed()
                    }
                }
            }
        } else if (nfcTag == null && !canCode.isNullOrEmpty()) {
            Log.d(TAG, "Esperando tag NFC - CAN ya ingresado: $canCode")
        } else {
            Log.d(TAG, "Esperando CAN y/o tag NFC")
        }
    }

    LaunchedEffect(canCode) {
        if (!canCode.isNullOrEmpty() && nfcTag == null) {
            delay(30000)
            if (nfcTag == null) {
                errorMessage = "No se detectó ningún DNI. Asegúrese de acercar el DNI al lector NFC."
                showErrorAlert = true
            }
        }
    }

    val onNfcButtonClicked: () -> Unit = {
        if (nfcAdapter?.isEnabled == true) {
            showCanDialog = true
            Log.d(TAG, "Botón NFC pulsado, mostrando diálogo CAN")
        } else {
            Toast.makeText(context, "Por favor, active el NFC", Toast.LENGTH_SHORT).show()
        }
    }
    val onCameraButtonClickedNew: () -> Unit = {
        showQrScanner = true // Abrimos el escáner de QR
        Log.d(TAG, "Botón de cámara pulsado, abriendo escáner QR")
    }

    if (showReadingNfcDialog) {
        ReadingNfcDialog()
    }
    if (showQrScanner) {
        QrScanner(
            onQrCodeScanned = { qrContent ->
                coroutineScope.launch {
                    showQrScanner = false
                    try {
                        // Procesar contenido QR
                        val qrData = qrContent.split(",").associate {
                            val (key, value) = it.split(":")
                            key.trim() to value.trim()
                        }
                        personaViewModel.updateNombre(qrData["Nombre"] ?: "")
                        personaViewModel.updateApellidos(qrData["Apellidos"] ?: "")
                        showSuccessAlert = true
                    } catch (e: Exception) {
                        errorMessage = "Error al procesar QR: ${e.message}"
                        showErrorAlert = true
                    }
                }
            },
            onDismiss = {
                showQrScanner = false
                Log.d(TAG, "Escáner QR cerrado")
            }
        )
    }
    if (showCanDialog) {
        CanDialog(
            onConfirm = { code ->
                personaViewModel.saveCodigoCan(context, code)
                showCanDialog = false
                Log.d(TAG, "Código CAN ingresado y guardado: $code")
                Toast.makeText(context, "Acerque el DNI al lector NFC", Toast.LENGTH_LONG).show()
            },
            onDismiss = {
                showCanDialog = false
                Log.d(TAG, "Diálogo CAN cancelado")
            }
        )
    }

    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = { showSuccessAlert = false },
            title = { Text("Éxito") },
            text = { Text("Los datos del DNI se han leído y cargado correctamente") },
            confirmButton = {
                Button(onClick = { showSuccessAlert = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { showErrorAlert = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = {
                    showErrorAlert = false
                    if (errorMessage.contains("Mantenga el DNI cerca")) {
                        hasAttemptedRead = false // Permitir reintento tras desconexión
                    }
                }) {
                    Text("Aceptar")
                }
            }
        )
    }

    PersonaScreenContent(
        navigateToScreen = navigateToScreen,
        personaViewModel = personaViewModel,
        onCameraButtonClicked = onCameraButtonClickedNew,
        onNfcButtonClicked = onNfcButtonClicked,
        onTextFieldChanged = { text -> personaViewModel.updateNombre(text) }
    )
}

/**
 * Diálogo que se muestra mientras se lee el DNI mediante NFC.
 */
@Composable
fun ReadingNfcDialog() {
    AlertDialog(
        onDismissRequest = { /* No permitir cierre manual */ },
        title = { Text("Leyendo DNI") },
        text = { Text("Por favor, mantenga el DNI cerca del lector NFC hasta que los datos se lean correctamente.") },
        confirmButton = { },
        dismissButton = { },
        modifier = Modifier
    )
}

/**
 * Diálogo que muestra los datos leídos del DNI.
 *
 * @param dniData Datos del DNI a mostrar.
 * @param onConfirm Callback que se ejecuta al confirmar los datos.
 * @param onDismiss Callback que se ejecuta al cerrar el diálogo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DniDataDialog(
    dniData: DniData,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datos del DNI") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Número de documento: ${dniData.numeroDocumento}")
                Text("Nombre: ${dniData.nombre}")
                Text("Apellidos: ${dniData.apellidos}")
                Text("Fecha de nacimiento: ${dniData.fechaNacimiento}")
                Text("Sexo: ${dniData.sexo}")
                Text("Nacionalidad: ${dniData.nacionalidad}")
                Text("Nombre del padre: ${dniData.nombrePadre}")
                Text("Nombre de la madre: ${dniData.nombreMadre}")
                Text("Lugar de nacimiento: ${dniData.lugarNacimiento}")
                Text("Domicilio: ${dniData.domicilio}")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Aceptar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/**
 * Actualiza el ViewModel con los datos leídos del DNI.
 *
 * @param viewModel ViewModel de la persona a actualizar.
 * @param data Datos del DNI a aplicar.
 */
private fun updatePersonaViewModel(viewModel: PersonaViewModel, data: DniData) {
    viewModel.updateFromDniData(data)
}

/**
 * Contenido principal de la pantalla PersonaScreen.
 *
 * Gestiona la interfaz de usuario con barra superior, inferior y campos de entrada.
 *
 * @param navigateToScreen Función lambda para navegar a otra pantalla.
 * @param personaViewModel ViewModel que contiene los datos de la persona.
 * @param onCameraButtonClicked Callback para el botón de la cámara.
 * @param onNfcButtonClicked Callback para el botón de NFC.
 * @param onTextFieldChanged Callback para cambios en los campos de texto.
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
        topBar = {
            ToolbarPersona(
                onCameraButtonClicked = onCameraButtonClicked,
                onNFCClicked = onNfcButtonClicked
            )
        },
        bottomBar = { BottomAppBarPersona(personaViewModel, navigateToScreen) }
    ) { paddingValues ->
        PersonaContent(
            modifier = Modifier.padding(paddingValues),
            onTextFieldChanged = onTextFieldChanged,
            personaViewModel = personaViewModel
        )
    }
}

/**
 * Barra inferior de la pantalla PersonaScreen.
 *
 * Contiene botones para guardar o limpiar los datos ingresados.
 *
 * @param personaViewModel ViewModel que gestiona los datos de la persona.
 * @param navigateToScreen Función lambda para navegar a otra pantalla.
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
        modifier = Modifier
            .wrapContentHeight()
            .padding(bottom = 30.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                modifier = Modifier.width(175.dp),
                tooltip = {
                    PlainTooltip {
                        Text("Pulse aquí para limpiar todos los datos almacenados en la aplicación")
                    }
                }
            ) {
                Button(
                    onClick = { personaViewModel.clearData(context) },
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
 * Barra superior de la pantalla PersonaScreen.
 *
 * Incluye el título y botones para acceder a la cámara y al lector NFC.
 *
 * @param onCameraButtonClicked Callback que se ejecuta al hacer clic en el botón de la cámara.
 * @param onNFCClicked Callback que se ejecuta al hacer clic en el botón de NFC.
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
                        painter = painterResource(id = R.drawable.qr_scanner_50),
                        contentDescription = "Botón para escanear un código QR",
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
 * Contenido principal de la pantalla PersonaScreen.
 *
 * Muestra los campos de entrada para los datos de la persona.
 *
 * @param modifier Modificador para personalizar el diseño del contenido.
 * @param onTextFieldChanged Callback que se ejecuta al cambiar el texto de los campos.
 * @param personaViewModel ViewModel que contiene los datos de la persona.
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
             * Campo de texto personalizado con borde.
             *
             * @param value Valor actual del campo.
             * @param onValueChange Callback para actualizar el valor del campo.
             * @param label Etiqueta del campo.
             * @param placeholder Texto de marcador de posición.
             * @param keyboardType Tipo de teclado a mostrar.
             * @param modifier Modificador para personalizar el diseño.
             * @param leadingIcon Icono opcional al inicio del campo.
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
 * @param onDateSelected Callback que recibe la fecha seleccionada como [String].
 * @param onDismiss Callback que se ejecuta al cerrar el diálogo.
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
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = state)
    }
}

/**
 * Menú desplegable para seleccionar el sexo.
 *
 * @param personaViewModel ViewModel que gestiona el género seleccionado.
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
 * Menú desplegable para seleccionar la nacionalidad.
 *
 * @param personaViewModel ViewModel que gestiona la nacionalidad seleccionada.
 * @param paises Lista de países disponibles para seleccionar.
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
 * Menú desplegable para seleccionar el tipo de documento y campo para el número.
 *
 * @param personaViewModel ViewModel que gestiona el tipo y número de documento.
 * @param onTextFieldChanged Callback para cambios en el campo de texto del número de documento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownDocumento(personaViewModel: PersonaViewModel, onTextFieldChanged: (String) -> Unit) {
    var isExpandedDocumento by remember { mutableStateOf(false) }
    val listDocumento = listOf("DNI", "NIE", "Pasaporte", "Per. Nacional Cond.", "Otros")
    val tipoDocumento by personaViewModel.tipoDocumento.observeAsState(initial = "DNI")
    val documento by personaViewModel.numeroDocumento.observeAsState(initial = "")

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
        singleLine = true
    )
}

/**
 * Obtiene la lista de países desde la base de datos.
 *
 * @param context Contexto de la aplicación.
 * @return Lista de nombres de países.
 */
fun getPaises(context: Context): List<String> {
    val myDB = AccesoBaseDatos(context, "paises.db", 1)
    return myDB.query("SELECT nombre FROM paises").map { it["nombre"] as String }
}

/**
 * Vista previa de la cámara para capturar imágenes.
 *
 * @param onImageCaptured Callback que recibe el [Bitmap] capturado.
 * @param onDismiss Callback que se ejecuta al cerrar la vista previa.
 */
@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
                val preview = Preview.Builder().build()
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

/**
 * Procesa una imagen para extraer texto mediante reconocimiento OCR.
 *
 * @param bitmap Imagen a procesar.
 * @param onTextRecognized Callback que recibe el texto reconocido.
 * @param onError Callback que se ejecuta si ocurre un error.
 */
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

/**
 * Diálogo para ingresar el código CAN del DNI.
 *
 * @param onConfirm Callback que recibe el código CAN ingresado.
 * @param onDismiss Callback que se ejecuta al cerrar el diálogo.
 */
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
                        Toast.makeText(
                            context,
                            "El código CAN debe tener 6 dígitos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) { Text("Aceptar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}