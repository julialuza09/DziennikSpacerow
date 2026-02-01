package pbs.edu.kurs2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pbs.edu.kurs2.screens.details.DetailsScreen
import pbs.edu.kurs2.screens.home.HomeScreen

@Composable
fun WalkNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = WalkScreens.HomeScreen.name
    ) {
        composable(WalkScreens.HomeScreen.name) {
            HomeScreen(navController)
        }
        composable("${WalkScreens.DetailsScreen.name}/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toIntOrNull()
            DetailsScreen(navController, id)
        }
    }
}
