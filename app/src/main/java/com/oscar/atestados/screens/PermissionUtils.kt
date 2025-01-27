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

object PermissionUtils {

    // Permisos de lectura y escritura en almacenamiento externo
    private val storagePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Verifica si los permisos ya están concedidos.
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
     * Solicita los permisos de almacenamiento.
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