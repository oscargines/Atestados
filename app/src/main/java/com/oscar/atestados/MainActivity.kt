package com.oscar.atestados

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
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
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.navigation.appNavigation
import com.oscar.atestados.ui.theme.AtestadosTheme
import com.oscar.atestados.ui.theme.BlueGray700
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.viewModel.NfcViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "MainActivity"

/**
 * Actividad principal de la aplicación Atestados que maneja:
 * - Permisos necesarios para la aplicación
 * - Funcionalidad NFC
 * - Carga de bases de datos iniciales
 * - Navegación entre pantallas
 */
class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var personaViewModel: PersonaViewModel
    private lateinit var nfcViewModel: NfcViewModel
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var requiredPermissions: Array<String>
    private var arePermissionsGranted by mutableStateOf(false)
    private var isDatabaseLoaded by mutableStateOf(false)
    private var loadingStatus by mutableStateOf("")

    /**
     * Método llamado cuando se crea la actividad.
     * Inicializa los ViewModels, adaptadores NFC y verifica los permisos necesarios.
     *
     * @param savedInstanceState Bundle que contiene el estado previamente guardado de la actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        personaViewModel = ViewModelProvider(this)[PersonaViewModel::class.java]
        nfcViewModel = ViewModelProvider(this)[NfcViewModel::class.java]

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_LONG).show()
            Log.w(TAG, "NFC no soportado en este dispositivo")
        }

        configurePermissionLauncher()
        checkAndRequestPermissions()

        setContent {
            AtestadosTheme {
                ContentRouter()
            }
        }

        processNfcIntent(intent)
    }

    /**
     * Método llamado cuando la actividad se reanuda.
     * Habilita el despacho en primer plano para NFC si está disponible.
     */
    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                )
                val techList = arrayOf(arrayOf("android.nfc.tech.IsoDep"), arrayOf("android.nfc.tech.NfcB"))
                adapter.enableForegroundDispatch(this, pendingIntent, null, techList)
                Log.d(TAG, "Foreground dispatch habilitado para NFC")
            } else {
                Toast.makeText(this, "Por favor, active el NFC", Toast.LENGTH_SHORT).show()
            }
        } ?: Log.d(TAG, "NFC no disponible, omitiendo enableForegroundDispatch")
    }

    /**
     * Método llamado cuando la actividad se pausa.
     * Deshabilita el despacho en primer plano para NFC si está disponible.
     */
    override fun onPause() {
        super.onPause()
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                adapter.disableForegroundDispatch(this)
                Log.d(TAG, "Foreground dispatch deshabilitado")
            }
        }
    }

    /**
     * Método llamado cuando se recibe un nuevo Intent.
     * Procesa el Intent para detectar tags NFC.
     *
     * @param intent El nuevo Intent recibido.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent llamado con intent: $intent, action: ${intent.action}")
        processNfcIntent(intent)
    }

    /**
     * Procesa un Intent para detectar y manejar tags NFC.
     *
     * @param intent Intent que potencialmente contiene información de un tag NFC.
     */
    private fun processNfcIntent(intent: Intent?) {
        Log.d(TAG, "Procesando intent: $intent")
        if (intent?.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
            }
            tag?.let {
                nfcViewModel.setNfcTag(it)
                Log.d(TAG, "Tag NFC detectado: ${it.id.joinToString { byte -> byte.toString(16) }}")
            } ?: run {
                Log.w(TAG, "No se detectó ningún tag NFC en el intent: $intent")
                Toast.makeText(this, "No se detectó un tag NFC válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Función composable que maneja el enrutamiento de contenido principal.
     * Muestra diferentes pantallas según el estado de permisos y carga de datos.
     */
    @Composable
    private fun ContentRouter() {
        val navController = rememberNavController()
        val context = LocalContext.current

        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                !arePermissionsGranted -> PermissionDeniedScreen(
                    onRetry = { checkAndRequestPermissions() },
                    onOpenSettings = { openAppSettings() }
                )
                !isDatabaseLoaded -> SplashScreen(loadingStatus = loadingStatus)
                else -> {
                    NavHost(navController = navController, startDestination = "MainScreen") {
                        appNavigation(
                            navController = navController,
                            personaViewModel = personaViewModel,
                            nfcViewModel = nfcViewModel
                        )
                    }
                }
            }
        }
    }

    /**
     * Configura el lanzador de permisos para manejar la respuesta de la solicitud de permisos.
     */
    private fun configurePermissionLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            arePermissionsGranted = permissions.all { it.value }
            if (arePermissionsGranted) {
                Log.d(TAG, "Todos los permisos concedidos")
                loadDatabases()
            } else {
                Log.w(TAG, "Algunos permisos denegados")
                showDeniedToast()
            }
        }
    }

    /**
     * Verifica y solicita los permisos necesarios para la aplicación.
     */
    private fun checkAndRequestPermissions() {
        requiredPermissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.NFC)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()

        arePermissionsGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (!arePermissionsGranted) {
            Log.d(TAG, "Solicitando permisos: ${requiredPermissions.joinToString()}")
            if (shouldShowRationale()) showPermissionRationaleDialog()
            else permissionLauncher.launch(requiredPermissions)
        } else {
            Log.d(TAG, "Permisos ya concedidos, cargando bases de datos")
            loadDatabases()
        }
    }

    /**
     * Verifica si se debe mostrar la explicación racional para los permisos.
     *
     * @return true si se debe mostrar la explicación, false en caso contrario.
     */
    private fun shouldShowRationale(): Boolean {
        return requiredPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
    }

    /**
     * Muestra un diálogo explicando la necesidad de los permisos.
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage("La aplicación necesita acceder a estos recursos, incluyendo la ubicación GPS, para funcionar correctamente.")
            .setPositiveButton("Conceder") { _, _ -> permissionLauncher.launch(requiredPermissions) }
            .setNegativeButton("Configuración") { _, _ -> openAppSettings() }
            .show()
    }

    /**
     * Abre la configuración de la aplicación para que el usuario pueda modificar los permisos manualmente.
     */
    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    /**
     * Muestra un mensaje indicando que algunos permisos fueron denegados.
     */
    private fun showDeniedToast() {
        Toast.makeText(this, "Funcionalidad limitada sin permisos, incluyendo ubicación GPS", Toast.LENGTH_LONG).show()
        loadDatabases()
    }

    /**
     * Carga las bases de datos necesarias para la aplicación en un hilo secundario.
     */
    private fun loadDatabases() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            listOf("paises.db", "juzgados.db", "dispositivos.db").forEach { dbName ->
                loadingStatus = "Cargando $dbName..."
                Log.d(TAG, "Cargando base de datos: $dbName")

                val dbHelper = AccesoBaseDatos(this@MainActivity, dbName)
                dbHelper.initializeDatabases()
            }
            loadingStatus = "Bases de datos listas"
            Log.d(TAG, "Bases de datos cargadas exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando bases de datos: ${e.message}", e)
            loadingStatus = "Error cargando datos"
        } finally {
            withContext(Dispatchers.Main) { isDatabaseLoaded = true }
        }
    }
}

/**
 * Pantalla que se muestra cuando los permisos necesarios no han sido concedidos.
 *
 * @param onRetry Función llamada cuando el usuario presiona el botón de reintentar.
 * @param onOpenSettings Función llamada cuando el usuario presiona el botón para abrir configuración.
 */
@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Se requieren permisos, incluyendo ubicación GPS, para el funcionamiento completo de la aplicación",
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Reintentar permisos") }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings) { Text("Abrir Configuración") }
        }
    }
}

/**
 * Pantalla de presentación que se muestra mientras se cargan los datos iniciales.
 *
 * @param loadingStatus Estado actual del proceso de carga.
 */
@Composable
fun SplashScreen(loadingStatus: String) {
    var showLoadingStatus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
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
            CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
            if (showLoadingStatus) {
                Spacer(Modifier.height(16.dp))
                Text(text = loadingStatus, fontSize = 14.sp, color = BlueGray700)
            }
        }
    }
}