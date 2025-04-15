package com.oscar.atestados.screens

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.utils.PDFLabelPrinterZebra
import com.oscar.atestados.utils.PDFToBitmapPrinter
import com.oscar.atestados.utils.PdfToBitmapConverter
import com.oscar.atestados.viewModel.BluetoothViewModelFactory
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModelFactory
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val scope = rememberCoroutineScope()
    var isPrintingAssistance by remember { mutableStateOf(false) }
    var isPrintingTest by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf<String?>(null) } // "assistance" o "test"
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPrintStatus by remember { mutableStateOf("Iniciando...") }
    val snackbarHostState = remember { SnackbarHostState() }
    val pdfToBitmapPrinter = remember { PDFToBitmapPrinter(context) }

    // Manejar la impresión tras confirmar el diálogo
    LaunchedEffect(showPreviewDialog, isPrintingAssistance, isPrintingTest) {
        if (showPreviewDialog == null && (isPrintingAssistance || isPrintingTest) && previewBitmap != null) {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                if (!macAddress.isNullOrEmpty()) {
                    val htmlAssetPath = when {
                        isPrintingAssistance -> "documents/asistencia_juridica_gratuita_zebra.html"
                        isPrintingTest -> "documents/test_label.html"
                        else -> throw IllegalStateException("Estado no reconocido")
                    }
                    currentPrintStatus = "Enviando a imprimir..."
                    val printResult = pdfToBitmapPrinter.printHtmlAsBitmap(
                        htmlAssetPath = htmlAssetPath,
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
                        Toast.makeText(context, "Por favor, seleccione una impresora", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentPrintStatus = "Error al imprimir: ${e.message}"
                    Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("OtrosDocumentosScreen", "Error en impresión: ${e.message}", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    previewBitmap?.recycle()
                    previewBitmap = null
                    if (isPrintingAssistance) isPrintingAssistance = false
                    if (isPrintingTest) isPrintingTest = false
                }
            }
        }
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
                .padding(paddingValues) // Corregido: padingValues -> paddingValues
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
                        currentPrintStatus = "Generando previsualización..."
                        scope.launch(Dispatchers.IO) {
                            try {
                                val htmlContent = context.assets.open("documents/asistencia_juridica_gratuita_zebra.html")
                                    .use { it.readBytes().toString(Charsets.UTF_8) }
                                val outputFile = File(context.getExternalFilesDir(null), "asistencia_preview.pdf")
                                if (outputFile.exists()) outputFile.delete()
                                val pdfLabelPrinter = PDFLabelPrinterZebra(context)
                                pdfLabelPrinter.generarEtiquetaPdf(htmlContent, outputFile)
                                currentPrintStatus = "PDF generado para previsualización"

                                val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(outputFile)
                                if (bitmaps.isNotEmpty() && bitmaps[0] != null) {
                                    previewBitmap = bitmaps[0]
                                    showPreviewDialog = "assistance"
                                    currentPrintStatus = "Mostrando previsualización"
                                } else {
                                    withContext(Dispatchers.Main) {
                                        currentPrintStatus = "Error al generar previsualización"
                                        Toast.makeText(context, "Error al generar la imagen", Toast.LENGTH_SHORT).show()
                                        isPrintingAssistance = false
                                    }
                                }
                                // Limpiar archivo temporal
                                outputFile.delete()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    currentPrintStatus = "Error: ${e.message}"
                                    Toast.makeText(context, "Error al generar previsualización: ${e.message}", Toast.LENGTH_LONG).show()
                                    isPrintingAssistance = false
                                    Log.e("OtrosDocumentosScreen", "Error en previsualización: ${e.message}", e)
                                }
                            }
                        }
                    }
                },
                text = if (isPrintingAssistance) "GENERANDO..." else "INFOR. ASISTENCIA LETRADA",
                mensaje = "Pulse aquí para previsualizar e imprimir información de asistencia letrada",
                enabled = !isPrintingAssistance
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBotonOtrosDoc(
                onClick = {
                    if (!isPrintingTest) {
                        isPrintingTest = true
                        currentPrintStatus = "Generando previsualización..."
                        scope.launch(Dispatchers.IO) {
                            try {
                                val htmlContent = context.assets.open("documents/test_label.html")
                                    .use { it.readBytes().toString(Charsets.UTF_8) }
                                val outputFile = File(context.getExternalFilesDir(null), "etiqueta_prueba_preview.pdf")
                                if (outputFile.exists()) outputFile.delete()
                                val pdfLabelPrinter = PDFLabelPrinterZebra(context)
                                pdfLabelPrinter.generarEtiquetaPdf(htmlContent, outputFile)
                                currentPrintStatus = "PDF generado para previsualización"

                                val bitmaps = PdfToBitmapConverter.convertAllPagesToBitmaps(outputFile)
                                if (bitmaps.isNotEmpty() && bitmaps[0] != null) {
                                    previewBitmap = bitmaps[0]
                                    showPreviewDialog = "test"
                                    currentPrintStatus = "Mostrando previsualización"
                                } else {
                                    withContext(Dispatchers.Main) {
                                        currentPrintStatus = "Error al generar previsualización"
                                        Toast.makeText(context, "Error al generar la imagen", Toast.LENGTH_SHORT).show()
                                        isPrintingTest = false
                                    }
                                }
                                // Limpiar archivo temporal
                                outputFile.delete()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    currentPrintStatus = "Error: ${e.message}"
                                    Toast.makeText(context, "Error al generar previsualización: ${e.message}", Toast.LENGTH_LONG).show()
                                    isPrintingTest = false
                                    Log.e("OtrosDocumentosScreen", "Error en previsualización: ${e.message}", e)
                                }
                            }
                        }
                    }
                },
                text = if (isPrintingTest) "GENERANDO..." else "PRUEBA IMPRESIÓN",
                mensaje = "Pulse aquí para previsualizar e imprimir una etiqueta de prueba",
                enabled = !isPrintingTest
            )
        }

        if (isPrintingAssistance || isPrintingTest) {
            PrintingProgressIndicator(text = currentPrintStatus)
        }
    }

    // Diálogo de previsualización renombrado para evitar conflictos
    CustomBitmapPreviewDialog(
        bitmap = previewBitmap,
        onConfirm = {
            showPreviewDialog = null // Dispara la impresión
        },
        onDismiss = {
            showPreviewDialog = null
            previewBitmap?.recycle()
            previewBitmap = null
            if (isPrintingAssistance) isPrintingAssistance = false
            if (isPrintingTest) isPrintingTest = false
            currentPrintStatus = "Impresión cancelada"
            scope.launch {
                Toast.makeText(context, "Impresión cancelada", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

// Diálogo de previsualización renombrado
@Composable
fun CustomBitmapPreviewDialog(
    bitmap: Bitmap?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (bitmap != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Previsualización") },
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Previsualización del documento",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("OtrosDocumentosScreen", "Botón Imprimir pulsado en diálogo")
                        onConfirm()
                    }
                ) {
                    Text("Imprimir")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
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