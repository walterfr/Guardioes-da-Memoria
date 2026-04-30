package br.com.guardioesdamemoria.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.guardioesdamemoria.domain.model.Memory
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import br.com.guardioesdamemoria.ui.theme.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LocationViewModel, onNavigateToRegistration: () -> Unit = {}) {
    val memories by viewModel.memories.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDecade by remember { mutableStateOf<String?>(null) }
    
    val decades = listOf("1970", "1980", "1990", "2000", "2010", "2020")

    val filteredMemories = memories.filter { 
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) || 
                          it.category.contains(searchQuery, ignoreCase = true) ||
                          it.authorName.contains(searchQuery, ignoreCase = true) ||
                          it.year.contains(searchQuery)
        
        val matchesDecade = if (selectedDecade == null) true else {
            // BUG FIX: it.year.dropLast(1) == selectedDecade!!.dropLast(1)
            // se year for "1974" e selectedDecade for "1970", dropLast(1) dá "197" == "197"
            it.year.isNotBlank() && it.year.dropLast(1) == selectedDecade!!.dropLast(1)
        }
        matchesSearch && matchesDecade
    }

    val featuredMemories = memories.take(3) // Exemplo: Primeiras 3 são destaque

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acervo Comunitário", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NightField, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(NightField)) {
            // Barra de Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por título, autor, ano...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // Timeline (Décadas)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDecade == null,
                    onClick = { selectedDecade = null },
                    label = { Text("Todas") }
                )
                decades.forEach { decade ->
                    FilterChip(
                        selected = selectedDecade == decade,
                        onClick = { selectedDecade = decade },
                        label = { Text("${decade}s") }
                    )
                }
            }

            if (filteredMemories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Nenhum relato encontrado.", color = Color.White.copy(alpha = 0.5f))
                        Text(
                            "Que tal ser o primeiro a registrar?", 
                            color = MemoryTeal, 
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable { onNavigateToRegistration() }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // SEÇÃO EM DESTAQUE (Polaroids)
                    if (selectedDecade == null && searchQuery.isBlank()) {
                        item {
                            Text("EM DESTAQUE", modifier = Modifier.padding(16.dp), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(featuredMemories) { memory ->
                                    PolaroidCard(memory)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("TODOS OS RELATOS", modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(filteredMemories) { memory ->
                        MemoryItemCard(memory, onPlay = { viewModel.togglePlayback() })
                    }
                }
            }
        }
    }
}

@Composable
fun PolaroidCard(memory: Memory) {
    // Polaroid Style Card (Levemente rotacionado)
    Card(
        modifier = Modifier.width(160.dp).graphicsLayer(rotationZ = -2f),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = memory.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(memory.title, color = Color.Black, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, maxLines = 1)
            Text(memory.category, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(memory.year, color = Color.Black, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun MemoryItemCard(memory: Memory, onPlay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = memory.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(memory.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${memory.category} • ${memory.authorName}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                Text(memory.year, color = MemoryTeal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            
            // Botão de Play direto no card
            FilledIconButton(
                onClick = onPlay,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MemoryTeal)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
        }
    }
}
