package com.oscar.atestados.screens

import android.content.Intent
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
import androidx.core.content.FileProvider
import com.itextpdf.io.source.ByteArrayOutputStream
import com.oscar.atestados.R
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.utils.PDFCreaterHelper
import com.oscar.atestados.utils.PDFCreaterHelperZebra
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.viewModel.CitacionViewModel
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
 * Pantalla para registrar una citación judicial.
 *
 * Esta pantalla muestra un formulario con campos para registrar información relevante sobre
 * la citación, como provincia, localidad, juzgado, fecha y hora, y permite imprimir una etiqueta.
 *
 * @param navigateToScreen Función lambda que recibe una [String] para navegar a otra pantalla.
 * @param citacionViewModel ViewModel que gestiona el estado y la lógica de los datos.
 */
@Composable
fun CitacionScreen(
    navigateToScreen: (String) -> Unit,
    citacionViewModel: CitacionViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var generateResult by remember { mutableStateOf<Result<String>?>(null) }
    var triggerGenerate by remember { mutableStateOf(false) }

    Log.v(TAG, "CitacionScreen iniciada")

    LaunchedEffect(triggerGenerate) {
        if (triggerGenerate) {
            Log.i(TAG, "Iniciando generación de PDFs")
            isGenerating = true
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Cargando archivo HTML desde assets/documents/acta_citacion.html")
                    val htmlFile = File(context.cacheDir, "acta_citacion.html").apply {
                        context.assets.open("documents/acta_citacion.html").use { input ->
                            outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    Log.d(TAG, "Archivo HTML copiado a caché: ${htmlFile.absolutePath}")

                    val replacements = mapOf(
                        "lugar" to "Desconocido",
                        "termino_municipal" to (citacionViewModel.localidad.value ?: ""),
                        "partido_judicial" to (citacionViewModel.localidad.value ?: ""),
                        "hora_diligencia" to (citacionViewModel.hora.value ?: ""),
                        "fecha_diligencia" to (citacionViewModel.fechaInicio.value ?: ""),
                        "abogado" to "",
                        "num_colegiado" to "",
                        "colegio_abogados" to "",
                        "telefonema" to "",
                        "tip_instructor" to "",
                        "tip_secretario" to ""
                    )
                    Log.d(TAG, "Reemplazos preparados: $replacements")

                    Log.i(TAG, "Generando PDF A4")
                    val pdfA4Helper = PDFCreaterHelper()
                    val pdfA4File = pdfA4Helper.convertHtmlToPdf(context, htmlFile, replacements, "citacion_a4")
                    Log.i(TAG, "PDF A4 generado en: ${pdfA4File.absolutePath}")

                    Log.i(TAG, "Generando PDF para Zebra")
                    val pdfZebraHelper = PDFCreaterHelperZebra()
                    val zebraBitmap = pdfZebraHelper.convertHtmlToImage(context, htmlFile, replacements)
                    val zebraFile = zebraBitmap?.let { bitmap ->
                        Log.d(TAG, "Bitmap generado para Zebra, dimensiones: ${bitmap.width}x${bitmap.height}")
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                        val bitmapByteArray = byteArrayOutputStream.toByteArray()
                        Log.d(TAG, "Bitmap convertido a ByteArray, tamaño: ${bitmapByteArray.size} bytes")

                        val file = File(context.getExternalFilesDir(null), "citacion_zebra.pdf")
                        val writer = com.itextpdf.kernel.pdf.PdfWriter(file)
                        val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
                        val document = com.itextpdf.layout.Document(pdf)
                        val imageData = com.itextpdf.io.image.ImageDataFactory.create(bitmapByteArray)
                        val image = com.itextpdf.layout.element.Image(imageData)
                        document.add(image)
                        document.close()
                        Log.i(TAG, "PDF Zebra generado en: ${file.absolutePath}")
                        file
                    } ?: run {
                        Log.w(TAG, "No se pudo generar el Bitmap para Zebra")
                        null
                    }

                    generateResult = Result.success("PDFs generados con éxito")

                    // Abrir los PDFs generados
                    withContext(Dispatchers.Main) {
                        openPdf(context, pdfA4File)
                        zebraFile?.let { openPdf(context, it) }
                    }

                    Log.i(TAG, "Generación de PDFs completada con éxito")
                } catch (e: Exception) {
                    Log.e(TAG, "Error durante la generación de PDFs: ${e.message}", e)
                    generateResult = Result.failure(e)
                }
            }
            isGenerating = false
            triggerGenerate = false
            Log.d(TAG, "Proceso de generación finalizado, isGenerating: $isGenerating")
        }
    }

    generateResult?.let { result ->
        LaunchedEffect(result) {
            result.onSuccess { status ->
                Log.i(TAG, "Resultado de generación exitoso: $status")
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Log.e(TAG, "Error en generación: ${exception.message}", exception)
                Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
            generateResult = null
            Log.d(TAG, "Resultado procesado y reseteado")
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
                Log.i(TAG, "Botón IMPRIMIR pulsado, iniciando generación")
                triggerGenerate = true
            },
            isGenerating = isGenerating
        )
    }

    if (isGenerating) {
        Log.d(TAG, "Mostrando CircularProgressIndicator")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.7f))
                .wrapContentSize(Alignment.Center)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        Log.v(TAG, "CircularProgressIndicator oculto")
    }
}
/**
 * Barra superior de la pantalla de citación.
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
 *
 * Incluye campos para provincia, localidad, juzgado, fecha y hora de la citación.
 * Muestra información adicional del juzgado seleccionado en "Datos del juzgado".
 *
 * @param modifier Modificador para personalizar el diseño del contenido.
 * @param citacionViewModel ViewModel que gestiona el estado y la lógica de los datos.
 * @param navigateToScreen Función para manejar la navegación.
 * @param onPrintTrigger Función para activar la impresión.
 * @param isPrinting Indica si la impresión está en curso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitacionContent(
    modifier: Modifier = Modifier,
    citacionViewModel: CitacionViewModel,
    navigateToScreen: (String) -> Unit,
    onGenerateTrigger: () -> Unit,
    isGenerating: Boolean
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

    val db = remember { AccesoBaseDatos(context, "juzgados.db", 1) }

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
            Text(if (isGenerating) "GENERANDO..." else "IMPRIMIR")
        }
    }
}

/**
 * Barra inferior con botones para guardar o limpiar los datos.
 */
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

