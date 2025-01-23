package com.oscar.atestados.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.BotonesNormales
import com.oscar.atestados.ui.theme.TextoBotonesNormales
import com.oscar.atestados.ui.theme.TextoNormales
import com.oscar.atestados.ui.theme.TextoTerciarios

@Composable
fun PersonaScreen(navigateToScreen: (String) -> Unit) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ToolbarPersona() },
        bottomBar = { BottomAppBarPersona() }
    ) { paddingValues ->
        PersonaContent(
            modifier = Modifier.padding(paddingValues),
            onNavigate = navigateToScreen
        )

    }

}

@Preview
@Composable
fun PersonaScreenPreview() {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ToolbarPersona() },
        bottomBar = { BottomAppBarPersona() }
    ) { paddingValues ->
        PersonaContent(modifier = Modifier.padding(paddingValues),
            onNavigate = { })

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarPersona() {
    val context = LocalContext.current
    val plainTooltipState = rememberTooltipState()

    androidx.compose.material3.BottomAppBar(
        //modifier = Modifier.height(60.dp),
        modifier = Modifier.wrapContentHeight(),
        containerColor = Color.Transparent,
        content = { // Aquí añadimos explícitamente el content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    state = plainTooltipState,
                    modifier = Modifier
                        .width(175.dp),
                    tooltip = {
                        PlainTooltip {
                            Text("Pulse aquí para guardar los datos introducidos")
                        }
                    }
                ) {
                    Button(
                        onClick = { /* Acción del botón "OTROS DOCUMENTOS" */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 5.dp),
                        enabled = true,
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotonesNormales,
                            contentColor = TextoBotonesNormales
                        )
                    ) {
                        Text(
                            "GUARDAR",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    state = plainTooltipState,
                    modifier = Modifier
                        .width(175.dp),
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
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        enabled = true,
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotonesNormales,
                            contentColor = TextoBotonesNormales
                        )
                    ) {
                        Text(
                            "LIMPIAR",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }

            }

        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarPersona() {
    var isDialogVisible by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                text = "Persona",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                color = TextoNormales,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            )
            IconButton(
                onClick = { },
                modifier = Modifier.fillMaxHeight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.camera_50),
                    contentDescription = "Botón para capturar información mediante " +
                            "el uso de la cámara",
                    tint = BotonesNormales
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaContent(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    val plainTooltipState = rememberTooltipState()
    var context = LocalContext.current
    var isDialogVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val navController = NavController(context)


    // Estados para los dropdowns
    var expandedGender by remember { mutableStateOf(false) }
    var expandedCountry by remember { mutableStateOf(false) }
    var expandedDNI by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("Masculino") }
    var selectedCountry by remember { mutableStateOf("España") }
    var selectedDNI by remember { mutableStateOf("DNI") }
    var documento by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var nombrePadre by remember { mutableStateOf("") }
    var nombreMadre by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var lugarNacimiento by remember { mutableStateOf("") }
    var domicilio by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correoElectronico by remember { mutableStateOf("") }


    Spacer(modifier = Modifier.height(80.dp))
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, bottom = 30.dp, top = 40.dp)

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


            }
            Column(
                modifier = Modifier.fillMaxSize(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Column {
                        Row {
                            DropDownSexo()
                            Spacer(modifier = Modifier.width(8.dp))
                            DropDownNacionalidad()

                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            DropDownDocumento()
                            /*Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = documento,
                                onValueChange = { documento = it },
                                label = { Text("Documento de identidad", color = TextoTerciarios) },
                                placeholder = {
                                    Text(
                                        "Introduzca el documento de identidad completo",
                                        color = TextoTerciarios,
                                        textDecoration = TextDecoration.Underline
                                    )
                                },
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .padding(end = 5.dp),
                                singleLine = true,

                                )*/
                        }

                    }
                }
                Row {
                    Column() {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre", color = TextoTerciarios) },
                            placeholder = {
                                Text(
                                    "Introduzca el nombre completo",
                                    color = TextoTerciarios,
                                    textDecoration = TextDecoration.Underline
                                )
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),
                            singleLine = true,

                            )
                        OutlinedTextField(
                            value = apellidos,
                            onValueChange = { apellidos = it },
                            label = { Text("Apellidos", color = TextoTerciarios) },
                            placeholder = {
                                Text(
                                    "Introduzca los apellidos completos",
                                    color = TextoTerciarios,
                                    textDecoration = TextDecoration.Underline
                                )
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),
                            singleLine = true,

                            )
                        OutlinedTextField(
                            value = nombrePadre,
                            onValueChange = { nombrePadre = it },
                            label = { Text("Nombre del padre", color = TextoTerciarios) },
                            shape = MaterialTheme.shapes.extraSmall,
                            placeholder = {
                                Text(
                                    "Introduzca el nombre del padre",
                                    color = TextoTerciarios,
                                    textDecoration = TextDecoration.Underline
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),
                            singleLine = true,

                            )
                        OutlinedTextField(
                            value = nombreMadre,
                            onValueChange = { nombreMadre = it },
                            label = { Text("Nombre de la Madre", color = TextoTerciarios) },
                            placeholder = {
                                Text(
                                    "Introduzca el nombre de la madre",
                                    color = TextoTerciarios,
                                    textDecoration = TextDecoration.Underline
                                )
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),
                            singleLine = true,

                            )
                        OutlinedTextField(
                            value = fechaNacimiento,
                            onValueChange = { fechaNacimiento = it },
                            label = { Text("Fecha de nacimiento", color = TextoTerciarios) },
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            "Se ha pulsado el calendario",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(24.dp),

                                    ) {
                                    Icon(
                                        painter = painterResource(
                                            id = R.drawable.calendar_ico
                                        ),
                                        tint = BotonesNormales,
                                        contentDescription = "Botón de acceso a calendario"
                                    )
                                }
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),
                            singleLine = true,

                            )
                        OutlinedTextField(
                            value = lugarNacimiento,
                            onValueChange = { lugarNacimiento = it },
                            label = { Text("Lugar de nacimiento", color = TextoTerciarios) },
                            placeholder = {
                                Text(
                                    "Introduzca el lugar de nacimiento",
                                    color = TextoTerciarios,
                                    textDecoration = TextDecoration.Underline
                                )
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),

                            )
                        OutlinedTextField(
                            value = domicilio,
                            onValueChange = { domicilio = it },
                            label = { Text("Domicilio", color = TextoTerciarios) },
                            placeholder = {
                                Text(
                                    "Introduzca el domicilio completo.",
                                    color = TextoTerciarios,
                                    textDecoration = TextDecoration.Underline
                                )
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),

                            )
                    }
                }
                Row {
                    Column {

                        OutlinedTextField(
                            value = codigoPostal,
                            onValueChange = { codigoPostal = it },
                            label = { Text("Código Postal", color = TextoTerciarios) },
                            shape = MaterialTheme.shapes.extraSmall,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(150.dp)
                                .padding(end = 5.dp),
                            singleLine = true,

                            )
                    }
                    Column {

                        OutlinedTextField(
                            value = telefono,
                            onValueChange = { telefono = it },
                            label = { Text("Teléfono", color = TextoTerciarios) },
                            shape = MaterialTheme.shapes.extraSmall,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(end = 5.dp),
                            singleLine = true,

                            )
                    }

                }
            }
            Row {
                Column {

                    OutlinedTextField(
                        value = correoElectronico,
                        onValueChange = { correoElectronico = it },
                        label = { Text("Correo electrónico", color = TextoTerciarios) },
                        placeholder = {
                            Text(
                                "Introduzca el correo electrónico, si lo tiene",
                                color = TextoTerciarios,
                                textDecoration = TextDecoration.Underline
                            )
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .padding(end = 5.dp),
                        singleLine = true,

                        )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownSexo() {
    var isExpandedSexo by remember { mutableStateOf(false) }
    val listSexo = listOf("Masculino", "Femenino")


    var selectedText by remember { mutableStateOf(listSexo[0]) }

    ExposedDropdownMenuBox(
        expanded = isExpandedSexo,
        onExpandedChange = { isExpandedSexo = !isExpandedSexo },
        modifier = Modifier.width(150.dp)
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedSexo) },
            modifier = Modifier.menuAnchor()

        )
        ExposedDropdownMenu(
            expanded = isExpandedSexo,
            onDismissRequest = { isExpandedSexo = false }
        ) {
            listSexo.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        selectedText = item
                        isExpandedSexo = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownNacionalidad() {

    var isExpandedNacionalidad by remember { mutableStateOf(false) }
    val listNacionalidad = listOf("España")

    var selectedText by remember { mutableStateOf(listNacionalidad[0]) }

    ExposedDropdownMenuBox(
        expanded = isExpandedNacionalidad,
        onExpandedChange = { isExpandedNacionalidad = !isExpandedNacionalidad }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedNacionalidad) },
            modifier = Modifier.menuAnchor()

        )
        ExposedDropdownMenu(
            expanded = isExpandedNacionalidad,
            onDismissRequest = { isExpandedNacionalidad = false }
        ) {
            listNacionalidad.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        selectedText = item
                        isExpandedNacionalidad = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }

    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownDocumento() {
    var isExpandedDocumento by remember { mutableStateOf(false) }
    val listDocumento = listOf("DNI", "NIE", "Pasaporte", "Per. Nacional Cond.", "Otros")

    var selectedText by remember { mutableStateOf(listDocumento[0]) }
    var documento by remember { mutableStateOf("") }

    ExposedDropdownMenuBox(
        expanded = isExpandedDocumento,
        onExpandedChange = { isExpandedDocumento = !isExpandedDocumento },
        modifier = Modifier.width(150.dp).padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedDocumento) },
            modifier = Modifier.menuAnchor()

        )
        ExposedDropdownMenu(
            expanded = isExpandedDocumento,
            onDismissRequest = { isExpandedDocumento = false }
        ) {
            listDocumento.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        selectedText = item
                        isExpandedDocumento = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }

    }
    Spacer(modifier = Modifier.width(8.dp))

    OutlinedTextField(
        value = documento,
        onValueChange = { documento = it },
        label = { Text("Documento de identidad", color = TextoTerciarios) },
        placeholder = {
            Text(
                "Introduzca el documento de identidad completo",
                color = TextoTerciarios,
                textDecoration = TextDecoration.Underline
            )
        },
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(end = 5.dp),
        singleLine = true,

        )

}

