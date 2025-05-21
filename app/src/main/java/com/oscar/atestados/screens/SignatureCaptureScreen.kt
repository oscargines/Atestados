package com.oscar.atestados.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oscar.atestados.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignatureCaptureScreen(
    onSignatureCaptured: (String) -> Unit, // Devuelve la ruta con prefijo file://
    onDismiss: () -> Unit
) {
    var path by remember { mutableStateOf(Path()) }
    var forceRedraw by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(DpSize(0.dp, 0.dp)) }
    var lastX by remember { mutableStateOf(0f) }
    var lastY by remember { mutableStateOf(0f) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(8.dp),
            color = White
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = BlueGray50,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box {
                        Text(
                            text = "Firme aquí",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .padding(start = 8.dp, top = 8.dp)
                                .align(Alignment.TopStart)
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BlueGray50)
                                .pointerInteropFilter { event ->
                                    val x = event.x
                                    val y = event.y
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            Log.d("SignatureScreen", "Inicio de toque en: ($x, $y)")
                                            path.moveTo(x, y)
                                            lastX = x
                                            lastY = y
                                            forceRedraw++
                                            true
                                        }
                                        MotionEvent.ACTION_MOVE -> {
                                            Log.d("SignatureScreen", "Movimiento a: ($x, $y)")
                                            val dx = kotlin.math.abs(x - lastX)
                                            val dy = kotlin.math.abs(y - lastY)
                                            if (dx >= 4f || dy >= 4f) {
                                                path.quadraticTo(
                                                    lastX,
                                                    lastY,
                                                    (x + lastX) / 2f,
                                                    (y + lastY) / 2f
                                                )
                                                lastX = x
                                                lastY = y
                                                forceRedraw++
                                            }
                                            true
                                        }
                                        MotionEvent.ACTION_UP -> {
                                            Log.d("SignatureScreen", "Fin de toque en: ($x, $y)")
                                            path.lineTo(lastX, lastY)
                                            forceRedraw++
                                            true
                                        }
                                        else -> false
                                    }
                                }
                        ) {
                            canvasSize = with(density) {
                                DpSize(size.width.toDp(), size.height.toDp())
                            }
                            drawPath(
                                path = path,
                                color = FirmaColor,
                                style = Stroke(width = 4f)
                            )
                            @Suppress("UNUSED_EXPRESSION")
                            forceRedraw
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { path = Path(); forceRedraw++ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BotonesSecundarios)
                    ) {
                        Text("Limpiar", color = White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val bitmapWidth = with(density) { canvasSize.width.roundToPx() }
                                val bitmapHeight = with(density) { canvasSize.height.roundToPx() }
                                val bitmap = Bitmap.createBitmap(
                                    bitmapWidth,
                                    bitmapHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(bitmap)
                                canvas.drawColor(android.graphics.Color.parseColor("#ECEFF1"))
                                canvas.drawPath(
                                    path.asAndroidPath(),
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#1A237E")
                                        strokeWidth = 4f
                                        style = android.graphics.Paint.Style.STROKE
                                    }
                                )
                                // Guardar el bitmap como archivo PNG
                                val signatureFile = File(context.cacheDir, "signature_${System.currentTimeMillis()}.png")
                                FileOutputStream(signatureFile).use { outputStream ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                }
                                bitmap.recycle() // Liberar memoria
                                val fileUri = "file://${signatureFile.absolutePath}"
                                Log.d("SignatureScreen", "Firma guardada en: $fileUri")
                                onSignatureCaptured(fileUri)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BotonesNormales)
                    ) {
                        Text("Guardar", color = White)
                    }
                }
            }
        }
    }
}