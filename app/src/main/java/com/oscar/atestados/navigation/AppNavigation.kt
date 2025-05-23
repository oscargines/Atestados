package com.oscar.atestados.navigation

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.oscar.atestados.screens.*
import com.oscar.atestados.viewModel.AlcoholemiaDosViewModel
import com.oscar.atestados.viewModel.AlcoholemiaUnoViewModel
import com.oscar.atestados.viewModel.BluetoothViewModel
import com.oscar.atestados.viewModel.CitacionViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.LecturaDerechosViewModel
import com.oscar.atestados.viewModel.NfcViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.viewModel.TomaDerechosViewModel
import com.oscar.atestados.viewModel.TomaManifestacionAlcoholViewModel
import com.oscar.atestados.viewModel.VehiculoViewModel

private const val TAG = "AppNavigation"

fun NavGraphBuilder.appNavigation(
    navController: NavHostController,
    personaViewModel: PersonaViewModel,
    nfcViewModel: NfcViewModel,
    version: String = "1.0.0"
) {
    composable(
        route = "MainScreen?showExitDialog={showExitDialog}",
        arguments = listOf(navArgument("showExitDialog") { type = NavType.BoolType; defaultValue = false })
    ) { backStackEntry ->
        val showExitDialog = backStackEntry.arguments?.getBoolean("showExitDialog") ?: false
        MainScreen(
            navigateToScreen = { route ->
                Log.d(TAG, "Navegando desde MainScreen a: $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            version = version,
            showExitDialog = showExitDialog
        )
    }

    composable("PersonaScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en PersonaScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        PersonaScreen(
            navigateToScreen = {
                Log.d(TAG, "navigateToScreen en PersonaScreen: Navegando a MainScreen")
                navController.navigate("MainScreen") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            personaViewModel = personaViewModel,
            nfcViewModel = nfcViewModel,
            onTagProcessed = { nfcViewModel.clearNfcTag() }
        )
    }

    composable("VehiculoScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en VehiculoScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val vehiculoViewModel: VehiculoViewModel = viewModel()
        VehiculoScreen(
            navigateToScreen = {
                Log.d(TAG, "navigateToScreen en VehiculoScreen: Navegando a MainScreen")
                navController.navigate("MainScreen") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            vehiculoViewModel = vehiculoViewModel
        )
    }

    composable("Alcoholemia01Screen") {
        BackHandler {
            Log.d(TAG, "BackHandler en Alcoholemia01Screen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel = viewModel()
        Alcoholemia01Screen(
            navigateToScreen = { route ->
                Log.d(TAG, "navigateToScreen en Alcoholemia01Screen: Navegando a $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            alcoholemiaUnoViewModel = alcoholemiaUnoViewModel
        )
    }

    composable("CarecerScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en CarecerScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        CarecerScreen {
            Log.d(TAG, "navigateToScreen en CarecerScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    composable("OtrosDocumentosScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en OtrosDocumentosScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val factory = OtrosDocumentosViewModelFactory(context = navController.context)
        val otrosDocumentosViewModel: OtrosDocumentosViewModel = viewModel(factory = factory)
        OtrosDocumentosScreen(
            navigateToScreen = { route ->
                Log.d(TAG, "navigateToScreen en OtrosDocumentosScreen: Navegando a $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            otrosDocumentosViewModel = otrosDocumentosViewModel
        )
    }

    composable("GuardiasScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en GuardiasScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val guardiasViewModel: GuardiasViewModel = viewModel()
        GuardiasScreen(
            navigateToScreen = {
                Log.d(TAG, "navigateToScreen en GuardiasScreen: Navegando a MainScreen")
                navController.navigate("MainScreen") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            guardiasViewModel = guardiasViewModel
        )
    }

    composable("ImpresoraScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en ImpresoraScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val bluetoothViewModel: BluetoothViewModel = viewModel()
        val factory = ImpresoraViewModelFactory(
            bluetoothViewModel = bluetoothViewModel,
            context = navController.context
        )
        val impresoraViewModel: ImpresoraViewModel = viewModel(factory = factory)
        ImpresoraScreen(
            navigateToScreen = {
                Log.d(TAG, "navigateToScreen en ImpresoraScreen: Navegando a MainScreen")
                navController.navigate("MainScreen") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            impresoraViewModel = impresoraViewModel
        )
    }

    composable("LecturaDerechosScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en LecturaDerechosScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val lecturaDerechosViewModel: LecturaDerechosViewModel = viewModel()
        LecturaDerechosScreen(
            navigateToScreen = { route ->
                Log.d(TAG, "navigateToScreen en LecturaDerechosScreen: Navegando a $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            lecturaDerechosViewModel = lecturaDerechosViewModel
        )
    }

    composable("TomaDerechosScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en TomaDerechosScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val tomaDerechosViewModel: TomaDerechosViewModel = viewModel()
        TomaDerechosScreen(
            navigateToScreen = { route ->
                Log.d(TAG, "navigateToScreen en TomaDerechosScreen: Navegando a $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            tomaDerechosViewModel = tomaDerechosViewModel
        )
    }

    composable("TomaManifestacionAlcoholScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en TomaManifestacionAlcoholScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val tomaManifestacionAlcoholViewModel: TomaManifestacionAlcoholViewModel = viewModel()
        TomaManifestacionAlcoholScreen(
            navigateToScreen = { route ->
                Log.d(TAG, "navigateToScreen en TomaManifestacionAlcoholScreen: Navegando a $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            tomaManifestacionAlcoholViewModel = tomaManifestacionAlcoholViewModel
        )
    }

    composable("Alcoholemia02Screen") {
        BackHandler {
            Log.d(TAG, "BackHandler en Alcoholemia02Screen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
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
                Log.d(TAG, "navigateToScreen en Alcoholemia02Screen: Navegando a $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            alcoholemiaDosViewModel = alcoholemiaDosViewModel,
            alcoholemiaUnoViewModel = alcoholemiaUnoViewModel,
            personaViewModel = personaViewModel,
            vehiculoViewModel = vehiculoViewModel,
            tomaDerechosViewModel = tomaDerechosViewModel,
            tomaManifestacionViewModel = tomaManifestacionViewModel,
            lecturaDerechosViewModel = lecturaDerechosViewModel,
            guardiasViewModel = guardiasViewModel,
            impresoraViewModel = impresoraViewModel
        )
    }

    composable("InformacionScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en InformacionScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        InformacionScreen { route ->
            Log.d(TAG, "navigateToScreen en InformacionScreen: Navegando a $route")
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    composable("CitacionScreen") {
        BackHandler {
            Log.d(TAG, "BackHandler en CitacionScreen: Navegando a MainScreen")
            navController.navigate("MainScreen") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
        val citacionViewModel: CitacionViewModel = viewModel()
        val guardiasViewModel: GuardiasViewModel = viewModel()
        val alcoholemiaDosViewModel: AlcoholemiaDosViewModel = viewModel()
        val bluetoothViewModel: BluetoothViewModel = viewModel()
        val factory = ImpresoraViewModelFactory(
            bluetoothViewModel = bluetoothViewModel,
            context = navController.context
        )
        val impresoraViewModel: ImpresoraViewModel = viewModel(factory = factory)
        CitacionScreen(
            navigateToScreen = { route ->
                Log.d(TAG, "navigateToScreen en CitacionScreen: Navegando a $route")
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            },
            citacionViewModel = citacionViewModel,
            personaViewModel = personaViewModel,
            guardiasViewModel = guardiasViewModel,
            alcoholemiaDosViewModel = alcoholemiaDosViewModel,
            impresoraViewModel = impresoraViewModel
        )
    }
}

class ImpresoraViewModelFactory(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImpresoraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImpresoraViewModel(bluetoothViewModel, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class OtrosDocumentosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtrosDocumentosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtrosDocumentosViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}