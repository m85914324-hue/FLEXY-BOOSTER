package com.flexy.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

enum class VoiceEffect(val emoji: String, val label: String) {
    ORIGINAL("🎤", "Original"),
    ROBOT("🤖", "Robot"),
    MONSTER("👹", "Monster"),
    ALIEN("👽", "Alien"),
    RADIO("📻", "Radio"),
    DEEP("🔊", "Deep Voice"),
    HIGH("🐿️", "High Voice"),
    ECHO("🌀", "Echo")
}

/**
 * Offline DSP applied to RECORDED audio (16-bit mono, 44.1 kHz).
 * Pitch effects use a WSOLA time-stretch + resample so speech keeps its length.
 */
object VoiceEffects {
    private const val TWO_PI = 2.0 * PI

    fun apply(input: ShortArray, effect: VoiceEffect, intensity: Float): ShortArray {
        val x = FloatArray(input.size) { input[it] / 32768f }
        val i = intensity.coerceIn(0f, 1f)
        val y = when (effect) {
            VoiceEffect.ORIGINAL -> x
            VoiceEffect.ROBOT -> robot(x, i)
            VoiceEffect.MONSTER -> monster(x, i)
            VoiceEffect.ALIEN -> alien(x, i)
            VoiceEffect.RADIO -> radio(x, i)
            VoiceEffect.DEEP -> deep(x, i)
            VoiceEffect.HIGH -> pitchShift(x, 1.25 + 0.65 * i)
            VoiceEffect.ECHO -> echo(x, i)
        }
        return toPcm(y)
    }

    fun applyGain(pcm: ShortArray, gain: Float): ShortArray =
        ShortArray(pcm.size) { (pcm[it] * gain).toInt().coerceIn(-32768, 32767).toShort() }

    // ---------------- effects ----------------

    private fun robot(x: FloatArray, i: Float): FloatArray {
        val w = TWO_PI * (45.0 + 85.0 * i) / SAMPLE_RATE
        val mix = 0.55f + 0.45f * i
        val out = FloatArray(x.size)
        for (n in x.indices) {
            val ring = x[n] * sin(w * n).toFloat()
            out[n] = x[n] * (1f - mix) + ring * mix
        }
        val d = (SAMPLE_RATE * 0.006).toInt()
        val fb = 0.35f + 0.3f * i
        for (n in d until out.size) out[n] += out[n - d] * fb
        return out
    }

    private fun monster(x: FloatArray, i: Float): FloatArray {
        val p = pitchShift(x, 0.78 - 0.30 * i)
        val drive = 1.5f + 4f * i
        val w = TWO_PI * 28.0 / SAMPLE_RATE
        for (n in p.indices) {
            val growl = 1f - 0.4f * i + 0.4f * i * sin(w * n).toFloat()
            p[n] = tanh(p[n] * drive * growl)
        }
        return p
    }

    private fun alien(x: FloatArray, i: Float): FloatArray {
        val p = pitchShift(x, 1.15 + 0.35 * i)
        val out = FloatArray(p.size)
        val depth = 18f + 40f * i
        val vibW = TWO_PI * 6.5 / SAMPLE_RATE
        val ringW = TWO_PI * 320.0 / SAMPLE_RATE
        val ringMix = 0.35f * i
        for (n in p.indices) {
            val delay = depth * (1f + sin(vibW * n).toFloat()) / 2f
            val pos = n - delay
            var s = 0f
            if (pos >= 0f) {
                val idx = pos.toInt()
                val frac = pos - idx
                if (idx + 1 < p.size) s = p[idx] * (1f - frac) + p[idx + 1] * frac
            }
            val ring = s * sin(ringW * n).toFloat()
            out[n] = s * (1f - ringMix) + ring * ringMix
        }
        return out
    }

    private fun radio(x: FloatArray, i: Float): FloatArray {
        val hp = Biquad.highPass(300f + 150f * i)
        val lp = Biquad.lowPass(3200f - 800f * i)
        val drive = 1.5f + 5f * i
        val out = FloatArray(x.size)
        var seed = 12345
        for (n in x.indices) {
            var s = lp.process(hp.process(x[n]))
            s = tanh(s * drive)
            seed = seed * 1103515245 + 12345
            val noise = ((seed shr 16) and 0x7fff) / 16384f - 1f
            out[n] = s + noise * 0.015f * i
        }
        return out
    }

    private fun deep(x: FloatArray, i: Float): FloatArray {
        val p = pitchShift(x, 0.88 - 0.25 * i)
        val lp = Biquad.lowPass(250f)
        val boost = 0.6f + 0.9f * i
        for (n in p.indices) p[n] = p[n] + lp.process(p[n]) * boost
        return p
    }

