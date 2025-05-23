package com.oscar.atestados.screens

import com.oscar.atestados.utils.QrScannerDialog
import android.content.Context
import android.nfc.NfcAdapter
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.datastore.preferences.preferencesDataStore
import com.oscar.atestados.R
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoTerciarios
import com.oscar.atestados.data.DniData
import com.oscar.atestados.utils.NfcReader
import com.oscar.atestados.utils.NfcDataParser
import com.oscar.atestados.viewModel.NfcViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val Context.dataStorePer by preferencesDataStore(name = "PERSONA_PREFERENCES_Nme")
private const val TAG = "PersonaScreen"
/**
 * Pantalla principal para la gestión de datos personales.
 * Permite la entrada manual de datos, lectura mediante NFC y escaneo de QR.
 */
@Composable
fun PersonaScreen(
    navigateToScreen: (String) -> Unit,
    personaViewModel: PersonaViewModel,
    nfcViewModel: NfcViewModel,
    onTagProcessed: () -> Unit = {},

) {
    val context = LocalContext.current
    val nfcTag by nfcViewModel.nfcTag.collectAsState() // Observar el tag desde el ViewModel
    var showCanDialog by remember { mutableStateOf(false) }
    var showWaitingForNfcDialog by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var qrResult by remember { mutableStateOf<DniData?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var dniData by remember { mutableStateOf<DniData?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    var isDataLoaded by remember { mutableStateOf(false) }
    var tempCanCode by remember { mutableStateOf<String?>(null) }
    var hasAttemptedRead by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        personaViewModel.loadData(context)
        isDataLoaded = true
        Log.d(TAG, "Cargando datos iniciales")
    }

    LaunchedEffect(nfcTag, tempCanCode, hasAttemptedRead) {
        Log.d(TAG, "LaunchedEffect ejecutado con nfcTag=$nfcTag, tempCanCode=$tempCanCode, hasAttemptedRead=$hasAttemptedRead")
        if (isDataLoaded && nfcTag != null && tempCanCode != null && !hasAttemptedRead) {
            Log.d(TAG, "Iniciando lectura NFC con CAN: $tempCanCode")
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    hasAttemptedRead = true
                    val nfcReader = NfcReader(context, nfcTag!!)
                    Log.d(TAG, "Llamando a readDni con tag: $nfcTag")
                    val rawData = nfcReader.readDni(tempCanCode!!)
                    Log.d(TAG, "Datos crudos obtenidos: $rawData")
                    val parser = NfcDataParser()
                    val readDniData = parser.parseRawData(rawData)
                    Log.d(TAG, "Datos parseados: $readDniData")
                    withContext(Dispatchers.Main) {
                        if (readDniData.error != null) {
                            errorMessage = readDniData.error!!
                            Log.e(TAG, "Error en lectura NFC: $errorMessage")
                            showErrorAlert = true
                        } else if (readDniData.numeroDocumento == null && readDniData.nombre == null && readDniData.apellidos == null) {
                            errorMessage = readDniData.error ?: "No se pudieron extraer datos válidos del DNI."
                            Log.e(TAG, "Datos vacíos en lectura NFC")
                            showErrorAlert = true
                        } else {
                            Log.i(TAG, "Datos del DNI leídos exitosamente: $readDniData")
                            dniData = readDniData
                            personaViewModel.updateFromDniData(readDniData)
                            showSuccessAlert = true
                        }
                        showWaitingForNfcDialog = false
                        tempCanCode = null
                        onTagProcessed()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error de IO al leer el tag NFC: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        errorMessage = if (e.message?.contains("CAN incorrecto") == true) {
                            "El código CAN introducido es incorrecto. Verifíquelo e intente de nuevo."
                        } else {
                            "Se perdió la conexión con el DNIe. Mantenga el DNI cerca del lector e intente de nuevo."
                        }
                        showErrorAlert = true
                        showWaitingForNfcDialog = false
                        tempCanCode = null
                        onTagProcessed()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inesperado al leer DNI: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        errorMessage = "Error inesperado al leer el DNI: ${e.message}"
                        showErrorAlert = true
                        showWaitingForNfcDialog = false
                        tempCanCode = null
                        onTagProcessed()
                    }
                }
            }
        }
    }

    val onNfcButtonClicked: () -> Unit = {
        if (nfcAdapter?.isEnabled == true) {
            showCanDialog = true
            hasAttemptedRead = false
            Log.d(TAG, "Botón NFC pulsado, mostrando diálogo CAN")
        } else {
            Toast.makeText(context, "Por favor, active el NFC", Toast.LENGTH_SHORT).show()
        }
    }

    if (showCanDialog) {
        CanDialog(
            onConfirm = { code ->
                showCanDialog = false
                tempCanCode = code
                showWaitingForNfcDialog = true
                Log.d(TAG, "CAN confirmado: $code, esperando tag NFC")
            },
            onDismiss = {
                showCanDialog = false
                hasAttemptedRead = false
            }
        )
    }

    if (showWaitingForNfcDialog) {
        AlertDialog(
            onDismissRequest = { /* No permitir cerrar manualmente */ },
            modifier = Modifier.padding(16.dp),
            title = { Text("Acerque el DNI") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Coloque el DNI cerca del lector NFC. Estará correctamente colocado cuando escuche un sonido, después espere a ser leído.")
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            },
            confirmButton = {},
            dismissButton = null
        )
    }

    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = { showSuccessAlert = false },
            title = { Text("Datos leídos con éxito") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    dniData?.let { data ->
                        // Definimos una constante para el tag del Logcat
                        val TAG = "DniDataDisplay"

                        // Mostrar y registrar cada campo
                        Text("Tipo de Documento: ${data.tipoDocumento ?: "No disponible"}").also {
                            Log.d(TAG, "Tipo de Documento: ${data.tipoDocumento ?: "No disponible"}")
                        }
                        Text("Número de Documento: ${data.numeroDocumento ?: "No disponible"}").also {
                            Log.d(TAG, "Número de Documento: ${data.numeroDocumento ?: "No disponible"}")
                        }
                        Text("Nombre: ${data.nombre ?: "No disponible"}").also {
                            Log.d(TAG, "Nombre: ${data.nombre ?: "No disponible"}")
                        }
                        Text("Apellidos: ${data.apellidos ?: "No disponible"}").also {
                            Log.d(TAG, "Apellidos: ${data.apellidos ?: "No disponible"}")
                        }
                        Text("Género: ${data.genero ?: "No disponible"}").also {
                            Log.d(TAG, "Género: ${data.genero ?: "No disponible"}")
                        }
                        Text("Nacionalidad: ${data.nacionalidad ?: "No disponible"}").also {
                            Log.d(TAG, "Nacionalidad: ${data.nacionalidad ?: "No disponible"}")
                        }
                        Text("Fecha de Nacimiento: ${data.fechaNacimiento ?: "No disponible"}").also {
                            Log.d(TAG, "Fecha de Nacimiento: ${data.fechaNacimiento ?: "No disponible"}")
                        }
                        Text("Lugar de Nacimiento: ${data.lugarNacimiento ?: "No disponible"}").also {
                            Log.d(TAG, "Lugar de Nacimiento: ${data.lugarNacimiento ?: "No disponible"}")
                        }
                        Text("Domicilio: ${data.domicilio ?: "No disponible"}").also {
                            Log.d(TAG, "Domicilio: ${data.domicilio ?: "No disponible"}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessAlert = false
                    hasAttemptedRead = false
                }) { Text("Aceptar") }
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
                    if (errorMessage.contains("Mantenga el DNI cerca") || errorMessage.contains("CAN incorrecto")) {
                        hasAttemptedRead = false
                    }
                }) { Text("Aceptar") }
            }
        )
    }
    if (showQrScannerDialog) {
        QrScannerDialog(
            onQrCodeScanned = { scannedDniData: DniData ->
                Log.d(TAG, "DniData del QR: $scannedDniData")
                qrResult = scannedDniData
                showQrScannerDialog = false
                Log.d(TAG, "qrResult establecido, debería mostrarse AlertDialog")
            },
            onDismiss = {
                showQrScannerDialog = false
                Log.d(TAG, "QrScannerDialog descartado")
            }
        )
    }

    qrResult?.let { result ->
        if (result.error != null) {
            AlertDialog(
                onDismissRequest = { qrResult = null },
                title = { Text("Error") },
                text = { Text(result.error!!) },
                confirmButton = {
                    Button(onClick = { qrResult = null }) {
                        Text("Aceptar")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { qrResult = null },
                title = { Text("QR Escaneado") },
                text = {
                    Column {
                        result.numeroDocumento?.let { Text("DNI: $it") }
                        result.nombre?.let { Text("Nombre: $it") }
                        result.apellidos?.let { Text("Apellidos: $it") }
                        result.fechaNacimiento?.let { Text("Fecha de Nacimiento: $it") }
                        result.domicilio?.let { Text("Domicilio: $it") }
                        result.nacionalidad?.let { Text("Nacionalidad: $it") }
                        result.genero?.let { Text("Género: $it") }
                        if (result.numeroDocumento == null && result.nombre == null) {
                            Text("No se encontraron datos válidos en el QR")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (result.numeroDocumento != null) {
                            personaViewModel.updateFromDniData(result)
                            Toast.makeText(context, "Datos del QR cargados en el formulario", Toast.LENGTH_SHORT).show()
                        }
                        qrResult = null
                    }) {
                        Text("Cargar datos")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { qrResult = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    PersonaScreenContent(
        navigateToScreen = navigateToScreen,
        personaViewModel = personaViewModel,
        onCameraButtonClicked = {
            Log.d(TAG, "Botón QR pulsado")
            showQrScannerDialog = true
        },
        onNfcButtonClicked = onNfcButtonClicked,
        onTextFieldChanged = {
            text -> personaViewModel.updateNombre(text)
        }
    )
}
@Composable
fun ReadingNfcDialog() {
    AlertDialog(
        onDismissRequest = { /* No permitir cierre manual */ },
        title = { Text("Leyendo DNI") },
        text = { Text("Por favor, mantenga el DNI cerca del lector NFC.") },
        confirmButton = { },
        dismissButton = { },
        modifier = Modifier
    )
}
/**
 * Diálogo para introducir el código CAN del DNIe.
 *
 * @param onConfirm Callback que se ejecuta cuando se confirma el código CAN.
 * @param onDismiss Callback que se ejecuta cuando se cancela el diálogo.
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
                Text("Introduzca el código CAN de 6 dígitos del DNI")
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
                        Toast.makeText(context, "El CAN debe tener 6 dígitos", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Aceptar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
/**
 * Contenido principal de la pantalla de persona.
 *
 * @param navigateToScreen Función para navegar a otras pantallas.
 * @param personaViewModel ViewModel que maneja los datos personales.
 * @param onCameraButtonClicked Callback para el botón de cámara/QR.
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
 * Barra de herramientas superior con botones de acción.
 *
 * @param onCameraButtonClicked Callback para el botón de cámara/QR.
 * @param onNFCClicked Callback para el botón de NFC.
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
                        contentDescription = "Botón para capturar información mediante NFC",
                        tint = BotonesNormales
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}
/**
 * Barra de herramientas inferior con botones de acción.
 *
 * @param personaViewModel ViewModel que maneja los datos personales.
 * @param navigateToScreen Función para navegar a otras pantallas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarPersona(
    personaViewModel: PersonaViewModel,
    navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val plainTooltipState = rememberTooltipState()
    val numeroDocumento by personaViewModel.numeroDocumento.observeAsState(initial = "")

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
                tooltip = { PlainTooltip { Text("Pulse aquí para guardar los datos introducidos") } }
            ) {
                Button(
                    onClick = {
                        if (numeroDocumento.isNullOrEmpty()) {
                            Toast.makeText(context, "Por favor, ingrese un número de documento válido", Toast.LENGTH_LONG).show()
                        } else {
                            personaViewModel.saveData(context)
                            navigateToScreen("MainScreen")
                        }
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
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text("GUARDAR", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                modifier = Modifier.width(175.dp),
                tooltip = { PlainTooltip { Text("Pulse aquí para limpiar todos los datos almacenados") } }
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
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Text("LIMPIAR", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }
    }
}
/**
 * Contenido del formulario de datos personales.
 *
 * @param modifier Modificador para personalizar el diseño.
 * @param onTextFieldChanged Callback para cambios en los campos de texto.
 * @param personaViewModel ViewModel que maneja los datos personales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaContent(
    modifier: Modifier = Modifier,
    onTextFieldChanged: (String) -> Unit,
    personaViewModel: PersonaViewModel
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
                DropDownDocumento(personaViewModel = personaViewModel, onTextFieldChanged = onTextFieldChanged)
            }

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
                value = nombre.toString(),
                onValueChange = { personaViewModel.updateNombre(it) },
                label = "Nombre",
                placeholder = "Introduzca el nombre completo"
            )

            CustomOutlinedTextField(
                value = apellidos.toString(),
                onValueChange = { personaViewModel.updateApellidos(it) },
                label = "Apellidos",
                placeholder = "Introduzca los apellidos completos"
            )

            CustomOutlinedTextField(
                value = nombrePadre.toString(),
                onValueChange = { personaViewModel.updateNombrePadre(it) },
                label = "Nombre del Padre",
                placeholder = "Introduzca el nombre del padre"
            )

            CustomOutlinedTextField(
                value = nombreMadre.toString(),
                onValueChange = { personaViewModel.updateNombreMadre(it) },
                label = "Nombre de la Madre",
                placeholder = "Introduzca el nombre de la madre"
            )

            CustomOutlinedTextField(
                value = fechaNacimiento.toString(),
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
                value = lugarNacimiento.toString(),
                onValueChange = { personaViewModel.updateLugarNacimiento(it) },
                label = "Lugar de nacimiento",
                placeholder = "Introduzca el lugar de nacimiento"
            )

            CustomOutlinedTextField(
                value = domicilio.toString(),
                onValueChange = { personaViewModel.updateDomicilio(it) },
                label = "Domicilio",
                placeholder = "Introduzca el domicilio completo"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CustomOutlinedTextField(
                    value = codigoPostal.toString(),
                    onValueChange = { personaViewModel.updateCodigoPostal(it) },
                    label = "Código Postal",
                    placeholder = "C.P.",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(150.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                CustomOutlinedTextField(
                    value = telefono.toString(),
                    onValueChange = { personaViewModel.updateTelefono(it) },
                    label = "Teléfono",
                    placeholder = "Teléfono",
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.weight(1f)
                )
            }

            CustomOutlinedTextField(
                value = correoElectronico.toString(),
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
 * Selector de fecha personalizado.
 *
 * @param onDateSelected Callback que se ejecuta cuando se selecciona una fecha.
 * @param onDismiss Callback que se ejecuta cuando se cancela el diálogo.
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
 * Campo desplegable para seleccionar el género.
 *
 * @param personaViewModel ViewModel que maneja los datos personales.
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
            value = selectedText ?: listSexo[0],
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
 * Campo desplegable para seleccionar la nacionalidad.
 *
 * @param personaViewModel ViewModel que maneja los datos personales.
 * @param paises Lista de países disponibles para selección.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownNacionalidad(
    personaViewModel: PersonaViewModel,
    paises: List<String>
) {
    var isExpandedNacionalidad by remember { mutableStateOf(false) }
    val selectedText by personaViewModel.nacionalidad.observeAsState(initial = paises.firstOrNull() ?: "")

    ExposedDropdownMenuBox(
        expanded = isExpandedNacionalidad,
        onExpandedChange = { isExpandedNacionalidad = !isExpandedNacionalidad }
    ) {
        OutlinedTextField(
            value = selectedText.toString(),
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
 * Campo desplegable para seleccionar el tipo de documento.
 *
 * @param personaViewModel ViewModel que maneja los datos personales.
 * @param onTextFieldChanged Callback para cambios en el campo de texto del documento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownDocumento(personaViewModel: PersonaViewModel, onTextFieldChanged: (String) -> Unit) {
    var isExpandedDocumento by remember { mutableStateOf(false) }
    val listDocumento = listOf("DNI", "NIE", "Pasaporte", "Per. Nacional Cond.", "Otros")
    val tipoDocumento by personaViewModel.tipoDocumento.observeAsState(initial = "DNI")
    val documento by personaViewModel.numeroDocumento.observeAsState(initial = "")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ExposedDropdownMenuBox(
            expanded = isExpandedDocumento,
            onExpandedChange = { isExpandedDocumento = !isExpandedDocumento },
            modifier = Modifier.width(150.dp)
        ) {
            OutlinedTextField(
                value = tipoDocumento.toString(),
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

        OutlinedTextField(
            value = documento.toString(),
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
                .weight(1f)
                .padding(start = 8.dp),
            singleLine = true
        )
    }
}
/**
 * Obtiene la lista de países desde la base de datos.
 *
 * @param context Contexto de la aplicación.
 * @return Lista de nombres de países.
 */
fun getPaises(context: Context): List<String> {
    val myDB = AccesoBaseDatos(context, "paises.db")
    return myDB.query("SELECT nombre FROM paises").map { it["nombre"] as String }
}