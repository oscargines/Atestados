package com.oscar.atestados.navigation

sealed class AppScrens (val route: String) {

    object SplashScreen: AppScrens("splash_screen")
    object MainScreen: AppScrens("main_screen")
    object Alcolemia01Screen: AppScrens("Alcolemia01Screen")
    object RegisterScreen: AppScrens("register_screen")


}