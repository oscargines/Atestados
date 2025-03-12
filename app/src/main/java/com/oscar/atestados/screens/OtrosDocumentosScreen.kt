package com.oscar.atestados.screens

import com.oscar.atestados.utils.ZebraPrinterHelper
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pantalla que muestra opciones para generar e imprimir documentos varios.
 * Incluye botones para generar actas, citaciones, derechos y una prueba de impresión.
 *
 * @param navigateToScreen Función de navegación para cambiar a otra pantalla.
 * @param otrosDocumentosViewModel ViewModel que gestiona el estado de la pantalla.
 */
@Composable
fun OtrosDocumentosScreen(
    navigateToScreen: (String) -> Unit,
    otrosDocumentosViewModel: OtrosDocumentosViewModel
) {
    val context = LocalContext.current
    val printerHelper = remember { ZebraPrinterHelper(context) }
    var isPrintingTest by remember { mutableStateOf(false) } // Para "PRUEBA IMPRESIÓN"
    var isPrintingAssistance by remember { mutableStateOf(false) } // Para "INFOR. ASISTENCIA LETRADA"
    var printResultTest by remember { mutableStateOf<Result<String>?>(null) } // Resultado de prueba
    var printResultAssistance by remember { mutableStateOf<Result<String>?>(null) } // Resultado de asistencia
    var triggerPrintTest by remember { mutableStateOf(false) } // Trigger para prueba
    var triggerPrintAssistance by remember { mutableStateOf(false) } // Trigger para asistencia
    val viewModel: OtrosDocumentosViewModel = viewModel(factory = OtrosDocumentosViewModelFactory(context))

    // Ejecutar la impresión de "prueba.prn" cuando triggerPrintTest cambia a true
    LaunchedEffect(triggerPrintTest) {
        if (triggerPrintTest) {
            isPrintingTest = true
            withContext(Dispatchers.IO) {
                printResultTest = printerHelper.printFromAsset("prueba.prn")
            }
            isPrintingTest = false
            triggerPrintTest = false
        }
    }

    // Ejecutar la impresión de "info_asistencia_jurica.prn" cuando triggerPrintAssistance cambia a true
    LaunchedEffect(triggerPrintAssistance) {
        if (triggerPrintAssistance) {
            isPrintingAssistance = true
            withContext(Dispatchers.IO) {
                printResultAssistance = printerHelper.printFromAsset("info_asistencia_juridica.prn")
            }
            isPrintingAssistance = false
            triggerPrintAssistance = false
        }
    }

    // Mostrar resultado de la impresión de "prueba.prn"
    printResultTest?.let { result ->
        LaunchedEffect(result) {
            result.onSuccess { status ->
                Toast.makeText(context, "Impresión exitosa (prueba): $status", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Toast.makeText(context, "Error (prueba): ${exception.message}", Toast.LENGTH_LONG).show()
            }
            printResultTest = null
        }
    }

    // Mostrar resultado de la impresión de "info_asistencia_jurica.prn"
    printResultAssistance?.let { result ->
        LaunchedEffect(result) {
            result.onSuccess { status ->
                Toast.makeText(context, "Impresión exitosa (asistencia): $status", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Toast.makeText(context, "Error (asistencia): ${exception.message}", Toast.LENGTH_LONG).show()
            }
            printResultAssistance = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { OtrosDocumentosTopBar(viewModel) },
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
                        triggerPrintAssistance = true
                    }
                },
                text = if (isPrintingAssistance) "IMPRIMIENDO..." else "INFOR. ASISTENCIA LETRADA",
                mensaje = "Pulse aquí para imprimir información de asistencia letrada",
                enabled = !isPrintingAssistance
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBotonOtrosDoc(
                onClick = {
                    if (!isPrintingTest) {
                        isPrintingTest = true
                        triggerPrintTest = true
                    }
                },
                text = if (isPrintingTest) "IMPRIMIENDO..." else "PRUEBA IMPRESIÓN",
                mensaje = "Pulse aquí para imprimir una etiqueta de prueba",
                enabled = !isPrintingTest
            )
        }
        // Mostrar CircularProgressIndicator mientras se imprime cualquiera de las etiquetas
        if (isPrintingTest || isPrintingAssistance) {
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
        }
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