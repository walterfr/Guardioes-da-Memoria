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

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(application, this)
        loadMemoriesFromDb()
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
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        imageUrl = entity.imageUrl,
                        audioUrl = entity.audioUrl
                    )
                }
                _memories.value = domainMemories
            }
        }
    }

    fun saveNewMemory(
        title: String, 
        description: String, 
        category: String,
        year: String,
        authorName: String,
        latitude: Double, 
        longitude: Double, 
        imageUrl: String? = null, 
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
                latitude = latitude,
                longitude = longitude,
                imageUrl = finalImageUrl,
                audioUrl = finalAudioUrl,
                createdAt = System.currentTimeMillis()
            )
            memoryDao.insertMemory(entity)
        }
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
        val file = File(getApplication<Application>().cacheDir, "recording_${System.currentTimeMillis()}.mp3")
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
        val triggerRadiusMeters = 50.0 

        // Encontra a memória MAIS PRÓXIMA dentro do raio, conforme sugerido na auditoria técnica
        val nearestMemory = _memories.value.map { memory ->
            val memoryLocation = Location("").apply {
                latitude = memory.latitude
                longitude = memory.longitude
            }
            Pair(memory, userLocation.distanceTo(memoryLocation))
        }
        .filter { it.second <= triggerRadiusMeters }
        .minByOrNull { it.second }

        val foundMemory = nearestMemory?.first
        _distanceToActive.value = nearestMemory?.second

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
    }

    fun simulateMemoryAtCurrentLocation() {
        val loc = _currentLocation.value
        val newMemory = if (loc != null) {
            Memory(
                id = "mock_${System.currentTimeMillis()}",
                title = "Relato Encontrado!",
                description = "Essa é uma memória georreferenciada. Imagine que agora você estaria ouvindo o relato de um morador da comunidade falando sobre as enchentes do passado.",
                latitude = loc.latitude,
                longitude = loc.longitude
            )
        } else {
            Memory(
                id = "mock_${System.currentTimeMillis()}",
                title = "Relato Encontrado!",
                description = "Essa é uma memória georreferenciada simulada sem GPS. Imagine que você está escutando um áudio histórico.",
                latitude = 0.0,
                longitude = 0.0
            )
        }
        
        _activeMemory.value = newMemory
        triggerVibration()
        playAudio(newMemory.description)
        awardGamificationPoints(newMemory)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

