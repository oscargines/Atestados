package com.oscar.atestados.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.screens.Alcoholemia01Screen
import com.oscar.atestados.screens.CarecerScreen
import com.oscar.atestados.screens.GuardiasScreen
import com.oscar.atestados.screens.ImpresoraScreen
import com.oscar.atestados.screens.LecturaDerechosScreen
import com.oscar.atestados.screens.MainScreen
import com.oscar.atestados.screens.OtrosDocumentosScreen
import com.oscar.atestados.screens.PersonaScreen
import com.oscar.atestados.screens.SplashScreen
import com.oscar.atestados.screens.VehiculoScreen
import com.oscar.atestados.viewModel.AlcoholemiaUnoViewModel
import com.oscar.atestados.viewModel.BluetoothViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.viewModel.VehiculoViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModel
import com.oscar.atestados.viewModel.LecturaDerechosViewModel

@Composable
fun AppNavigation() {
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
            OtrosDocumentosScreen {
                navController.navigate("MainScreen") {
                    popUpTo("MainScreen") { inclusive = true }
                }
            }
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
                navigateToScreen = {
                    navController.navigate("MainScreen") {
                        popUpTo("MainScreen") { inclusive = true }
                    }
                },
                lecturaDerechosViewModel = lecturaDerechosViewModel
            )
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