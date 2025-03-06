package com.oscar.atestados.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.AtestadosTheme
import com.oscar.atestados.ui.theme.BlueGray700
import kotlinx.coroutines.delay

/**
 * Pantalla de carga (splash screen) de la aplicación.
 *
 * Muestra un logotipo, texto descriptivo y un indicador de progreso durante un tiempo determinado
 * antes de navegar a la siguiente pantalla.
 *
 * @param onSplashFinished Callback que se ejecuta cuando finaliza la duración del splash screen,
 *                         generalmente para navegar a la pantalla principal.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    LaunchedEffect(key1 = true) {
        delay(3000) // Duración del splash en milisegundos (3 segundos)
        onSplashFinished()
    }

    AtestadosTheme(darkTheme = false, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.escudo_bw),
                    contentDescription = "Logo",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Atestados",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "App para la creación de atestados\nen carretera",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp),
                    color = BlueGray700
                )

                Spacer(modifier = Modifier.height(30.dp))

                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/**
 * Vista previa de la pantalla de carga para desarrollo y diseño.
 *
 * Muestra cómo se verá la pantalla SplashScreen en el entorno de vista previa de Android Studio.
 */
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onSplashFinished = {})
}