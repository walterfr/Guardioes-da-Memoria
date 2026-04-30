package br.com.guardioesdamemoria.ui.badges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import br.com.guardioesdamemoria.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    viewModel: LocationViewModel,
    onOpenTeacher: () -> Unit = {}
) {
    val earnedBadges by viewModel.earnedBadges.collectAsState()
    val userPoints by viewModel.userPoints.collectAsState()
    var showTeacherPin by remember { mutableStateOf(false) }
    var teacherPin by remember { mutableStateOf("") }
    
    // Níveis de Guardião
    val levelInfo = when {
        userPoints < 100 -> Pair("Explorador", 100)
        userPoints < 500 -> Pair("Guardião", 500)
        else -> Pair("Mestre", 1000)
    }

    // Lista completa de insígnias com EMOJIS (Conforme sugestão Claude)
    val allBadges = listOf(
        BadgeInfo("Primeira pista", "Encontrou sua primeira memória", "👁️", 1),
        BadgeInfo("Pesquisador", "Encontrou 5 memórias", "🔎", 5),
        BadgeInfo("Cartógrafo", "Encontrou 10 memórias", "🗺️", 10),
        BadgeInfo("Arquivo vivo", "Ouviu um relato completo", "🎙️", 1),
        BadgeInfo("Sentinela", "Alcançou 100 pontos", "🛡️", 100),
        BadgeInfo("Veterano", "Alcançou 500 pontos", "🎖️", 500),
        BadgeInfo("Relíquia", "Encontrou todas as memórias", "🏛️", 1000)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conquistas", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { showTeacherPin = true }) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NightField, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().background(NightField).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card de Nível (Refinado)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MemoryTeal)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = "PROGRESSO DA CAÇA", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(text = "${userPoints} XP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text(text = levelInfo.first, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Barra de Progresso Real de Nível
                    val progress = (userPoints % levelInfo.second).toFloat() / levelInfo.second.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "${levelInfo.second - (userPoints % levelInfo.second)} XP para o próximo nível",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Grid de Insígnias
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allBadges) { badge ->
                    val earnedBadge = earnedBadges.find { it.title == badge.title }
                    val isEarned = earnedBadge != null
                    
                    // Cálculo de progresso real (Simulado aqui, no real viria do ViewModel)
                    val badgeProgress = if (isEarned) 1f else {
                        when (badge.title) {
                            "Pesquisador" -> (earnedBadges.size % 5) / 5f
                            "Cartógrafo" -> (earnedBadges.size % 10) / 10f
                            else -> 0f
                        }
                    }

                    BadgeCard(badge, isEarned, badgeProgress, earnedDate = "29 abr") // Exemplo de data
                }
            }
        }
    }

    if (showTeacherPin) {
        AlertDialog(
            onDismissRequest = { showTeacherPin = false },
            title = { Text("Acesso Professor") },
            text = {
                OutlinedTextField(
                    value = teacherPin,
                    onValueChange = { teacherPin = it },
                    label = { Text("PIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.isTeacherPinValid(teacherPin)) {
                        showTeacherPin = false
                        onOpenTeacher()
                    }
                }) {
                    Text("Validar")
                }
            }
        )
    }
}

data class BadgeInfo(val title: String, val description: String, val icon: String, val requirement: Int)

@Composable
fun BadgeCard(badge: BadgeInfo, isEarned: Boolean, progress: Float, earnedDate: String?) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEarned) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f)
        ),
        border = if (isEarned) BorderStroke(1.dp, MemoryTeal.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(if (isEarned) MemoryTeal.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.icon,
                    fontSize = 28.sp,
                    modifier = Modifier.graphicsLayer(alpha = if (isEarned) 1f else 0.2f)
                )
                if (!isEarned) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(text = badge.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = if (isEarned) Color.White else Color.White.copy(alpha = 0.3f))
            
            if (isEarned && earnedDate != null) {
                Text(text = "Conquistada em $earnedDate", style = MaterialTheme.typography.labelSmall, color = MemoryTeal, modifier = Modifier.padding(top = 2.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MemoryTeal,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}
