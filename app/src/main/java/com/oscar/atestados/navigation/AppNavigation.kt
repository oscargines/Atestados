package com.oscar.atestados.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.screens.Alcoholemia01Screen
import com.oscar.atestados.screens.MainScreen
import com.oscar.atestados.screens.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController,
        startDestination = AppScrens.SplashScreen.route){
        composable(AppScrens.SplashScreen.route){
            SplashScreen(navController)
        }
        composable(AppScrens.MainScreen.route){
            MainScreen()
        }
        composable(AppScrens.Alcoholemia01Screen.route){
            Alcoholemia01Screen()

        }



    }
}