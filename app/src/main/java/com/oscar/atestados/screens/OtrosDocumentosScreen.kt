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


@Composable
fun OtrosDocumentosScreen(navigateToScreen: (String) -> Unit) {
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
                Text(
                    text = "Estamos trabajando para hacer funcionar esta pantalla",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

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