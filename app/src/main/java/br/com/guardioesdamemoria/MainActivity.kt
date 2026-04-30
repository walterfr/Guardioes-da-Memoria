package br.com.guardioesdamemoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import br.com.guardioesdamemoria.ui.moderation.ModerationScreen
import br.com.guardioesdamemoria.ui.auth.AuthScreen
import br.com.guardioesdamemoria.ui.about.AboutScreen
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import br.com.guardioesdamemoria.ui.theme.GuardioesDaMemoriaTheme
import br.com.guardioesdamemoria.ui.theme.MemoryTeal
import br.com.guardioesdamemoria.ui.theme.NightField

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
    var teacherMode by remember { mutableStateOf(false) }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val noNavBarRoutes = listOf("splash", "auth", "registration", "about")

    Scaffold(
        bottomBar = {
            if (currentRoute !in noNavBarRoutes) {
                NavigationBar(
                    containerColor = NightField,
                    tonalElevation = 0.dp
                ) {
                    val items = listOf(
                        NavigationItem("Explorar", "camera", Icons.Default.Search),
                        NavigationItem("Mapa Vivo", "map", Icons.Default.LocationOn),
                        NavigationItem("Acervo", "library", Icons.AutoMirrored.Filled.List),
                        NavigationItem("Conquistas", "badges", Icons.Default.Star)
                    )
                    
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.name) },
                            label = { Text(item.name, style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == item.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MemoryTeal,
                                selectedTextColor = MemoryTeal,
                                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                                unselectedTextColor = Color.White.copy(alpha = 0.4f),
                                indicatorColor = Color.Transparent
                            ),
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
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(padding)
        ) {
            composable("splash") {
                SplashScreen(
                    viewModel = viewModel,
                    onFinish = { navController.navigate("auth") },
                    onTeacherAccess = {
                        teacherMode = true
                        navController.navigate("moderation")
                    },
                    onAboutClick = { navController.navigate("about") }
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable("auth") {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = { isTeacher ->
                        teacherMode = isTeacher
                        navController.navigate("camera") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            composable("camera") { 
                CameraScreen(
                    viewModel = viewModel,
                    isTeacher = teacherMode,
                    onNavigateToRegistration = { navController.navigate("registration") }
                ) 
            }
            composable("map") { 
                MapScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = { navController.navigate("camera") }
                ) 
            }
            composable("library") { 
                LibraryScreen(
                    viewModel = viewModel,
                    onNavigateToRegistration = { navController.navigate("registration") }
                ) 
            }
            composable("badges") { 
                BadgesScreen(
                    viewModel = viewModel,
                    onOpenTeacher = { navController.navigate("moderation") }
                ) 
            }
            composable("registration") { 
                RegistrationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                ) 
            }
            composable("moderation") {
                ModerationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}


@Composable
fun SplashScreen(
    viewModel: LocationViewModel,
    onFinish: () -> Unit,
    onTeacherAccess: () -> Unit,
    onAboutClick: () -> Unit
) {
    val memories by viewModel.memories.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(NightField),
        contentAlignment = Alignment.Center
    ) {
        // ... (Background e Overlay) ...
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        Box(modifier = Modifier.fillMaxSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color.Transparent, NightField.copy(alpha = 0.8f), NightField)
            )
        ))

        // Ícones de Topo
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onAboutClick) {
                Icon(Icons.Default.Info, contentDescription = "Sobre", tint = Color.White.copy(alpha = 0.3f))
            }
            IconButton(onClick = { showPinDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Config", tint = Color.White.copy(alpha = 0.15f))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            // LOGOTIPO OFICIAL
            Surface(
                modifier = Modifier.size(100.dp),
                color = Color.White,
                shape = CircleShape,
                shadowElevation = 8.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_main),
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "GUARDIÕES DA MEMÓRIA",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            
            Surface(
                modifier = Modifier.padding(top = 12.dp),
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Grande Bom Jardim, Fortaleza",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MemoryTeal)
            ) {
                Text("INICIAR EXPLORAÇÃO", fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${memories.size} memórias para descobrir hoje",
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Acesso Professor") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text("PIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.isTeacherPinValid(pinInput)) {
                        showPinDialog = false
                        onTeacherAccess()
                    }
                }) {
                    Text("Validar")
                }
            }
        )
    }
}

data class NavigationItem(val name: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
