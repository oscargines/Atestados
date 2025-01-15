package com.oscar.atestados

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oscar.atestados.ui.theme.AtestadosTheme
import com.oscar.atestados.ui.theme.BlueGray700
import kotlinx.coroutines.delay
import navigation.AppScrens


@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(3000)
        navController.popBackStack()
        navController.navigate(AppScrens.MainScreen.route)
    }
    BackgroundImage()
    Splash()

}

@Composable
fun Splash() {
    AtestadosTheme(darkTheme = false, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) { // Usamos Box para posicionar elementos libremente
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.escudo_bw),
                    contentDescription = "Logo"
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    "Atestados",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "App para la creación de atestados\nen carretera",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp,),
                    color = BlueGray700
                )

                Spacer(modifier = Modifier.height(30.dp)) // Espacio entre el texto y el indicador

                CircularProgressIndicator( // Indicador de carga
                    color = MaterialTheme.colorScheme.primary, // Color del progreso
                    strokeWidth = 4.dp, // Ancho del indicador
                    modifier = Modifier.size(40.dp) // Tamaño del indicador
                )


            }
        }
    }
}

@Composable
fun BackgroundImage(){
    Box(modifier = Modifier.fillMaxSize()) { // Usamos Box para posicionar elementos libremente
        Image(
                painter = painterResource(id = R.drawable.escudo_parcial),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .graphicsLayer(alpha = 0.1f)
                    //.fillMaxWidth()
            )
    }

}


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    BackgroundImage()
    Splash()

}