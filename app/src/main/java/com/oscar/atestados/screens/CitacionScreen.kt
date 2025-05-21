package com.oscar.atestados.screens

import android.content.ActivityNotFoundException
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
import com.oscar.atestados.utils.PdfToBitmapConverter
import com.tuapp.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private const val TAG = "CitacionScreen"

/**
 * Pantalla principal para gestionar la diligencia de citación.
 * Permite al usuario introducir datos de la citación, como fecha, hora, juzgado y detalles del abogado,
 * y generar un documento PDF para impresión en formato Zebra y A4.
 *
 * @param navigateToScreen Función de navegación para cambiar a otras pantallas.
 * @param citacionViewModel ViewModel que gestiona los datos específicos de la citación.
 * @param personaViewModel ViewModel que proporciona los datos del denunciado (nombre y DNI).
 * @param guardiasViewModel ViewModel que proporciona los datos de los guardias intervinientes (TIPs y unidad).
 * @param alcoholemiaDosViewModel ViewModel que proporciona datos contextuales como fecha y hora de notificación.
 * @param impresoraViewModel ViewModel que gestiona la configuración de la impresora.
 */
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
    val scope = rememberCoroutineScope()
    var isPrintingActa by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPrintStatus by remember { mutableStateOf("") }
    var showMissingFieldsDialog by remember { mutableStateOf(false) }
    var missingFieldsToShow by remember { mutableStateOf<List<String>>(emptyList()) }
    var isDataLoaded by remember { mutableStateOf(false) }
    var isPrintingZebra by remember { mutableStateOf(false) }
    val htmlParser = remember { HtmlParser(context) }
    val pdfToBitmapPrinter = remember { PDFToBitmapPrinter(context) }
    val pdfA4Printer = remember { PDFA4Printer(context) }
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

    // Carga inicial de datos y validación de numeroDocumento
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                citacionViewModel.loadData(context)
                personaViewModel.loadData(context)
                guardiasViewModel.loadData(context)
                alcoholemiaDosViewModel.loadSavedData()
                Log.d("CitacionScreen", "Datos cargados exitosamente")
            } catch (e: Exception) {
                Log.e("CitacionScreen", "Error cargando datos: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error cargando datos", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isDataLoaded = true
            }
        }
    }

    // Validar numeroDocumento solo después de que los datos estén cargados
    LaunchedEffect(isDataLoaded, numeroDocumento) {
        if (isDataLoaded) {
            Log.d("CitacionScreen", "Validando numeroDocumento: $numeroDocumento")
            if (numeroDocumento.isNullOrEmpty()) {
                Log.w("CitacionScreen", "numeroDocumento está vacío, redirigiendo a PersonaScreen")
                navigateToScreen("PersonaScreen")
                Toast.makeText(context, "Por favor, ingrese el número de documento", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("CitacionScreen", "numeroDocumento válido: $numeroDocumento")
            }
        }
    }

    // Mostrar indicador de progreso mientras los datos se cargan
    if (!isDataLoaded) {
        FullScreenProgressIndicator(text = "Cargando datos...")
        return
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

                // Guardar datos de todos los ViewModels antes de validar
                citacionViewModel.guardarDatos(context)
                personaViewModel.saveData(context)
                guardiasViewModel.saveData(context)
                alcoholemiaDosViewModel.guardarDatos(context)
                Log.d("CitacionScreen", "Datos guardados antes de validar, numeroDocumento: ${personaViewModel.numeroDocumento.value}")

                val (isValid, missingFields) = dataProvider.validateData()
                if (!isValid) {
                    missingFieldsToShow = missingFields
                    showMissingFieldsDialog = true
                    currentPrintStatus = "Datos incompletos"
                    isPrintingActa = false
                    Log.w("CitacionScreen", "Validación fallida, campos faltantes: $missingFields")
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

                // Generar el PDF para Zebra para previsualización
                currentPrintStatus = "Generando PDF para impresora Zebra..."
                val zebraPrinter = PDFLabelPrinterZebra(context)
                val previewFile = File.createTempFile("citacion_zebra_preview", ".pdf", context.cacheDir)
                zebraPrinter.generarEtiquetaPdf(htmlContent, previewFile)

                if (!previewFile.exists() || previewFile.length() == 0L) {
                    currentPrintStatus = "Error al generar PDF para Zebra"
                    Toast.makeText(context, "Error al generar PDF para Zebra", Toast.LENGTH_LONG).show()
                    isPrintingActa = false
                    withContext(Dispatchers.IO) { File(tempHtmlFilePath).delete() }
                    return@LaunchedEffect
                }

                // Generar PDF A4 para almacenamiento usando PdfUtils
                currentPrintStatus = "Generando PDF A4..."
                val outputFile = PdfUtils.writePdfToStorage(htmlContent, "acta_citacion_a4.pdf", pdfA4Printer, context)
                if (outputFile == null) {
                    currentPrintStatus = "Error al generar PDF A4"
                    Toast.makeText(context, "Error al generar PDF A4", Toast.LENGTH_LONG).show()
                    isPrintingActa = false
                    withContext(Dispatchers.IO) {
                        File(tempHtmlFilePath).delete()
                        previewFile.delete()
                    }
                    return@LaunchedEffect
                }

                // Abrir el PDF A4 usando FileProvider
                withContext(Dispatchers.Main) {
                    try {
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "com.oscar.atestados.fileprovider",
                            outputFile as File
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Seleccionar aplicación para abrir PDF")
                        )
                        currentPrintStatus = "PDF A4 abierto"
                        Log.d("CitacionScreen", "PDF intent lanzado para abrir: $contentUri")
                    } catch (e: ActivityNotFoundException) {
                        currentPrintStatus = "No hay aplicación para abrir PDFs"
                        Log.w("CitacionScreen", "No se encontró aplicación para abrir PDFs", e)
                    }
                }

                // Previsualizar el PDF de Zebra
                if (!isValidPdf(previewFile)) {
                    currentPrintStatus = "Error: El archivo PDF no es válido"
                    Toast.makeText(context, "Error: El archivo PDF no es válido", Toast.LENGTH_LONG).show()
                    withContext(Dispatchers.IO) {
                        File(tempHtmlFilePath).delete()
                        previewFile.delete()
                    }
                    isPrintingActa = false
                } else {
                    val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(previewFile)
                    if (bitmaps.isNotEmpty() && bitmaps[0] != null) {
                        previewBitmap = bitmaps[0]
                        showPreviewDialog = true
                        currentPrintStatus = "Mostrando previsualización"
                    } else {
                        currentPrintStatus = "Error al generar previsualización"
                        Toast.makeText(context, "Error al generar la imagen", Toast.LENGTH_SHORT).show()
                        withContext(Dispatchers.IO) {
                            File(tempHtmlFilePath).delete()
                            previewFile.delete()
                        }
                        isPrintingActa = false
                    }
                }
            } catch (e: Exception) {
                currentPrintStatus = "Error al generar documentos: ${e.message}"
                Toast.makeText(context, "Error al generar documentos: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("CitacionScreen", "Error en generación de documentos: ${e.message}", e)
                isPrintingActa = false
            }
        }
    }

    // Lógica para manejar la confirmación de la previsualización e impresión
    LaunchedEffect(showPreviewDialog, isPrintingActa, previewBitmap) {
        if (!showPreviewDialog && isPrintingActa && previewBitmap != null) {
            isPrintingZebra = true // Activar indicador de impresión
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                    ?: throw Exception("No hay impresora seleccionada")
                val tempHtmlFilePath = withContext(Dispatchers.IO) {
                    htmlParser.generateHtmlFile(
                        templateAssetPath = "documents/acta_citacion.html",
                        dataProvider = dataProvider
                    )
                }
                val htmlContent = withContext(Dispatchers.IO) {
                    File(tempHtmlFilePath).readText(Charsets.UTF_8)
                }

                // Verificar si el HTML contiene datos críticos
                if (!htmlContent.contains("OSCAR ISRAEL GINES RIVALLO") || htmlContent.contains("<span id=\"nombre_completo\"></span>")) {
                    throw Exception("El HTML generado está incompleto")
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
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Acta de citación enviada a imprimir",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // Guardar PDF en formato A4
                        val pdfFileName = "Acta_Citacion_${System.currentTimeMillis()}.pdf"
                        val savedFile =
                            writePdfToStorage(htmlContent, pdfFileName, pdfA4Printer, context)
                        if (savedFile != null && isValidPdf(savedFile)) {
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "PDF guardado en Documents/Atestados/$pdfFileName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    is PDFToBitmapPrinter.PrintResult.Error -> {
                        currentPrintStatus = "Error: ${printResult.message}"
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Error al imprimir: ${printResult.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                withContext(Dispatchers.IO) { File(tempHtmlFilePath).delete() }
            } catch (e: Exception) {
                currentPrintStatus = "Error al imprimir: ${e.message}"
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                Log.e(TAG, "Error en impresión: ${e.message}", e)
            } finally {
                previewBitmap?.recycle()
                previewBitmap = null
                isPrintingActa = false
                isPrintingZebra = false // Desactivar indicador de impresión
            }
        }
    }

    // Diálogo para campos faltantes
    if (showMissingFieldsDialog) {
        MissingFieldsDialog(
            missingFields = missingFieldsToShow,
            onDismiss = { showMissingFieldsDialog = false },
            navigateToScreen = navigateToScreen
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
            isPrintingActa = isPrintingActa,
            onPrintActaTrigger = {
                Log.d(TAG, "Botón 'IMPRIMIR ACTA' presionado")
                // Log para depurar numeroDocumento antes de printActa
                Log.d("CitacionScreen", "numeroDocumento antes de printActa: ${personaViewModel.numeroDocumento.value ?: "NULO"}")
                isPrintingActa = true
                currentPrintStatus = "Preparando documento..."
                citacionViewModel.printActa(
                    context = context,
                    htmlParser = htmlParser,
                    dataProvider = dataProvider,
                    zebraPrinter = PDFLabelPrinterZebra(context),
                    impresoraViewModel = impresoraViewModel,
                    personaViewModel = personaViewModel, // Añadido
                    guardiasViewModel = guardiasViewModel, // Añadido
                    alcoholemiaDosViewModel = alcoholemiaDosViewModel, // Añadido
                    onStatusUpdate = { status ->
                        Log.d("CitacionScreen", "Estado de impresión: $status")
                        currentPrintStatus = status
                        if (status == "Previsualización lista") {
                            showPreviewDialog = true
                        }
                    },
                    onError = { error ->
                        Log.e("CitacionScreen", "Error en printActa: $error")
                        currentPrintStatus = error
                        missingFieldsToShow = if (error.contains("Datos incompletos")) {
                            error.substringAfter("Datos incompletos: ").split(", ")
                        } else {
                            emptyList()
                        }
                        showMissingFieldsDialog = missingFieldsToShow.isNotEmpty()
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                        isPrintingActa = false
                        previewBitmap?.recycle()
                        previewBitmap = null
                    },
                    onComplete = {
                        scope.launch(Dispatchers.IO) {
                            val tempPdfFile =
                                File(context.cacheDir, "acta_citacion_zebra_preview.pdf")
                            if (tempPdfFile.exists() && isValidPdf(tempPdfFile)) {
                                val bitmaps =
                                    com.oscar.atestados.utils.PdfToBitmapConverter.convertAllPagesToBitmaps(
                                        tempPdfFile
                                    )
                                withContext(Dispatchers.Main) {
                                    previewBitmap = bitmaps.firstOrNull()
                                    if (previewBitmap != null) {
                                        Log.d(
                                            TAG,
                                            "Previsualización generada: ${bitmaps.size} páginas"
                                        )
                                        currentPrintStatus = "Previsualización lista"
                                        showPreviewDialog = true
                                    } else {
                                        currentPrintStatus =
                                            "Error: No se pudo generar previsualización"
                                        scope.launch {
                                            Toast.makeText(
                                                context,
                                                "Error al generar previsualización",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    currentPrintStatus = "Error: Archivo PDF no válido"
                                    scope.launch {
                                        Toast.makeText(
                                            context,
                                            "Error: Archivo PDF no válido",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            isPrintingActa = false
                        }
                    }
                )
            }
        )
    }

    // Indicador de progreso para la generación del acta
    if (isPrintingActa && !showPreviewDialog) {
        FullScreenProgressIndicator(text = currentPrintStatus)
    }

    // Indicador de progreso para la impresión Zebra
    if (isPrintingZebra) {
        FullScreenProgressIndicator(text = currentPrintStatus)
    }

    // Diálogo de previsualización
    if (showPreviewDialog && previewBitmap != null) {
        BitmapPreviewDialog(
            bitmap = previewBitmap,
            onConfirm = {
                showPreviewDialog = false
                // isPrintingZebra se activa en el LaunchedEffect
            },
            onDismiss = {
                showPreviewDialog = false
                previewBitmap?.recycle()
                previewBitmap = null
                isPrintingActa = false
                isPrintingZebra = false
                currentPrintStatus = "Impresión cancelada"
                scope.launch {
                    Toast.makeText(context, "Impresión cancelada", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * Barra superior de la pantalla de citación.
 * Muestra el título "Citación" y un subtítulo con información adicional.
 */
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

/**
 * Contenido principal de la pantalla de citación.
 * Incluye campos para introducir datos de la citación, como fechas, horas, juzgado y detalles del abogado.
 *
 * @param modifier Modificador para personalizar el diseño.
 * @param citacionViewModel ViewModel que gestiona los datos de la citación.
 * @param navigateToScreen Función de navegación para cambiar a otras pantallas.
 * @param isPrintingActa Indica si se está procesando la impresión del acta.
 * @param onPrintActaTrigger Función para iniciar el proceso de impresión.
 */
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

    // Diálogo para seleccionar la fecha del juicio
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

    // Diálogo para seleccionar la hora del juicio
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

    // Diálogo para seleccionar la fecha de notificación
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

    // Diálogo para seleccionar la hora de notificación
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
                        oficio = !isChecked && abogadoOficio
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
                        designado = !isChecked && abogadoDesignado,
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

/**
 * Barra inferior de la pantalla de citación.
 * Incluye botones para guardar los datos y limpiar el formulario.
 *
 * @param viewModel ViewModel que gestiona los datos de la citación.
 * @param navigateToScreen Función de navegación para cambiar a otras pantallas.
 * @param context Contexto de la aplicación.
 */
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

/**
 * Campo desplegable para seleccionar opciones como provincia, localidad o juzgado.
 *
 * @param value Valor actual seleccionado.
 * @param onValueChange Función para manejar cambios en el valor seleccionado.
 * @param label Etiqueta del campo.
 * @param options Lista de opciones disponibles.
 */
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

/**
 * Diálogo para seleccionar una hora utilizando un TimePicker.
 *
 * @param onDismissRequest Función para cerrar el diálogo.
 * @param confirmButton Botón de confirmación.
 * @param modifier Modificador para personalizar el diseño.
 * @param content Contenido del diálogo (TimePicker).
 */
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

/**
 * Campo de texto personalizado para entradas de usuario.
 *
 * @param value Valor actual del campo.
 * @param onValueChange Función para manejar cambios en el valor.
 * @param label Etiqueta del campo.
 * @param modifier Modificador para personalizar el diseño.
 */
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

/**
 * Obtiene la lista de provincias desde la base de datos.
 *
 * @param db Instancia de AccesoBaseDatos para consultas.
 * @return Lista de nombres de provincias.
 */
private fun getProvincias(db: AccesoBaseDatos): List<String> {
    return db.query("SELECT Provincia FROM PROVINCIAS").map { it["Provincia"] as String }
}

/**
 * Obtiene la lista de municipios para una provincia dada.
 *
 * @param db Instancia de AccesoBaseDatos para consultas.
 * @param provincia Nombre de la provincia.
 * @return Lista de nombres de municipios.
 */
private fun getMunicipios(db: AccesoBaseDatos, provincia: String): List<String> {
    if (provincia.isEmpty()) return emptyList()
    val query =
        "SELECT M.Municipio FROM MUNICIPIOS M JOIN PROVINCIAS P ON M.idProvincia = P.idProvincia JOIN SEDES S ON M.Municipio = S.municipio WHERE P.Provincia = '$provincia' GROUP by M.municipio;"
    return db.query(query).map { it["Municipio"] as String }
}

/**
 * Obtiene la lista de sedes judiciales para un municipio dado.
 *
 * @param db Instancia de AccesoBaseDatos para consultas.
 * @param municipio Nombre del municipio.
 * @return Lista de nombres de sedes.
 */
private fun getSedes(db: AccesoBaseDatos, municipio: String): List<String> {
    if (municipio.isEmpty()) return emptyList()
    val query = "SELECT nombre FROM SEDES WHERE municipio = '$municipio'"
    return db.query(query).map { it["nombre"] as String }
}

/**
 * Obtiene la información de un juzgado desde la base de datos.
 *
 * @param db Instancia de AccesoBaseDatos para consultas.
 * @param nombreJuzgado Nombre del juzgado.
 * @return Mapa con los detalles del juzgado o null si no se encuentra.
 */
private fun getJuzgadoInfo(db: AccesoBaseDatos, nombreJuzgado: String): Map<String, String>? {
    if (nombreJuzgado.isEmpty()) return null
    val query = "SELECT * FROM SEDES WHERE nombre = '$nombreJuzgado'"
    val result = db.query(query)
    return if (result.isNotEmpty()) result[0] as Map<String, String> else null
}

/**
 * Verifica si un archivo PDF es válido comprobando si tiene páginas.
 *
 * @param file Archivo PDF a verificar.
 * @return True si el PDF es válido, false en caso contrario.
 */
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

/**
 * Escribe un archivo PDF en el almacenamiento externo en el directorio Documents/Atestados.
 * También indexa el archivo en MediaStore y ejecuta un escaneo para hacerlo visible en el sistema.
 *
 * @param content Contenido HTML para convertir a PDF.
 * @param fileName Nombre del archivo PDF a generar.
 * @param pdfA4Printer Instancia de PDFA4Printer para generar el documento.
 * @param context Contexto de la aplicación.
 * @return Archivo PDF generado o null si ocurre un error.
 */
suspend fun writePdfToStorage(
    content: String,
    fileName: String,
    pdfA4Printer: PDFA4Printer,
    context: Context
): File? {
    return withContext(Dispatchers.IO) {
        Log.d(TAG, "writePdfToStorage: Iniciando escritura de PDF, fileName: $fileName")
        try {
            val documentsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            val atestadosDir = File(documentsDir, "Atestados")
            if (!atestadosDir.exists()) {
                atestadosDir.mkdirs()
                Log.d(
                    TAG,
                    "writePdfToStorage: Directorio Atestados creado en ${atestadosDir.absolutePath}"
                )
            }
            val outputFile = File(atestadosDir, fileName)
            pdfA4Printer.generarDocumentoA4(htmlContent = content, outputFile = outputFile)
            Log.d(TAG, "writePdfToStorage: PDF generado en ${outputFile.absolutePath}")

            if (!outputFile.exists() || outputFile.length() == 0L) {
                Log.e(
                    TAG,
                    "writePdfToStorage: El archivo PDF no se creó correctamente o está vacío"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error: El archivo PDF no se creó correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            try {
                outputFile.setReadable(true, false)
                outputFile.setWritable(true, false)
                Log.d(
                    TAG,
                    "writePdfToStorage: Permisos establecidos para ${outputFile.absolutePath}"
                )
            } catch (e: SecurityException) {
                Log.w(
                    TAG,
                    "writePdfToStorage: No se pudieron establecer permisos: ${e.message}"
                )
            }

            try {
                val contentUri = MediaStore.Files.getContentUri("external")
                val values = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DATA, outputFile.absolutePath)
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                    put(
                        MediaStore.Files.FileColumns.DATE_MODIFIED,
                        System.currentTimeMillis() / 1000
                    )
                }
                context.contentResolver.insert(contentUri, values)
                Log.d(
                    TAG,
                    "writePdfToStorage: Archivo indexado en MediaStore: ${outputFile.absolutePath}"
                )
            } catch (e: Exception) {
                Log.w(TAG, "writePdfToStorage: Error al indexar en MediaStore: ${e.message}")
            }

            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("application/pdf")
                ) { path, uri ->
                    Log.d(
                        TAG,
                        "writePdfToStorage: MediaScanner completado para $path, URI: $uri"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "writePdfToStorage: Error al escanear archivo: ${e.message}")
            }

            outputFile
        } catch (e: SecurityException) {
            Log.e(TAG, "writePdfToStorage: Error de permisos al escribir PDF: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error de permisos al guardar PDF", Toast.LENGTH_SHORT)
                    .show()
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "writePdfToStorage: Error al escribir PDF: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Error al guardar PDF: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            null
        }
    }
}