package com.oscar.atestados.screens

import android.R.attr.bottom
import android.R.attr.end
import android.R.attr.start
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoSecundarios


/**
 * Pantalla principal de la aplicación.
 *
 * Esta pantalla sirve como punto de entrada, mostrando opciones para navegar a diferentes
 * funcionalidades de la aplicación, como gestionar personas, vehículos, alcoholemias, etc.
 *
 * @param navigateToScreen Función lambda que recibe una [String] para navegar a otra pantalla.
 */
@Composable
fun MainScreen(
    navigateToScreen: (String) -> Unit,
    version: String,
    showExitDialog: Boolean = false
) {
    var isExitDialogVisible by remember { mutableStateOf(showExitDialog) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ToolbarMain(navigateToScreen, version) },
        bottomBar = { BottomAppBar(navigateToScreen) }
    ) { paddingValues ->
        Content(
            modifier = Modifier
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = paddingValues.calculateBottomPadding()
                ), // Eliminamos el padding superior por defecto
            onNavigate = navigateToScreen
        )
    }
    // Diálogo de confirmación de salida
    if (isExitDialogVisible) {
        AlertDialog(
            onDismissRequest = { isExitDialogVisible = false },
            title = { Text("Salir de la aplicación") },
            text = { Text("¿Estás seguro de que quieres salir?") },
            confirmButton = {
                TextButton(onClick = {
                    isExitDialogVisible = false
                    context.findActivity()?.finish() // Usar el Context almacenado
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { isExitDialogVisible = false }) {
                    Text("No")
                }
            }
        )
    }
}
// Utilidad para obtener la actividad desde el contexto
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Barra superior de la pantalla principal.
 *
 * Contiene botones para acceder a la gestión de actuantes e impresoras, y un botón de versión
 * que muestra un diálogo con información de contacto.
 *
 * @param onNavigate Función lambda para navegar a otra pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarMain(
    onNavigate: (String) -> Unit,
    version: String
) {

    var isDialogVisible by remember { mutableStateOf(false) }
    val plainTooltipState = rememberTooltipState()

    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 4.dp),
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
                                onClick = { onNavigate("GuardiasScreen") },
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
                                onClick = { onNavigate("ImpresoraScreen") },
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

                // Botón de versión
                TextButton(onClick = { isDialogVisible = true }) {
                    Text(
                        text = "Versión $version",
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
        message = "Esta app se encuentra en versión $version." +
                "\n\nEste es un proyecto realizado para el módilo de Proyecto de " +
                "desarrollo de aplicaciones multiplataforma, para el Ciclo Formativo de " +
                "Grado Superior de Desarrollo de Aplicaciones Multiplataforma."
                +"\nProyecto presentado en junio de 2025 en el IES San Andrés de Villabalter, León." +
                "\n\nTutorado por Marta Garrido Vega y Raquel Barreales Quintanilla"
    )
}

/**
 * Barra inferior de la pantalla principal.
 *
 * Contiene botones para acceder a otros documentos y limpiar datos de la aplicación.
 *
 * @param onNavigate Función lambda para navegar a otra pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBar(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val plainTooltipState = rememberTooltipState()

    BottomAppBar(
        modifier = Modifier.wrapContentHeight(),
        containerColor = Color.Transparent,
        content = {
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
                        onClick = { onNavigate("OtrosDocumentosScreen") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 5.dp),
                        enabled = true,
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotonesNormales,
                            contentColor = TextoBotonesNormales
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
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
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
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

/**
 * Contenido principal de la pantalla.
 *
 * Muestra el logotipo de la Agrupación de Tráfico y botones de navegación a las diferentes
 * secciones de la aplicación.
 *
 * @param modifier Modificador para personalizar el diseño del contenido.
 * @param onNavigate Función lambda para navegar a otra pantalla.
 */
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
            .padding(vertical = 60.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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

            // Botones de navegación
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
                mensaje = "Pulse aquí para iniciar un Atestado por Alcoholemia"
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
                "Grado Superior de Desarrollo de Aplicaciones Multiplataforma." +
                "\nCreada por el Guardia Civil Óscar I. Ginés R., destinado en" +
                " el Destacamento de Tráfico de Ribadesella, Asturias."
    )
}

/**
 * Botón personalizado con tooltip.
 *
 * Crea un botón con un mensaje emergente (tooltip) que describe su función.
 *
 * @param onClick Callback que se ejecuta al hacer clic en el botón.
 * @param text Texto mostrado en el botón.
 * @param mensaje Mensaje del tooltip que describe la acción del botón.
 * @param enabled Indica si el botón está habilitado (por defecto, true).
 */
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
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
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

/**
 * Diálogo de alerta genérico.
 *
 * Muestra un diálogo con un título, mensaje y un botón de confirmación.
 *
 * @param showDialog Indica si el diálogo debe mostrarse.
 * @param onDismiss Callback que se ejecuta al cerrar el diálogo.
 * @param title Título del diálogo.
 * @param message Mensaje mostrado en el diálogo.
 */
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