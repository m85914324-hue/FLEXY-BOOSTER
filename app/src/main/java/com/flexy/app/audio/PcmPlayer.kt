package com.flexy.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/** Plays 16-bit mono PCM through the normal media output. */
class PcmPlayer {
    private var job: Job? = null

    @Volatile
    private var track: AudioTrack? = null

    fun play(
        scope: CoroutineScope,
        data: ShortArray,
        volume: Float,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit
    ) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track = t
            try {
                t.setVolume(volume)
                t.play()
                var offset = 0
                while (isActive && offset < data.size) {
                    val n = min(4096, data.size - offset)
                    val written = t.write(data, offset, n)
                    if (written < 0) break
                    offset += written
                    onProgress(min(1f, t.playbackHeadPosition.toFloat() / data.size))
                }
                if (isActive) {
                    t.stop()
                    var waited = 0
                    while (isActive && t.playbackHeadPosition < data.size && waited < 1500) {
                        delay(30)
                        waited += 30
                    }
                }
            } finally {
                runCatching { t.release() }
                if (track === t) track = null
            }
            if (isActive) onComplete()
        }
    }

    fun setVolume(volume: Float) {
        runCatching { track?.setVolume(volume) }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
