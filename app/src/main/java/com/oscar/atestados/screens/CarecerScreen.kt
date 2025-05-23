package com.oscar.atestados.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales

/**
 * Pantalla que muestra un mensaje indicando que está en desarrollo y un botón para regresar.
 *
 * Esta función Composable crea una interfaz de usuario simple con un mensaje centrado y un botón
 * para navegar de vuelta a la pantalla principal. Utiliza un [Scaffold] como estructura base y
 * organiza los elementos en una columna centrada.
 *
 * @param navigateToScreen Función lambda que recibe una [String] como destino de navegación.
 *                         Se utiliza para redirigir al usuario a otra pantalla (por ejemplo, "MainScreen").
 */
@Composable
fun CarecerScreen(navigateToScreen: (String) -> Unit) {
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
                 * Texto que informa al usuario que la pantalla está en desarrollo.
                 */
                Text(
                    text = "Estamos trabajando para hacer funcionar esta pantalla",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                /**
                 * Botón que permite al usuario regresar a la pantalla principal.
                 *
                 * Este botón utiliza colores personalizados definidos en el tema de la aplicación
                 * ([BotonesNormales] y [TextoBotonesNormales]) y navega a "MainScreen" al hacer clic.
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
                    Text("ACEPTAR")
                }
            }
        }
    }
}