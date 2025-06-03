package com.oscar.atestados.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.oscar.atestados.ui.theme.BlueGray400

@Composable
fun BitmapPreviewDialogCompact(
    bitmaps: List<Bitmap?>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (bitmaps.isEmpty() || bitmaps.all { it == null }) return

    // Agregar log para depurar el número de bitmaps y sus dimensiones
    Log.d("BitmapPreviewDialog", "Número de bitmaps recibidos: ${bitmaps.size}")
    bitmaps.forEachIndexed { index, bitmap ->
        bitmap?.let {
            Log.d("BitmapPreviewDialog", "Bitmap $index: ${it.width}x${it.height} píxeles")
        } ?: Log.w("BitmapPreviewDialog", "Bitmap $index es nulo")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Previsualización del documento",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Configurar el Pager para mostrar múltiples páginas
                val pagerState = rememberPagerState(pageCount = { bitmaps.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false) // Permitir que el Pager use el espacio disponible
                ) { page ->
                    bitmaps[page]?.let { bitmap ->
                        // Usar Box con desplazamiento vertical
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Previsualización de página ${page + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(bitmap.width.toFloat() / bitmap.height)
                            )
                        }
                    }
                }
                // Indicador de página
                Text(
                    text = "Página ${pagerState.currentPage + 1} de ${bitmaps.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlueGray400,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Cancelar")
                    }
                    Button(onClick = onConfirm) {
                        Text("Imprimir")
                    }
                }
            }
        }
    }
}