package com.oscar.atestados.screens

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios


@Composable
fun MainScreen(navigateToScreen: (String) -> Unit) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { Toolbar() },
        bottomBar = { BottomAppBar()}
    ) { paddingValues ->
        Content(
            modifier = Modifier.padding(paddingValues),
            onNavigate = navigateToScreen
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ViewContainer() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { Toolbar() },
        bottomBar = { BottomAppBar()}

    ) { paddingValues ->
        Content(
            modifier = Modifier.padding(paddingValues),
            onNavigate = { }
        )
    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = plainTooltipState,
                        tooltip = {
                            PlainTooltip {
                                Text("Añadir/modificar componentes")
                            }
                        },
                        content = {
                            IconButton(
                                onClick = { /* Handle left icon click */ },
                                modifier = Modifier.fillMaxHeight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.person_50),
                                    contentDescription = "Botón para añadir a los actuantes",
                                    tint = BotonesNormales
                                )
                            }
                        }
                    )
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = plainTooltipState,
                        tooltip = {
                            PlainTooltip {
                                Text("Añadir/modificar impresoras")
                            }
                        },
                        content = {
                            IconButton(
                                onClick = { /* Handle right icon click */ },
                                modifier = Modifier.fillMaxHeight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_50),
                                    contentDescription = "Botón de impresión",
                                    tint = BotonesNormales
                                )
                            }
                        }
                    )
                }

                // TextButton for version info
                TextButton(onClick = { isDialogVisible = true }) {
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
        message = "Para informar de errores o mandar sugerencias, mande un correo electrónico a oscargines@guardiacivil.es.\nPor favor, tenga paciencia en la contestación."
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBar() {
    val context = LocalContext.current
    val plainTooltipState = rememberTooltipState()

    BottomAppBar(
        //modifier = Modifier.height(60.dp),
        modifier = Modifier.wrapContentHeight(),
        containerColor = Color.Transparent,
        content = { // Aquí añadimos explícitamente el content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    state = plainTooltipState,
                    tooltip = {
                        PlainTooltip {
                            Text("Pulse aquí para confeccionar e imprimir otros documentos")
                        }
                    }
                ) {
                    Button(
                        onClick = { /* Acción del botón "OTROS DOCUMENTOS" */ },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 5.dp),
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
                            fontSize = 12.sp
                        )
                    }
                }

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    state = plainTooltipState,
                    tooltip = {
                        PlainTooltip {
                            Text(
                                "Pulse aquí para limpiar todos los datos almacenados " +
                                        "en la aplicación"
                            )
                        }
                    }
                ) {
                    Button(
                        onClick = { /* Acción del botón "LIMPIAR DATOS" */ },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 5.dp),
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
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun Content(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    var context = LocalContext.current
    var isDialogVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 60.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
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
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Atestados",
                color = TextoSecundarios,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Navigation buttons
            CreaBoton(
                onClick = { onNavigate("PersonaScreen") },
                text = "PERSONA",
                mensaje = "Pulse aquí para introducir los datos de la persona investigada"
            )
            CreaBoton(
                onClick = { onNavigate("VehiculoScreen") },
                text = "VEHÍCULO",
                mensaje = "Pulse aquí para introducir los datos del vehículo implicado"
            )
            Spacer(modifier = Modifier.height(30.dp))
            CreaBoton(
                onClick = { onNavigate("Alcoholemia01Screen") },
                text = "ALCOHOLEMIA",
                mensaje = "Pulse aquí para inicar un Atestado por Alcoholemia"
            )
            CreaBoton(
                onClick = { onNavigate("CarecerScreen") },
                text = "CARECER DE PERMISO",
                mensaje = "Este botón se encuentra deshabilitado,\nestamos trabajando para hacerlo funcionar"
            )
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
fun CreaBoton(
    onClick: () -> Unit,
    text: String,
    mensaje: String,
    enabled: Boolean = true
) {
    val plainTooltipState = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = plainTooltipState,
        tooltip = {
            PlainTooltip {
                Text(mensaje)
            }
        }
    ) {
        Button(
            enabled = enabled,
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            onClick = onClick
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

