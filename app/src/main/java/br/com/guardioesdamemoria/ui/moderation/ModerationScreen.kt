package br.com.guardioesdamemoria.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.guardioesdamemoria.domain.model.Memory
import br.com.guardioesdamemoria.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    viewModel: LocationViewModel,
    onBack: () -> Unit
) {
    val pendingMemories by viewModel.pendingMemories.collectAsState()
    var showPinDialog by remember { mutableStateOf(true) }
    var pinInput by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(false) }
    
    var selectedMemoryForAction by remember { mutableStateOf<Pair<Memory, Boolean>?>(null) } // Memory to Approve (true) or Reject (false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel do Orientador", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (!isAuthenticated) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Acesso Restrito", style = MaterialTheme.typography.headlineSmall)
                    Text("Apenas professores podem aprovar relatos.")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showPinDialog = true }) {
                        Text("Digitar PIN")
                    }
                }
            }
        } else {
            if (pendingMemories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum relato pendente de aprovação.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingMemories) { memory ->
                        PendingMemoryCard(
                            memory = memory,
                            onApprove = { selectedMemoryForAction = Pair(memory, true) },
                            onReject = { selectedMemoryForAction = Pair(memory, false) }
                        )
                    }
                }
            }
        }
    }

    // Dialogo de Autenticação Inicial
    if (showPinDialog && !isAuthenticated) {
        AlertDialog(
            onDismissRequest = { if (!isAuthenticated) onBack() },
            title = { Text("Verificação de Identidade") },
            text = {
                Column {
                    Text("Digite o PIN do Professor para acessar o painel:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.isTeacherPinValid(pinInput)) {
                        isAuthenticated = true
                        showPinDialog = false
                    }
                }) {
                    Text("Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("Cancelar") }
            }
        )
    }

    // Confirmação de Ação (Aprovar/Rejeitar)
    selectedMemoryForAction?.let { (memory, isApprove) ->
        AlertDialog(
            onDismissRequest = { selectedMemoryForAction = null },
            title = { Text(if (isApprove) "Aprovar Relato" else "Rejeitar Relato") },
            text = {
                Text("Deseja realmente ${if (isApprove) "aprovar" else "recluir"} o relato '${memory.title}'? Relatos aprovados ficarão visíveis para todos os alunos no mapa.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isApprove) {
                            viewModel.approveMemory(memory.id, pinInput)
                        } else {
                            viewModel.rejectMemory(memory.id, pinInput)
                        }
                        selectedMemoryForAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApprove) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMemoryForAction = null }) { Text("Voltar") }
            }
        )
    }
}

@Composable
fun PendingMemoryCard(
    memory: Memory,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = memory.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = memory.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(text = memory.year, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Autor: ${memory.authorName}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = memory.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aprovar")
                }
                
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rejeitar")
                }
            }
        }
    }
}
