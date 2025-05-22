package com.oscar.atestados.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.itextpdf.kernel.pdf.PdfDocument
import com.oscar.atestados.R
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.utils.PDFToBitmapPrinter
import com.oscar.atestados.utils.PdfToBitmapConverter
import com.oscar.atestados.utils.HtmlParser
import com.oscar.atestados.utils.PDFA4Printer
import com.oscar.atestados.data.AlcoholemiaDataProvider
import com.oscar.atestados.ui.composables.MissingFieldsDialog
import com.oscar.atestados.ui.composables.FullScreenProgressIndicator
import com.oscar.atestados.ui.composables.BitmapPreviewDialog
import com.oscar.atestados.viewModel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.text.Regex
import com.itextpdf.kernel.pdf.PdfReader
import com.oscar.atestados.utils.PDFLabelPrinterZebra
import com.oscar.atestados.utils.PdfUtils

private const val TAG = "Alcoholemia02Screen"

/**
 * Pantalla principal para el segundo paso del proceso de alcoholemia.
 * Muestra y gestiona los datos de inicio de diligencias, incluyendo fecha, hora, ubicación y firmas.
 *
 * @param navigateToScreen Función para navegar a otras pantallas.
 * @param alcoholemiaDosViewModel ViewModel que contiene la lógica y estado de la pantalla.
 * @param alcoholemiaUnoViewModel ViewModel con datos de la pantalla Alcoholemia01.
 * @param personaViewModel ViewModel con datos de la persona investigada.
 * @param vehiculoViewModel ViewModel con datos del vehículo.
 * @param tomaDerechosViewModel ViewModel con datos de la toma de derechos.
 * @param tomaManifestacionViewModel ViewModel con datos de la toma de manifestación.
 * @param lecturaDerechosViewModel ViewModel con datos de la lectura de derechos.
 * @param guardiasViewModel ViewModel con datos de los guardias intervinientes.
 * @param impresoraViewModel ViewModel para gestionar la configuración de la impresora.
 */