/**
 * Campo desplegable personalizado para seleccionar opciones.
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
 * Diálogo que muestra un selector de tiempo para elegir una hora.
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
 * Campo de texto personalizado para entrada de datos.
 *
 * @param value Valor actual del campo.
 * @param onValueChange Función que se ejecuta cuando cambia el valor.
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
 */
private fun getProvincias(db: AccesoBaseDatos): List<String> {
    return db.query("SELECT Provincia FROM PROVINCIAS").map { it["Provincia"] as String }
}

/**
 * Obtiene la lista de municipios filtrados por provincia desde la base de datos.
 */
private fun getMunicipios(db: AccesoBaseDatos, provincia: String): List<String> {
    if (provincia.isEmpty()) return emptyList()
    val query = "SELECT M.Municipio FROM MUNICIPIOS M JOIN PROVINCIAS P ON M.idProvincia = P.idProvincia JOIN SEDES S ON M.Municipio = S.municipio WHERE P.Provincia = '$provincia' GROUP by M.municipio;"
    return db.query(query).map { it["Municipio"] as String }
}

/**
 * Obtiene la lista de sedes (juzgados) filtradas por municipio desde la base de datos.
 */
private fun getSedes(db: AccesoBaseDatos, municipio: String): List<String> {
    if (municipio.isEmpty()) return emptyList()
    val query = "SELECT nombre FROM SEDES WHERE municipio = '$municipio'"
    return db.query(query).map { it["nombre"] as String }
}

/**
 * Obtiene la información completa de un juzgado seleccionado desde la base de datos.
 */
private fun getJuzgadoInfo(db: AccesoBaseDatos, nombreJuzgado: String): Map<String, String>? {
    if (nombreJuzgado.isEmpty()) return null
    val query = "SELECT * FROM SEDES WHERE nombre = '$nombreJuzgado'"
    val result = db.query(query)
    return if (result.isNotEmpty()) result[0] as Map<String, String> else null
}
// Función para abrir un PDF
private fun openPdf(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.i(TAG, "Intent lanzado para abrir PDF: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e(TAG, "Error al abrir PDF: ${e.message}", e)
        Toast.makeText(context, "No se pudo abrir el PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}