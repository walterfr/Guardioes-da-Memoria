package br.com.guardioesdamemoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import br.com.guardioesdamemoria.ui.camera.CameraScreen
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
                MainContent(viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: LocationViewModel) {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinish = { showSplash = false })
        return
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavigationItem("Explorar", "camera", Icons.Default.Search),
                    NavigationItem("Mapa Vivo", "map", Icons.Default.LocationOn),
                    NavigationItem("Acervo", "library", Icons.Default.List),
                    NavigationItem("Conquistas", "badges", Icons.Default.Star),
                    NavigationItem("Registrar", "registration", Icons.Default.Add)
                )
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.name) },
                        label = { Text(item.name, style = MaterialTheme.typography.labelSmall) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "camera",
            modifier = Modifier.padding(padding)
        ) {
            composable("camera") { 
                CameraScreen(
                    viewModel = viewModel,
                    onNavigateToRegistration = { navController.navigate("registration") }
                ) 
            }
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

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GUARDIÕES DA MEMÓRIA",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "📍 Fortaleza, Ceará",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "🕰️ \"As ruas guardam histórias...\"",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ENTRAR NA EXPLORAÇÃO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class NavigationItem(val name: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