@Composable
fun Alcoholemia02Screen(
    navigateToScreen: (String) -> Unit,
    alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel,
    personaViewModel: PersonaViewModel,
    vehiculoViewModel: VehiculoViewModel,
    tomaDerechosViewModel: TomaDerechosViewModel,
    tomaManifestacionViewModel: TomaManifestacionAlcoholViewModel,
    lecturaDerechosViewModel: LecturaDerechosViewModel,
    guardiasViewModel: GuardiasViewModel,
    impresoraViewModel: ImpresoraViewModel
) {
    var showDatePickerFechaDiligencias by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isPrintingAtestado by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var currentPrintStatus by remember { mutableStateOf("Iniciando...") }
    var showInvalidLugarInvestigacionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = remember { AccesoBaseDatos(context, "juzgados.db") }
    val htmlParser = remember { HtmlParser(context) }
    val pdfA4Printer = remember { PDFA4Printer(context) }
    val pdfToBitmapPrinter = remember { PDFToBitmapPrinter(context) }
    val dataProvider = remember {
        AlcoholemiaDataProvider(
            alcoholemiaDosViewModel,
            alcoholemiaUnoViewModel,
            personaViewModel,
            vehiculoViewModel,
            tomaDerechosViewModel,
            tomaManifestacionViewModel,
            lecturaDerechosViewModel,
            guardiasViewModel,
            db
        )
    }
    var showMissingFieldsDialog by remember { mutableStateOf(false) }
    var missingFieldsToShow by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(isPrintingAtestado) {
        if (isPrintingAtestado) {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                if (macAddress.isNullOrEmpty()) {
                    currentPrintStatus = "No hay impresora seleccionada"
                    Toast.makeText(context, "No hay impresora seleccionada", Toast.LENGTH_SHORT)
                        .show()
                    isPrintingAtestado = false
                    return@LaunchedEffect
                }

                // Guardar datos de todos los ViewModels antes de validar
                alcoholemiaDosViewModel.guardarDatos(context)
                alcoholemiaUnoViewModel.guardarDatos(context)
                personaViewModel.saveData(context)
                vehiculoViewModel.saveData(context)
                guardiasViewModel.saveData(context)
                tomaDerechosViewModel.guardarDatos(context)
                tomaManifestacionViewModel.guardarDatos(context)
                lecturaDerechosViewModel.guardarDatos(context)
                Log.d(TAG, "Datos guardados antes de validar")

                val (isValid, missingFields) = dataProvider.validateData()
                if (!isValid) {
                    missingFieldsToShow = missingFields
                    showMissingFieldsDialog = true
                    currentPrintStatus = "Datos incompletos"
                    isPrintingAtestado = false
                    return@LaunchedEffect
                }

                currentPrintStatus = "Preparando documento..."
                val tempHtmlFilePath = withContext(Dispatchers.IO) {
                    htmlParser.generateHtmlFile(
                        templateAssetPath = "documents/ah01.html",
                        dataProvider = dataProvider
                    )
                }
                val htmlContent = withContext(Dispatchers.IO) {
                    File(tempHtmlFilePath).readText(Charsets.UTF_8)
                }

                // Generar el PDF para Zebra para previsualización
                currentPrintStatus = "Generando PDF para impresora Zebra..."
                val zebraPrinter = PDFLabelPrinterZebra(context)
                val previewFile =
                    File.createTempFile("atestado_zebra_preview", ".pdf", context.cacheDir)
                zebraPrinter.generarEtiquetaPdf(htmlContent, previewFile)

                if (!previewFile.exists() || previewFile.length() == 0L) {
                    currentPrintStatus = "Error al generar PDF para Zebra"
                    Toast.makeText(
                        context,
                        "Error al generar PDF para impresora Zebra",
                        Toast.LENGTH_LONG
                    ).show()
                    isPrintingAtestado = false
                    withContext(Dispatchers.IO) { File(tempHtmlFilePath).delete() }
                    return@LaunchedEffect
                }

                // Generar PDF A4 solo para guardar en almacenamiento usando PdfUtils
                currentPrintStatus = "Generando PDF A4..."
                val outputFile =
                    PdfUtils.writePdfToStorage(htmlContent, "atestado_a4.pdf", pdfA4Printer, context)
                if (outputFile == null) {
                    currentPrintStatus = "Error al generar PDF A4"
                    Toast.makeText(context, "Error al generar PDF A4", Toast.LENGTH_LONG).show()
                    isPrintingAtestado = false
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
                            outputFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(contentUri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    "Seleccionar aplicación para abrir PDF"
                                )
                            )
                            currentPrintStatus = "PDF A4 abierto"
                            Log.d(TAG, "PDF intent lanzado para abrir: $contentUri")
                        } catch (e: ActivityNotFoundException) {
                            currentPrintStatus = "No hay aplicación para abrir PDFs"
                            Log.w(TAG, "No se encontró aplicación para abrir PDFs", e)
                        }
                    } catch (e: Exception) {
                        currentPrintStatus = "Error al abrir PDF A4: ${e.message}"
                        Log.e(TAG, "Error al abrir PDF A4: ${e.message}", e)
                    }
                }

                // Previsualizar el PDF de Zebra
                if (!isValidPdf(previewFile)) {
                    currentPrintStatus = "Error: El archivo PDF no es válido"
                    Toast.makeText(context, "Error: El archivo PDF no es válido", Toast.LENGTH_LONG)
                        .show()
                    withContext(Dispatchers.IO) {
                        File(tempHtmlFilePath).delete()
                        previewFile.delete()
                    }
                    isPrintingAtestado = false
                } else {
                    val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(previewFile)
                    if (bitmaps.isNotEmpty() && bitmaps[0] != null) {
                        previewBitmap = bitmaps[0]
                        showPreviewDialog = true
                        currentPrintStatus = "Mostrando previsualización"
                    } else {
                        currentPrintStatus = "Error al generar previsualización"
                        Toast.makeText(context, "Error al generar la imagen", Toast.LENGTH_SHORT)
                            .show()
                        withContext(Dispatchers.IO) {
                            File(tempHtmlFilePath).delete()
                            previewFile.delete()
                        }
                        isPrintingAtestado = false
                    }
                }
            } catch (e: Exception) {
                currentPrintStatus = "Error al generar documentos: ${e.message}"
                Toast.makeText(
                    context,
                    "Error al generar documentos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Error en generación de documentos: ${e.message}", e)
                isPrintingAtestado = false
            }
        }
    }

    // Manejar la confirmación de impresión desde el diálogo
    LaunchedEffect(showPreviewDialog) {
        if (!showPreviewDialog && isPrintingAtestado && previewBitmap != null) {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                    ?: throw Exception("No hay impresora seleccionada")
                val tempHtmlFilePath = withContext(Dispatchers.IO) {
                    htmlParser.generateHtmlFile(
                        templateAssetPath = "documents/ah01.html",
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
                    outputFileName = "atestado_temp.pdf",
                    htmlContent = htmlContent,
                    onStatusUpdate = { status ->
                        scope.launch(Dispatchers.Main) { currentPrintStatus = status }
                    }
                )

                when (printResult) {
                    is PDFToBitmapPrinter.PrintResult.Success -> {
                        currentPrintStatus = "Impresión enviada"
                        Toast.makeText(context, "Atestado enviado a imprimir", Toast.LENGTH_SHORT)
                            .show()
                    }

                    is PDFToBitmapPrinter.PrintResult.Error -> {
                        currentPrintStatus = "Error: ${printResult.message}"
                        Toast.makeText(
                            context,
                            "Error al imprimir: ${printResult.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                withContext(Dispatchers.IO) { File(tempHtmlFilePath).delete() }
            } catch (e: Exception) {
                currentPrintStatus = "Error al imprimir: ${e.message}"
                Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error en impresión: ${e.message}", e)
            } finally {
                previewBitmap?.recycle()
                previewBitmap = null
                isPrintingAtestado = false
            }
        }
    }

    if (showMissingFieldsDialog) {
        MissingFieldsDialog(
            missingFields = missingFieldsToShow,
            onDismiss = { showMissingFieldsDialog = false },
            navigateToScreen = navigateToScreen
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AlcoholemiaTopBar() },
        bottomBar = { AlcoholemiaBottomBar(alcoholemiaDosViewModel, navigateToScreen) }
    ) { paddingValues ->
        Alcoholemia02Content(
            modifier = Modifier.padding(paddingValues),
            alcoholemiaDosViewModel = alcoholemiaDosViewModel,
            impresoraViewModel = impresoraViewModel,
            lecturaDerechosViewModel = lecturaDerechosViewModel,
            onDatePickerFechaDiligenciasClicked = { showDatePickerFechaDiligencias = true },
            showDatePickerFechaDiligencias = showDatePickerFechaDiligencias,
            isPrintingAtestado = isPrintingAtestado,
            onPrintAtestadoTrigger = {
                Log.d(TAG, "Botón 'IMPRIMIR ATESTADO' presionado")
                isPrintingAtestado = true
            },
            onInvalidLugarInvestigacion = { showInvalidLugarInvestigacionDialog = true },
            onOpenStorage = {
                Log.d(TAG, "Botón 'ABRIR ALMACENAMIENTO' presionado")
                try {
                    val atestadosDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        "Atestados"
                    )
                    if (!atestadosDir.exists()) {
                        atestadosDir.mkdirs()
                        Log.d(TAG, "Directorio Atestados creado en ${atestadosDir.absolutePath}")
                    }
                    // Log files in directory for debugging
                    atestadosDir.listFiles()?.forEach { file ->
                        Log.d(
                            TAG,
                            "Archivo en Atestados: ${file.name}, readable: ${file.canRead()}, size: ${file.length()}"
                        )
                    } ?: Log.d(TAG, "No hay archivos en Atestados o directorio inaccesible")
                    // Crear una DocumentsContract URI
                    val documentsUri =
                        Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FAtestados")
                    // Intent para DocumentsUI
                    val documentsUiIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra("android.content.extra.SHOW_ADVANCED", true)
                        putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                        setPackage("com.google.android.documentsui")
                    }
                    // Intent genérico como fallback
                    val genericIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra("android.content.extra.SHOW_ADVANCED", true)
                        putExtra("android.provider.extra.INITIAL_URI", documentsUri)
                    }
                    // Verificar si DocumentsUI está disponible
                    val documentsUiAvailable =
                        context.packageManager.queryIntentActivities(documentsUiIntent, 0)
                            .isNotEmpty()
                    val chosenIntent =
                        if (documentsUiAvailable) documentsUiIntent else genericIntent
                    // Log apps que pueden manejar el intent
                    val resolveInfoList =
                        context.packageManager.queryIntentActivities(chosenIntent, 0)
                    if (resolveInfoList.isNotEmpty()) {
                        Log.d(TAG, "Apps que pueden manejar ACTION_GET_CONTENT:")
                        resolveInfoList.forEach {
                            Log.d(TAG, "- ${it.activityInfo.packageName}: ${it.activityInfo.name}")
                        }
                    } else {
                        Log.w(TAG, "Ninguna app puede manejar ACTION_GET_CONTENT")
                    }
                    // Mostrar toast de guía
                    Toast.makeText(
                        context,
                        "Busque los PDFs en Documents > Atestados",
                        Toast.LENGTH_LONG
                    ).show()
                    try {
                        context.startActivity(
                            Intent.createChooser(
                                chosenIntent,
                                "Seleccionar aplicación para abrir carpeta"
                            )
                        )
                        Log.d(
                            TAG,
                            "openStorage: Intent lanzado con ACTION_GET_CONTENT, URI: $documentsUri"
                        )
                    } catch (e: ActivityNotFoundException) {
                        Log.w(
                            TAG,
                            "openStorage: No se encontró aplicación para abrir directorio",
                            e
                        )
                        Toast.makeText(
                            context,
                            "No hay administrador de archivos instalado. Instale 'Archivos de Google' desde Google Play.",
                            Toast.LENGTH_LONG
                        ).show()
                        try {
                            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                data =
                                    Uri.parse("market://details?id=com.google.android.apps.nbu.files")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(playStoreIntent)
                        } catch (e: ActivityNotFoundException) {
                            Log.w(TAG, "No se pudo abrir Google Play Store", e)
                            Toast.makeText(
                                context,
                                "No se pudo abrir Google Play. Busque 'Archivos de Google' manualmente.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "openStorage: Error al abrir directorio: ${e.message}", e)
                    Toast.makeText(
                        context,
                        "Error al abrir el directorio: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        if (showDatePickerFechaDiligencias) {
            getDateDialogDiligencias(
                onDateSelected = { fechaSeleccionada ->
                    alcoholemiaDosViewModel.updateFechaInicio(fechaSeleccionada)
                    showDatePickerFechaDiligencias = false
                },
                onDismiss = { showDatePickerFechaDiligencias = false }
            )
        }
        if (isPrintingAtestado) {
            FullScreenProgressIndicator(text = "Imprimiendo atestado...")
        }
        BitmapPreviewDialog(
            bitmap = previewBitmap,
            onConfirm = { showPreviewDialog = false },
            onDismiss = {
                showPreviewDialog = false
                previewBitmap?.recycle()
                previewBitmap = null
                isPrintingAtestado = false
                currentPrintStatus = "Impresión cancelada"
                scope.launch {
                    Toast.makeText(context, "Impresión cancelada", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * Obtiene los datos de ubicación actual usando FusedLocationProviderClient.
 *
 * @param fusedLocationClient Cliente para obtener la ubicación.
 * @param context Contexto de la aplicación.
 * @param viewModel ViewModel para procesar la ubicación.
 * @param onLocationReceived Callback que recibe la descripción de la ubicación.
 */
fun getLocationData(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    viewModel: AlcoholemiaDosViewModel,
    onLocationReceived: (String) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.d(
                            TAG,
                            "Ubicación obtenida: lat=${location.latitude}, lon=${location.longitude}"
                        )
                        var thoroughfare = "Carretera desconocida"
                        var pk = "PK no disponible"
                        var municipio = "Municipio desconocido"
                        var provincia = "Provincia desconocida"

                        // Intentar con Geocoder como respaldo
                        val geocoder = Geocoder(context, Locale.getDefault())
                        try {
                            val addresses =
                                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (addresses?.isNotEmpty() == true) {
                                val address = addresses[0]
                                thoroughfare = address.thoroughfare ?: "Carretera desconocida"
                                val featureName = address.featureName ?: ""
                                pk =
                                    if (featureName.matches(Regex("\\d+\\.?\\d*"))) "PK $featureName" else "PK no disponible"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al usar Geocoder: ${e.message}", e)
                        }

                        // Usar Nominatim para detalles precisos
                        viewModel.viewModelScope.launch {
                            try {
                                val client = OkHttpClient()
                                val url =
                                    "https://nominatim.openstreetmap.org/reverse?format=json&lat=${location.latitude}&lon=${location.longitude}&zoom=16&addressdetails=1"
                                val request = Request.Builder()
                                    .url(url)
                                    .header("User-Agent", "AtestadosApp/1.0")
                                    .build()
                                val response = withContext(Dispatchers.IO) {
                                    client.newCall(request).execute()
                                }
                                if (response.isSuccessful) {
                                    val json = JSONObject(response.body?.string() ?: "{}")
                                    val addressJson = json.optJSONObject("address")
                                    if (addressJson != null) {
                                        thoroughfare = addressJson.optString("ref", thoroughfare)
                                        municipio = addressJson.optString(
                                            "municipality",
                                            addressJson.optString(
                                                "town",
                                                addressJson.optString(
                                                    "city",
                                                    "Municipio desconocido"
                                                )
                                            )
                                        )
                                        if (addressJson.has("village") && municipio == addressJson.optString(
                                                "village"
                                            )
                                        ) {
                                            municipio = addressJson.optString(
                                                "municipality",
                                                "Municipio desconocido"
                                            )
                                        }
                                        provincia =
                                            addressJson.optString("state", "Provincia desconocida")
                                        provincia = provincia.split("/")[0].trim()
                                    }
                                    Log.d(
                                        TAG,
                                        "Nominatim response - Vía: $thoroughfare, Municipio: $municipio, Provincia: $provincia"
                                    )
                                } else {
                                    Log.w(TAG, "Error en Nominatim: ${response.code}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al consultar Nominatim: ${e.message}", e)
                            }

                            municipio = municipio.replace(Regex("[^A-Za-zÀ-ÿ\\s-]"), "").trim()
                            provincia = provincia.replace(Regex("[^A-Za-zÀ-ÿ\\s-]"), "").trim()

                            val locationDetails = "$thoroughfare, $pk, $municipio, $provincia"
                            Log.d(TAG, "LocationDetails generado: $locationDetails")
                            viewModel.updateMunicipio(municipio)
                            viewModel.updateLatitud(location.latitude.toString())
                            viewModel.updateLongitud(location.longitude.toString())
                            viewModel.updateLugarDiligencias(locationDetails)

                            val partidoJudicial = getPartidoJudicial(locationDetails, context)
                            viewModel.updatePartidoJudicial(partidoJudicial)
                            Log.d(TAG, "Partido judicial actualizado: $partidoJudicial")

                            onLocationReceived(locationDetails)
                        }
                    } else {
                        Log.w(TAG, "Ubicación no disponible")
                        onLocationReceived("Ubicación no disponible")
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error al obtener ubicación: ${e.message}", e)
                    onLocationReceived("Error al obtener ubicación: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al obtener ubicación: ${e.message}", e)
            onLocationReceived("Error al obtener la ubicación: ${e.message}")
        }
    } else {
        Log.w(TAG, "Permiso de ubicación no otorgado")
        onLocationReceived("Permiso de ubicación no otorgado")
    }
}

/**
 * Normaliza el nombre del municipio para coincidir con los datos en juzgados.db.
 *
 * @param municipio Nombre del municipio devuelto por Nominatim.
 * @return Municipio normalizado.
 */
private fun normalizeMunicipio(municipio: String): String {
    // Simplemente limpiar caracteres no válidos, ya que "Llanera" es correcto
    return municipio.replace(Regex("[^A-Za-zÀ-ÿ\\s-]"), "").trim()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

/**
 * Obtiene el partido judicial a partir del lugar de diligencias consultando juzgados.db.
 *
 * @param lugarDiligencias Cadena con el lugar de diligencias (formato: "carretera, pk, municipio, provincia").
 * @return Nombre del partido judicial o un mensaje por defecto si no se encuentra.
 */
fun getPartidoJudicial(lugarDiligencias: String, context: Context): String {
    if (lugarDiligencias.isBlank()) {
        Log.w(TAG, "lugarDiligencias está vacío")
        return "no disponible"
    }

    // Extraer el municipio (tercer elemento después de dividir por ", ")
    val partes = lugarDiligencias.split(", ")
    if (partes.size < 3) {
        Log.w(TAG, "Formato de lugarDiligencias inválido: $lugarDiligencias")
        return "no disponible"
    }
    val municipio = partes[2]
    // Validar que municipio contiene solo letras, espacios o guiones
    val municipioRegex = Regex("^[A-Za-zÀ-ÿ\\s-]+$")
    if (!municipioRegex.matches(municipio)) {
        Log.w(TAG, "Municipio inválido: $municipio")
        return "no disponible"
    }

    // Normalizar el municipio
    val normalizedMunicipio = normalizeMunicipio(municipio)
    Log.d(TAG, "Municipio normalizado: $normalizedMunicipio")

    try {
        val db = AccesoBaseDatos(context, "juzgados.db")
        val query = "SELECT partido_judicial FROM partidos_judiciales WHERE municipio = ?"
        val args = arrayOf(normalizedMunicipio)
        val result = db.query(query, args)
        if (result.isNotEmpty()) {
            val partidoJudicial = result[0]["partido_judicial"]?.toString() ?: "no disponible"
            Log.d(
                TAG,
                "Partido judicial encontrado: $partidoJudicial para municipio: $normalizedMunicipio"
            )
            return partidoJudicial
        } else {
            Log.w(TAG, "No se encontró partido judicial para municipio: $normalizedMunicipio")
            return "no disponible"
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error consultando partido judicial: ${e.message}", e)
        return "no disponible"
    }
}

/**
 * Contenido principal de la pantalla de alcoholemia (paso 2).
 * Muestra los campos para fecha, hora, ubicación, opciones de firma y vehículo, y botones de acción.
 *
 * @param modifier Modificador para ajustar el diseño.
 * @param alcoholemiaDosViewModel ViewModel que maneja el estado de la pantalla.
 * @param impresoraViewModel ViewModel para gestionar la configuración de la impresora.
 * @param lecturaDerechosViewModel ViewModel con datos de la lectura de derechos.
 * @param onDatePickerFechaDiligenciasClicked Callback cuando se hace clic en el selector de fecha.
 * @param showDatePickerFechaDiligencias Estado que controla si se muestra el selector de fecha.
 * @param isPrintingAtestado Estado que indica si se está imprimiendo el atestado.
 * @param onPrintAtestadoTrigger Callback para disparar la impresión del atestado.
 * @param onInvalidLugarInvestigacion Callback para manejar lugarInvestigacion inválido.
 * @param onOpenStorage Callback para abrir el directorio de almacenamiento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Alcoholemia02Content(
    modifier: Modifier = Modifier,
    alcoholemiaDosViewModel: AlcoholemiaDosViewModel,
    impresoraViewModel: ImpresoraViewModel,
    lecturaDerechosViewModel: LecturaDerechosViewModel,
    onDatePickerFechaDiligenciasClicked: () -> Unit,
    showDatePickerFechaDiligencias: Boolean,
    isPrintingAtestado: Boolean,
    onPrintAtestadoTrigger: () -> Unit,
    onInvalidLugarInvestigacion: () -> Unit,
    onOpenStorage: () -> Unit
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    // Inicializar cliente de ubicación
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    // Estado para la ubicación actual
    var locationText by remember { mutableStateOf("") }

    // Estados del ViewModel
    val fechaInicio by alcoholemiaDosViewModel.fechaInicio.observeAsState("")
    val horaInicio by alcoholemiaDosViewModel.horaInicio.observeAsState("")
    val lugarCoincide by alcoholemiaDosViewModel.lugarCoincide.observeAsState(false)
    val lugarDiligencias by alcoholemiaDosViewModel.lugarDiligencias.observeAsState("")
    val deseaFirmar by alcoholemiaDosViewModel.deseaFirmar.observeAsState(false)
    val inmovilizaVehiculo by alcoholemiaDosViewModel.inmovilizaVehiculo.observeAsState(false)
    val haySegundoConductor by alcoholemiaDosViewModel.haySegundoConductor.observeAsState(false)
    val nombreSegundoConductor by alcoholemiaDosViewModel.nombreSegundoConductor.observeAsState("")
    val lugarInvestigacion by lecturaDerechosViewModel.lugarInvestigacion.observeAsState("")
    val partidoJudicial by alcoholemiaDosViewModel.partidoJudicial.observeAsState("no disponible")

    var showSignatureDialog by remember { mutableStateOf(false) }
    var signatureType by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Log para cambios en lugarCoincide
    LaunchedEffect(lugarCoincide, lugarInvestigacion) {
        if (lugarCoincide && lugarInvestigacion.isNotBlank()) {
            Log.d(TAG, "lugarCoincide es true, usando lugarInvestigacion: $lugarInvestigacion")
            alcoholemiaDosViewModel.updateLugarDiligencias(lugarInvestigacion)
            val nuevoPartidoJudicial = getPartidoJudicial(lugarInvestigacion, context)
            alcoholemiaDosViewModel.updatePartidoJudicial(nuevoPartidoJudicial)
            Log.d(TAG, "partidoJudicial actualizado a: $nuevoPartidoJudicial")
        } else if (lugarCoincide && lugarInvestigacion.isBlank()) {
            Log.w(TAG, "lugarInvestigacion está vacío aunque lugarCoincide es true")
            onInvalidLugarInvestigacion()
        }
    }

    // Inicializar partido judicial al cargar la pantalla
    LaunchedEffect(Unit) {
        if (lugarCoincide && lugarInvestigacion.isNotBlank()) {
            val nuevoPartidoJudicial = getPartidoJudicial(lugarInvestigacion, context)
            alcoholemiaDosViewModel.updatePartidoJudicial(nuevoPartidoJudicial)
        } else if (lugarDiligencias.isNotBlank()) {
            val nuevoPartidoJudicial = getPartidoJudicial(lugarDiligencias, context)
            alcoholemiaDosViewModel.updatePartidoJudicial(nuevoPartidoJudicial)
        }
    }

    // TimePicker para primera hora
    if (showTimePicker) {
        TimePickerDialogAlcoholemia(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        alcoholemiaDosViewModel.updateHoraInicio(
                            "${timePickerState.hour}:${
                                timePickerState.minute.toString().padStart(2, '0')
                            }"
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
        // Sección Fecha y Hora de Inicio
        Text(
            text = "Fecha y hora de inicio",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomOutlinedTextFieldAlcohol(
                modifier = Modifier.weight(1.2f),
                value = fechaInicio,
                onValueChange = { alcoholemiaDosViewModel.updateFechaInicio(it) },
                label = "Fecha",
                placeholder = "Seleccione la fecha",
                keyboardType = KeyboardType.Decimal,
                leadingIcon = {
                    IconButton(
                        onClick = onDatePickerFechaDiligenciasClicked,
                        modifier = Modifier.size(35.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar_ico),
                            tint = BotonesNormales,
                            contentDescription = "Botón de acceso a calendario"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                modifier = Modifier.weight(0.8f),
                value = horaInicio,
                onValueChange = { alcoholemiaDosViewModel.updateHoraInicio(it) },
                label = { Text("Hora", color = TextoTerciarios) },
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.reloj_ico),
                            contentDescription = "Seleccionar hora"
                        )
                    }
                }
            )
        }

        // Sección Lugar de la investigación
        Text(
            text = "¿El lugar de la investigación coincide con el de instrucción?",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )

        CheckboxOption(
            text = "Lugar coincide",
            checked = lugarCoincide,
            onCheckedChange = { alcoholemiaDosViewModel.updateLugarCoincide(it) }
        )

        CustomTextField(
            value = lugarDiligencias,
            onValueChange = { alcoholemiaDosViewModel.updateLugarDiligencias(it) },
            label = "Carretera, PK, municipio, provincia",
            enabled = !lugarCoincide
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Partido Judicial de $partidoJudicial",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = {
                Log.d(TAG, "Botón 'Obtener Ubicación Actual' presionado")
                scope.launch {
                    Toast.makeText(
                        context,
                        "Espere a que se calcule y se muestre la ubicación",
                        Toast.LENGTH_SHORT
                    ).show()
                    getLocationData(
                        fusedLocationClient,
                        context,
                        alcoholemiaDosViewModel
                    ) { locationDetails ->
                        Log.d(TAG, "Ubicación obtenida: $locationDetails")
                        locationText = locationDetails
                        alcoholemiaDosViewModel.updateLugarDiligencias(locationDetails)
                        Log.d(
                            "Alcoholemia02",
                            "Latitud: ${alcoholemiaDosViewModel.latitud.value}, Longitud: ${alcoholemiaDosViewModel.longitud.value}"
                        )
                    }
                }
            },
            enabled = !lugarCoincide,
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("Obtener Ubicación Actual")
        }

        // Sección Opciones de firma y vehículo
        Text(
            text = "Opciones adicionales",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center,
            fontSize = 20.sp
        )

        CheckboxOption(
            text = "¿Desea firmar?",
            checked = deseaFirmar,
            onCheckedChange = { alcoholemiaDosViewModel.updateDeseaFirmar(it) }
        )

        CheckboxOption(
            text = "¿Se inmoviliza el vehículo?",
            checked = inmovilizaVehiculo,
            onCheckedChange = { alcoholemiaDosViewModel.updateInmovilizaVehiculo(it) }
        )

        CheckboxOption(
            text = "¿Hay segundo conductor?",
            checked = haySegundoConductor,
            onCheckedChange = { alcoholemiaDosViewModel.updateHaySegundoConductor(it) }
        )

        if (haySegundoConductor) {
            CustomTextField(
                value = nombreSegundoConductor,
                onValueChange = { alcoholemiaDosViewModel.updateNombreSegundoConductor(it) },
                label = "Nombre y Apellidos (DNI) segundo conductor"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showSignatureDialog) {
            SignatureCaptureScreen(
                onSignatureCaptured = { filePath ->
                    when (signatureType) {
                        "investigado" -> alcoholemiaDosViewModel.updateFirmaInvestigado(filePath)
                        "segundo_conductor" -> alcoholemiaDosViewModel.updateFirmaSegundoConductor(filePath)
                        "instructor" -> alcoholemiaDosViewModel.updateFirmaInstructor(filePath)
                        "secretario" -> alcoholemiaDosViewModel.updateFirmaSecretario(filePath)
                    }
                    showSignatureDialog = false
                },
                onDismiss = { showSignatureDialog = false }
            )
        }

        // Botones de firma
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (deseaFirmar) {
                Button(
                    onClick = {
                        signatureType = "investigado"
                        showSignatureDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesFirmaAjena,
                        contentColor = TextoBotonesNormales
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        text = "FIRMA DEL INVESTIGADO",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (haySegundoConductor) {
                Button(
                    onClick = {
                        signatureType = "segundo_conductor"
                        showSignatureDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesFirmaAjena,
                        contentColor = TextoBotonesNormales
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        text = "FIRMA SEGUNDO CONDUCTOR",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    signatureType = "instructor"
                    showSignatureDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotonesNormales,
                    contentColor = TextoBotonesNormales
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(
                    text = "FIRMA DEL INSTRUCTOR",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    signatureType = "secretario"
                    showSignatureDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotonesNormales,
                    contentColor = TextoBotonesNormales
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(
                    text = "FIRMA DEL SECRETARIO",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para imprimir atestado
        Button(
            onClick = {
                if (!isPrintingAtestado) {
                    Log.d(TAG, "Botón 'IMPRIMIR ATESTADO' presionado")
                    onPrintAtestadoTrigger()
                }
            },
            enabled = !isPrintingAtestado,
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(
                text = if (isPrintingAtestado) "IMPRIMIENDO..." else "IMPRIMIR ATESTADO",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Botón para abrir almacenamiento
        Button(
            onClick = {
                Log.d(TAG, "Botón 'ABRIR ALMACENAMIENTO' presionado")
                onOpenStorage()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text(
                text = "ABRIR ALMACENAMIENTO",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Barra superior de la pantalla de alcoholemia.
 * Muestra el título "Alcoholemia" y el subtítulo "Datos inicio diligencias".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlcoholemiaTopBar() {
    CenterAlignedTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Alcoholemia",
                    textAlign = TextAlign.Center,
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Datos inicio diligencias",
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
 * Barra inferior de la pantalla de alcoholemia con botones de acción.
 * Incluye botones para guardar y limpiar los datos.
 *
 * @param viewModel ViewModel para manejar las acciones.
 * @param navigateToScreen Función para navegar a otras pantallas.
 */
@Composable
private fun AlcoholemiaBottomBar(
    viewModel: AlcoholemiaDosViewModel,
    navigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = {
                scope.launch {
                    viewModel.guardarDatos(context)
                    navigateToScreen("MainScreen")
                }
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
 * Campo de texto personalizado con estilo específico para la pantalla de alcoholemia.
 *
 * @param value Valor actual del campo.
 * @param onValueChange Callback cuando cambia el valor.
 * @param label Texto de la etiqueta.
 * @param modifier Modificador para ajustar el diseño.
 * @param enabled Estado de habilitación del campo.
 */
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoTerciarios) },
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        placeholder = {
            Text(
                "Introduzca $label",
                color = TextoTerciarios,
                textDecoration = TextDecoration.Underline
            )
        }
    )
}

/**
 * Campo de texto personalizado con icono principal para la pantalla de alcoholemia.
 *
 * @param value Valor actual del campo.
 * @param onValueChange Callback cuando cambia el valor.
 * @param label Texto de la etiqueta.
 * @param placeholder Texto del marcador de posición.
 * @param keyboardType Tipo de teclado a mostrar.
 * @param modifier Modificador para ajustar el diseño.
 * @param leadingIcon Icono principal del campo.
 */
@Composable
private fun CustomOutlinedTextFieldAlcohol(
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

/**
 * Opción de checkbox con estilo personalizado.
 *
 * @param text Texto descriptivo de la opción.
 * @param checked Estado actual del checkbox.
 * @param onCheckedChange Callback cuando cambia el estado.
 */
@Composable
private fun CheckboxOption(
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
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = BotonesNormales,
                uncheckedColor = BotonesSecundarios
            )
        )
        Text(
            text = text,
            color = Color.Black,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Diálogo para seleccionar fecha con formato específico.
 * Muestra un selector de fecha y formatea la selección como "d 'de' MMMM 'de' yyyy".
 *
 * @param onDateSelected Callback cuando se selecciona una fecha.
 * @param onDismiss Callback cuando se cancela el diálogo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getDateDialogDiligencias(
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
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = state)
    }
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