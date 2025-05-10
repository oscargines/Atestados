package com.oscar.atestados.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.oscar.atestados.R
import com.oscar.atestados.ui.theme.*
import com.oscar.atestados.viewModel.LecturaDerechosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private const val TAG = "LecturaDerechosScreen"

@Composable
fun LecturaDerechosScreen(
    navigateToScreen: (String) -> Unit,
    lecturaDerechosViewModel: LecturaDerechosViewModel
) {
    var showInitialDialog by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    if (showInitialDialog) {
        AlertDialog(
            onDismissRequest = {
                showInitialDialog = false
                Log.d(TAG, "Initial dialog dismissed")
            },
            title = {
                Text(
                    "ATENCIÓN",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Rellene todos los campos y antes de que guarde la información, " +
                            "informe al investigado sobre la información que ha introducido.\n\n " +
                            "Hágalo de forma clara y entendible para el usuario." +
                            "\n\nHágase entender.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextoNormales
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInitialDialog = false
                        Log.d(TAG, "Initial dialog confirmed with ENTENDIDO button")
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = BotonesNormales)
                ) {
                    Text("ENTENDIDO")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        topBar = { LecturaDerechosTopBar() },
        bottomBar = { LecturaDerechosBottomBar(lecturaDerechosViewModel, navigateToScreen, context) }
    ) { paddingValues ->
        LecturaDerechosContent(
            modifier = Modifier.padding(paddingValues),
            lecturaDerechosViewModel = lecturaDerechosViewModel,
            fusedLocationClient = fusedLocationClient
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LecturaDerechosTopBar() {
    CenterAlignedTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        title = {
            Column {
                Text(
                    text = "Lectura de derechos",
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoNormales,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Datos relativos a la investigación",
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoSecundarios
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun LecturaDerechosContent(
    modifier: Modifier = Modifier,
    lecturaDerechosViewModel: LecturaDerechosViewModel,
    fusedLocationClient: FusedLocationProviderClient
) {
    val momentoLectura by lecturaDerechosViewModel.momentoLectura.observeAsState("Tomada en el momento")
    val lugarInvestigacion by lecturaDerechosViewModel.lugarInvestigacion.observeAsState("")
    val lugarDelito by lecturaDerechosViewModel.lugarDelito.observeAsState("")
    val resumenHechos by lecturaDerechosViewModel.resumenHechos.observeAsState("")
    val calificacionHechos by lecturaDerechosViewModel.calificacionHechos.observeAsState("")
    val relacionIndicios by lecturaDerechosViewModel.relacionIndicios.observeAsState("")
    val context = LocalContext.current

    // Log initial state of all fields
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initial state - MomentoLectura: $momentoLectura, " +
                "LugarInvestigacion: $lugarInvestigacion, " +
                "LugarDelito: $lugarDelito, " +
                "ResumenHechos: $resumenHechos, " +
                "CalificacionHechos: $calificacionHechos, " +
                "RelacionIndicios: $relacionIndicios")
    }

    // Launcher para solicitar permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Permiso de ubicación concedido")
        } else {
            Log.w(TAG, "Permiso de ubicación denegado")
            lecturaDerechosViewModel.updateLugarInvestigacion("Permiso de ubicación denegado", null, null)
            Log.d(TAG, "LugarInvestigacion actualizado a: Permiso de ubicación denegado")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            listOf(
                "Tomada en el momento",
                "En otro momento"
            ).forEach { opcion ->
                RadioOption(
                    text = opcion,
                    selected = momentoLectura == opcion,
                    onSelect = {
                        lecturaDerechosViewModel.setMomentoLectura(opcion)
                        Log.d(TAG, "MomentoLectura changed to: $opcion")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = lugarInvestigacion,
            onValueChange = {
                lecturaDerechosViewModel.updateLugarInvestigacion(it, null, null)
                Log.d(TAG, "LugarInvestigacion updated to: $it")
            },
            label = "Lugar, hora y fecha de la investigación",
            singleLine = false,
            maxLines = 1,
            leadingIcon = {
                IconButton(
                    onClick = {
                        Log.d(TAG, "Botón GPS presionado para obtener ubicación")
                        Toast.makeText(context, "Espere a que la aplicación obtenga y cargue los datos", Toast.LENGTH_SHORT).show()
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { location ->
                                    location?.let {
                                        Log.d(TAG, "Ubicación obtenida: lat=${it.latitude}, lon=${it.longitude}")
                                        var thoroughfare = "Carretera desconocida"
                                        var pk = "PK no disponible"
                                        // Intentar con Geocoder como respaldo
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        try {
                                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                            if (addresses?.isNotEmpty() == true) {
                                                val address = addresses[0]
                                                thoroughfare = address.thoroughfare ?: "Carretera desconocida"
                                                val featureName = address.featureName ?: ""
                                                pk = if (featureName.matches(Regex("\\d+\\.?\\d*"))) "PK $featureName" else "PK no disponible"
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error al usar Geocoder: ${e.message}", e)
                                        }
                                        // Usar Nominatim para obtener la denominación de la vía
                                        CoroutineScope(Dispatchers.Main).launch {
                                            try {
                                                val client = OkHttpClient()
                                                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=${it.latitude}&lon=${it.longitude}&zoom=16&addressdetails=1"
                                                val request = Request.Builder()
                                                    .url(url)
                                                    .header("User-Agent", "AtestadosApp/1.0")
                                                    .build()
                                                val response = withContext(Dispatchers.IO) {
                                                    client.newCall(request).execute()
                                                }
                                                if (response.isSuccessful) {
                                                    val json = JSONObject(response.body?.string() ?: "{}")
                                                    val addressJson = json.optJSONObject("address")
                                                    if (addressJson != null) {
                                                        // Priorizar ref sobre road
                                                        thoroughfare = addressJson.optString("ref", thoroughfare)
                                                    }
                                                    Log.d(TAG, "Nominatim response - Vía: $thoroughfare")
                                                } else {
                                                    Log.w(TAG, "Error en Nominatim: ${response.code}")
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error al consultar Nominatim: ${e.message}", e)
                                            }

                                            // Formatear fecha y hora
                                            val currentDateTime = Instant.now()
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDateTime()
                                            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "ES"))
                                            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))
                                            val formattedTime = currentDateTime.format(timeFormatter)
                                            val formattedDate = currentDateTime.format(dateFormatter)
                                            val locationDetails = "$thoroughfare, $pk, $formattedTime, $formattedDate"

                                            Log.d(TAG, "LocationDetails generado: $locationDetails")
                                            lecturaDerechosViewModel.updateLugarInvestigacion(locationDetails, it.latitude, it.longitude)
                                            lecturaDerechosViewModel.updateLugarDelito(locationDetails, it.latitude, it.longitude)
                                        }
                                    } ?: run {
                                        Log.w(TAG, "Ubicación no disponible")
                                        lecturaDerechosViewModel.updateLugarInvestigacion("Ubicación no disponible", null, null)
                                    }
                                }.addOnFailureListener { e ->
                                    Log.e(TAG, "Error al obtener ubicación: ${e.message}", e)
                                    lecturaDerechosViewModel.updateLugarInvestigacion("Error al obtener ubicación: ${e.message}", null, null)
                                }
                        } else {
                            Log.w(TAG, "Permisos de ubicación no concedidos, solicitando permiso")
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share_location_24),
                        contentDescription = "Obtener ubicación GPS",
                        tint = BotonesNormales
                    )
                }
            }
        )

        CustomTextField(
            value = lugarDelito,
            onValueChange = {
                lecturaDerechosViewModel.updateLugarDelito(it, null, null)
                Log.d(TAG, "LugarDelito updated to: $it")
            },
            label = "Lugar, hora y fecha de la comisión del delito",
            singleLine = false,
            maxLines = 1
        )

        CustomTextField(
            value = resumenHechos,
            onValueChange = {
                lecturaDerechosViewModel.updateResumenHechos(it)
                Log.d(TAG, "ResumenHechos updated to: $it")
            },
            label = "Breve resumen de los hechos",
            modifier = Modifier.height(150.dp),
            singleLine = false,
            maxLines = 5
        )

        CustomTextField(
            value = calificacionHechos,
            onValueChange = {
                lecturaDerechosViewModel.updateCalificacionHechos(it)
                Log.d(TAG, "CalificacionHechos updated to: $it")
            },
            label = "Calificación provisional de los hechos",
            modifier = Modifier.height(100.dp),
            singleLine = false,
            maxLines = 3
        )

        CustomTextField(
            value = relacionIndicios,
            onValueChange = {
                lecturaDerechosViewModel.updateRelacionIndicios(it)
                Log.d(TAG, "RelacionIndicios updated to: $it")
            },
            label = "Relación de indicios de los que se deduce la investigación",
            modifier = Modifier.height(250.dp),
            singleLine = false,
            maxLines = 10
        )
    }
}

@Composable
private fun LecturaDerechosBottomBar(
    viewModel: LecturaDerechosViewModel,
    navigateToScreen: (String) -> Unit,
    context: android.content.Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = {
                Log.d(TAG, "Botón GUARDAR presionado")
                viewModel.guardarDatos(context)
                Log.d(TAG, "Datos guardados, navegando a TomaDerechosScreen")
                navigateToScreen("TomaDerechosScreen")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("GUARDAR")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = {
                Log.d(TAG, "Botón LIMPIAR presionado")
                viewModel.limpiarDatos()
                Log.d(TAG, "Datos limpiados")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BotonesNormales,
                contentColor = TextoBotonesNormales
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("LIMPIAR")
        }
    }
}

@Composable
private fun RadioOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = BotonesNormales,
                unselectedColor = TextoSecundarios
            )
        )
        Text(
            text = text,
            color = TextoNormales,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoTerciarios) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = singleLine,
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = TextoSecundarios,
            focusedBorderColor = BotonesNormales
        )
    )
}