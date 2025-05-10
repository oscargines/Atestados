package com.oscar.atestados.screens

import android.graphics.Bitmap
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
import com.oscar.atestados.R
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.data.CitacionDataProvider
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.utils.HtmlParser
import com.oscar.atestados.utils.PDFLabelPrinterZebra
import com.oscar.atestados.utils.PDFToBitmapPrinter
import com.oscar.atestados.utils.PdfToBitmapConverter
import com.oscar.atestados.viewModel.CitacionViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModel
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
    impresoraViewModel: ImpresoraViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var generateResult by remember { mutableStateOf<Result<String>?>(null) }
    var triggerGenerate by remember { mutableStateOf(false) }
    var isPrintingActa by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPrintStatus by remember { mutableStateOf("Iniciando...") }
    var showMissingFieldsDialog by remember { mutableStateOf(false) }
    var missingFieldsToShow by remember { mutableStateOf<List<String>>(emptyList()) }
    val htmlParser = remember { HtmlParser(context) }
    val pdfToBitmapPrinter = remember { PDFToBitmapPrinter(context) }
    val dataProvider = remember { CitacionDataProvider(citacionViewModel) }

    Log.v(TAG, "CitacionScreen iniciada")

    // Manejar el trigger de generación
    LaunchedEffect(triggerGenerate) {
        if (triggerGenerate) {
            isGenerating = true
            try {
                val (isValid, missingFields) = dataProvider.validateData()
                if (!isValid) {
                    missingFieldsToShow = missingFields
                    showMissingFieldsDialog = true
                    generateResult = Result.failure(Exception("Por favor, complete todos los campos requeridos"))
                } else {
                    val filePath = withContext(Dispatchers.IO) {
                        htmlParser.generateHtmlFile(
                            templateAssetPath = "documents/acta_citacion.html",
                            dataProvider = dataProvider
                        )
                    }
                    generateResult = Result.success("Datos guardados y archivo HTML generado en: $filePath")
                }
            } catch (e: Exception) {
                generateResult = Result.failure(e)
            } finally {
                isGenerating = false
                triggerGenerate = false
            }
        }
    }

    // Manejar la impresión del acta
    LaunchedEffect(isPrintingActa) {
        if (isPrintingActa) {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                if (macAddress.isNullOrEmpty()) {
                    currentPrintStatus = "No hay impresora seleccionada"
                    Toast.makeText(context, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show()
                    isPrintingActa = false
                    return@LaunchedEffect
                }

                val (isValid, missingFields) = dataProvider.validateData()
                if (!isValid) {
                    missingFieldsToShow = missingFields
                    showMissingFieldsDialog = true
                    currentPrintStatus = "Datos incompletos"
                    Toast.makeText(context, "Por favor, complete todos los campos requeridos", Toast.LENGTH_SHORT).show()
                    isPrintingActa = false
                    return@LaunchedEffect
                }

                currentPrintStatus = "Preparando documento..."
                val tempHtmlFilePath = withContext(Dispatchers.IO) {
                    htmlParser.generateHtmlFile(
                        templateAssetPath = "documents/acta_citacion.html",
                        dataProvider = dataProvider
                    )
                }
                val htmlContent = withContext(Dispatchers.IO) {
                    File(tempHtmlFilePath).readText(Charsets.UTF_8)
                }

                // Generar el PDF para previsualización
                val outputFile = File(context.getExternalFilesDir(null), "acta_citacion_preview.pdf")
                if (outputFile.exists()) outputFile.delete()
                val pdfLabelPrinter = PDFLabelPrinterZebra(context)
                pdfLabelPrinter.generarEtiquetaPdf(htmlContent, outputFile)
                currentPrintStatus = "PDF generado para previsualización"

                // Convertir a Bitmap para previsualización
                val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(outputFile)
                if (bitmaps.isNotEmpty() && bitmaps[0] != null) {
                    previewBitmap = bitmaps[0]
                    showPreviewDialog = true
                    currentPrintStatus = "Mostrando previsualización"
                } else {
                    currentPrintStatus = "Error al generar previsualización"
                    Toast.makeText(context, "Error al generar la imagen", Toast.LENGTH_SHORT).show()
                    withContext(Dispatchers.IO) {
                        File(tempHtmlFilePath).delete()
                    }
                    isPrintingActa = false
                }
            } catch (e: Exception) {
                currentPrintStatus = "Error al generar previsualización: ${e.message}"
                Toast.makeText(context, "Error al generar previsualización: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error en previsualización: ${e.message}", e)
                isPrintingActa = false
            }
        }
    }

    // Manejar la confirmación de impresión desde el diálogo
    LaunchedEffect(showPreviewDialog) {
        if (!showPreviewDialog && isPrintingActa && previewBitmap != null) {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac() ?: throw Exception("No hay impresora seleccionada")
                val tempHtmlFilePath = withContext(Dispatchers.IO) {
                    htmlParser.generateHtmlFile(
                        templateAssetPath = "documents/acta_citacion.html",
                        dataProvider = dataProvider
                    )
                }
                val htmlContent = withContext(Dispatchers.IO) {
                    File(tempHtmlFilePath).readText(Charsets.UTF_8)
                }

                currentPrintStatus = "Enviando a imprimir..."
                val printResult = pdfToBitmapPrinter.printHtmlAsBitmap(
                    htmlAssetPath = "",
                    macAddress = macAddress,
                    outputFileName = "acta_citacion_temp.pdf",
                    htmlContent = htmlContent,
                    onStatusUpdate = { status ->
                        scope.launch(Dispatchers.Main) { currentPrintStatus = status }
                    }
                )

                when (printResult) {
                    is PDFToBitmapPrinter.PrintResult.Success -> {
                        currentPrintStatus = "Impresión enviada"
                        Toast.makeText(context, "Acta de citación enviada a imprimir", Toast.LENGTH_SHORT).show()
                    }
                    is PDFToBitmapPrinter.PrintResult.Error -> {
                        currentPrintStatus = "Error: ${printResult.message}"
                        Toast.makeText(context, "Error al imprimir: ${printResult.message}", Toast.LENGTH_LONG).show()
                    }
                }

                withContext(Dispatchers.IO) {
                    File(tempHtmlFilePath).delete()
                }
            } catch (e: Exception) {
                currentPrintStatus = "Error al imprimir: ${e.message}"
                Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error en impresión: ${e.message}", e)
            } finally {
                previewBitmap?.recycle()
                previewBitmap = null
                isPrintingActa = false
            }
        }
    }

    // Manejar el resultado de la generación
    generateResult?.let { result ->
        LaunchedEffect(result) {
            result.onSuccess { status ->
                Log.i(TAG, "Operación exitosa: $status")
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Log.e(TAG, "Error: ${exception.message}", exception)
                Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
            generateResult = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        topBar = { CitacionTopBar() },
        bottomBar = { CitacionBottomBar(citacionViewModel, navigateToScreen) }
    ) { paddingValues ->
        CitacionContent(
            modifier = Modifier.padding(paddingValues),
            citacionViewModel = citacionViewModel,
            navigateToScreen = navigateToScreen,
            onGenerateTrigger = {
                Log.i(TAG, "Botón GUARDAR pulsado, guardando datos")
                triggerGenerate = true
            },
            isGenerating = isGenerating,
            impresoraViewModel = impresoraViewModel,
            isPrintingActa = isPrintingActa,
            onPrintActaTrigger = { isPrintingActa = true }
        )
    }

    if (isGenerating || isPrintingActa) {
        FullScreenProgressIndicator(text = if (isGenerating) "Guardando datos..." else "Imprimiendo acta...")
    }

    BitmapPreviewDialog(
        bitmap = previewBitmap,
        onConfirm = {
            showPreviewDialog = false // Continúa con la impresión
        },
        onDismiss = {
            showPreviewDialog = false
            previewBitmap?.recycle()
            previewBitmap = null
            isPrintingActa = false
            currentPrintStatus = "Impresión cancelada"
            scope.launch {
                Toast.makeText(context, "Impresión cancelada", Toast.LENGTH_SHORT).show()
            }
        }
    )
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
    onGenerateTrigger: () -> Unit,
    isGenerating: Boolean,
    impresoraViewModel: ImpresoraViewModel, // Nuevo parámetro
    isPrintingActa: Boolean, // Nuevo parámetro
    onPrintActaTrigger: () -> Unit // Nuevo callback para disparar la impresión
) {
    val context = LocalContext.current

    val provincia by citacionViewModel.provincia.observeAsState("")
    val localidad by citacionViewModel.localidad.observeAsState("")
    val juzgado by citacionViewModel.juzgado.observeAsState("")
    val fechaInicio by citacionViewModel.fechaInicio.observeAsState("")
    val hora by citacionViewModel.hora.observeAsState("")
    val numeroDiligencias by citacionViewModel.numeroDiligencias.observeAsState("")

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Fecha y hora juicio",
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
                label = { Text("Fecha inicio", color = TextoTerciarios) },
                modifier = Modifier
                    .weight(2f)
                    .padding(end = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar_ico),
                            contentDescription = "Seleccionar fecha",
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
                label = { Text("Hora", color = TextoTerciarios) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.reloj_ico),
                            contentDescription = "Seleccionar hora",
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = juzgadoInfo!!["nombre"] ?: "N/A",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = juzgadoInfo!!["direccion"] ?: "N/A",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = juzgadoInfo!!["telefono"] ?: "N/A",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Código Postal: ${juzgadoInfo!!["codigo_postal"] ?: "N/A"}",
                    color = TextoSecundarios,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = "Seleccione un juzgado para ver los detalles",
                color = TextoSecundarios,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {
                if (!isGenerating) {
                    citacionViewModel.guardarDatos()
                    onGenerateTrigger()
                }
            },
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(if (isGenerating) "GUARDANDO..." else "GUARDAR")
        }

        Spacer(modifier = Modifier.height(16.dp)) // Espacio entre botones

        Button(
            onClick = {
                if (!isPrintingActa) {
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
    val query = "SELECT M.Municipio FROM MUNICIPIOS M JOIN PROVINCIAS P ON M.idProvincia = P.idProvincia JOIN SEDES S ON M.Municipio = S.municipio WHERE P.Provincia = '$provincia' GROUP by M.municipio;"
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