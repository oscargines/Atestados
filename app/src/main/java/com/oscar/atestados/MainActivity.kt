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
 * Constante para el tag de logging utilizado en toda la actividad.
 */
private const val TAG = "MainActivity"

/**
 * Actividad principal de la aplicación que gestiona NFC, permisos y navegación.
 * Solicita permisos necesarios, incluyendo GPS, y carga bases de datos iniciales.
 */
class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null // Adaptador NFC, nullable para manejar dispositivos sin soporte
    private lateinit var personaViewModel: PersonaViewModel // ViewModel para gestionar datos de personas
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>> // Lanzador para solicitud de permisos
    private lateinit var requiredPermissions: Array<String> // Lista de permisos requeridos
    private var pendingNfcTag by mutableStateOf<Tag?>(null) // Tag NFC pendiente de procesamiento
    private var arePermissionsGranted by mutableStateOf(false) // Estado de permisos concedidos
    private var isDatabaseLoaded by mutableStateOf(false) // Estado de carga de bases de datos
    private var loadingStatus by mutableStateOf("") // Estado de carga para mostrar en pantalla

    /**
     * Método llamado al crear la actividad. Inicializa el ViewModel, NFC y permisos.
     *
     * @param savedInstanceState Estado guardado de la actividad, si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        personaViewModel = ViewModelProvider(this)[PersonaViewModel::class.java]

        // Inicializar NFC de manera segura
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
     * Habilita el despacho de NFC en primer plano cuando la actividad se reanuda.
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
     * Deshabilita el despacho de NFC en primer plano cuando la actividad se pausa.
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
     * Maneja nuevos intents recibidos, como la detección de tags NFC.
     *
     * @param intent El intent recibido.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent llamado con intent: $intent, action: ${intent.action}")
        processNfcIntent(intent)
    }

    /**
     * Procesa un intent para detectar tags NFC y almacenarlos como pendientes.
     *
     * @param intent El intent a procesar, puede contener información de NFC.
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
                pendingNfcTag = it
                Log.d(TAG, "Tag NFC detectado: ${it.id.joinToString { byte -> byte.toString(16) }}")
                Log.d(TAG, "Tecnologías soportadas: ${it.techList.joinToString()}")
            } ?: run {
                Log.w(TAG, "No se detectó ningún tag NFC en el intent: $intent")
                Toast.makeText(this, "No se detectó un tag NFC válido", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Intent no es ACTION_TECH_DISCOVERED: ${intent?.action}")
        }
    }

    /**
     * Componente Composable que define el enrutamiento de la interfaz según permisos y estado.
     */
    @Composable
    private fun ContentRouter() {
        val navController = rememberNavController()
        val context = LocalContext.current
        val localNfcAdapter = NfcAdapter.getDefaultAdapter(context)
        var shouldNavigateToPersona by remember { mutableStateOf(false) }

        LaunchedEffect(isDatabaseLoaded, pendingNfcTag) {
            if (isDatabaseLoaded && pendingNfcTag != null && localNfcAdapter?.isEnabled == true) {
                Log.d(
                    TAG,
                    "Condiciones cumplidas: isDatabaseLoaded=$isDatabaseLoaded, pendingNfcTag=$pendingNfcTag, NFC habilitado=${localNfcAdapter.isEnabled}"
                )
                shouldNavigateToPersona = true
            } else {
                Log.d(
                    TAG,
                    "Condiciones no cumplidas: isDatabaseLoaded=$isDatabaseLoaded, pendingNfcTag=$pendingNfcTag, NFC habilitado=${localNfcAdapter?.isEnabled}"
                )
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                !arePermissionsGranted -> PermissionDeniedScreen(
                    onRetry = { checkAndRequestPermissions() },
                    onOpenSettings = { openAppSettings() }
                )
                !isDatabaseLoaded -> SplashScreen(loadingStatus = loadingStatus)
                else -> {
                    NavHost(navController, startDestination = "MainScreen") {
                        composable("MainScreen") {
                            AppNavigation(navController)
                            LaunchedEffect(shouldNavigateToPersona) {
                                if (shouldNavigateToPersona) {
                                    Log.d(TAG, "Navegando a PersonaScreen con nfcTag: $pendingNfcTag")
                                    navController.navigate("PersonaScreen")
                                    shouldNavigateToPersona = false
                                }
                            }
                        }
                        composable("PersonaScreen") {
                            PersonaScreen(
                                navigateToScreen = { route ->
                                    navController.navigate(route) {
                                        popUpTo("MainScreen") { inclusive = false }
                                    }
                                },
                                personaViewModel = personaViewModel,
                                nfcTag = pendingNfcTag,
                                onTagProcessed = {
                                    pendingNfcTag = null
                                    Log.d(TAG, "Tag NFC procesado y limpiado desde PersonaScreen")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Configura el lanzador de permisos para manejar múltiples solicitudes.
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
     * Verifica y solicita los permisos necesarios, incluyendo GPS para ubicación.
     */
    private fun checkAndRequestPermissions() {
        requiredPermissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
                add(Manifest.permission.ACCESS_FINE_LOCATION) // Necesario para GPS en versiones antiguas
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.NFC)
            // Permisos de ubicación para GPS
            add(Manifest.permission.ACCESS_FINE_LOCATION) // Precisión fina para GPS
            add(Manifest.permission.ACCESS_COARSE_LOCATION) // Precisión aproximada como respaldo
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
        requiredPermissions.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Permiso $permission: ${if (isGranted) "concedido" else "no concedido"}")
        }
    }

    /**
     * Determina si se debe mostrar un diálogo explicativo para los permisos.
     *
     * @return Verdadero si algún permiso requiere una explicación, falso en caso contrario.
     */
    private fun shouldShowRationale(): Boolean {
        return requiredPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
    }

    /**
     * Muestra un diálogo explicativo antes de solicitar permisos si es necesario.
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
     * Abre la configuración de la aplicación para que el usuario gestione los permisos manualmente.
     */
    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    /**
     * Muestra un mensaje toast cuando algunos permisos son denegados.
     */
    private fun showDeniedToast() {
        Toast.makeText(this, "Funcionalidad limitada sin permisos, incluyendo ubicación GPS", Toast.LENGTH_LONG).show()
        loadDatabases()
    }

    /**
     * Carga las bases de datos necesarias en un hilo de IO.
     */
    private fun loadDatabases() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            listOf("paises.db", "juzgados.db", "dispositivos.db").forEach { dbName ->
                loadingStatus = "Cargando $dbName..."
                Log.d(TAG, "Cargando base de datos: $dbName")
                AccesoBaseDatos(this@MainActivity, dbName, 1).use { it.createDatabase() }
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
 * Pantalla mostrada cuando los permisos necesarios no están concedidos.
 *
 * @param onRetry Acción al intentar solicitar permisos nuevamente.
 * @param onOpenSettings Acción para abrir la configuración de la aplicación.
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
 * Pantalla de carga mostrada mientras se inicializan las bases de datos.
 *
 * @param loadingStatus Mensaje de estado actual de la carga.
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