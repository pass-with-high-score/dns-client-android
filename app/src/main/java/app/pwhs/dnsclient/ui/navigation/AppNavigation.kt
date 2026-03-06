package app.pwhs.dnsclient.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    DestinationsNavHost(
        navGraph = NavGraphs.root,
        navController = navController
    )
}
