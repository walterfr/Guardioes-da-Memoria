package br.com.guardioesdamemoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import br.com.guardioesdamemoria.ui.camera.CameraPreviewContent
import br.com.guardioesdamemoria.ui.map.MapScreen
import br.com.guardioesdamemoria.ui.registration.RegistrationScreen
import br.com.guardioesdamemoria.ui.library.LibraryScreen
import br.com.guardioesdamemoria.ui.badges.BadgesScreen
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import br.com.guardioesdamemoria.ui.theme.GuardioesDaMemoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardioesDaMemoriaTheme {
                val viewModel: LocationViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LocationViewModel) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("AR") },
                    selected = currentDestination?.route == "camera",
                    onClick = { navController.navigate("camera") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Mapa") },
                    selected = currentDestination?.route == "map",
                    onClick = { navController.navigate("map") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Álbum") },
                    selected = currentDestination?.route == "library",
                    onClick = { navController.navigate("library") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Insígnias") },
                    selected = currentDestination?.route == "badges",
                    onClick = { navController.navigate("badges") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Cadastro") },
                    selected = currentDestination?.route == "registration",
                    onClick = { navController.navigate("registration") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "camera",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("camera") { CameraPreviewContent(viewModel) }
            composable("map") { MapScreen(viewModel) }
            composable("library") { LibraryScreen(viewModel) }
            composable("badges") { BadgesScreen(viewModel) }
            composable("registration") { 
                RegistrationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                ) 
            }
        }
    }
}
