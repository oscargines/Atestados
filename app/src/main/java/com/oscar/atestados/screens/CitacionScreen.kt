package com.oscar.atestados.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.oscar.atestados.R
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.data.CitacionDataProvider
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.utils.PdfUtils
import com.oscar.atestados.utils.HtmlParser
import com.oscar.atestados.utils.PDFA4Printer
import com.oscar.atestados.utils.PDFLabelPrinterZebra
import com.oscar.atestados.utils.PDFToBitmapPrinter
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel
import com.oscar.atestados.viewModel.CitacionViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.ui.composables.MissingFieldsDialog
import com.oscar.atestados.ui.composables.FullScreenProgressIndicator
import com.oscar.atestados.ui.composables.BitmapPreviewDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private const val TAG = "CitacionScreen"

@Composable
fun CitacionScreen(
    navigateToScreen: (String) -> Unit,
    citacionViewModel: CitacionViewModel,
    personaViewModel: PersonaViewModel,
    guardiasViewModel: GuardiasViewModel,
    alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    impresoraViewModel: ImpresoraViewModel
) {
    val context = LocalContext.current
    val printStatus by citacionViewModel.printStatus.collectAsState()
    val showPreviewDialog by citacionViewModel.showPreviewDialog.collectAsState()
    val previewBitmap by citacionViewModel.previewBitmap.collectAsState()
    val missingFields by citacionViewModel.missingFields.collectAsState()
    var isDataLoaded by remember { mutableStateOf(false) }
    val htmlParser = remember { HtmlParser(context) }
    val pdfToBitmapPrinter = remember { PDFToBitmapPrinter(context) }
    val db = remember { AccesoBaseDatos(context, "juzgados.db") }
    val juzgado by citacionViewModel.juzgado.observeAsState("")
    val juzgadoInfo by remember(juzgado) { mutableStateOf(getJuzgadoInfo(db, juzgado)) }
    val info = remember(juzgadoInfo) {
        juzgadoInfo?.let {
            mapOf(
                "direccion" to (it["direccion"] ?: ""),
                "telefono" to (it["telefono"] ?: ""),
                "codigo_postal" to (it["codigo_postal"] ?: "")
            )
        } ?: emptyMap()
    }
    val dataProvider = remember {
        CitacionDataProvider(
            citacionViewModel,
            personaViewModel,
            guardiasViewModel,
            alcoholemiaDosViewModel
        )
    }
    val numeroDocumento by personaViewModel.numeroDocumento.observeAsState("")
    val scope = rememberCoroutineScope()

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                citacionViewModel.loadData(context)
                personaViewModel.loadData(context)
                guardiasViewModel.loadData(context)
                alcoholemiaDosViewModel.loadSavedData()
                isDataLoaded = true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error cargando datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Validar numeroDocumento
    LaunchedEffect(isDataLoaded, numeroDocumento, printStatus) {
        if (isDataLoaded && numeroDocumento.isNullOrEmpty() && printStatus.isEmpty()) {
            navigateToScreen("PersonaScreen")
            Toast.makeText(context, "Por favor, ingrese el número de documento", Toast.LENGTH_SHORT).show()
        }
    }

    // Mostrar diálogo de campos faltantes
    if (missingFields.isNotEmpty()) {
        MissingFieldsDialog(
            missingFields = missingFields,
            onDismiss = { citacionViewModel.updateMissingFields(emptyList()) },
            navigateToScreen = navigateToScreen
        )
    }

    // Mostrar indicador de progreso
    if (printStatus.isNotEmpty() && !showPreviewDialog) {
        FullScreenProgressIndicator(text = printStatus)
    }

    // Mostrar diálogo de previsualización
    if (showPreviewDialog && previewBitmap != null) {
        BitmapPreviewDialog(
            bitmap = previewBitmap,
            onConfirm = {
                citacionViewModel.confirmPrint(
                    context = context,
                    htmlParser = htmlParser,
                    dataProvider = dataProvider,
                    pdfToBitmapPrinter = pdfToBitmapPrinter,
                    impresoraViewModel = impresoraViewModel
                )
            },
            onDismiss = {
                scope.launch {
                    citacionViewModel.updateShowPreviewDialog(false)
                    citacionViewModel.updatePreviewBitmap(null)
                    citacionViewModel.updatePrintStatus("Impresión cancelada")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Impresión cancelada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        topBar = { CitacionTopBar() },
        bottomBar = { CitacionBottomBar(citacionViewModel, navigateToScreen, context) }
    ) { paddingValues ->
        CitacionContent(
            modifier = Modifier.padding(paddingValues),
            citacionViewModel = citacionViewModel,
            navigateToScreen = navigateToScreen,
            isPrintingActa = printStatus.isNotEmpty(),
            onPrintActaTrigger = {
                citacionViewModel.generateAndPrintActa(
                    context = context,
                    htmlParser = htmlParser,
                    dataProvider = dataProvider,
                    zebraPrinter = PDFLabelPrinterZebra(context),
                    impresoraViewModel = impresoraViewModel,
                    personaViewModel = personaViewModel,
                    guardiasViewModel = guardiasViewModel,
                    alcoholemiaDosViewModel = alcoholemiaDosViewModel
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitacionTopBar() {
    CenterAlignedTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Citación",
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Datos de la diligencia",
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoSecundarios,
                    textAlign = TextAlign.Center
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitacionContent(
    modifier: Modifier = Modifier,
    citacionViewModel: CitacionViewModel,
    navigateToScreen: (String) -> Unit,
    isPrintingActa: Boolean,
    onPrintActaTrigger: () -> Unit
) {
    val context = LocalContext.current
    val provincia by citacionViewModel.provincia.observeAsState("")
    val localidad by citacionViewModel.localidad.observeAsState("")
    val juzgado by citacionViewModel.juzgado.observeAsState("")
    val fechaInicio by citacionViewModel.fechaInicio.observeAsState("")
    val hora by citacionViewModel.hora.observeAsState("")
    val numeroDiligencias by citacionViewModel.numeroDiligencias.observeAsState("")
    val fechaNotificacion by citacionViewModel.fechaNotificacion.observeAsState("")
    val horaNotificacion by citacionViewModel.horaNotificacion.observeAsState("")
    val abogadoNombre by citacionViewModel.abogadoNombre.observeAsState("")
    val abogadoColegiado by citacionViewModel.abogadoColegiado.observeAsState("")
    val abogadoColegio by citacionViewModel.abogadoColegio.observeAsState("")
    val comunicacionNumero by citacionViewModel.comunicacionNumero.observeAsState("")
    val abogadoDesignado by citacionViewModel.abogadoDesignado.observeAsState(false)
    val abogadoOficio by citacionViewModel.abogadoOficio.observeAsState(false)

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNotificacionDatePicker by remember { mutableStateOf(false) }
    var showNotificacionTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    val notificacionDatePickerState = rememberDatePickerState()
    val notificacionTimePickerState = rememberTimePickerState()

    val db = remember { AccesoBaseDatos(context, "juzgados.db") }
    val provincias by remember { mutableStateOf(getProvincias(db)) }
    val municipios by remember(provincia) { mutableStateOf(getMunicipios(db, provincia)) }
    val sedes by remember(localidad) { mutableStateOf(getSedes(db, localidad)) }
    val juzgadoInfo by remember(juzgado) { mutableStateOf(getJuzgadoInfo(db, juzgado)) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let {
                        val localDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val formatter = DateTimeFormatter
                            .ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                        localDate.format(formatter)
                    } ?: ""
                    citacionViewModel.updateFechaInicio(selectedDate)
                    showDatePicker = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialogCitacion(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        citacionViewModel.updateHora(
                            "${timePickerState.hour}:${timePickerState.minute.toString().padStart(2, '0')}"
                        )
                        showTimePicker = false
                    }
                ) { Text("OK") }
            }
        ) { TimePicker(state = timePickerState) }
    }

    if (showNotificacionDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showNotificacionDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val selectedDate = notificacionDatePickerState.selectedDateMillis?.let {
                        val localDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val formatter = DateTimeFormatter
                            .ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                        localDate.format(formatter)
                    } ?: ""
                    citacionViewModel.updateFechaNotificacion(selectedDate)
                    showNotificacionDatePicker = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNotificacionDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = notificacionDatePickerState)
        }
    }

    if (showNotificacionTimePicker) {
        TimePickerDialogCitacion(
            onDismissRequest = { showNotificacionTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        citacionViewModel.updateHoraNotificacion(
                            "${notificacionTimePickerState.hour}:${notificacionTimePickerState.minute.toString().padStart(2, '0')}"
                        )
                        showNotificacionTimePicker = false
                    }
                ) { Text("OK") }
            }
        ) { TimePicker(state = notificacionTimePickerState) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Fecha y hora de notificación",
            color = TextoNormales,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = fechaNotificacion,
                onValueChange = { citacionViewModel.updateFechaNotificacion(it) },
                label = { Text("Fecha notificación", color = TextoTerciarios) },
                modifier = Modifier
                    .weight(2f)
                    .padding(end = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { showNotificacionDatePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar_ico),
                            contentDescription = "Seleccionar fecha notificación",
                            tint = TextoSecundarios
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = TextoSecundarios,
                    focusedBorderColor = BotonesNormales
                ),
                readOnly = true
            )

            OutlinedTextField(
                value = horaNotificacion,
                onValueChange = { citacionViewModel.updateHoraNotificacion(it) },
                label = { Text("Hora notificación", color = TextoTerciarios) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { showNotificacionTimePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.reloj_ico),
                            contentDescription = "Seleccionar hora notificación",
                            tint = TextoSecundarios
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = TextoSecundarios,
                    focusedBorderColor = BotonesNormales
                ),
                readOnly = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Fecha y hora del juicio",
            color = TextoNormales,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = fechaInicio,
                onValueChange = { citacionViewModel.updateFechaInicio(it) },
                label = { Text("Fecha juicio", color = TextoTerciarios) },
                modifier = Modifier
                    .weight(2f)
                    .padding(end = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar_ico),
                            contentDescription = "Seleccionar fecha juicio",
                            tint = TextoSecundarios
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = TextoSecundarios,
                    focusedBorderColor = BotonesNormales
                ),
                readOnly = true
            )

            OutlinedTextField(
                value = hora,
                onValueChange = { citacionViewModel.updateHora(it) },
                label = { Text("Hora juicio", color = TextoTerciarios) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.reloj_ico),
                            contentDescription = "Seleccionar hora juicio",
                            tint = TextoSecundarios
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = TextoSecundarios,
                    focusedBorderColor = BotonesNormales
                ),
                readOnly = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Número de diligencias",
            color = TextoNormales,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CustomEditText(
            value = numeroDiligencias,
            onValueChange = { citacionViewModel.updateNumeroDiligencias(it) },
            label = "Número de diligencias",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Juzgado",
            color = TextoNormales,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DropdownFieldCitacion(
            value = provincia,
            onValueChange = { newProvincia ->
                citacionViewModel.updateProvincia(newProvincia)
                citacionViewModel.updateLocalidad("")
                citacionViewModel.updateJuzgado("")
            },
            label = "Provincia",
            options = provincias
        )

        DropdownFieldCitacion(
            value = localidad,
            onValueChange = { citacionViewModel.updateLocalidad(it) },
            label = "Localidad",
            options = municipios
        )

        DropdownFieldCitacion(
            value = juzgado,
            onValueChange = { citacionViewModel.updateJuzgado(it) },
            label = "Juzgado",
            options = sedes
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Datos del juzgado",
            color = TextoNormales,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (juzgado.isNotEmpty() && juzgadoInfo != null) {
            val info = juzgadoInfo!! // Afirmar que no es nulo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = info["nombre"] ?: "N/A",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = info["direccion"] ?: "N/A",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = info["telefono"] ?: "N/A",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Código Postal: ${info["codigo_postal"] ?: "N/A"}",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Datos del abogado",
            color = TextoNormales,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = abogadoDesignado,
                onCheckedChange = { isChecked ->
                    citacionViewModel.updateAbogadoSelection(
                        designado = isChecked,
                        oficio = abogadoOficio && !isChecked
                    )
                }
            )
            Text(
                text = "Designa abogado",
                color = TextoNormales,
                fontSize = 14.sp
            )
        }

        if (abogadoDesignado) {
            CustomEditText(
                value = abogadoNombre,
                onValueChange = { citacionViewModel.updateAbogadoNombre(it) },
                label = "Nombre del abogado",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            CustomEditText(
                value = abogadoColegiado,
                onValueChange = { citacionViewModel.updateAbogadoColegiado(it) },
                label = "Número de colegiado",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            CustomEditText(
                value = abogadoColegio,
                onValueChange = { citacionViewModel.updateAbogadoColegio(it) },
                label = "Colegio de abogados",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = abogadoOficio,
                onCheckedChange = { isChecked ->
                    citacionViewModel.updateAbogadoSelection(
                        designado = abogadoDesignado && !isChecked,
                        oficio = isChecked
                    )
                }
            )
            Text(
                text = "Solicita abogado de oficio",
                color = TextoNormales,
                fontSize = 14.sp
            )
        }

        if (abogadoOficio) {
            CustomEditText(
                value = comunicacionNumero,
                onValueChange = { citacionViewModel.updateComunicacionNumero(it) },
                label = "Número de comunicación",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {
                if (!isPrintingActa) {
                    citacionViewModel.guardarDatos(context)
                    onPrintActaTrigger()
                }
            },
            enabled = !isPrintingActa,
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(if (isPrintingActa) "IMPRIMIENDO..." else "IMPRIMIR ACTA")
        }
    }
}

@Composable
private fun CitacionBottomBar(
    viewModel: CitacionViewModel,
    navigateToScreen: (String) -> Unit,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = {
                viewModel.guardarDatos(context)
                navigateToScreen("OtrosDocumentosScreen")
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

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = { viewModel.limpiarDatos(context) },
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
private fun DropdownFieldCitacion(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            label = { Text(label, color = TextoTerciarios) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Desplegar",
                    tint = TextoSecundarios
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = TextoSecundarios,
                focusedBorderColor = BotonesNormales
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextoNormales) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TimePickerDialogCitacion(
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
fun CustomEditText(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoTerciarios) },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = TextoSecundarios,
            focusedBorderColor = BotonesNormales
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextoNormales)
    )
}

private fun getProvincias(db: AccesoBaseDatos): List<String> {
    return db.query("SELECT Provincia FROM PROVINCIAS").map { it["Provincia"] as String }
}

private fun getMunicipios(db: AccesoBaseDatos, provincia: String): List<String> {
    if (provincia.isEmpty()) return emptyList()
    val query =
        "SELECT M.Municipio FROM MUNICIPIOS M JOIN PROVINCIAS P ON M.idProvincia = P.idProvincia JOIN SEDES S ON M.Municipio = S.municipio WHERE P.Provincia = '$provincia' GROUP by M.municipio;"
    return db.query(query).map { it["Municipio"] as String }
}

private fun getSedes(db: AccesoBaseDatos, municipio: String): List<String> {
    if (municipio.isEmpty()) return emptyList()
    val query = "SELECT nombre FROM SEDES WHERE municipio = '$municipio'"
    return db.query(query).map { it["nombre"] as String }
}

private fun getJuzgadoInfo(db: AccesoBaseDatos, nombreJuzgado: String): Map<String, String>? {
    if (nombreJuzgado.isEmpty()) return null
    val query = "SELECT * FROM SEDES WHERE nombre = '$nombreJuzgado'"
    val result = db.query(query)
    return if (result.isNotEmpty()) result[0] as Map<String, String> else null
}

private fun isValidPdf(file: File): Boolean {
    return try {
        PdfReader(file).use { reader ->
            PdfDocument(reader).use { document ->
                document.numberOfPages > 0
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Invalid PDF file: ${file.absolutePath}, error: ${e.message}", e)
        false
    }
}