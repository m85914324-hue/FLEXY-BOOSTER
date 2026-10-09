package com.flexy.app.audio

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flexy.app.FlexyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

enum class RecState { IDLE, RECORDING, READY }

data class VoiceUiState(
    val state: RecState = RecState.IDLE,
    val elapsedMs: Long = 0,
    val liveLevels: List<Float> = emptyList(),
    val waveform: List<Float> = emptyList(),
    val effect: VoiceEffect = VoiceEffect.ROBOT,
    val intensity: Float = 0.6f,
    val volume: Float = 0.9f,
    val processing: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val savedName: String? = null,
    val error: String? = null
)

/**
 * Mode 1 only: effects applied to RECORDED audio.
 * (No real-time effects, and nothing is sent into other apps.)
 */
class VoiceViewModel(app: Application) : AndroidViewModel(app) {
    private val defaults = (app as FlexyApplication).settings.state.value
    private val recorder = VoiceRecorder()
    private val player = PcmPlayer()
    private var original: ShortArray? = null
    private var processed: ShortArray? = null
    private val version = AtomicInteger(0)

    private val _ui = MutableStateFlow(
        VoiceUiState(volume = defaults.voiceVolume, intensity = defaults.voiceIntensity)
    )
    val ui: StateFlow<VoiceUiState> = _ui.asStateFlow()

    private val _saved = MutableStateFlow(Recordings.list(app))
    val saved: StateFlow<List<File>> = _saved.asStateFlow()

    // ---------- recording ----------

    fun startRecording() {
        if (_ui.value.state == RecState.RECORDING) return
        stopPlayback()
        original = null
        processed = null
        version.incrementAndGet()
        _ui.update {
            it.copy(
                state = RecState.RECORDING, elapsedMs = 0, liveLevels = emptyList(),
                waveform = emptyList(), savedName = null, error = null, processing = false
            )
        }
        val startNs = System.nanoTime()
        viewModelScope.launch {
            val data = recorder.record { level ->
                _ui.update { s ->
                    s.copy(
                        elapsedMs = (System.nanoTime() - startNs) / 1_000_000,
                        liveLevels = (s.liveLevels + level).takeLast(48)
                    )
                }
            }
            when {
                data == null -> _ui.update {
                    it.copy(state = RecState.IDLE, error = "Couldn't open the microphone. Another app may be using it.")
                }
                data.size < SAMPLE_RATE / 3 -> _ui.update {
                    it.copy(state = RecState.IDLE, error = "That recording was too short. Hold on a little longer.")
                }
                else -> {
                    original = data
                    _ui.update { it.copy(state = RecState.READY, elapsedMs = data.size * 1000L / SAMPLE_RATE) }
                    reprocess()
                }
            }
        }
    }

    fun stopRecording() {
        if (_ui.value.state == RecState.RECORDING) recorder.requestStop()
    }

    // ---------- effects ----------

    fun selectEffect(effect: VoiceEffect) {
        _ui.update { it.copy(effect = effect) }
        if (original != null) {
            stopPlayback()
            reprocess()
        }
    }

    fun setIntensity(v: Float) = _ui.update { it.copy(intensity = v) }

    fun commitIntensity() {
        if (original != null) {
            stopPlayback()
            reprocess()
        }
    }

    fun setVolume(v: Float) {
        _ui.update { it.copy(volume = v, savedName = null) }
        player.setVolume(v)
    }

    private fun reprocess() {
        val src = original ?: return
        val s = _ui.value
        val v = version.incrementAndGet()
        _ui.update { it.copy(processing = true, savedName = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val out = VoiceEffects.apply(src, s.effect, s.intensity)
            if (v != version.get()) return@launch
            processed = out
            _ui.update { it.copy(processing = false, waveform = Waveform.bars(out, 48)) }
        }
    }

    // ---------- playback ----------

    fun togglePlayback() {
        if (_ui.value.isPlaying) {
            stopPlayback()
            return
        }
        val data = processed ?: return
        _ui.update { it.copy(isPlaying = true, progress = 0f) }
        player.play(
            scope = viewModelScope,
            data = data,
            volume = _ui.value.volume,
            onProgress = { p -> _ui.update { it.copy(progress = p) } },
            onComplete = { _ui.update { it.copy(isPlaying = false, progress = 0f) } }
        )
    }

    fun stopPlayback() {
        player.stop()
        _ui.update { it.copy(isPlaying = false, progress = 0f) }
    }

    // ---------- saving / sharing ----------

    fun saveCurrent(onDone: (File?) -> Unit) {
        val pcm = processed ?: return onDone(null)
        val s = _ui.value
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val f = File(Recordings.dir(getApplication<Application>()), "FLEXY_${stamp}_${s.effect.label.replace(' ', '_')}.wav")
                    WavUtil.write(f, VoiceEffects.applyGain(pcm, s.volume))
                    f
                }.getOrNull()
            }
            _saved.value = Recordings.list(getApplication<Application>())
            _ui.update {
                it.copy(savedName = file?.name, error = if (file == null) "Couldn't save the recording." else null)
            }
            onDone(file)
        }
    }

    /** Copies a saved file to Music/FLEXY so other apps can see it (Android 10+). */
    fun exportToMusic(file: File, onDone: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 29) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/FLEXY")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("insert failed")
                    resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                }.getOrDefault(false)
            }
            onDone(ok)
        }
    }

    fun delete(file: File) {
        file.delete()
        _saved.value = Recordings.list(getApplication<Application>())
    }

    fun refreshSaved() {
        _saved.value = Recordings.list(getApplication<Application>())
    }

    override fun onCleared() {
        recorder.requestStop()
        player.stop()
        super.onCleared()
    }
}
