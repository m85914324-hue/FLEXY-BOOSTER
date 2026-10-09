package com.flexy.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

const val SAMPLE_RATE = 44100

/**
 * Records from the microphone ONLY while record() is running.
 * The caller must already hold the RECORD_AUDIO permission and show a recording indicator.
 */
class VoiceRecorder {
    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    /** Returns 16-bit mono PCM, or null if the microphone could not be opened. */
    @SuppressLint("MissingPermission")
    suspend fun record(maxSeconds: Int = 120, onLevel: (Float) -> Unit): ShortArray? =
        withContext(Dispatchers.IO) {
            stopRequested = false
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return@withContext null
            val chunk = 2048
            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    max(minBuf, chunk * 4)
                )
            } catch (e: Exception) {
                return@withContext null
            }
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return@withContext null
            }

            val maxSamples = SAMPLE_RATE * maxSeconds
            var data = ShortArray(SAMPLE_RATE * 10)
            var size = 0
            val buf = ShortArray(chunk)
            try {
                recorder.startRecording()
                while (!stopRequested && isActive && size < maxSamples) {
                    val n = recorder.read(buf, 0, buf.size)
                    if (n < 0) break
                    if (n == 0) continue
                    if (size + n > data.size) data = data.copyOf(max(data.size * 2, size + n))
                    System.arraycopy(buf, 0, data, size, n)
                    size += n
                    var sum = 0.0
                    for (i in 0 until n) {
                        val v = buf[i] / 32768.0
                        sum += v * v
                    }
                    onLevel(min(1f, (sqrt(sum / n) * 5.0).toFloat()))
                }
            } finally {
                try {
                    recorder.stop()
                } catch (e: IllegalStateException) {
                    // already stopped
                }
                recorder.release()
            }
            data.copyOf(size)
        }
}
