package br.com.guardioesdamemoria.ui.registration

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.guardioesdamemoria.viewmodel.LocationViewModel
import br.com.guardioesdamemoria.ui.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    viewModel: LocationViewModel,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    
    // Dados do Relato
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Relato") }
    var year by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("") }
    var authorAge by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var imageSource by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf<String?>(null) }
    
    // Localização
    val currentLocation by viewModel.currentLocation.collectAsState()
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var triggerRadiusMeters by remember { mutableFloatStateOf(50f) }
    
    // Consentimento
    var isConsented by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Novo Registro", fontWeight = FontWeight.Black)
                        Text("Passo $currentStep de 3", style = MaterialTheme.typography.labelSmall, color = MemoryTeal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep > 1) currentStep-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NightField, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = NightField, tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.height(56.dp).weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Voltar", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                if (latitude != null && longitude != null && isConsented) {
                                    viewModel.saveNewMemory(
                                        title, description, category, year, authorName, authorAge,
                                        latitude!!, longitude!!, triggerRadiusMeters.toDouble(),
                                        imageUrl, imageSource, audioUrl
                                    )
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.height(56.dp).weight(1.5f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MemoryTeal),
                        enabled = when(currentStep) {
                            1 -> title.isNotBlank() && description.isNotBlank()
                            2 -> authorName.isNotBlank() && year.isNotBlank()
                            3 -> isConsented && (latitude != null || currentLocation != null)
                            else -> true
                        }
                    ) {
                        Text(if (currentStep == 3) "FINALIZAR" else "CONTINUAR", fontWeight = FontWeight.Black)
                        if (currentStep < 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().background(NightField).verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            // Indicador de Progresso Visual
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (index + 1 <= currentStep) MemoryTeal else Color.White.copy(alpha = 0.1f)))
                }
            }

            AnimatedContent(targetState = currentStep, label = "StepTransition") { step ->
                when (step) {
                    1 -> StepOne(title, { title = it }, description, { description = it }, imageUrl, { imagePicker.launch("image/*") })
                    2 -> StepTwo(authorName, { authorName = it }, authorAge, { authorAge = it }, year, { year = it }, category, { category = it }, viewModel, audioUrl, { audioUrl = it })
                    3 -> StepThree(currentLocation, { lat, lon -> latitude = lat; longitude = lon }, triggerRadiusMeters, { triggerRadiusMeters = it }, isConsented, { isConsented = it })
                }
            }
        }
    }
}

@Composable
fun StepOne(title: String, onTitleChange: (String) -> Unit, description: String, onDescChange: (String) -> Unit, imageUrl: String?, onPickImage: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("O que aconteceu?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        
        OutlinedTextField(
            value = title, onValueChange = onTitleChange,
            label = { Text("Título da Memória") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
        )
        
        OutlinedTextField(
            value = description, onValueChange = onDescChange,
            label = { Text("Relato escrito") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
        )
        
        Text("Imagem Histórica", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MemoryTeal, modifier = Modifier.size(48.dp))
                    Text("Adicionar Foto", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepTwo(name: String, onNameChange: (String) -> Unit, age: String, onAgeChange: (String) -> Unit, year: String, onYearChange: (String) -> Unit, category: String, onCatChange: (String) -> Unit, viewModel: LocationViewModel, audioUrl: String?, onAudioRecorded: (String?) -> Unit) {
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingTime by viewModel.recordingTime.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Quem narrou?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Nome do entrevistado") }, modifier = Modifier.fillMaxWidth())
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = age, onValueChange = onAgeChange, label = { Text("Idade") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = year, onValueChange = onYearChange, label = { Text("Ano do evento") }, modifier = Modifier.weight(1f))
        }
        
        Text("Tipo de memória", style = MaterialTheme.typography.titleMedium, color = Color.White)
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cats = listOf("🌊 Alagamento", "☀️ Seca", "🚧 Transtornos de Obras", "⛰️ Desmoronamento", "💬 Relato")
            cats.forEach { cat ->
                val selected = category == cat.substring(3)
                FilterChip(
                    selected = selected,
                    onClick = { onCatChange(cat.substring(3)) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MemoryTeal,
                        selectedLabelColor = Color.White,
                        labelColor = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        // Botão de Gravar: TOQUE E SEGURE (Sugestão Claude)
        Text("Relato Oral", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isRecording) Color.Red.copy(alpha = 0.2f) else MemoryTeal.copy(alpha = 0.1f))
                .border(2.dp, if (isRecording) Color.Red else MemoryTeal, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            viewModel.startRecording()
                            try { awaitRelease() } finally { 
                                val path = viewModel.stopRecording()
                                onAudioRecorded(path)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isRecording) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = null,
                    tint = if (isRecording) Color.Red else MemoryTeal
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (isRecording) "GRAVANDO... (${recordingTime}s)" else if (audioUrl != null) "RELATO GRAVADO (Segure para regravar)" else "SEGURE PARA GRAVAR RELATO",
                    color = if (isRecording) Color.Red else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StepThree(currentLoc: android.location.Location?, onLocManual: (Double, Double) -> Unit, radius: Float, onRadiusChange: (Float) -> Unit, consented: Boolean, onConsentChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Onde foi?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = MemoryTeal)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Localização Atual", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        if (currentLoc != null) "Lat: %.4f, Lon: %.4f".format(currentLoc.latitude, currentLoc.longitude) else "Obtendo GPS...",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Text("Raio de ativação: ${radius.toInt()} metros", color = Color.White)
        Slider(value = radius, onValueChange = onRadiusChange, valueRange = 20f..200f, colors = SliderDefaults.colors(thumbColor = MemoryTeal, activeTrackColor = MemoryTeal))

        Spacer(modifier = Modifier.height(16.dp))
        
        // Termo de Consentimento (Mantido conforme sugestão Claude)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (consented) MemoryTeal else Color.White.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consented, onCheckedChange = onConsentChange, colors = CheckboxDefaults.colors(checkedColor = MemoryTeal))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Declaro que possuo o consentimento do entrevistado para registrar e publicar este relato no acervo comunitário.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
