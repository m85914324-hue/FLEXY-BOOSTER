package com.flexy.app.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object WavUtil {
    /** Writes a standard 16-bit mono PCM .wav file. */
    fun write(file: File, pcm: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        val dataLen = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)            // PCM
        header.putShort(1)            // mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2) // byte rate
        header.putShort(2)            // block align
        header.putShort(16)           // bits per sample
        header.put("data".toByteArray())
        header.putInt(dataLen)

        file.outputStream().buffered().use { out ->
            out.write(header.array())
            val bb = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
            var i = 0
            while (i < pcm.size) {
                bb.clear()
                val end = min(pcm.size, i + 4096)
                while (i < end) {
                    bb.putShort(pcm[i])
                    i++
                }
                out.write(bb.array(), 0, bb.position())
            }
        }
    }
}
