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

@Composable
fun QrScannerDialog(
    onQrCodeScanned: (String) -> Unit,
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

    // Verificar permisos al iniciar
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            errorMessage = "Se requieren permisos de cámara para escanear QR"
            Log.e("QrScannerDialog", "Permiso de cámara no otorgado")
        } else {
            Log.d("QrScannerDialog", "Permiso de cámara otorgado, iniciando escáner ZXing")
        }
    }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escanear Código QR") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Área grande para QR complejos
            ) {
                AndroidView(
                    factory = { ctx ->
                        val barcodeView = DecoratedBarcodeView(ctx).apply {
                            // Configurar el decodificador para QR
                            val formats = listOf(BarcodeFormat.QR_CODE)
                            decoderFactory = DefaultDecoderFactory(formats)

                            // Optimizar para QR grandes
                            cameraSettings.isAutoFocusEnabled = true
                            cameraSettings.isContinuousFocusEnabled = true // Enfoque continuo
                            cameraSettings.requestedCameraId = -1 // Cámara trasera

                            // Ajustar el tamaño del escáner
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )

                            // Callback para manejar resultados
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    val qrContent = result.text
                                    if (qrContent != null) {
                                        Log.d("QrScannerDialog", "QR detectado: $qrContent")
                                        onQrCodeScanned(qrContent)
                                        pause() // Pausar tras detectar
                                    } else {
                                        Log.w("QrScannerDialog", "QR detectado pero sin contenido")
                                    }
                                }

                                override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                                    Log.d("QrScannerDialog", "Puntos detectados: ${resultPoints?.size ?: 0}")
                                }
                            })

                            // Mensaje para el usuario
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

    DisposableEffect(Unit) {
        onDispose {
            Log.d("QrScannerDialog", "Escáner cerrado")
        }
    }
}