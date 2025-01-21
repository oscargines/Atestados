package com.oscar.atestados.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.screens.Alcoholemia01Screen
import com.oscar.atestados.screens.CarecerScreen
import com.oscar.atestados.screens.MainScreen
import com.oscar.atestados.screens.PersonaScreen
import com.oscar.atestados.screens.SplashScreen
import com.oscar.atestados.screens.VehiculoScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "SplashScreen"
    ) {
        composable("SplashScreen") {
            SplashScreen { navController.navigate("MainScreen") }
        }
        composable("MainScreen") {
            MainScreen { route ->
                navController.navigate(route)
            }
        }
        composable("PersonaScreen") {
            PersonaScreen { navController.navigate("MainScreen") }
        }
        composable("VehiculoScreen") {
            VehiculoScreen { navController.navigate("MainScreen") }
        }
        composable("Alcoholemia01Screen") {
            Alcoholemia01Screen()
        }
        composable("CarecerScreen") {
            CarecerScreen {
                navController.navigate("MainScreen") {
                    popUpTo("MainScreen") {
                        inclusive = true
                    }
                }
            }
        }
    }
}