package com.oscar.atestados.navigation

sealed class AppScrens (val route: String) {

    object SplashScreen: AppScrens("splash_screen")
    object MainScreen: AppScrens("main_screen")
    object Alcoholemia01Screen: AppScrens("Alcoholemia_01_screen")



}