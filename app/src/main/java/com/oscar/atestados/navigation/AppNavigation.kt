package com.oscar.atestados.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.screens.Alcoholemia01Screen
import com.oscar.atestados.screens.CarecerScreen
import com.oscar.atestados.screens.MainScreen
import com.oscar.atestados.screens.PersonaScreen
import com.oscar.atestados.screens.SplashScreen
import com.oscar.atestados.screens.VehiculoScreen
import com.oscar.atestados.viewModel.PersonaViewModel
import com.oscar.atestados.viewModel.VehiculoViewModel

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
            Alcoholemia01Screen()
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
    }
}
