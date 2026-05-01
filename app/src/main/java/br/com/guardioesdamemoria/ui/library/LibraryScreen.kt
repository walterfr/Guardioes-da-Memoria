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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.guardioesdamemoria.domain.model.Memory
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import br.com.guardioesdamemoria.ui.theme.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LocationViewModel, onNavigateToRegistration: () -> Unit = {}) {
    val memories by viewModel.memories.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val playingMemoryId by viewModel.playingMemoryId.collectAsState()
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

    var selectedMemoryForDetail by remember { mutableStateOf<Memory?>(null) }
    val sheetState = rememberModalBottomSheetState()

    if (selectedMemoryForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMemoryForDetail = null },
            sheetState = sheetState,
            containerColor = NightPanel,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = selectedMemoryForDetail!!.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = selectedMemoryForDetail!!.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(selectedMemoryForDetail!!.category, color = MemoryTeal, fontWeight = FontWeight.Bold)
                    Text(" • ", color = Color.White.copy(alpha = 0.3f))
                    Text(selectedMemoryForDetail!!.year, color = Color.White.copy(alpha = 0.6f))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedMemoryForDetail!!.description,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MemoryTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedMemoryForDetail!!.authorName.take(1), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Relatado por", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                        Text(selectedMemoryForDetail!!.authorName, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    val isThisPlaying = isAudioPlaying && playingMemoryId == selectedMemoryForDetail!!.id
                    Button(
                        onClick = { viewModel.playAudio(selectedMemoryForDetail!!.description, selectedMemoryForDetail!!.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isThisPlaying) Color.Red else MemoryTeal)
                    ) {
                        Icon(if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isThisPlaying) "PARAR" else "OUVIR")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // SEÇÃO DE FEEDBACK QUALITATIVO
                Text(
                    "Como você se sentiu com esse relato?",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Conectado", "Surpreso", "Inspirado").forEach { feeling ->
                        var isSelected by remember { mutableStateOf(false) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                isSelected = true
                                viewModel.reactToMemory(selectedMemoryForDetail!!.id.toLong(), feeling)
                            },
                            label = { Text(feeling) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MemoryTeal,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // MODERAÇÃO
                TextButton(
                    onClick = { 
                        viewModel.reportMemory(selectedMemoryForDetail!!.id.toLong(), "Conteúdo inadequado")
                        selectedMemoryForDetail = null
                    },
                    modifier = Modifier.alpha(0.5f)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Denunciar conteúdo inadequado", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

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
                                    PolaroidCard(memory, onClick = { selectedMemoryForDetail = memory })
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("TODOS OS RELATOS", modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(filteredMemories) { memory ->
                        val isThisPlaying = isAudioPlaying && playingMemoryId == memory.id
                        MemoryItemCard(
                            memory, 
                            isThisPlaying = isThisPlaying,
                            onPlay = { viewModel.playAudio(memory.description, memory.id) },
                            onClick = { selectedMemoryForDetail = memory }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PolaroidCard(memory: Memory, onClick: () -> Unit) {
    // Polaroid Style Card (Levemente rotacionado)
    Card(
        modifier = Modifier.width(160.dp).graphicsLayer(rotationZ = -2f).clickable { onClick() },
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
fun MemoryItemCard(memory: Memory, isThisPlaying: Boolean, onPlay: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() },
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
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isThisPlaying) Color.Red else MemoryTeal)
            ) {
                Icon(if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
        }
    }
}
