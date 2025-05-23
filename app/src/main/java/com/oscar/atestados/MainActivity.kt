package com.oscar.atestados

import android.Manifest
import android.content.Context
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
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.ui.theme.AtestadosTheme
import com.oscar.atestados.ui.theme.BlueGray700
import com.oscar.atestados.data.AccesoBaseDatos
import com.oscar.atestados.navigation.appNavigation
import com.oscar.atestados.utils.AppConfig
import com.oscar.atestados.viewModel.NfcViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var personaViewModel: PersonaViewModel
    private lateinit var nfcViewModel: NfcViewModel
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private var arePermissionsGranted by mutableStateOf(false)
    private var isDatabaseLoaded by mutableStateOf(false)
    private var loadingStatus by mutableStateOf("")
    private lateinit var requiredPermissions: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicialización de ViewModels
        personaViewModel = ViewModelProvider(this)[PersonaViewModel::class.java]
        nfcViewModel = ViewModelProvider(this)[NfcViewModel::class.java]

        // Configuración NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this).also { adapter ->
            if (adapter == null) {
                Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_LONG).show()
                Log.w(TAG, "NFC no soportado en este dispositivo")
            }
        }

        // Configuración de permisos
        configurePermissionLauncher()
        checkAndRequestPermissions()

        // Control de versión
        AppConfig.VERSION_NAME.let { currentVersion ->
            getVersionFromPreferences()?.let { storedVersion ->
                if (storedVersion != currentVersion) {
                    saveVersionToPreferences(currentVersion)
                    Log.d(TAG, "Versión actualizada a: $currentVersion")
                } else {
                    Log.d(TAG, "Versión recuperada: $storedVersion")
                }
            } ?: saveVersionToPreferences(currentVersion)
        }

        // UI inicial
        setContent {
            AtestadosTheme {
                navController = rememberNavController()
                ContentRouter(navController, getVersionFromPreferences() ?: AppConfig.VERSION_NAME)
            }
        }

        // Configurar OnBackPressedDispatcher para manejar retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestination = navController.currentBackStackEntry?.destination?.route
                Log.d(TAG, "onBackPressed: currentDestination=$currentDestination")
                if (currentDestination?.startsWith("MainScreen") == true && !isFinishing) {
                    // Mostrar el diálogo de salida navegando con el parámetro
                    navController.navigate("MainScreen?showExitDialog=true")
                } else if (!isFinishing) {
                    Log.d(TAG, "onBackPressed: Navegando a MainScreen")
                    navController.navigate("MainScreen") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            }
        })
        processNfcIntent(intent)
    }

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

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)?.also {
            Log.d(TAG, "Foreground dispatch deshabilitado")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent llamado con intent: $intent, action: ${intent.action}")
        processNfcIntent(intent)
    }

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

    @Composable
    private fun ContentRouter(navController: NavHostController, version: String) {
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
                            nfcViewModel = nfcViewModel,
                            version = version
                        )
                    }
                }
            }
        }
    }

    private fun configurePermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
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
            addAll(listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.NFC,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
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

    private fun shouldShowRationale(): Boolean {
        return requiredPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }
    }

    private fun showPermissionRationaleDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage("La aplicación necesita acceder a estos recursos, incluyendo la ubicación GPS, para funcionar correctamente.")
            .setPositiveButton("Conceder") { _, _ -> permissionLauncher.launch(requiredPermissions) }
            .setNegativeButton("Configuración") { _, _ -> openAppSettings() }
            .show()
    }

    private fun saveVersionToPreferences(version: String) {
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .edit()
            .putString("app_version", version)
            .apply()
    }

    private fun getVersionFromPreferences(): String? {
        return getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .getString("app_version", null)
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    private fun showDeniedToast() {
        Toast.makeText(
            this,
            "Funcionalidad limitada sin permisos, incluyendo ubicación GPS",
            Toast.LENGTH_LONG
        ).show()
        loadDatabases()
    }

    private fun loadDatabases() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            listOf("paises.db", "juzgados.db", "dispositivos.db").forEach { dbName ->
                loadingStatus = "Cargando $dbName..."
                Log.d(TAG, "Cargando base de datos: $dbName")

                AccesoBaseDatos(this@MainActivity, dbName).apply {
                    initializeDatabases()
                }
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

@Composable
fun PermissionDeniedScreen(onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
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

@Composable
fun SplashScreen(loadingStatus: String) {
    var showLoadingStatus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        showLoadingStatus = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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