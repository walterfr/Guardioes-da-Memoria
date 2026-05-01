package br.com.guardioesdamemoria.ui.map

import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import br.com.guardioesdamemoria.ui.theme.*
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons

import org.osmdroid.tileprovider.cachemanager.CacheManager
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: LocationViewModel, onNavigateToCamera: () -> Unit = {}) {
    val memories by viewModel.memories.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    // Configuração do User-Agent e Storage necessária para o OSM
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("osm_pref", 0)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue = context.packageName
        
        // Força o uso do diretório interno do app para evitar crashes de permissão de escrita (comum em tablets/Android recente)
        val osmPath = java.io.File(context.filesDir, "osmdroid")
        if (!osmPath.exists()) osmPath.mkdirs()
        Configuration.getInstance().osmdroidBasePath = osmPath
        Configuration.getInstance().osmdroidTileCache = java.io.File(osmPath, "tiles")
    }

    val categories = listOf("Todas", "Alagamento", "Seca", "Transtornos de Obras", "Desmoronamento", "Relato")
    var selectedCategory by remember { mutableStateOf("Todas") }
    val filteredMemories = if (selectedCategory == "Todas") memories else memories.filter { it.category == selectedCategory }

    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Gerenciamento de Ciclo de Vida do MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun downloadOfflineMap() {
        val gbjBoundingBox = BoundingBox(-3.77, -38.58, -3.81, -38.62)
        val cacheManager = CacheManager(mapView)
        
        scope.launch {
            isDownloading = true
            try {
                cacheManager.downloadAreaAsync(context, gbjBoundingBox, 14, 18, object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() {
                        isDownloading = false
                        Toast.makeText(context, "Download concluído!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onTaskFailed(errors: Int) {
                        isDownloading = false
                        Toast.makeText(context, "Erro no download: $errors falhas", Toast.LENGTH_LONG).show()
                    }
                    override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                        downloadProgress = progress / 100f
                    }
                    override fun downloadStarted() {}
                    override fun setPossibleTilesInArea(total: Int) {}
                })
            } catch (e: Exception) {
                isDownloading = false
                Toast.makeText(context, "Erro crítico: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { 
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    
                    // Centraliza inicialmente no GBJ se não houver GPS
                    val startPoint = currentLocation?.let { GeoPoint(it.latitude, it.longitude) } 
                                    ?: GeoPoint(-3.7915, -38.5990)
                    controller.setCenter(startPoint)

                    // Camada de Localização do Usuário
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    overlays.add(locationOverlay)
                }
            },
            update = { view ->
                // Limpa marcadores anteriores (mantendo a camada de localização que é a primeira)
                val overlaysToKeep = view.overlays.filterIsInstance<MyLocationNewOverlay>()
                view.overlays.clear()
                view.overlays.addAll(overlaysToKeep)

                // Adiciona novos marcadores filtrados
                filteredMemories.forEach { memory ->
                    val marker = Marker(view)
                    marker.position = GeoPoint(memory.latitude, memory.longitude)
                    marker.title = memory.title
                    marker.subDescription = memory.category
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    view.overlays.add(marker)
                }
                view.invalidate()
            }
        )
        
        // Overlay de Filtros e Download
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.size) { index ->
                    val cat = categories[index]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
            
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MemoryTeal,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        // Botão de Download Flutuante
        SmallFloatingActionButton(
            onClick = { downloadOfflineMap() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp),
            containerColor = if (isDownloading) Color.Gray else MemoryAmber,
            contentColor = Color.White
        ) {
            Icon(
                if (isDownloading) androidx.compose.material.icons.Icons.Default.CloudDownload 
                else androidx.compose.material.icons.Icons.Default.Download, 
                contentDescription = "Baixar mapa offline"
            )
        }

        // Resumo inferior
        val newMemoriesCount by viewModel.newMemoriesCount.collectAsState()

        Card(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Mapa Vivo (OSM)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(text = "${filteredMemories.size} locais encontrados", style = MaterialTheme.typography.bodySmall)
                }
                
                BadgedBox(
                    badge = {
                        if (newMemoriesCount > 0) {
                            Badge { Text(newMemoriesCount.toString()) }
                        }
                    }
                ) {
                    androidx.compose.material3.FilledTonalButton(onClick = { 
                        viewModel.clearNewMemoriesCount()
                        onNavigateToCamera()
                    }) {
                        Text("Ir para explorar")
                    }
                }
            }
        }
    }
}
