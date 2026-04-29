package br.com.guardioesdamemoria.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: LocationViewModel) {
    val memories by viewModel.memories.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    
    val categories = listOf("Todas", "Enchente", "Seca", "Tempestade", "Ciclone", "Geral")
    var selectedCategory by remember { mutableStateOf("Todas") }
    
    val filteredMemories = if (selectedCategory == "Todas") memories else memories.filter { it.category == selectedCategory }

    val initialPos = currentLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(-3.7319, -38.5267)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 15f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = currentLocation != null),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
            filteredMemories.forEach { memory ->
                Marker(
                    state = MarkerState(position = LatLng(memory.latitude, memory.longitude)),
                    title = memory.title,
                    snippet = memory.category,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (memory.category) {
                            "Enchente" -> BitmapDescriptorFactory.HUE_BLUE
                            "Seca" -> BitmapDescriptorFactory.HUE_ORANGE
                            "Tempestade" -> BitmapDescriptorFactory.HUE_VIOLET
                            else -> BitmapDescriptorFactory.HUE_RED
                        }
                    )
                )
            }
        }
        
        // Overlay de Filtros
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Overlay de Resumo (Inferior)
        val newMemoriesCount by viewModel.newMemoriesCount.collectAsState()

        Card(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Pontos de Memória", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        viewModel.triggerVibration()
                        // Lógica de centralizar aqui se necessário
                    }) {
                        Text("Explorar")
                    }
                }
            }
        }
    }
}
