package br.com.guardioesdamemoria.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun LibraryScreen(viewModel: LocationViewModel) {
    val memories by viewModel.memories.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDecade by remember { mutableStateOf<String?>(null) }
    
    val decades = listOf("1970", "1980", "1990", "2000", "2010", "2020")

    val filteredMemories = memories.filter { 
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) || 
                          it.category.contains(searchQuery, ignoreCase = true)
        val matchesDecade = if (selectedDecade == null) true else {
            it.year.startsWith(selectedDecade!!.substring(0, 3))
        }
        matchesSearch && matchesDecade
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Acervo Comunitário", fontWeight = FontWeight.Black)
                        Text("Explorando a linha do tempo", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Barra de Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar no arquivo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            // Filtro de Décadas (Timeline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
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
                    Text("Nenhum relato encontrado neste período.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredMemories) { memory ->
                        MemoryItemCard(memory)
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryItemCard(memory: Memory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6)), // Estilo Papel Antigo
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.height(220.dp)) {
            // Foto de Fundo
            if (memory.imageUrl != null) {
                AsyncImage(
                    model = memory.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.9f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFD2B48C))) // Tan color
            }
            
            // Gradiente para legibilidade (Estilo Vinheta)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 300f
                    )
                )
            )
            
            // Conteúdo
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = memory.category.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (memory.year.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "DÉCADA: ${memory.year.substring(0, 3)}0s", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Narrado por: ${if (memory.authorName.isNotBlank()) memory.authorName else "Comunidade"}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Selo de "Arquivo"
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    .size(44.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
        }
    }
}
