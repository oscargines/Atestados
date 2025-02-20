package com.oscar.atestados
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
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
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var requiredPermissions = emptyArray<String>()

    // Estados
    private var arePermissionsGranted by mutableStateOf(false)
    private var isDatabaseLoaded by mutableStateOf(false)
    private var loadingStatus by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configurePermissionLauncher()
        checkAndRequestPermissions()

        setContent {
            AtestadosTheme {
                ContentRouter()
            }
        }
    }

    @Composable
    private fun ContentRouter() {
        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                !arePermissionsGranted -> PermissionDeniedScreen(
                    onRetry = { checkAndRequestPermissions() },
                    onOpenSettings = { openAppSettings() }
                )

                !isDatabaseLoaded -> SplashScreen(loadingStatus = loadingStatus)

                else -> AppNavigation()
            }
        }
    }

    private fun configurePermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            arePermissionsGranted = permissions.all { it.value }
            if (arePermissionsGranted) loadDatabases()
            else showDeniedToast()
        }
    }

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

            add(Manifest.permission.CAMERA)
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

    private fun shouldShowRationale() = requiredPermissions.any {
        ActivityCompat.shouldShowRequestPermissionRationale(this, it)
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage("La aplicación necesita acceder a estos recursos para funcionar correctamente.")
            .setPositiveButton("Conceder") { _, _ -> permissionLauncher.launch(requiredPermissions) }
            .setNegativeButton("Configuración") { _, _ -> openAppSettings() }
            .show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    private fun showDeniedToast() {
        Toast.makeText(this, "Funcionalidad limitada sin permisos", Toast.LENGTH_LONG).show()
        loadDatabases() // Cargar BD incluso sin permisos si es posible
    }

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
}

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