package com.oscar.atestados.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales

/**
 * Pantalla informativa que indica que una funcionalidad está en desarrollo.
 *
 * Esta pantalla muestra un mensaje centrado notificando que la funcionalidad está en proceso
 * de implementación, junto con un botón para regresar a la pantalla principal.
 *
 * @param navigateToScreen Función lambda que recibe una [String] como destino de navegación.
 *                         Se utiliza para redirigir al usuario a la pantalla principal ("MainScreen").
 */
@Composable
fun InformacionScreen(navigateToScreen: (String) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {},
        bottomBar = {}
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                /**
                 * Texto informativo sobre el estado de la funcionalidad.
                 */
                Text(
                    text = "Estamos trabajando en esta funcionalidad",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                /**
                 * Botón para regresar a la pantalla principal.
                 *
                 * Al hacer clic, navega a "MainScreen" utilizando la función [navigateToScreen].
                 */
                Button(
                    onClick = { navigateToScreen("MainScreen") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesNormales,
                        contentColor = TextoBotonesNormales
                    ),
                    modifier = Modifier.width(180.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("VOLVER")
                }
            }
        }
    }
}