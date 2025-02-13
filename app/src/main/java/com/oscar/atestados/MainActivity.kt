package com.oscar.atestados

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oscar.atestados.ui.theme.AtestadosTheme
import com.oscar.atestados.navigation.AppNavigation
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.ui.theme.BlueGray700
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // Variable para lanzar la solicitud de permisos
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Variables de estado
    private var arePermissionsGranted by mutableStateOf(false)
    private var isDatabaseLoaded by mutableStateOf(false)
    private var loadingStatus by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) { // Agregar el parámetro correctamente
        super.onCreate(savedInstanceState)

        // Inicializa el lanzador de permisos
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Verifica si todos los permisos han sido concedidos
            arePermissionsGranted = permissions.entries.all { it.value }
            if (!arePermissionsGranted) {
                Toast.makeText(this, "Para el uso de la app se necesitan permisos", Toast.LENGTH_LONG).show()
            }
        }

        // Solicitar permisos si no están concedidos
        checkAndRequestPermissions()

        // Configuración de la interfaz
        setContent {
            AtestadosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        !arePermissionsGranted -> {
                            PermissionDeniedScreen(onRetry = { checkAndRequestPermissions() })
                        }
                        !isDatabaseLoaded -> {
                            SplashScreen(
                                onSplashFinished = { loadDatabases() },
                                loadingStatus = loadingStatus
                            )
                        }
                        else -> {
                            AppNavigation() // Navega en la app si los permisos están concedidos y la BD está cargada
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION // Opcional según versión
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }.plus(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
        // Verifica si los permisos ya han sido concedidos
        arePermissionsGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (!arePermissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        }
    }
    /**
     * Carga las bases de datos en un hilo de fondo.
     */
    private fun loadDatabases() {
        // Cargar bases de datos en un hilo de fondo
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                loadingStatus = "Cargando paises.db..."
                val dbHelperPais = AccesoBaseDatos(this@MainActivity, "paises.db", 1)
                dbHelperPais.createDatabase()
                dbHelperPais.close()

                loadingStatus = "Cargando juzgados.db..."
                val dbHelperJuzgados = AccesoBaseDatos(this@MainActivity, "juzgados.db", 1)
                dbHelperJuzgados.createDatabase()
                dbHelperJuzgados.close()

                loadingStatus = "Cargando dispositivos.db..."
                val dbHelperDispositivos = AccesoBaseDatos(this@MainActivity, "dispositivos.db", 1)
                dbHelperDispositivos.createDatabase()
                dbHelperDispositivos.close()

                loadingStatus = "Creada las bases de datos con éxito"

            } catch (e: Exception) {
                Log.e("CargarBasesDatos", "Error cargando la base de datos: ${e.message}")
                loadingStatus = "Error cargando bases de datos"
            } finally {
                isDatabaseLoaded = true
                withContext(Dispatchers.Main) {
                    // Cambiar la UI para navegar a la pantalla principal
                    setContent {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
/**
 * Pantalla de Permiso Denegado.
 */
@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No se han concedido los permisos necesarios. " +
                        "Por favor, activa los permisos solicitados para continuar." +
                        "\nReinicie la aplicación para continuar y conceda los permisos solicitados" +
                        " si es necesario.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Intentar de nuevo")
            }
        }
    }
}

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    loadingStatus: String
) {
    var isDelayCompleted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(3000) // Espera 3 segundos para la animación del splash
        isDelayCompleted = true
    }
    LaunchedEffect(Unit) {
        delay(3000) // Espera 3 segundos para la animación del splash
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
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = if (isDelayCompleted) {
                        // Muestra el estado de la carga después de 3 segundos
                        loadingStatus.ifEmpty { "Cargando bases de datos..." }
                    } else {
                        "Estamos comprobando\npermisos y bases de datos"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp),
                    color = BlueGray700
                )
            }
        }
    }
}


