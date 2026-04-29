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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
            enter = fadeIn(animationSpec = tween(1000)),
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
                            alpha = 0.45f,
                            scaleX = pulseScale,
                            scaleY = pulseScale
                        ),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Camada de gradiente para contraste
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
        )

        // Cabeçalho Premium
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GUARDIÕES DA MEMÓRIA",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            
            // Indicador de Proximidade "Glass"
            distanceToActive?.let { dist ->
                Surface(
                    modifier = Modifier.padding(top = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = (if (dist < 12) Color(0xFF4CAF50) else Color.White).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (dist < 12) "📍 VOCÊ CHEGOU!" else "📍 a ${dist.toInt()} metros",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Pontuação "Glass"
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 20.dp)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "⭐ $userPoints",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
        
        // Notificação de Insígnia (Animação Premium)
        AnimatedVisibility(
            visible = recentBadge != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 140.dp)
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
                            Text("NOVA INSÍGNIA!", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                            Text(badge.title, style = MaterialTheme.typography.titleLarge, color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Overlay de Memória Ativa (Card Glassmorphism)
        AnimatedVisibility(
            visible = activeMemory != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            activeMemory?.let { memory ->
                Card(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (memory.category) {
                                            "Enchente" -> ColorEnchente
                                            "Seca" -> ColorSeca
                                            "Tempestade" -> ColorTempestade
                                            else -> ColorGeral
                                        }
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = memory.category.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = memory.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.dismissActiveMemory() },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Legendas Dinâmicas (Efeito Karaoke)
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    val textStr = memory.description
                                    if (spokenRange != null && spokenRange!!.second <= textStr.length) {
                                        // Texto já falado (Highlight)
                                        withStyle(style = SpanStyle(color = Color.Yellow, fontWeight = FontWeight.Bold)) {
                                            append(textStr.substring(0, spokenRange!!.second))
                                        }
                                        // Resto do texto
                                        append(textStr.substring(spokenRange!!.second))
                                    } else {
                                        append(textStr)
                                    }
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 5
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Barra de Áudio Glass
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { audioProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = Color.Yellow,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (memory.authorName.isNotBlank()) memory.authorName else "Relato Local",
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledIconButton(
                                        onClick = { viewModel.togglePlayback() },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Estado de Busca "Glass"
        if (activeMemory == null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Explorando memórias próximas...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
