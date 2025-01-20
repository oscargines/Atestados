package com.oscar.atestados.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oscar.atestados.R
import com.oscar.atestados.navigation.AppScrens
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.BotonesSecundarios
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios


@Composable
fun MainScreen() {
    ViewContainer()

}

@Preview(showBackground = true)
@Composable
fun ViewContainer() {
    Scaffold(
        topBar = { Toolbar() },
        content = { Content() },
        bottomBar = { BottonAppBar() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar() {
    var isDialogVisible by remember { mutableStateOf(false) }
    val plainTooltipState = rememberTooltipState()
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Columna Izquierda
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = plainTooltipState,
                        tooltip ={
                            PlainTooltip {
                                Text("Pulse aquí para añadir o modificar los componentes")
                            }

                        }
                    ) {
                        IconButton(
                            onClick = { },
                            modifier = Modifier.fillMaxHeight(1f)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.person_50),
                                contentDescription = "Botón para añadir a los actuantes",
                                tint = BotonesNormales
                            )
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = plainTooltipState,
                        tooltip ={
                            PlainTooltip {
                                Text("Pulse aquí para añadir o modificar las impresorar enlazadas")
                            }

                        }
                    ) {

                        IconButton(
                            onClick = { },
                            modifier = Modifier.fillMaxHeight(1f)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.print_50),
                                contentDescription = "Botón de impresión",
                                tint = BotonesNormales
                            )
                        }
                    }
                }
                //Columna derecha
                TextButton(onClick = {
                    isDialogVisible = true

                }) {
                    Text(
                        text = "Versión 1.0",
                        fontSize = 8.sp,
                        color = TextoSecundarios,
                        textAlign = TextAlign.Center
                    )

                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
    LanzaAlert(
        showDialog = isDialogVisible,
        onDismiss = { isDialogVisible = false },
        title = "Información",
        message = "Para informar de errores o mandar sigerencias, m" +
                "ande un correo electrónico a oscargines@guardiacivil.es." +
                "\nPor favor, tenga paciencia en la contestación."
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottonAppBar() {
    var context = LocalContext.current
    val plainTooltipState = rememberTooltipState()
    BottomAppBar(
        modifier = Modifier.height(60.dp),
        containerColor = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                tooltip ={
                    PlainTooltip {
                        Text("Pulse aquí para confeccionar e imprimir otros documentos")
                    }

                }
            ) {

                Button(
                    onClick = { /* Acción del botón "OTROS DOCUMENTOS" */ },
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp, 0.dp, 10.dp, 0.dp),
                    enabled = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesNormales,
                        contentColor = TextoBotonesNormales
                    )
                ) {
                    Text(
                        "OTROS DOCUMENTOS",
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp
                    )
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                state = plainTooltipState,
                tooltip ={
                    PlainTooltip {
                        Text("Pulse aquí para limpiar todos los datos almacenados " +
                                "en la aplicación")
                    }

                }
            ) {
                Button(
                    onClick = { /* Acción del botón "LIMPIAR DATOS" */ },
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp, 0.dp, 10.dp, 0.dp), // Ajusta automáticamente el ancho
                    enabled = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonesNormales,
                        contentColor = TextoBotonesNormales
                    )
                ) {
                    Text(
                        "LIMPIAR DATOS",
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Content() {
    var context = LocalContext.current
    var isDialogVisible by remember { mutableStateOf(false) }
    val navController = NavController(context)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.escudo_bw),
                contentDescription = "Escudo de la Agrupación",
                modifier = Modifier
                    .padding(0.dp, 30.dp, 0.dp, 0.dp)
                    .clickable { isDialogVisible = true }

            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Agrupación de Tráfico",
                color = TextoNormales,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Atestados",
                color = TextoSecundarios,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            CreaBoton({
                navController.popBackStack()
                navController.navigate(AppScrens.Alcoholemia01Screen.route)
                      },
                "PERSONA",
                "Pulse aquí para introducir los datos de la persona investigada")
            //Spacer(modifier = Modifier.height(1.dp))
            CreaBoton({
                navController.popBackStack()
                navController.navigate(AppScrens.Alcoholemia01Screen.route)
            },
                "VEHÍCULO",
                "Pulse aquí para introducir los datos del vehículo implicado")
            Spacer(modifier = Modifier.height(30.dp))
            CreaBoton({
                navController.popBackStack()
                navController.navigate(AppScrens.Alcoholemia01Screen.route)
                      },
                "ALCOHOLEMIA",
                "Pulse aquí para inicar un Atestado por Alcoholemia" )
            //Spacer(modifier = Modifier.height(10.dp))
            CreaBoton({  },
                "CARECER DE PERMISO",
                "Este botón se encuentra deshabilitado, estamos trabajando para hacerlo funcionar")

        }
    }
    LanzaAlert(
        showDialog = isDialogVisible,
        onDismiss = { isDialogVisible = false },
        title = "Información de la app",
        message = "Esta app ha sido creada como proyecto final del Ciclo de " +
                "Grado Superior de Darrollo de Aplicaciones Multiplataforma." +
                "\nCreada por el Guardia Civil Óscar I. Ginés R., destinado en" +
                " el Destacamento de Tráfico de Ribadesella, Asturias."
    )


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreaBoton(onClick: () -> Unit, text: String, mensaje: String) {
    val plainTooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = plainTooltipState,
        tooltip ={
            PlainTooltip {
                Text(mensaje)
            }

        }
    ) {
        Button(
            enabled = true,
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(10.dp, 0.dp, 10.dp, 0.dp),
            onClick = {}
        ) {
            Text(text = text)
        }
    }

}

@Composable
fun LanzaAlert(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    title: String,
    message: String
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "Aceptar")
                }
            }
        )
    }
}


fun onClick() {

}
