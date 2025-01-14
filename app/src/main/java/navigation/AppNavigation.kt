package navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oscar.atestados.MainScreen
import com.oscar.atestados.SplashScreen

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

    }
}