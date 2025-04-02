package com.oscar.atestados.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.utils.PDFToBitmapPrinter
import com.oscar.atestados.utils.PdfToBitmapConverter
import com.oscar.atestados.utils.XmlPrinterHelperZebra
import com.oscar.atestados.viewModel.BluetoothViewModelFactory
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModelFactory
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun OtrosDocumentosScreen(
    navigateToScreen: (String) -> Unit,
    impresoraViewModel: ImpresoraViewModel = viewModel(
        factory = ImpresoraViewModelFactory(
            bluetoothViewModel = viewModel(
                factory = BluetoothViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
            ),
            context = LocalContext.current
        )
    ),
    otrosDocumentosViewModel: OtrosDocumentosViewModel = viewModel(
        factory = OtrosDocumentosViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    var isPrintingAssistance by remember { mutableStateOf(false) }
    var isPrintingTest by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pdfToBitmapPrinter = remember { PDFToBitmapPrinter(context) }

    var currentPrintStatus by remember { mutableStateOf("Iniciando...") }

    // Diálogo de previsualización
    if (showPreviewDialog && previewBitmap != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Previsualización de la etiqueta") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Previsualización de etiqueta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("¿Deseas imprimir esta etiqueta?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPreviewDialog = false
                        scope.launch(Dispatchers.IO) {
                            isPrintingTest = true
                            try {
                                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                                if (!macAddress.isNullOrEmpty()) {
                                    withContext(Dispatchers.Main) { currentPrintStatus = "Enviando a imprimir..." }
                                    val printResult = pdfToBitmapPrinter.printHtmlAsBitmap(
                                        htmlAssetPath = "documents/test_label.html",
                                        macAddress = macAddress,
                                        onStatusUpdate = { status ->
                                            scope.launch(Dispatchers.Main) { currentPrintStatus = status }
                                        }
                                    )
                                    withContext(Dispatchers.Main) {
                                        when (printResult) {
                                            is PDFToBitmapPrinter.PrintResult.Success -> {
                                                currentPrintStatus = "Impresión exitosa: ${printResult.details}"
                                                Toast.makeText(context, "Etiqueta impresa correctamente", Toast.LENGTH_SHORT).show()
                                            }
                                            is PDFToBitmapPrinter.PrintResult.Error -> {
                                                currentPrintStatus = "Error: ${printResult.message}"
                                                Toast.makeText(context, "Error al imprimir: ${printResult.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        currentPrintStatus = "No hay impresora seleccionada"
                                        Toast.makeText(context, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    currentPrintStatus = "Error al imprimir: ${e.message}"
                                    Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) { isPrintingTest = false }
                            }
                        }
                    }
                ) {
                    Text("Imprimir")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showPreviewDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = { OtrosDocumentosTopBar(otrosDocumentosViewModel) },
        bottomBar = { OtrosDocumentosBottomBar(navigateToScreen) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CreaBotonOtrosDoc(
                onClick = { navigateToScreen("InformacionScreen") },
                text = "ACTA INMOVILIZACIÓN",
                mensaje = "Pulse aquí para generar un acta de inmovilización"
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBotonOtrosDoc(
                onClick = { navigateToScreen("CitacionScreen") },
                text = "CITACIÓN JUDICIAL",
                mensaje = "Pulse aquí para generar una citación judicial"
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBotonOtrosDoc(
                onClick = { navigateToScreen("InformacionScreen") },
                text = "DERECHOS",
                mensaje = "Pulse aquí para generar un documento de derechos"
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBotonOtrosDoc(
                onClick = {
                    if (!isPrintingAssistance) {
                        isPrintingAssistance = true
                        currentPrintStatus = "Generando documento..."
                        scope.launch {
                            try {
                                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                                if (!macAddress.isNullOrEmpty()) {
                                    val printResult = pdfToBitmapPrinter.printHtmlAsBitmap(
                                        htmlAssetPath = "documents/asistencia_juridica_gratuita_zebra.html",
                                        macAddress = macAddress,
                                        onStatusUpdate = { status ->
                                            scope.launch(Dispatchers.Main) { currentPrintStatus = status }
                                        }
                                    )
                                    withContext(Dispatchers.Main) {
                                        when (printResult) {
                                            is PDFToBitmapPrinter.PrintResult.Success -> {
                                                currentPrintStatus = "Impresión exitosa: ${printResult.details}"
                                                Toast.makeText(context, "Documento impreso correctamente", Toast.LENGTH_SHORT).show()
                                            }
                                            is PDFToBitmapPrinter.PrintResult.Error -> {
                                                currentPrintStatus = "Error: ${printResult.message}"
                                                Toast.makeText(context, "Error al imprimir: ${printResult.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        currentPrintStatus = "No hay impresora seleccionada"
                                        Toast.makeText(context, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    currentPrintStatus = "Error: ${e.message}"
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) { isPrintingAssistance = false }
                            }
                        }
                    }
                },
                text = if (isPrintingAssistance) "IMPRIMIENDO..." else "INFOR. ASISTENCIA LETRADA",
                mensaje = "Pulse aquí para generar e imprimir información de asistencia letrada",
                enabled = !isPrintingAssistance
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBotonOtrosDoc(
                onClick = {
                    if (!isPrintingTest) {
                        isPrintingTest = true
                        currentPrintStatus = "Generando previsualización..."
                        scope.launch {
                            try {
                                // 1. Generar el PDF
                                val htmlContent = context.assets.open("documents/test_label.html")
                                    .use { it.readBytes().toString(Charsets.UTF_8) }
                                val outputFile = File(context.getExternalFilesDir(null), "etiqueta_prueba_preview.pdf")
                                Log.d("OtrosDocumentosScreen", "PDF generado: ${outputFile.absolutePath}, tamaño: ${outputFile.length()} bytes")

                                // 2. Convertir a Bitmap (usamos la primera página para previsualización)
                                val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(outputFile)

                                if (bitmaps.isNotEmpty() && bitmaps[0] != null) {
                                    val bitmap = bitmaps[0]!!
                                    // Guardar el Bitmap como archivo para inspección
                                    val bitmapFile = File(context.getExternalFilesDir(null), "test_bitmap.png")
                                    FileOutputStream(bitmapFile).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    Log.d("OtrosDocumentosScreen", "Bitmap guardado en: ${bitmapFile.absolutePath}, dimensiones: ${bitmap.width}x${bitmap.height}")

                                    previewBitmap = bitmap
                                    showPreviewDialog = true
                                    currentPrintStatus = "Previsualización lista"
                                } else {
                                    withContext(Dispatchers.Main) {
                                        currentPrintStatus = "Error al generar previsualización"
                                        Toast.makeText(context, "Error al generar la imagen", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    currentPrintStatus = "Error: ${e.message}"
                                    Toast.makeText(context, "Error al generar previsualización: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                if (!showPreviewDialog) {
                                    withContext(Dispatchers.Main) { isPrintingTest = false }
                                }
                            }
                        }
                    }
                },
                text = if (isPrintingTest) "GENERANDO PREVISUALIZACIÓN..." else "PRUEBA IMPRESIÓN",
                mensaje = "Pulse aquí para previsualizar e imprimir una etiqueta de prueba",
                enabled = !isPrintingTest
            )
        }

        if (isPrintingTest) {
            PrintingProgressIndicator(text = currentPrintStatus)
        }
        if (isPrintingAssistance) {
            PrintingProgressIndicator(text = currentPrintStatus)
        }
    }
}

@Composable
private fun PrintingProgressIndicator(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Procesando...",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(all = 2.dp)
            )
        }
    }
}

@Composable
private fun OtrosDocumentosTopBar(viewModel: OtrosDocumentosViewModel) {
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Otros Documentos",
            color = TextoNormales,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Seleccione el documento a generar",
            color = TextoSecundarios,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Impresora enlazada: $selectedPrinter",
            color = TextoSecundarios,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OtrosDocumentosBottomBar(navigateToScreen: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { navigateToScreen("MainScreen") },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("VOLVER")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = { /* TODO: Implement data clearing logic */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("LIMPIAR DATOS")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreaBotonOtrosDoc(
    onClick: () -> Unit,
    text: String,
    mensaje: String,
    enabled: Boolean = true
) {
    val plainTooltipState = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = plainTooltipState,
        tooltip = {
            PlainTooltip {
                Text(mensaje)
            }
        }
    ) {
        Button(
            enabled = enabled,
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            onClick = onClick
        ) {
            Text(text = text, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}