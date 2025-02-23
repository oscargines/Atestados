package com.oscar.atestados

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.navigation.AppNavigation
import com.oscar.atestados.screens.PersonaScreen
import com.oscar.atestados.ui.theme.AtestadosTheme
import com.oscar.atestados.ui.theme.BlueGray700
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.viewModel.PersonaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Actividad principal de la aplicación Atestados.
 * Gestiona la inicialización de permisos, carga de bases de datos y navegación entre pantallas,
 * incluyendo el soporte para lectura NFC del DNI electrónico.
 */
class MainActivity : ComponentActivity() {
    /** Lanzador para solicitar múltiples permisos al usuario */
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    /** Lista de permisos requeridos por la aplicación */
    private var requiredPermissions = emptyArray<String>()

    /** ViewModel para gestionar los datos de la persona */
    private lateinit var personaViewModel: PersonaViewModel

    // Estados
    private var arePermissionsGranted by mutableStateOf(false)
    private var isDatabaseLoaded by mutableStateOf(false)
    private var loadingStatus by mutableStateOf("")

    /**
     * Configura la actividad al crearse, inicializando el ViewModel y la interfaz de usuario.
     *
     * @param savedInstanceState Estado guardado de la actividad, si existe
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewModel
        personaViewModel = ViewModelProvider(this)[PersonaViewModel::class.java]

        configurePermissionLauncher()
        checkAndRequestPermissions()

        setContent {
            AtestadosTheme {
                ContentRouter(nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG))
            }
        }
    }

    /**
     * Componente composable que determina qué pantalla mostrar basado en el estado de permisos y carga.
     *
     * @param nfcTag Tag NFC detectado, si existe
     */
    @Composable
    private fun ContentRouter(nfcTag: android.nfc.Tag?) {
        val navController = rememberNavController()

        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                !arePermissionsGranted -> PermissionDeniedScreen(
                    onRetry = { checkAndRequestPermissions() },
                    onOpenSettings = { openAppSettings() }
                )

                !isDatabaseLoaded -> SplashScreen(loadingStatus = loadingStatus)

                else -> NavHost(navController, startDestination = "MainScreen") {
                    composable("MainScreen") {
                        AppNavigation(navController)
                    }
                    composable("PersonaScreen") {
                        PersonaScreen(
                            navigateToScreen = { route -> navController.navigate(route) },
                            personaViewModel = personaViewModel,
                            nfcTag = nfcTag
                        )
                    }
                }
            }
        }
    }

    /**
     * Configura el lanzador de permisos para manejar las respuestas del usuario.
     */
    private fun configurePermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            arePermissionsGranted = permissions.all { it.value }
            if (arePermissionsGranted) loadDatabases()
            else showDeniedToast()
        }
    }

    /**
     * Verifica y solicita los permisos necesarios para la aplicación.
     */
    private fun checkAndRequestPermissions() {
        requiredPermissions = buildList {
            // Permisos Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            // Permisos almacenamiento
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            // Permisos cámara y NFC
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.NFC)
        }.toTypedArray()

        arePermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!arePermissionsGranted) {
            if (shouldShowRationale()) showPermissionRationaleDialog()
            else permissionLauncher.launch(requiredPermissions)
        } else {
            loadDatabases()
        }
    }

    /**
     * Determina si se debe mostrar una explicación para los permisos.
     *
     * @return true si se debe mostrar una explicación, false en caso contrario
     */
    private fun shouldShowRationale() = requiredPermissions.any {
        ActivityCompat.shouldShowRequestPermissionRationale(this, it)
    }

    /**
     * Muestra un diálogo explicando por qué se necesitan los permisos.
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage("La aplicación necesita acceder a estos recursos para funcionar correctamente.")
            .setPositiveButton("Conceder") { _, _ -> permissionLauncher.launch(requiredPermissions) }
            .setNegativeButton("Configuración") { _, _ -> openAppSettings() }
            .show()
    }

    /**
     * Abre la configuración de la aplicación en los ajustes del sistema.
     */
    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    /**
     * Muestra un mensaje cuando los permisos son denegados.
     */
    private fun showDeniedToast() {
        Toast.makeText(this, "Funcionalidad limitada sin permisos", Toast.LENGTH_LONG).show()
        loadDatabases() // Cargar BD incluso sin permisos si es posible
    }

    /**
     * Carga las bases de datos necesarias en un hilo secundario.
     */
    private fun loadDatabases() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            listOf("paises.db", "juzgados.db", "dispositivos.db").forEach { dbName ->
                loadingStatus = "Cargando $dbName..."
                AccesoBaseDatos(this@MainActivity, dbName, 1).use { it.createDatabase() }
            }
            loadingStatus = "Bases de datos listas"
        } catch (e: Exception) {
            Log.e("DB_LOAD", "Error: ${e.message}")
            loadingStatus = "Error cargando datos"
        } finally {
            withContext(Dispatchers.Main) { isDatabaseLoaded = true }
        }
    }

    /**
     * Maneja nuevos intents recibidos, como detecciones NFC, actualizando la interfaz.
     *
     * @param intent El nuevo intent recibido
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent { // Actualizar la UI cuando se reciba un nuevo intent
            AtestadosTheme {
                ContentRouter(nfcTag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG))
            }
        }
    }
}

/**
 * Pantalla que se muestra cuando los permisos necesarios no están concedidos.
 *
 * @param onRetry Acción a realizar al intentar solicitar permisos nuevamente
 * @param onOpenSettings Acción para abrir la configuración del sistema
 */
@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Se requieren permisos para el funcionamiento completo de la aplicación",
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onRetry) {
                Text("Reintentar permisos")
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onOpenSettings) {
                Text("Abrir Configuración")
            }
        }
    }
}

/**
 * Pantalla de carga inicial que muestra el logo y el estado de carga de las bases de datos.
 *
 * @param loadingStatus Mensaje de estado actual de la carga
 */
@Composable
fun SplashScreen(loadingStatus: String) {
    val context = LocalContext.current
    var showLoadingStatus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000) // Animación mínima del splash
        showLoadingStatus = true
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.escudo_bw),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(24.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )

            if (showLoadingStatus) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = loadingStatus,
                    fontSize = 14.sp,
                    color = BlueGray700
                )
            }
        }
    }
}