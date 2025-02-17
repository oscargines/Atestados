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
import com.oscar.atestados.screens.MainScreen
import com.oscar.atestados.screens.PersonaScreen
import com.oscar.atestados.screens.SplashScreen
import com.oscar.atestados.screens.VehiculoScreen
import com.oscar.atestados.viewModel.BluetoothViewModel
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.viewModel.VehiculoViewModel
import com.oscar.atestados.viewModel.GuardiasViewModel
import com.oscar.atestados.viewModel.ImpresoraViewModel

/**
 * Función principal de navegación de la aplicación.
 * Define la estructura de navegación entre las diferentes pantallas de la aplicación utilizando Jetpack Navigation.
 *
 * Esta función configura un [NavHost] que gestiona la navegación entre las pantallas definidas en la aplicación.
 * Cada pantalla está asociada a una ruta específica y se define mediante el método [composable].
 *
 * @see NavHost
 * @see NavController
 * @see rememberNavController
 * @see composable
 */
@Composable
fun AppNavigation() {
    // Controlador de navegación que gestiona la navegación entre las pantallas.
    val navController = rememberNavController()

    // Creamos una instancia del BluetoothViewModel que será compartida
    val bluetoothViewModel = viewModel<BluetoothViewModel>()

    // Configuración del NavHost, que define la estructura de navegación de la aplicación.
    NavHost(
        navController = navController,
        startDestination = "MainScreen"  // Pantalla inicial de la aplicación.
    ) {
        // Definición de la pantalla "MainScreen".
        composable("MainScreen") {
            MainScreen { route ->
                // Navega a la pantalla especificada por la ruta.
                navController.navigate(route)
            }
        }

        // Definición de la pantalla "PersonaScreen".
        composable("PersonaScreen") {
            // Obtiene una instancia de ViewModel asociada a la pantalla.
            val personaViewModel = viewModel<PersonaViewModel>()
            PersonaScreen(
                navigateToScreen = { route ->
                    // Navega de vuelta a la pantalla principal ("MainScreen").
                    navController.navigate("MainScreen")
                },
                personaViewModel = personaViewModel
            )
        }

        // Definición de la pantalla "VehiculoScreen".
        composable("VehiculoScreen") {
            val vehiculoViewModel = viewModel<VehiculoViewModel>()
            VehiculoScreen(
                navigateToScreen = { route ->
                    // Navega de vuelta a la pantalla principal ("MainScreen").
                    navController.navigate("MainScreen")
                },
                vehiculoViewModel = vehiculoViewModel
            )
        }

        // Definición de la pantalla "Alcoholemia01Screen".
        composable("Alcoholemia01Screen") {
            Alcoholemia01Screen(
                navigateToScreen = { route ->
                    // Navega de vuelta a la pantalla principal ("MainScreen").
                    navController.navigate("MainScreen")
                },
                alcoholemiaUnoViewModel = viewModel()
            )
        }

        // Definición de la pantalla "CarecerScreen".
        composable("CarecerScreen") {
            CarecerScreen {
                // Navega de vuelta a la pantalla principal ("MainScreen") y limpia la pila de navegación.
                navController.navigate("MainScreen") {
                    popUpTo("MainScreen") {
                        inclusive = true  // Elimina todas las pantallas anteriores de la pila.
                    }
                }
            }
        }
        // Definición de la pantalla "GuardiasScreen".
        composable("GuardiasScreen") {
            val guardiasViewModel = viewModel<GuardiasViewModel>()
            GuardiasScreen(
                navigateToScreen = {
                    navController.navigate("MainScreen") {
                        popUpTo("MainScreen") {
                            inclusive = true
                        }
                    }
                },
                guardiasViewModel = guardiasViewModel
            )
        }
        // Definición de la pantalla "ImpresoraScreen".
        composable("ImpresoraScreen") {
            val context = LocalContext.current
            
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
                        popUpTo("MainScreen") {
                            inclusive = true
                        }
                    }

                },
                impresoraViewModel = impresoraViewModel
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
