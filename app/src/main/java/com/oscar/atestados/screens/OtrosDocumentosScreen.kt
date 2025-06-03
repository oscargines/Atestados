package com.oscar.atestados.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.oscar.atestados.utils.StorageUtils.openStorageDirectory
import com.oscar.atestados.utils.StorageUtils.refreshAtestadosFolder
import com.oscar.atestados.viewModel.BluetoothViewModelFactory
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModelFactory
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "OtrosDocumentosScreen"
private const val PRINT_TIMEOUT_MS = 10000L // 10 segundos

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
    var isPrinting by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmaps by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }
    var currentPrintStatus by remember { mutableStateOf("Iniciando...") }

    // Temporizador para cerrar diálogo y progreso después de 10 segundos
    LaunchedEffect(isPrinting, showPreviewDialog) {
        if (isPrinting && !showPreviewDialog) {
            delay(PRINT_TIMEOUT_MS)
            if (isPrinting) {
                isPrinting = false
                showPreviewDialog = false
                previewBitmaps.forEach { it?.recycle() }
                previewBitmaps = emptyList()
                currentPrintStatus = "Tiempo de espera agotado"
                Toast.makeText(context, "Tiempo de espera agotado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Manejar la impresión tras confirmar el diálogo
    LaunchedEffect(showPreviewDialog) {
        if (!showPreviewDialog && isPrinting && previewBitmaps.isNotEmpty()) {
            try {
                val macAddress = impresoraViewModel.getSelectedPrinterMac()
                    ?: throw Exception("No hay impresora seleccionada")

                currentPrintStatus = "Enviando a imprimir..."
                val printResult = PDFToBitmapPrinter(context).printHtmlAsBitmap(
                    htmlAssetPath = "documents/asistencia_juridica_gratuita_zebra.html",
                    macAddress = macAddress,
                    onStatusUpdate = { status ->
                        scope.launch(Dispatchers.Main) { currentPrintStatus = status }
                    }
                )

                when (printResult) {
                    is PDFToBitmapPrinter.PrintResult.Success -> {
                        currentPrintStatus = "Impresión exitosa"
                        Toast.makeText(
                            context,
                            "Documento impreso correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
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
            } catch (e: Exception) {
                currentPrintStatus = "Error al imprimir: ${e.message}"
                Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error en impresión: ${e.message}", e)
            } finally {
                // Reciclar todos los bitmaps
                previewBitmaps.forEach { it?.recycle() }
                previewBitmaps = emptyList()
                isPrinting = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { OtrosDocumentosTopBar(otrosDocumentosViewModel) },
        bottomBar = { OtrosDocumentosBottomBar(navigateToScreen) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
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
                    if (!isPrinting) {
                        isPrinting = true
                        currentPrintStatus = "Generando documento..."
                        scope.launch(Dispatchers.IO) {
                            try {
                                val htmlContent =
                                    context.assets.open("documents/asistencia_juridica_gratuita_zebra.html")
                                        .use { it.readBytes().toString(Charsets.UTF_8) }

                                // Generar PDF temporal para previsualización
                                currentPrintStatus = "Generando previsualización..."
                                val previewFile = File.createTempFile(
                                    "asistencia_preview",
                                    ".pdf",
                                    context.cacheDir
                                )
                                val zebraPrinter = PDFLabelPrinterZebra(context)
                                zebraPrinter.generarEtiquetaPdf(htmlContent, previewFile)

                                if (!previewFile.exists() || previewFile.length() == 0L) {
                                    throw Exception("Error al generar PDF para previsualización")
                                }

                                // Convertir a bitmaps
                                val bitmaps =
                                    PdfToBitmapConverter.convertAllPagesToBitmaps(previewFile)
                                if (bitmaps.isNotEmpty() && bitmaps.any { it != null }) {
                                    withContext(Dispatchers.Main) {
                                        previewBitmaps = bitmaps
                                        showPreviewDialog = true
                                        currentPrintStatus = "Mostrando previsualización"
                                    }
                                } else {
                                    throw Exception("Error al generar imágenes para previsualización")
                                }

                                // Limpiar archivo temporal
                                previewFile.delete()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    currentPrintStatus = "Error: ${e.message}"
                                    Toast.makeText(
                                        context,
                                        "Error al generar documento: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    isPrinting = false
                                    Log.e(TAG, "Error en generación de documento: ${e.message}", e)
                                }
                            }
                        }
                    }
                },
                text = if (isPrinting) "GENERANDO..." else "INFOR. ASISTENCIA LETRADA",
                mensaje = "Pulse aquí para previsualizar e imprimir información de asistencia letrada",
                enabled = !isPrinting
            )

            Spacer(modifier = Modifier.height(70.dp))

            CreaBotonOtrosDoc(
                onClick = {
                    refreshAtestadosFolder(context)
                    openStorageDirectory(context)
                },
                text = "ABRIR ALMACENAMIENTO",
                mensaje = "Pulse aquí para abrir el directorio de almacenamiento Documents/Atestados"
            )
        }

        // Mostrar diálogo de previsualización si está activo
        if (showPreviewDialog) {
            BitmapPreviewDialogCompact(
                bitmaps = previewBitmaps,
                onConfirm = {
                    showPreviewDialog = false
                    // La lógica de impresión ya está en el LaunchedEffect
                },
                onDismiss = {
                    showPreviewDialog = false
                    previewBitmaps.forEach { it?.recycle() }
                    previewBitmaps = emptyList()
                    isPrinting = false
                    currentPrintStatus = "Impresión cancelada"
                    scope.launch {
                        Toast.makeText(context, "Impresión cancelada", Toast.LENGTH_SHORT).show()
                    }
                },
                onPrintingStarted = {
                    isPrinting = true
                    currentPrintStatus = "Iniciando impresión..."
                }
            )
        }

        // Mostrar el indicador de progreso solo si está imprimiendo y no hay diálogo
        if (isPrinting && !showPreviewDialog) {
            FullScreenProgressIndicator(text = currentPrintStatus)
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