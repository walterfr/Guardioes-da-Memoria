package br.com.guardioesdamemoria.ui.teacher

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import br.com.guardioesdamemoria.domain.model.Memory
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherReviewScreen(viewModel: LocationViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingMemories by viewModel.pendingMemories.collectAsState()
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    var action by remember { mutableStateOf<ReviewAction?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Curadoria", fontWeight = FontWeight.Black)
                        Text("${pendingMemories.size} aguardando revisão", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        }
    ) { padding ->
        if (pendingMemories.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 920.dp).fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Nenhuma memória pendente. Novos envios dos alunos aparecerão aqui para revisão.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ExportButton(
                        isExporting = isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val uri = viewModel.exportDatabaseZip()
                                isExporting = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Exportar acervo")
                                )
                            }
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = Modifier.widthIn(max = 1040.dp).fillMaxWidth()) {
                    ExportButton(
                        isExporting = isExporting,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val uri = viewModel.exportDatabaseZip()
                                isExporting = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Exportar acervo")
                                )
                            }
                        }
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pendingMemories) { memory ->
                            PendingMemoryCard(
                                memory = memory,
                                onApprove = {
                                    selectedMemory = memory
                                    action = ReviewAction.Approve
                                    pinInput = ""
                                    pinError = false
                                },
                                onReject = {
                                    selectedMemory = memory
                                    action = ReviewAction.Reject
                                    pinInput = ""
                                    pinError = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedMemory != null && action != null) {
        AlertDialog(
            onDismissRequest = {
                selectedMemory = null
                action = null
            },
            title = { Text(if (action == ReviewAction.Approve) "Aprovar memória" else "Rejeitar memória") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(selectedMemory!!.title, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (action == ReviewAction.Approve) {
                            "Após a aprovação, esta memória aparece na caça, no mapa e no acervo."
                        } else {
                            "A rejeição remove este registro deste aparelho."
                        }
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("PIN do professor") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = pinError,
                        supportingText = {
                            if (pinError) Text("PIN incorreto")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = if (action == ReviewAction.Approve) {
                            viewModel.approveMemory(selectedMemory!!.id, pinInput)
                        } else {
                            viewModel.rejectMemory(selectedMemory!!.id, pinInput)
                        }
                        if (success) {
                            selectedMemory = null
                            action = null
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text(if (action == ReviewAction.Approve) "Aprovar" else "Rejeitar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedMemory = null
                    action = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ExportButton(
    isExporting: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = !isExporting,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isExporting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preparando ZIP")
        } else {
            Text("Exportar acervo em ZIP")
        }
    }
}

@Composable
private fun PendingMemoryCard(
    memory: Memory,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (memory.imageUrl != null) {
                AsyncImage(
                    model = memory.imageUrl,
                    contentDescription = "Foto enviada",
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sem foto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(memory.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(memory.description, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${memory.category} | ${memory.year.ifBlank { "ano não informado" }}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Entrevistado: ${memory.authorName.ifBlank { "não informado" }} ${memory.authorAge.ifBlank { "" }}",
                    style = MaterialTheme.typography.labelMedium
                )
                if (memory.imageSource.isNotBlank()) {
                    Text("Fonte da imagem: ${memory.imageSource}", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = "Ativação em ${memory.triggerRadiusMeters.toInt()}m",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rejeitar")
                }
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aprovar")
                }
            }
        }
    }
}

private enum class ReviewAction {
    Approve,
    Reject
}
