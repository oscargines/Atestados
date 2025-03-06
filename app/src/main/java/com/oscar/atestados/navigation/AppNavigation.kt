package com.oscar.atestados.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.screens.Alcoholemia01Screen
import com.oscar.atestados.screens.Alcoholemia02Screen
import com.oscar.atestados.screens.CarecerScreen
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
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.viewModel.VehiculoViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.LecturaDerechosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModel
import com.oscar.atestados.viewModel.OtrosDocumentosViewModelFactory
import com.oscar.atestados.viewModel.TomaDerechosViewModel
import com.oscar.atestados.viewModel.TomaManifestacionAlcoholViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    val navController = rememberNavController()
    val bluetoothViewModel: BluetoothViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "MainScreen"
    ) {
        composable("MainScreen") {
            MainScreen { route ->
                navController.navigate(route)
            }
        }

        composable("PersonaScreen") {
            val personaViewModel: PersonaViewModel = viewModel()
            PersonaScreen(
                navigateToScreen = { route ->
                    navController.navigate("MainScreen")
                },
                personaViewModel = personaViewModel
            )
        }

        composable("VehiculoScreen") {
            val vehiculoViewModel: VehiculoViewModel = viewModel()
            VehiculoScreen(
                navigateToScreen = { route ->
                    navController.navigate("MainScreen")
                },
                vehiculoViewModel = vehiculoViewModel
            )
        }

        composable("Alcoholemia01Screen") {
            val alcoholemiaUnoViewModel: AlcoholemiaUnoViewModel = viewModel()
            Alcoholemia01Screen(
                navigateToScreen = { route ->
                    navController.navigate(route)
                },
                alcoholemiaUnoViewModel = alcoholemiaUnoViewModel
            )
        }

        composable("CarecerScreen") {
            CarecerScreen {
                navController.navigate("MainScreen") {
                    popUpTo("MainScreen") { inclusive = true }
                }
            }
        }

        composable("OtrosDocumentosScreen") {
            val context = LocalContext.current // Obtén el contexto de la aplicación
            val factory = remember {
                OtrosDocumentosViewModelFactory(context = context)
            }
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

        composable("ImpresoraScreen") {
            val factory = remember {
                ImpresoraViewModelFactory(
                    bluetoothViewModel = bluetoothViewModel,
                    context = navController.context
                )
            }
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

        composable("LecturaDerechosScreen") {
            val lecturaDerechosViewModel: LecturaDerechosViewModel = viewModel()
            LecturaDerechosScreen(
                navigateToScreen = { route ->
                    navController.navigate(route)
                },
                lecturaDerechosViewModel = lecturaDerechosViewModel
            )
        }
        composable("TomaDerechosScreen") {
            val tomaDerechosViewModel: TomaDerechosViewModel = viewModel()
            TomaDerechosScreen(
                navigateToScreen = { route ->
                    navController.navigate(route)
                },
                tomaDerechosViewModel = tomaDerechosViewModel
            )
        }
        composable("TomaManifestacionAlcoholScreen") {
            val tomaManifestacionAlcoholViewModel: TomaManifestacionAlcoholViewModel = viewModel()
            TomaManifestacionAlcoholScreen(
                navigateToScreen = { route ->
                    navController.navigate(route)
                },
                tomaManifestacionAlcoholViewModel = tomaManifestacionAlcoholViewModel
            )
        }
        composable("Alcoholemia02Screen") {
            val alcoholemiaDosViewModel: AlcoholemiaDosViewModel = viewModel()
            Alcoholemia02Screen(
                navigateToScreen = { route ->
                    navController.navigate(route)
                },
                alcoholemiaDosViewModel = alcoholemiaDosViewModel
            )
        }
        composable("InformacionScreen") {
            InformacionScreen { route ->
                navController.navigate(route) {
                    popUpTo("MainScreen") { inclusive = true }
                }
            }
        }
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