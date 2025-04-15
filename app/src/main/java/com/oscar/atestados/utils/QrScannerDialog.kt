package com.oscar.atestados.utils

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.oscar.atestados.data.DniData

/**
 * Diálogo componible que maneja el escaneo de códigos QR y el parseo de datos de DNI.
 *
 * Este componente integra la cámara del dispositivo para escanear códigos QR y parsea automáticamente
 * el contenido en datos estructurados [DniData] usando [QrDataParser]. Maneja los permisos de cámara
 * y proporciona retroalimentación al usuario.
 *
 * @sample Ejemplo de uso:
 * ```
 * QrScannerDialog(
 *     onQrCodeScanned = { dniData ->
 *         // Manejar los datos del DNI parseados
 *     },
 *     onDismiss = {
 *         // Manejar el cierre del diálogo
 *     }
 * )
 * ```
 */
@Composable
fun QrScannerDialog(
    /**
     * Callback que se invoca cuando se escanea y parsea correctamente un código QR.
     * @param dniData Los datos del DNI parseados que contienen todos los campos extraídos.
     */
    onQrCodeScanned: (DniData) -> Unit,

    /**
     * Callback que se invoca cuando el usuario cierra el diálogo.
     */
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val qrDataParser = remember { QrDataParser() }

    // Verificar permisos al iniciar
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            errorMessage = "Se requieren permisos de cámara para escanear QR"
            Log.e("QrScannerDialog", "Permiso de cámara no otorgado")
        } else {
            Log.d("QrScannerDialog", "Permiso de cámara otorgado, iniciando escáner ZXing")
        }
    }

    // Mostrar diálogo de solicitud de permisos si es necesario
    if (!hasCameraPermission) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Permiso Requerido") },
            text = { Text("Esta función requiere acceso a la cámara. Por favor, otorgue el permiso en la configuración.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        )
        return
    }

    /**
     * Diálogo principal del escáner QR que contiene la vista previa de la cámara.
     */
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escanear Código QR") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                /**
                 * Vista personalizada de Android que implementa el escáner QR usando la biblioteca ZXing.
                 */
                AndroidView(
                    factory = { ctx ->
                        val barcodeView = DecoratedBarcodeView(ctx).apply {
                            // Configurar decodificador solo para códigos QR
                            val formats = listOf(BarcodeFormat.QR_CODE)
                            decoderFactory = DefaultDecoderFactory(formats)

                            // Optimizar configuración del escáner
                            cameraSettings.isAutoFocusEnabled = true
                            cameraSettings.isContinuousFocusEnabled = true
                            cameraSettings.requestedCameraId = -1 // Usar cámara trasera

                            // Establecer layout a tamaño completo
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )

                            /**
                             * Callback que maneja los resultados del escaneo QR.
                             */
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    val qrContent = result.text
                                    if (qrContent != null) {
                                        Log.d("QrScannerDialog", "QR detectado: $qrContent")
                                        // Parsear contenido QR a DniData
                                        val dniData = qrDataParser.parseQrContent(qrContent)
                                        onQrCodeScanned(dniData)
                                        pause() // Pausar después de escaneo exitoso
                                    } else {
                                        Log.w("QrScannerDialog", "QR detectado pero sin contenido")
                                    }
                                }

                                override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                                    Log.d("QrScannerDialog", "Puntos detectados: ${resultPoints?.size ?: 0}")
                                }
                            })

                            // Texto de guía para el usuario
                            statusView.text = "Apunte al QR y mantenga la cámara estable"
                            statusView.setTextColor(android.graphics.Color.WHITE)
                        }
                        barcodeView.resume()
                        barcodeView
                    },
                    modifier = Modifier.fillMaxWidth(),
                    update = { barcodeView ->
                        barcodeView.resume()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    /**
     * Diálogo de error que se muestra cuando hay problemas en el proceso de escaneo.
     */
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("Entendido")
                }
            }
        )
    }

    /**
     * Efecto de limpieza que se ejecuta cuando el componente se descompone.
     */
    DisposableEffect(Unit) {
        onDispose {
            Log.d("QrScannerDialog", "Escáner cerrado")
        }
    }
}