package br.com.guardioesdamemoria.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.guardioesdamemoria.ui.theme.*
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import coil.compose.AsyncImage

@Composable
fun CameraScreen(
    viewModel: LocationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToRegistration: () -> Unit
) {
    val context = LocalContext.current
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermissions = permissions[Manifest.permission.CAMERA] == true &&
                             (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            viewModel.startLocationUpdates()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationUpdates()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (hasPermissions) {
                FloatingActionButton(
                    onClick = onNavigateToRegistration,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Memória")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (hasPermissions) {
                CameraPreviewContent(viewModel)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Acesso à Câmera e Localização Necessários",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { 
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) {
                            Text("Conceder Permissões")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewContent(viewModel: LocationViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeMemory by viewModel.activeMemory.collectAsState()
    val spokenRange by viewModel.spokenTextRange.collectAsState()
    val userPoints by viewModel.userPoints.collectAsState()
    val recentBadge by viewModel.recentBadge.collectAsState()
    val audioProgress by viewModel.audioProgress.collectAsState()
    val distanceToActive by viewModel.distanceToActive.collectAsState()
    
    // Estado para o Slider de Realidade (Antes x Depois)
    var imageAlpha by remember { mutableStateOf(0.45f) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay de Foto Histórica (Efeito AR)
        val infiniteTransition = rememberInfiniteTransition()
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            )
        )

        AnimatedVisibility(
            visible = activeMemory?.imageUrl != null,
            enter = fadeIn(animationSpec = tween(1500)) + expandVertically(),
            exit = fadeOut()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val adaptiveHeight = maxHeight * 0.7f
                AsyncImage(
                    model = activeMemory?.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(adaptiveHeight)
                        .align(Alignment.Center)
                        .graphicsLayer(
                            alpha = imageAlpha,
                            scaleX = pulseScale,
                            scaleY = pulseScale
                        ),
                    contentScale = ContentScale.Crop
                )
                
                // Slider Antes x Depois ( HUD Mission Style)
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PASSADO", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Slider(
                        value = imageAlpha,
                        onValueChange = { imageAlpha = it },
                        modifier = Modifier.graphicsLayer(rotationZ = 270f).width(120.dp),
                        valueRange = 0f..1f
                    )
                    Text("AGORA", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        // Camada de gradiente para contraste HUD
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.8f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
        )

        // HUD SUPERIOR (Estilo Jogo/Missão)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📍 GRANDE BOM JARDIM",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (activeMemory != null) "✨ MEMÓRIA DETECTADA" else "📡 BUSCANDO SINAIS...",
                    color = if (activeMemory != null) Color.Yellow else Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "XP: $userPoints", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }

        // RADAR PULSANTE (Centro)
        if (activeMemory == null) {
            val radarTransition = rememberInfiniteTransition()
            val radarAlpha by radarTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Restart)
            )
            val radarScale by radarTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 2f,
                animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Restart)
            )

            Box(
                modifier = Modifier.align(Alignment.Center).size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = radarScale, scaleY = radarScale, alpha = radarAlpha)
                        .background(Color.Cyan.copy(alpha = 0.3f), CircleShape)
                )
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Cyan.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
            }
        }
        
        // Notificação de Insígnia (Animação Premium)
        AnimatedVisibility(
            visible = recentBadge != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp)
        ) {
            recentBadge?.let { badge ->
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = badge.icon, style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("CONQUISTA DESBLOQUEADA!", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                            Text(badge.title, style = MaterialTheme.typography.titleLarge, color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Overlay de Memória Ativa (Arquivo Histórico Aesthetic)
        AnimatedVisibility(
            visible = activeMemory != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            activeMemory?.let { memory ->
                Card(
                    modifier = Modifier.padding(20.dp).fillMaxWidth()
                        .drawBehind {
                            // Efeito de borda "brilhante" ao detectar
                            drawRoundRect(
                                color = Color.Yellow.copy(alpha = 0.3f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                            )
                        },
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6)), // Cor de papel antigo/pergaminho
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = when (memory.category) {
                                        "Enchente" -> ColorEnchente
                                        "Seca" -> ColorSeca
                                        "Tempestade" -> ColorTempestade
                                        else -> ColorGeral
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ARQUIVO: ${memory.category.uppercase()}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = memory.title,
                                    color = Color(0xFF3E2723), // Marrom café profundo
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.dismissActiveMemory() },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Informações do Narrador (Estilo Ficha)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF5D4037), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NARRADOR: ${if (memory.authorName.isNotBlank()) memory.authorName else "DESCONHECIDO"}",
                                color = Color(0xFF5D4037),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (memory.year.isNotBlank()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "|", color = Color.Black.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "ANO: ${memory.year}", color = Color(0xFF5D4037), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Legendas Dinâmicas (Karaoke com estilo de papel)
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    val textStr = memory.description
                                    if (spokenRange != null && spokenRange!!.second <= textStr.length) {
                                        withStyle(style = SpanStyle(color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)) {
                                            append(textStr.substring(0, spokenRange!!.second))
                                        }
                                        append(textStr.substring(spokenRange!!.second))
                                    } else {
                                        append(textStr)
                                    }
                                },
                                color = Color(0xFF4E342E),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 5
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Barra de Áudio (Estilo Analógico)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = { viewModel.togglePlayback() },
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF3E2723))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            LinearProgressIndicator(
                                progress = { audioProgress },
                                modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                                color = Color(0xFF8B4513),
                                trackColor = Color.Black.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }

        // Dica de Exploração HUD
        if (activeMemory == null) {
            Text(
                text = "MOVA A CÂMERA PARA ENCONTRAR SINAIS HISTÓRICOS",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
