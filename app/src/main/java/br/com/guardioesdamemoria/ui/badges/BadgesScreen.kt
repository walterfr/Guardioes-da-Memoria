package br.com.guardioesdamemoria.ui.badges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.guardioesdamemoria.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(viewModel: LocationViewModel) {
    val earnedBadges by viewModel.earnedBadges.collectAsState()
    val userPoints by viewModel.userPoints.collectAsState()
    
    // Lista completa de insígnias possíveis
    val allBadges = listOf(
        BadgeInfo("Novato", "Encontrou sua primeira memória", "🌱", 10),
        BadgeInfo("Historiador", "Encontrou 5 memórias", "📚", 50),
        BadgeInfo("Explorador Urbano", "Encontrou 10 memórias", "🏙️", 100),
        BadgeInfo("Sobrevivente", "Ouviu sobre a grande enchente", "🌊", 30),
        BadgeInfo("Guardião de Bronze", "Alcançou 100 pontos", "🥉", 100),
        BadgeInfo("Guardião de Prata", "Alcançou 500 pontos", "🥈", 500),
        BadgeInfo("Mestre da Memória", "Encontrou todas as memórias", "👑", 1000)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conquistas", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card de Pontuação Premium
            Card(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Círculos decorativos
                    Box(modifier = Modifier.size(100.dp).offset(x = (-20).dp, y = (-20).dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
                    
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "SUA PONTUAÇÃO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(text = "$userPoints", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = "PONTOS DE CONHECIMENTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Minhas Insígnias",
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allBadges) { badge ->
                    val isEarned = earnedBadges.any { it.title == badge.title }
                    BadgeCard(badge, isEarned)
                }
            }
        }
    }
}

data class BadgeInfo(val title: String, val description: String, val icon: String, val requirement: Int)

@Composable
fun BadgeCard(badge: BadgeInfo, isEarned: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEarned) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isEarned) BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape)
                    .background(if (isEarned) Color(0xFFFFD700).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.icon,
                    fontSize = 32.sp,
                    modifier = Modifier.graphicsLayer(alpha = if (isEarned) 1f else 0.2f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = badge.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = if (isEarned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            
            Text(
                text = badge.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 12.sp
            )
            
            if (!isEarned) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Gray.copy(alpha = 0.2f)
                )
            }
        }
    }
}
