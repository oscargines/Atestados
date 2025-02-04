package com.oscar.atestados.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

@Composable
fun VehiculoScreen(navigateToScreen: () -> Unit) {
    VehiculoScreenContent(navigateToScreen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiculoScreenContent(navigateToScreen: () -> Unit) {
    val plainTooltipState = rememberTooltipState()
    var context = LocalContext.current
    var isDialogVisible by remember { mutableStateOf(false) }
    val navController = NavController(context)
    Box {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text(text = "Estamos trabajando para habilitar esta opción." +
                    "\nEn breve la tendremos activa.\n\n\nVEHICULO",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                color = BotonesNormales
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotonesNormales,
                    contentColor = TextoBotonesNormales
                ),
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(10.dp),
                onClick = {
                    navigateToScreen()
                }
            ) { Text(text = "Volver") }

        }

    }

}

@Preview(showBackground = true)
@Composable
fun VehiculoScreenPreview() {
    VehiculoScreenContent(
        navigateToScreen = TODO()
    )


}