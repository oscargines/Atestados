package com.oscar.atestados.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.oscar.atestados.screens.Alcoholemia01Screen
import com.oscar.atestados.screens.Alcoholemia02Screen
import com.oscar.atestados.screens.CarecerScreen
import com.oscar.atestados.screens.CitacionScreen
import com.oscar.atestados.screens.GuardiasScreen
import com.oscar.atestados.screens.ImpresoraScreen
import com.oscar.atestados.screens.InformacionScreen
import com.oscar.atestados.screens.LecturaDerechosScreen
import com.oscar.atestados.screens.MainScreen
import com.oscar.atestados.screens.OtrosDocumentosScreen
import com.oscar.atestados.screens.PersonaScreen
import com.oscar.atestados.screens.TomaDerechosScreen
import com.oscar.atestados.screens.TomaManifestacionAlcoholScreen
import com.oscar.atestados.screens.VehiculoScreen
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel
import com.oscar.atestados.viewModel.AlcoholemiaUnoViewModel
import com.oscar.atestados.viewModel.BluetoothViewModel
import com.oscar.atestados.viewModel.CitacionViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.LecturaDerechosViewModel
import com.oscar.atestados.viewModel.NfcViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModelFactory
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.viewModel.TomaDerechosViewModel
import com.oscar.atestados.viewModel.TomaManifestacionAlcoholViewModel
import com.oscar.atestados.viewModel.VehiculoViewModel

/**
 * Función composable que inicializa la navegación de la aplicación.
 * Actualmente solo inicializa el ViewModel de Bluetooth.
 *
 * @param navController Controlador de navegación para manejar transiciones entre pantallas.
 * @param personaViewModel ViewModel para manejar datos de personas.
 * @param nfcViewModel ViewModel para manejar funcionalidad NFC.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    personaViewModel: PersonaViewModel,
    nfcViewModel: NfcViewModel
) {
    val bluetoothViewModel: BluetoothViewModel = viewModel()
}

/**
 * Define el grafo de navegación de la aplicación con todas las pantallas disponibles.
 *
 * @param navController Controlador de navegación para manejar transiciones entre pantallas.
 * @param personaViewModel ViewModel para manejar datos de personas.
 * @param nfcViewModel ViewModel para manejar funcionalidad NFC.
 */
