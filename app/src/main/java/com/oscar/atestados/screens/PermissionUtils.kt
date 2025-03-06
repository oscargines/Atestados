package com.oscar.atestados.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Utilidad para gestionar permisos de almacenamiento en la aplicación.
 *
 * Este objeto proporciona métodos para verificar y solicitar permisos de lectura y escritura
 * en el almacenamiento externo, necesarios para ciertas funcionalidades de la aplicación.
 */
object PermissionUtils {

    // Permisos de lectura y escritura en almacenamiento externo
    private val storagePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Verifica si los permisos de almacenamiento ya están concedidos.
     *
     * Comprueba si todos los permisos definidos en [storagePermissions] han sido otorgados
     * en el contexto proporcionado.
     *
     * @param context Contexto de la aplicación usado para verificar los permisos.
     * @return [Boolean] que indica si todos los permisos están concedidos (`true`) o no (`false`).
     */
    fun hasPermissions(context: Context): Boolean {
        return storagePermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Solicita los permisos de almacenamiento al usuario.
     *
     * Registra un lanzador de resultados para solicitar los permisos de almacenamiento externo.
     * Si todos los permisos son concedidos, ejecuta el callback [onPermissionsGranted]. Si no,
     * muestra un mensaje de error.
     *
     * @param activity Actividad de tipo [FragmentActivity] desde la cual se solicitan los permisos.
     * @param onPermissionsGranted Callback que se ejecuta si todos los permisos son concedidos.
     */
    fun requestPermissions(activity: FragmentActivity, onPermissionsGranted: () -> Unit) {
        val permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                onPermissionsGranted() // Llamada a una función normal
            } else {
                // Manejar el caso en que los permisos no sean concedidos
                Toast.makeText(activity, "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }

        // Solicitar los permisos
        permissionLauncher.launch(storagePermissions)
    }
}