package com.flexy.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flexy.app.audio.RecState
import com.flexy.app.audio.Recordings
import com.flexy.app.audio.VoiceEffect
import com.flexy.app.audio.VoiceViewModel
import com.flexy.app.system.SystemShortcuts
import com.flexy.app.system.formatClip
import com.flexy.app.ui.components.FlexyIconButton
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.LiveDot
import com.flexy.app.ui.components.NeonButton
import com.flexy.app.ui.components.SectionTitle
import com.flexy.app.ui.components.StatusPill
import com.flexy.app.ui.components.WaveformView
import com.flexy.app.ui.components.flexyClickable
import com.flexy.app.ui.components.rememberToast
import java.io.File

private val RecRed = Color(0xFFFF3B4F)

private fun hasMic(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

@Composable
fun VoiceScreen(vm: VoiceViewModel = viewModel()) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val ui by vm.ui.collectAsState()
    val saved by vm.saved.collectAsState()
    val toast = rememberToast()

    var micGranted by remember { mutableStateOf(hasMic(context)) }
    var askedOnce by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (granted) vm.startRecording() else askedOnce = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        micGranted = hasMic(context)
        vm.refreshSaved()
    }
    // Privacy: the mic is never left running in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stopRecording() }
    DisposableEffect(Unit) {
        onDispose {
            vm.stopRecording()
            vm.stopPlayback()
        }
    }
    LaunchedEffect(ui.error) { ui.error?.let { toast(it) } }

    val recording = ui.state == RecState.RECORDING
    val ready = ui.state == RecState.READY

    fun onRecordClick() {
        when {
            recording -> vm.stopRecording()
            !micGranted -> showRationale = true
            else -> vm.startRecording()
        }
    }

    fun withSavedFile(action: (File) -> Unit) {
        val existing = saved.firstOrNull { it.name == ui.savedName }
        if (existing != null) action(existing)
        else vm.saveCurrent { file -> if (file != null) action(file) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- always-visible recording indicator ----
        if (recording) {
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot()
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("RECORDING", style = MaterialTheme.typography.labelLarge, color = RecRed)
                            Text("The microphone is on. Tap Stop when you're done.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Text(formatClip(ui.elapsedMs), fontSize = 22.sp, fontFamily = FontFamily.Monospace, color = cs.onSurface)
                    }
                }
            }
        }

        // ---- microphone permission ----
        if (!micGranted && !recording) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Microphone access needed", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "FLEXY only records after you tap the record button, always shows a red recording indicator, and keeps recordings on your phone.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    NeonButton("Allow microphone", { showRationale = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Mic)
                    if (askedOnce) {
                        Spacer(Modifier.height(8.dp))
                        NeonButton(
                            "Open app settings",
                            { if (!SystemShortcuts.openAppSettings(context)) toast("Couldn't open settings") },
                            Modifier.fillMaxWidth(), filled = false
                        )
                    }
                }
            }
        }

        // ---- waveform ----
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    when {
                        recording -> WaveformView(
                            ui.liveLevels, Modifier.fillMaxWidth().height(110.dp), progress = 1f, activeColor = RecRed
                        )
                        ready -> WaveformView(
                            ui.waveform, Modifier.fillMaxWidth().height(110.dp),
                            progress = if (ui.isPlaying) ui.progress else 1f,
                            idleColor = cs.primary.copy(alpha = 0.35f)
                        )
                        else -> Text(
                            "Tap the mic to record your voice",
                            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (ui.processing) {
                        Text(
                            "Applying effect...",
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(cs.surface.copy(alpha = 0.85f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium, color = cs.primary
                        )
                    }
                }
                if (ready) {
                    Text(
                        "Clip length ${formatClip(ui.elapsedMs)}  •  ${ui.effect.emoji} ${ui.effect.label}",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
        }

        // ---- record + playback controls ----
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(if (recording) RecRed else cs.primary)
                        .flexyClickable { onRecordClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        if (recording) "Stop recording" else "Start recording",
                        tint = if (recording) Color.White else cs.onPrimary, modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (recording) "Tap to stop" else if (ready) "Tap to record again" else "Tap to record",
                    style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        if (ui.isPlaying) "Stop" else "Play",
                        { vm.togglePlayback() },
                        Modifier.weight(1f),
                        icon = if (ui.isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        enabled = ready && !ui.processing
                    )
                    NeonButton(
                        if (ui.savedName != null) "Saved" else "Save",
                        { vm.saveCurrent { f -> toast(if (f != null) "Saved on this phone: ${f.name}" else "Couldn't save") } },
                        Modifier.weight(1f), icon = Icons.Rounded.Save, filled = false,
                        enabled = ready && !ui.processing && ui.savedName == null
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        "Share", { withSavedFile { Recordings.share(context, it) } },
                        Modifier.weight(1f), icon = Icons.Rounded.Share, filled = false,
                        enabled = ready && !ui.processing
                    )
                    NeonButton(
                        "To Music folder",
                        {
                            withSavedFile { file ->
                                vm.exportToMusic(file) { ok ->
                                    toast(
                                        when {
                                            ok -> "Copied to Music/FLEXY"
                                            Build.VERSION.SDK_INT < 29 -> "Android 8-9 can't do this without storage access. Use Share instead."
                                            else -> "Couldn't copy the file"
                                        }
                                    )
                                }
                            }
                        },
                        Modifier.weight(1f), icon = Icons.Rounded.Download, filled = false,
                        enabled = ready && !ui.processing
                    )
                }
            }
        }

        // ---- effects ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Voice effects")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(VoiceEffect.entries.toList(), key = { it.name }) { effect ->
                        EffectTile(effect, selected = effect == ui.effect) { vm.selectEffect(effect) }
                    }
                }
            }
        }

        // ---- sliders ----
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                val colors = SliderDefaults.colors(
                    thumbColor = cs.primary, activeTrackColor = cs.primary,
                    inactiveTrackColor = cs.outline
                )
                Row {
                    Text("Effect intensity", style = MaterialTheme.typography.titleMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
                    Text("${(ui.intensity * 100).toInt()}%", color = cs.primary, style = MaterialTheme.typography.labelLarge)
                }
                Slider(
                    value = ui.intensity, onValueChange = { vm.setIntensity(it) },
                    onValueChangeFinished = { vm.commitIntensity() }, colors = colors
                )
                Row {
                    Text("Volume", style = MaterialTheme.typography.titleMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
                    Text("${(ui.volume * 100).toInt()}%", color = cs.primary, style = MaterialTheme.typography.labelLarge)
                }
                Slider(value = ui.volume, onValueChange = { vm.setVolume(it) }, colors = colors)
            }
        }

        // ---- saved recordings ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Saved on this phone")
                if (saved.isEmpty()) {
                    Text(
                        "Nothing saved yet. Recordings you save stay in FLEXY's private storage.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
        items(saved, key = { it.name }) { file ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    FlexyIconButton(Icons.Rounded.Share, "Share ${file.name}", { Recordings.share(context, file) })
                    FlexyIconButton(Icons.Rounded.Delete, "Delete ${file.name}", { vm.delete(file) })
                }
            }
        }

        // ---- the three voice modes, explained honestly ----
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = cs.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("What FLEXY's voice changer can and can't do", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                }
                ModeRow(
                    "1. Effects on recorded audio", "Available",
                    true, "Record, pick an effect, play, save or share. This is what this screen does."
                )
                ModeRow(
                    "2. Real-time microphone effects", "Not in v1.0",
                    false, "An app can process your mic live inside itself (needs a low-latency audio engine), but FLEXY v1.0 only processes recordings."
                )
                ModeRow(
                    "3. In-game voice chat", "Not possible",
                    false, "Android limitation: each game opens the microphone itself, and Android doesn't let one app replace another app's mic audio. FLEXY can't change your voice inside other games. Closest alternative: record an effect here and share the clip in chat."
                )
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            containerColor = cs.surfaceVariant,
            title = { Text("Allow microphone?") },
            text = {
                Text(
                    "FLEXY needs the microphone to record your voice so it can add effects. " +
                        "It records only after you tap the record button, shows a red indicator the whole time, " +
                        "stops if you leave the app, and keeps the audio on your phone. Nothing is uploaded unless you tap Share."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text("Not now") } }
        )
    }
}

@Composable
private fun EffectTile(effect: VoiceEffect, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .width(92.dp)
            .clip(shape)
            .background(if (selected) cs.primary.copy(alpha = 0.18f) else cs.surfaceVariant)
            .border(1.5.dp, if (selected) cs.primary else cs.outline, shape)
            .flexyClickable(onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(effect.emoji, fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            effect.label, style = MaterialTheme.typography.labelMedium,
            color = if (selected) cs.primary else cs.onSurface, textAlign = TextAlign.Center, maxLines = 1
        )
    }
}

@Composable
private fun ModeRow(title: String, status: String, ok: Boolean, detail: String) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
            StatusPill(status, ok)
        }
        Spacer(Modifier.height(4.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}
