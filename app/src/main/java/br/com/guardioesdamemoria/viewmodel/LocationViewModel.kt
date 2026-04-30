package br.com.guardioesdamemoria.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.guardioesdamemoria.domain.model.Badge
import br.com.guardioesdamemoria.domain.model.Memory
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

import br.com.guardioesdamemoria.data.local.AppDatabase
import br.com.guardioesdamemoria.data.local.MemoryEntity

import br.com.guardioesdamemoria.util.AudioRecorder
import br.com.guardioesdamemoria.util.DatabaseExporter
import android.media.MediaPlayer
import java.io.File

import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.FileOutputStream

class LocationViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val db = AppDatabase.getDatabase(application)
    private val memoryDao = db.memoryDao()
    
    private val audioRecorder by lazy { AudioRecorder(application) }
    private var mediaPlayer: MediaPlayer? = null
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _activeMemory = MutableStateFlow<Memory?>(null)
    val activeMemory: StateFlow<Memory?> = _activeMemory.asStateFlow()

    private val _distanceToActive = MutableStateFlow<Float?>(null)
    val distanceToActive: StateFlow<Float?> = _distanceToActive.asStateFlow()

    private val _spokenTextRange = MutableStateFlow<Pair<Int, Int>?>(null)
    val spokenTextRange: StateFlow<Pair<Int, Int>?> = _spokenTextRange.asStateFlow()

    private val _memories = MutableStateFlow<List<Memory>>(emptyList())
    val memories: StateFlow<List<Memory>> = _memories.asStateFlow()

    private val _nearestMemoryDistance = MutableStateFlow<Float?>(null)
    val nearestMemoryDistance: StateFlow<Float?> = _nearestMemoryDistance.asStateFlow()

    private val _nearestMemoryBearing = MutableStateFlow<Float?>(null)
    val nearestMemoryBearing: StateFlow<Float?> = _nearestMemoryBearing.asStateFlow()

    private val _pendingMemories = MutableStateFlow<List<Memory>>(emptyList())
    val pendingMemories: StateFlow<List<Memory>> = _pendingMemories.asStateFlow()

    private val _audioProgress = MutableStateFlow(0f)
    val audioProgress: StateFlow<Float> = _audioProgress.asStateFlow()

    // Estados de Áudio
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var currentRecordingPath: String? = null

    // Sistema de Gamificação
    private val _discoveredMemories = MutableStateFlow<Set<String>>(emptySet())
    
    private val _userPoints = MutableStateFlow(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _recentBadge = MutableStateFlow<Badge?>(null)
    val recentBadge: StateFlow<Badge?> = _recentBadge.asStateFlow()

    private val _earnedBadges = MutableStateFlow<List<Badge>>(emptyList())
    val earnedBadges: StateFlow<List<Badge>> = _earnedBadges.asStateFlow()

    private val _recordingTime = MutableStateFlow(0)
    val recordingTime: StateFlow<Int> = _recordingTime.asStateFlow()

    private val _newMemoriesCount = MutableStateFlow(0)
    val newMemoriesCount: StateFlow<Int> = _newMemoriesCount.asStateFlow()

    private var teacherPin: String = "2024" // PIN padrão inicial

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(application, this)
        loadMemoriesFromDb()
        checkAndInsertDemoMemories()
    }

    private fun checkAndInsertDemoMemories() {
        viewModelScope.launch {
            val current = memoryDao.getAllMemoriesOnce()
            if (current.isEmpty()) {
                val demoMemories = listOf(
                    MemoryEntity(
                        title = "O Grande Alagamento de 85",
                        description = "Nesse ano a água chegou no peito. Lembro de ver os vizinhos se ajudando com canoas improvisadas. Foi um tempo de muita luta, mas de muita união no Bom Jardim.",
                        category = "Alagamento",
                        year = "1985",
                        authorName = "Seu Zé do Egito",
                        authorAge = "74",
                        latitude = -3.7915,
                        longitude = -38.5990,
                        triggerRadiusMeters = 100.0,
                        imageUrl = "android.resource://${getApplication<Application>().packageName}/drawable/memoria_alagamento",
                        isApproved = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    MemoryEntity(
                        title = "Chegada do Saneamento",
                        description = "As obras eram barulhentas e traziam muita poeira, mas sabíamos que era o progresso chegando. Foi quando o bairro começou a ter cara de cidade.",
                        category = "Transtornos de Obras",
                        year = "1994",
                        authorName = "Dona Maria da Penha",
                        authorAge = "62",
                        latitude = -3.7920,
                        longitude = -38.5980,
                        triggerRadiusMeters = 80.0,
                        imageUrl = "android.resource://${getApplication<Application>().packageName}/drawable/memoria_obras",
                        isApproved = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    MemoryEntity(
                        title = "Resiliência no Morro",
                        description = "A chuva derrubou algumas casas, mas a comunidade se uniu para reconstruir tudo. O Bom Jardim é feito de gente que não desiste.",
                        category = "Desmoronamento",
                        year = "2002",
                        authorName = "Raimundo Nonato",
                        authorAge = "45",
                        latitude = -3.7910,
                        longitude = -38.6000,
                        triggerRadiusMeters = 120.0,
                        imageUrl = "android.resource://${getApplication<Application>().packageName}/drawable/memoria_desmoronamento",
                        isApproved = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
                demoMemories.forEach { memoryDao.insertMemory(it) }
                loadMemoriesFromDb()
            }
        }
    }

    private fun loadMemoriesFromDb() {
        viewModelScope.launch {
            memoryDao.getAllMemories().collect { entities ->
                val domainMemories = entities.map { entity ->
                    Memory(
                        id = entity.id.toString(),
                        title = entity.title,
                        description = entity.description,
                        category = entity.category,
                        year = entity.year,
                        authorName = entity.authorName,
                        authorAge = entity.authorAge,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        triggerRadiusMeters = entity.triggerRadiusMeters,
                        imageUrl = entity.imageUrl,
                        imageSource = entity.imageSource,
                        audioUrl = entity.audioUrl,
                        isApproved = entity.isApproved
                    )
                }
                _memories.value = domainMemories.filter { it.isApproved }
                _pendingMemories.value = domainMemories.filter { !it.isApproved }
            }
        }
    }

    fun saveNewMemory(
        title: String, 
        description: String, 
        category: String,
        year: String,
        authorName: String,
        authorAge: String,
        latitude: Double, 
        longitude: Double, 
        triggerRadiusMeters: Double = 50.0,
        imageUrl: String? = null, 
        imageSource: String = "",
        audioUrl: String? = null
    ) {
        viewModelScope.launch {
            // Copia arquivos para o diretório interno para evitar perda de acesso à URI
            val finalImageUrl = if (imageUrl?.startsWith("content://") == true) {
                copyToInternalStorage(imageUrl, "img")
            } else imageUrl

            val finalAudioUrl = if (audioUrl?.startsWith("content://") == true) {
                copyToInternalStorage(audioUrl, "aud")
            } else audioUrl

            val entity = MemoryEntity(
                title = title,
                description = description,
                category = category,
                year = year,
                authorName = authorName,
                authorAge = authorAge,
                latitude = latitude,
                longitude = longitude,
                triggerRadiusMeters = triggerRadiusMeters,
                imageUrl = finalImageUrl,
                imageSource = imageSource,
                audioUrl = finalAudioUrl,
                isApproved = false,
                createdAt = System.currentTimeMillis()
            )
            memoryDao.insertMemory(entity)
        }
    }

    fun approveMemory(memoryId: String, pin: String): Boolean {
        if (!isTeacherPinValid(pin)) return false
        viewModelScope.launch {
            memoryDao.approveMemory(memoryId.toLong())
        }
        return true
    }

    fun rejectMemory(memoryId: String, pin: String): Boolean {
        if (!isTeacherPinValid(pin)) return false
        viewModelScope.launch {
            memoryDao.deleteMemoryById(memoryId.toLong())
        }
        return true
    }



    suspend fun exportDatabaseZip(): Uri {
        val exporter = DatabaseExporter(getApplication())
        return exporter.export(memoryDao.getAllMemoriesOnce())
    }

    private fun copyToInternalStorage(uriString: String?, fileNamePrefix: String): String? {
        if (uriString == null) return null
        val context = getApplication<Application>()
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "bin"
            val file = File(context.filesDir, "${fileNamePrefix}_${System.currentTimeMillis()}.$extension")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Lógica de Gravação
    fun startRecording() {
        val file = File(getApplication<Application>().filesDir, "recording_${System.currentTimeMillis()}.m4a")
        currentRecordingPath = file.absolutePath
        audioRecorder.start(file.absolutePath)
        _isRecording.value = true
        _recordingTime.value = 0
        triggerVibration() // Feedback ao iniciar

        viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingTime.value += 1
            }
        }
    }

    fun stopRecording(): String? {
        audioRecorder.stop()
        _isRecording.value = false
        triggerVibration() // Feedback ao parar
        return currentRecordingPath
    }

    fun clearNewMemoriesCount() {
        _newMemoriesCount.value = 0
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
            isTtsReady = true
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _spokenTextRange.value = Pair(0, 0)
                }

                override fun onDone(utteranceId: String?) {
                    _spokenTextRange.value = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _spokenTextRange.value = null
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    _spokenTextRange.value = Pair(start, end)
                }
            })
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _currentLocation.value = loc
                checkProximityToMemories(loc)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun checkProximityToMemories(userLocation: Location) {
        // Encontra a memória MAIS PRÓXIMA absoluta para o HUD de busca
        val allDistances = _memories.value.map { memory ->
            val memoryLocation = Location("").apply {
                latitude = memory.latitude
                longitude = memory.longitude
            }
            Triple(memory, userLocation.distanceTo(memoryLocation), userLocation.bearingTo(memoryLocation))
        }

        val closest = allDistances.minByOrNull { it.second }
        _nearestMemoryDistance.value = closest?.second
        _nearestMemoryBearing.value = closest?.third

        // Encontra a memória dentro do raio para ativação
        val foundMemory = closest?.takeIf { it.second <= it.first.triggerRadiusMeters }?.first
        _distanceToActive.value = if (foundMemory != null) closest.second else null

        if (foundMemory != null && _activeMemory.value?.id != foundMemory.id) {
            _activeMemory.value = foundMemory
            triggerVibration()
            
            // Incrementa contador de descobertas se ainda não foi descoberta nesta sessão
            if (!_discoveredMemories.value.contains(foundMemory.id)) {
                _newMemoriesCount.value += 1
            }

            // Toca áudio real ou TTS
            if (foundMemory.audioUrl != null) {
                playRealAudio(foundMemory.audioUrl)
            } else {
                playAudio(foundMemory.description)
            }
            
            awardGamificationPoints(foundMemory)
        } else if (foundMemory == null && _activeMemory.value != null && !_activeMemory.value!!.id.startsWith("mock_")) {
            // Se saiu do raio e não é uma simulação, limpa
            _activeMemory.value = null
            stopAudio()
        }
    }
    
    private fun playRealAudio(url: String) {
        stopAudio()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepare()
                start()
                
                // Monitora o progresso do áudio
                viewModelScope.launch {
                    while (mediaPlayer != null && isPlaying) {
                        _audioProgress.value = currentPosition.toFloat() / duration.toFloat()
                        delay(500)
                    }
                }

                setOnCompletionListener { 
                    it.release()
                    mediaPlayer = null
                    _audioProgress.value = 0f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback para TTS se o arquivo falhar
            _activeMemory.value?.let { playAudio(it.description) }
        }
    }

    fun dismissActiveMemory() {
        _activeMemory.value = null
        stopAudio()
    }

    fun togglePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
        }
    }

    private fun awardGamificationPoints(memory: Memory) {
        if (!_discoveredMemories.value.contains(memory.id)) {
            _discoveredMemories.value = _discoveredMemories.value + memory.id
            _userPoints.value += 10
            
            val newBadge = Badge(
                id = "badge_${memory.id}",
                title = "Pesquisador Nato",
                description = "Você encontrou o relato: ${memory.title}!",
                icon = "🏆",
                points = 10
            )
            
            _recentBadge.value = newBadge
            _earnedBadges.value = _earnedBadges.value + newBadge
            
            // Lógica para insígnias especiais baseadas em contagem
            checkSpecialAchievements()
            
            viewModelScope.launch {
                delay(6000)
                if (_recentBadge.value?.id == "badge_${memory.id}") {
                    _recentBadge.value = null
                }
            }
        }
    }

    private fun checkSpecialAchievements() {
        val discoveryCount = _discoveredMemories.value.size
        
        if (discoveryCount == 5 && !_earnedBadges.value.any { it.id == "special_historian" }) {
            val historianBadge = Badge("special_historian", "Historiador", "Encontrou 5 memórias", "📚", 50)
            _earnedBadges.value = _earnedBadges.value + historianBadge
            _recentBadge.value = historianBadge
            _userPoints.value += 50
        }
        
        if (discoveryCount == 10 && !_earnedBadges.value.any { it.id == "special_explorer" }) {
            val explorerBadge = Badge("special_explorer", "Explorador Urbano", "Encontrou 10 memórias", "🏙️", 100)
            _earnedBadges.value = _earnedBadges.value + explorerBadge
            _recentBadge.value = explorerBadge
            _userPoints.value += 100
        }
    }

    fun triggerVibration() {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(500)
        }
    }

    private fun playAudio(text: String) {
        if (isTtsReady) {
            val params = android.os.Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "memory_utterance")
        }
    }

    private fun stopAudio() {
        tts?.stop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _audioProgress.value = 0f
    }

    fun simulateMemoryAtCurrentLocation() {
        val loc = _currentLocation.value
        val newMemory = if (loc != null) {
            Memory(
                id = "mock_${System.currentTimeMillis()}",
                title = "Relato Encontrado!",
                description = "Essa é uma memória georreferenciada. Imagine que agora você estaria ouvindo o relato de um morador da comunidade falando sobre as enchentes do passado.",
                latitude = loc.latitude,
                longitude = loc.longitude,
                isApproved = true
            )
        } else {
            Memory(
                id = "mock_${System.currentTimeMillis()}",
                title = "Relato Encontrado!",
                description = "Essa é uma memória georreferenciada simulada sem GPS. Imagine que você está escutando um áudio histórico.",
                latitude = 0.0,
                longitude = 0.0,
                isApproved = true
            )
        }
        
        _activeMemory.value = newMemory
        triggerVibration()
        playAudio(newMemory.description)
        awardGamificationPoints(newMemory)
    }

    fun isTeacherPinValid(pin: String): Boolean {
        return pin == teacherPin
    }

    fun setTeacherPin(newPin: String) {
        teacherPin = newPin
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
