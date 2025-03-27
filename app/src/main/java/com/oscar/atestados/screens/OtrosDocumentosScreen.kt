package com.oscar.atestados.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.utils.PDFCreaterHelper
import com.oscar.atestados.utils.PDFCreaterHelperZebra
import com.oscar.atestados.utils.ZebraPrinterHelper
import com.oscar.atestados.viewModel.BluetoothViewModelFactory
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModelFactory
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModelFactory
import kotlinx.coroutines.launch
import java.io.File

/**
 * Pantalla que muestra opciones para generar e imprimir documentos varios.
 * Incluye botones para generar actas, citaciones, derechos y asistencia letrada.
 *
 * @param navigateToScreen Función de navegación para cambiar a otra pantalla.
 * @param impresoraViewModel ViewModel para gestionar la impresora.
 * @param otrosDocumentosViewModel ViewModel que gestiona el estado de la pantalla.
 */
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pdfCreaterHelper = remember { PDFCreaterHelper() }
    val pdfCreaterHelperZebra = remember { PDFCreaterHelperZebra() }
    val zebraPrinterHelper = remember { ZebraPrinterHelper(context) }

    // Ubicación fija para el PDF A4
    val fixedA4File = File(context.getExternalFilesDir(null), "Documento_Otros.pdf")

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
                        scope.launch {
                            try {
                                // Cargar el archivo HTML desde assets/documents
                                val htmlAssetPath = "documents/asistencia_juridica_gratuita.html"
                                val htmlInputStream = context.assets.open(htmlAssetPath)
                                val htmlContent = htmlInputStream.readBytes().toString(Charsets.UTF_8)
                                htmlInputStream.close()
                                val htmlFile = File(context.cacheDir, "asistencia_juridica_gratuita_temp.html").apply {
                                    writeText(htmlContent)
                                }

                                // Mapa de reemplazos (puedes ajustarlo según el contenido del HTML)
                                val replacements = mapOf<String, String>()

                                // Generar y sobrescribir PDF A4
                                if (fixedA4File.exists()) fixedA4File.delete()
                                val a4File = pdfCreaterHelper.convertHtmlToPdf(context, htmlFile, replacements, "Asistencia_Juridica_Gratuita")
                                a4File.copyTo(fixedA4File, overwrite = true)

                                // Abrir el PDF A4
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fixedA4File)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(intent, "Abrir PDF con"))

                                // Generar Bitmap para Zebra
                                val zebraBitmap = pdfCreaterHelperZebra.convertHtmlToImage(context, htmlFile, replacements)
                                if (zebraBitmap != null) {
                                    val macAddress = impresoraViewModel.getSelectedPrinterMac()
                                    if (!macAddress.isNullOrEmpty()) {
                                        val printResult = zebraPrinterHelper.printBitmap(macAddress, zebraBitmap)
                                        printResult.onSuccess {
                                            Toast.makeText(context, "Impresión enviada correctamente", Toast.LENGTH_SHORT).show()
                                            Log.d("OtrosDocumentosScreen", "Impresión Zebra exitosa")
                                        }.onFailure { e ->
                                            Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
                                            Log.e("OtrosDocumentosScreen", "Error al imprimir en Zebra: ${e.message}")
                                        }
                                    } else {
                                        Log.w("OtrosDocumentosScreen", "No hay impresora seleccionada")
                                        Toast.makeText(context, "No hay impresora seleccionada", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    throw Exception("No se pudo generar el Bitmap para Zebra")
                                }

                                // Limpiar archivo temporal
                                htmlFile.delete()
                            } catch (e: Exception) {
                                Log.e("OtrosDocumentosScreen", "Error general: ${e.message}", e)
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isPrintingAssistance = false
                            }
                        }
                    }
                },
                text = if (isPrintingAssistance) "IMPRIMIENDO..." else "INFOR. ASISTENCIA LETRADA",
                mensaje = "Pulse aquí para imprimir y visualizar información de asistencia letrada",
                enabled = !isPrintingAssistance
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBotonOtrosDoc(
                onClick = {
                    if (!isPrintingTest) {
                        Log.d("OtrosDocumentosScreen", "Iniciando impresión de prueba")
                        isPrintingTest = true
                        scope.launch {
                            impresoraViewModel.printFile("ActaCitacion.txt")
                            isPrintingTest = false
                        }
                    }
                },
                text = if (isPrintingTest) "IMPRIMIENDO..." else "PRUEBA IMPRESIÓN",
                mensaje = "Pulse aquí para imprimir una etiqueta de prueba",
                enabled = !isPrintingTest
            )
        }
        if (isPrintingTest || isPrintingAssistance) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Procesando...",
                        color = Color.Black,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Abre un archivo PDF utilizando una intención en Android.
 *
 * @param context Contexto de la aplicación.
 * @param pdfFile Archivo PDF a abrir.
 */
private fun openPdf(context: Context, pdfFile: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        Log.d("OtrosDocumentosScreen", "Intentando abrir PDF con URI: $uri")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir PDF con"))
        Log.d("OtrosDocumentosScreen", "Intento de apertura de PDF enviado")
    } catch (e: Exception) {
        Log.e("OtrosDocumentosScreen", "Error al abrir PDF: ${e.message}", e)
        Toast.makeText(context, "Error al abrir el PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Barra superior de la pantalla OtrosDocumentosScreen.
 * Muestra el título, una descripción y la impresora seleccionada.
 *
 * @param viewModel ViewModel que proporciona el estado de la impresora seleccionada.
 */
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

/**
 * Barra inferior de la pantalla OtrosDocumentosScreen.
 * Contiene botones para volver y limpiar datos.
 *
 * @param navigateToScreen Función de navegación para volver a la pantalla principal.
 */
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

/**
 * Crea un botón personalizado con un tooltip para la pantalla OtrosDocumentosScreen.
 *
 * @param onClick Acción a realizar cuando se presiona el botón.
 * @param text Texto a mostrar en el botón.
 * @param mensaje Mensaje a mostrar en el tooltip.
 * @param enabled Indica si el botón está habilitado o no (por defecto true).
 */
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