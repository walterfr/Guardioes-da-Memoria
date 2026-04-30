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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
    isTeacher: Boolean = false,
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
            if (hasPermissions && isTeacher) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToRegistration,
                    containerColor = MemoryTeal,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("REGISTRAR") },
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (hasPermissions) {
                CameraPreviewContent(viewModel)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(NightField),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Acesso à Câmera Necessário", color = Color.White)
                        Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)) }) {
                            Text("Permitir")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewContent(viewModel: LocationViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeMemory by viewModel.activeMemory.collectAsState()
    val spokenRange by viewModel.spokenTextRange.collectAsState()
    val userPoints by viewModel.userPoints.collectAsState()
    val recentBadge by viewModel.recentBadge.collectAsState()
    val audioProgress by viewModel.audioProgress.collectAsState()
    val distanceToActive by viewModel.distanceToActive.collectAsState()
    val nearestDistance by viewModel.nearestMemoryDistance.collectAsState()
    val nearestBearing by viewModel.nearestMemoryBearing.collectAsState()
    
    var imageAlpha by remember { mutableStateOf(0.6f) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                    } catch (exc: Exception) { exc.printStackTrace() }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camada AR: Foto Histórica (Sobreposta diretamente na câmera)
        activeMemory?.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer(alpha = imageAlpha),
                contentScale = ContentScale.Crop
            )
            
            // Slider Vertical de Opacidade (Lado Direito)
            Box(modifier = Modifier.fillMaxHeight().width(60.dp).align(Alignment.CenterEnd).padding(vertical = 120.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
                    Text("OPAC.", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    Slider(
                        value = imageAlpha,
                        onValueChange = { imageAlpha = it },
                        modifier = Modifier.weight(1f).graphicsLayer(rotationZ = 270f),
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = MemoryTeal)
                    )
                }
            }
        }

        // Overlay de Gradiente HUD
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Black.copy(alpha = 0.9f)))
        ))

        // HUD Superior: Bairro e XP
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("GUARDIÕES DA MEMÓRIA", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Grande Bom Jardim", color = MemoryTeal, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.1f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$userPoints XP", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }

        // RADAR: Anéis de Pulso Animados
        if (activeMemory == null) {
            val infiniteTransition = rememberInfiniteTransition()
            val radius1 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Restart))
            val radius2 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, 1000), RepeatMode.Restart))
            
            Box(modifier = Modifier.align(Alignment.Center).size(300.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = MemoryTeal.copy(alpha = 1f - radius1), radius = 150.dp.toPx() * radius1, style = Stroke(2.dp.toPx()))
                    drawCircle(color = MemoryTeal.copy(alpha = 1f - radius2), radius = 150.dp.toPx() * radius2, style = Stroke(2.dp.toPx()))
                }
                
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    nearestDistance?.let { dist ->
                        val direction = when {
                            nearestBearing == null -> ""
                            nearestBearing!! < -20 -> "vire à esquerda"
                            nearestBearing!! > 20 -> "vire à direita"
                            else -> "siga em frente"
                        }
                        Text("A ${dist.toInt()}m", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(direction.uppercase(), color = MemoryTeal, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    } ?: run {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MemoryTeal, modifier = Modifier.size(48.dp))
                        Text("BUSCANDO SINAL...", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Card de Memória (Arquivo Histórico Aesthetic)
        AnimatedVisibility(
            visible = activeMemory != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            activeMemory?.let { memory ->
                Card(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6)) // Papel Antigo
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(color = MemoryTeal, shape = RoundedCornerShape(4.dp)) {
                                    Text(memory.category.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(memory.title, color = Color(0xFF3E2723), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            }
                            IconButton(onClick = { viewModel.dismissActiveMemory() }, modifier = Modifier.background(Color.Black.copy(alpha = 0.1f), CircleShape)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black)
                            }
                        }
                        
                        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Narrador: ${memory.authorName}", color = Color(0xFF5D4037), style = MaterialTheme.typography.labelMedium)
                            // Corrigido: Separador usa cor do tema
                            Text(" | ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            Text(text = "Ano: ${memory.year}", color = Color(0xFF5D4037), style = MaterialTheme.typography.labelMedium)
                        }

                        // Legendas Karaoke
                        Box(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth().background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text(
                                text = buildAnnotatedString {
                                    val textStr = memory.description
                                    if (spokenRange != null && spokenRange!!.second <= textStr.length) {
                                        withStyle(SpanStyle(color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)) { append(textStr.substring(0, spokenRange!!.second)) }
                                        append(textStr.substring(spokenRange!!.second))
                                    } else { append(textStr) }
                                },
                                color = Color(0xFF4E342E), style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Audio Player
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            FilledIconButton(onClick = { viewModel.togglePlayback() }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF3E2723))) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            LinearProgressIndicator(progress = { audioProgress }, modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape), color = Color(0xFF8B4513), trackColor = Color.Black.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }
}
