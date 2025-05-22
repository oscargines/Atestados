package com.oscar.atestados.ui.composables

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales

/**
 * Diálogo que muestra los campos requeridos faltantes antes de realizar una acción.
 *
 * @param missingFields Lista de campos faltantes.
 * @param onDismiss Callback cuando se cancela el diálogo.
 * @param navigateToScreen Función para navegar a otras pantallas.
 */
@Composable
fun MissingFieldsDialog(
    missingFields: List<String>,
    onDismiss: () -> Unit,
    navigateToScreen: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Campos requeridos faltantes") },
        text = {
            Column {
                Text("Por favor, complete los siguientes campos:")
                Spacer(modifier = Modifier.height(8.dp))
                missingFields.forEach { field ->
                    Text("• $field")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (missingFields.contains("Nombre y apellidos") || missingFields.contains("Documento de identidad")) {
                    navigateToScreen("Alcoholemia01Screen")
                } else if (missingFields.contains("TIP del instructor")) {
                    navigateToScreen("GuardiasScreen")
                } else {
                    onDismiss()
                }
            }) {
                Text("Corregir")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Indicador de progreso a pantalla completa con texto descriptivo.
 *
 * @param text Texto a mostrar junto al indicador de progreso.
 */
@Composable
fun FullScreenProgressIndicator(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = BotonesNormales,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Diálogo para previsualizar un bitmap antes de imprimir.
 *
 * @param bitmap Bitmap a previsualizar.
 * @param onConfirm Callback cuando se confirma la acción.
 * @param onDismiss Callback cuando se cancela el diálogo.
 */
@Composable
fun BitmapPreviewDialog(
    bitmap: Bitmap?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (bitmap != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Previsualización del documento",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BotonesNormales,
                                contentColor = TextoBotonesNormales
                            )
                        ) {
                            Text("Imprimir")
                        }
                    }
                }
            }
        }
    }
}