    private fun echo(x: FloatArray, i: Float): FloatArray {
        val delay = (SAMPLE_RATE * 0.30).toInt()
        val fb = 0.30f + 0.35f * i
        val tail = (SAMPLE_RATE * (1.0 + 1.5 * i)).toInt()
        val out = FloatArray(x.size + tail)
        for (n in out.indices) {
            val dry = if (n < x.size) x[n] else 0f
            val e = if (n >= delay) out[n - delay] else 0f
            out[n] = dry + e * fb
        }
        return out
    }

    // ---------------- pitch shifting ----------------

    /** ratio > 1 raises pitch, ratio < 1 lowers it. Duration stays the same. */
    fun pitchShift(x: FloatArray, ratio: Double): FloatArray {
        if (x.size < 4096 || abs(ratio - 1.0) < 1e-3) return x.copyOf()
        val stretched = wsolaStretch(x, ratio)
        val out = FloatArray(x.size)
        for (n in out.indices) {
            val pos = n * ratio
            val idx = pos.toInt()
            if (idx + 1 >= stretched.size) break
            val frac = (pos - idx).toFloat()
            out[n] = stretched[idx] * (1f - frac) + stretched[idx + 1] * frac
        }
        return out
    }

    /** Waveform-similarity overlap-add time stretch (output length = input * ratio). */
    private fun wsolaStretch(x: FloatArray, ratio: Double): FloatArray {
        val win = 2048
        val hs = win / 2
        val ha = hs / ratio
        val seek = 256
        val corrLen = 512
        val w = FloatArray(win) { (0.5 - 0.5 * cos(TWO_PI * it / win)).toFloat() }
        val outLen = (x.size * ratio).toInt() + win
        val out = FloatArray(outLen)
        var k = 0
        var prevPos = 0
        while (true) {
            val outPos = k * hs
            if (outPos + win > outLen) break
            val nominal = (k * ha).toInt()
            if (nominal >= x.size) break
            var chosen = nominal
            if (k > 0) {
                val ref = prevPos + hs
                var best = -Float.MAX_VALUE
                var d = -seek
                while (d <= seek) {
                    val cand = nominal + d
                    if (cand >= 0 && cand + corrLen < x.size && ref + corrLen < x.size) {
                        var c = 0f
                        var j = 0
                        while (j < corrLen) {
                            c += x[ref + j] * x[cand + j]
                            j += 2
                        }
                        if (c > best) {
                            best = c
                            chosen = cand
                        }
                    }
                    d += 2
                }
            }
            for (j in 0 until win) {
                val src = chosen + j
                val s = if (src < x.size) x[src] else 0f
                out[outPos + j] += s * w[j]
            }
            prevPos = chosen
            k++
        }
        return out.copyOf((x.size * ratio).toInt())
    }

    private fun toPcm(y: FloatArray): ShortArray {
        var peak = 0f
        for (v in y) peak = max(peak, abs(v))
        val scale = if (peak > 1e-4f) min(0.89f / peak, 8f) else 1f
        return ShortArray(y.size) { (y[it] * scale * 32767f).toInt().coerceIn(-32768, 32767).toShort() }
    }
}

/** Simple 2nd-order filter (RBJ cookbook, Q = 0.707). */
private class Biquad(
    private val b0: Float, private val b1: Float, private val b2: Float,
    private val a1: Float, private val a2: Float
) {
    private var z1 = 0f
    private var z2 = 0f

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    companion object {
        fun lowPass(fc: Float) = make(fc, true)
        fun highPass(fc: Float) = make(fc, false)

        private fun make(fc: Float, low: Boolean): Biquad {
            val w0 = 2.0 * PI * fc / SAMPLE_RATE
            val alpha = sin(w0) / (2.0 * 0.7071)
            val c = cos(w0)
            val b0: Double
            val b1: Double
            val b2: Double
            if (low) {
                b0 = (1 - c) / 2; b1 = 1 - c; b2 = (1 - c) / 2
            } else {
                b0 = (1 + c) / 2; b1 = -(1 + c); b2 = (1 + c) / 2
            }
            val a0 = 1 + alpha
            return Biquad(
                (b0 / a0).toFloat(), (b1 / a0).toFloat(), (b2 / a0).toFloat(),
                (-2 * c / a0).toFloat(), ((1 - alpha) / a0).toFloat()
            )
        }
    }
}

object Waveform {
    /** Downsamples audio to [count] bar heights (0..1) for drawing. */
    fun bars(pcm: ShortArray, count: Int): List<Float> {
        if (pcm.isEmpty()) return emptyList()
        val bin = max(1, pcm.size / count)
        return List(count) { b ->
            val start = b * bin
            val end = min(pcm.size, start + bin)
            var peak = 0
            var i = start
            while (i < end) {
                val v = abs(pcm[i].toInt())
                if (v > peak) peak = v
                i++
            }
            sqrt((peak / 32768f).coerceIn(0f, 1f))
        }
    }
}