fun NavGraphBuilder.appNavigation(
    navController: NavHostController,
    personaViewModel: PersonaViewModel,
    nfcViewModel: NfcViewModel
) {
    /**
     * Pantalla principal de la aplicación.
     */
    composable("MainScreen") {
        MainScreen { route ->
            navController.navigate(route)
        }
    }

    /**
     * Pantalla para manejar información de personas.
     */
    composable("PersonaScreen") {
        PersonaScreen(
            navigateToScreen = { route ->
                navController.navigate("MainScreen")
            },
            personaViewModel = personaViewModel,
            nfcViewModel = nfcViewModel,
            onTagProcessed = { nfcViewModel.clearNfcTag() }
        )
    }

    /**
     * Pantalla para manejar información de vehículos.
     */
    composable("VehiculoScreen") {
        val vehiculoViewModel: VehiculoViewModel = viewModel()
        VehiculoScreen(
            navigateToScreen = { route ->
                navController.navigate("MainScreen")
            },
            vehiculoViewModel = vehiculoViewModel
        )
    }

    /**
     * Pantalla para el primer paso del proceso de alcoholemia.
     */
    composable("Alcoholemia01Screen") {
        val alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel = viewModel()
        Alcoholemia01Screen(
            navigateToScreen = { route ->
                navController.navigate(route)
            },
            alcoholemiaUnoViewModel = alcoholemiaUnoViewModel
        )
    }

    /**
     * Pantalla para el proceso de carecer.
     */
    composable("CarecerScreen") {
        CarecerScreen {
            navController.navigate("MainScreen") {
                popUpTo("MainScreen") { inclusive = true }
            }
        }
    }

    /**
     * Pantalla para otros documentos.
     */
    composable("OtrosDocumentosScreen") {
        val factory = OtrosDocumentosViewModelFactory(context = navController.context)
        val otrosDocumentosViewModel: OtrosDocumentosViewModel = viewModel(factory = factory)
        OtrosDocumentosScreen(
            navigateToScreen = { route ->
                navController.navigate(route) {
                    popUpTo("MainScreen") { inclusive = true }
                }
            },
            otrosDocumentosViewModel = otrosDocumentosViewModel
        )
    }

    /**
     * Pantalla para manejar información de guardias.
     */
    composable("GuardiasScreen") {
        val guardiasViewModel: GuardiasViewModel = viewModel()
        GuardiasScreen(
            navigateToScreen = {
                navController.navigate("MainScreen") {
                    popUpTo("MainScreen") { inclusive = true }
                }
            },
            guardiasViewModel = guardiasViewModel
        )
    }

    /**
     * Pantalla para manejar la funcionalidad de impresión.
     */
    composable("ImpresoraScreen") {
        val bluetoothViewModel: BluetoothViewModel = viewModel()
        val factory = ImpresoraViewModelFactory(
            bluetoothViewModel = bluetoothViewModel,
            context = navController.context
        )
        val impresoraViewModel: ImpresoraViewModel = viewModel(factory = factory)
        ImpresoraScreen(
            navigateToScreen = {
                navController.navigate("MainScreen") {
                    popUpTo("MainScreen") { inclusive = true }
                }
            },
            impresoraViewModel = impresoraViewModel
        )
    }

    /**
     * Pantalla para la lectura de derechos.
     */
    composable("LecturaDerechosScreen") {
        val lecturaDerechosViewModel: LecturaDerechosViewModel = viewModel()
        LecturaDerechosScreen(
            navigateToScreen = { route ->
                navController.navigate(route)
            },
            lecturaDerechosViewModel = lecturaDerechosViewModel
        )
    }

    /**
     * Pantalla para la toma de derechos.
     */
    composable("TomaDerechosScreen") {
        val tomaDerechosViewModel: TomaDerechosViewModel = viewModel()
        TomaDerechosScreen(
            navigateToScreen = { route ->
                navController.navigate(route)
            },
            tomaDerechosViewModel = tomaDerechosViewModel
        )
    }

    /**
     * Pantalla para la toma de manifestación sobre alcohol.
     */
    composable("TomaManifestacionAlcoholScreen") {
        val tomaManifestacionAlcoholViewModel: TomaManifestacionAlcoholViewModel = viewModel()
        TomaManifestacionAlcoholScreen(
            navigateToScreen = { route ->
                navController.navigate(route)
            },
            tomaManifestacionAlcoholViewModel = tomaManifestacionAlcoholViewModel
        )
    }

    /**
     * Pantalla para el segundo paso del proceso de alcoholemia.
     */
    composable("Alcoholemia02Screen") {
        val alcoholemiaDosViewModel: AlcoholemiaDosViewModel = viewModel()
        val alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel = viewModel()
        val vehiculoViewModel: VehiculoViewModel = viewModel()
        val tomaDerechosViewModel: TomaDerechosViewModel = viewModel()
        val tomaManifestacionViewModel: TomaManifestacionAlcoholViewModel = viewModel()
        val lecturaDerechosViewModel: LecturaDerechosViewModel = viewModel()
        val guardiasViewModel: GuardiasViewModel = viewModel()
        val bluetoothViewModel: BluetoothViewModel = viewModel()
        val factory = ImpresoraViewModelFactory(
            bluetoothViewModel = bluetoothViewModel,
            context = navController.context
        )
        val impresoraViewModel: ImpresoraViewModel = viewModel(factory = factory)

        Alcoholemia02Screen(
            navigateToScreen = { route ->
                navController.navigate(route)
            },
            alcoholemiaDosViewModel = alcoholemiaDosViewModel,
            alcoholemiaUnoViewModel = alcoholemiaUnoViewModel,
            personaViewModel = personaViewModel, // Reutilizando desde los parámetros de appNavigation
            vehiculoViewModel = vehiculoViewModel,
            tomaDerechosViewModel = tomaDerechosViewModel,
            tomaManifestacionViewModel = tomaManifestacionViewModel,
            lecturaDerechosViewModel = lecturaDerechosViewModel,
            guardiasViewModel = guardiasViewModel,
            impresoraViewModel = impresoraViewModel
        )
    }

    /**
     * Pantalla de información general.
     */
    composable("InformacionScreen") {
        InformacionScreen { route ->
            navController.navigate(route) {
                popUpTo("MainScreen") { inclusive = true }
            }
        }
    }

    /**
     * Pantalla para manejar citaciones.
     */
    composable("CitacionScreen") {
        val citacionViewModel: CitacionViewModel = viewModel()
        val bluetoothViewModel: BluetoothViewModel = viewModel()
        val factory = ImpresoraViewModelFactory(
            bluetoothViewModel = bluetoothViewModel,
            context = navController.context
        )
        val impresoraViewModel: ImpresoraViewModel = viewModel(factory = factory)
        CitacionScreen(
            navigateToScreen = { route ->
                navController.navigate(route)
            },
            citacionViewModel = citacionViewModel,
            impresoraViewModel = impresoraViewModel
        )
    }
}

/**
 * Factory para crear instancias de ImpresoraViewModel con dependencias inyectadas.
 *
 * @property bluetoothViewModel ViewModel para manejar funcionalidad Bluetooth.
 * @property context Contexto de la aplicación.
 */
class ImpresoraViewModelFactory(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModelProvider.Factory {
    /**
     * Crea una nueva instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel a crear.
     * @return Una nueva instancia del ViewModel.
     * @throws IllegalArgumentException si la clase del ViewModel no es reconocida.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImpresoraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImpresoraViewModel(bluetoothViewModel, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Factory para crear instancias de OtrosDocumentosViewModel con dependencias inyectadas.
 *
 * @property context Contexto de la aplicación.
 */
class OtrosDocumentosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    /**
     * Crea una nueva instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel a crear.
     * @return Una nueva instancia del ViewModel.
     * @throws IllegalArgumentException si la clase del ViewModel no es reconocida.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtrosDocumentosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtrosDocumentosViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}