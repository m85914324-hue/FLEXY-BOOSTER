package com.flexy.app.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flexy.app.BuildConfig
import com.flexy.app.ads.Plan
import com.flexy.app.audio.Recordings
import com.flexy.app.flexy
import com.flexy.app.navigation.Route
import com.flexy.app.system.Connectivity
import com.flexy.app.system.SessionNotifier
import com.flexy.app.system.SystemShortcuts
import com.flexy.app.system.appVersion
import com.flexy.app.ui.components.ChoiceChip
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.InfoRow
import com.flexy.app.ui.components.NeonButton
import com.flexy.app.ui.components.SectionTitle
import com.flexy.app.ui.components.StatusPill
import com.flexy.app.ui.components.findComponentActivity
import com.flexy.app.ui.components.flexyClickable
import com.flexy.app.ui.components.rememberToast
import com.flexy.app.ui.theme.AccentColor
import com.flexy.app.ui.theme.ThemeMode

@Composable
fun SettingsScreen(onNavigate: (Route) -> Unit, onOpenPrivacy: () -> Unit) {
    val context = LocalContext.current
    val app = context.flexy
    val cs = MaterialTheme.colorScheme
    val settings by app.settings.state.collectAsState()
    val plan by app.plans.plan.collectAsState()
    val price by app.billing.price.collectAsState()
    val privacyOptions by app.ads.privacyOptionsRequired.collectAsState()
    val toast = rememberToast()
    var showNotifRationale by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            app.settings.update { it.copy(sessionNotification = true) }
            app.sessions.refreshNotification()
        } else {
            toast("Notification permission was denied. You can allow it in system settings.")
        }
    }
    val sliderColors = SliderDefaults.colors(
        thumbColor = cs.primary, activeTrackColor = cs.primary, inactiveTrackColor = cs.outline
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- plan ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("FLEXY plan")
                GlassCard(Modifier.fillMaxWidth(), glow = plan == Plan.PLUS) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (plan == Plan.PLUS) "FLEXY PLUS" else "FLEXY FREE",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (plan == Plan.PLUS) cs.primary else cs.onSurface
                            )
                            Text(
                                if (plan == Plan.PLUS) "Thank you! FLEXY is completely ad-free and never requests ads."
                                else "FREE shows one ad when you open the app. All features are included.",
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                            )
                        }
                        StatusPill(if (plan == Plan.PLUS) "PLUS" else "FREE", plan == Plan.PLUS)
                    }
                    if (plan == Plan.FREE) {
                        Spacer(Modifier.height(14.dp))
                        NeonButton(
                            text = price?.let { "Go ad-free with PLUS ($it)" } ?: "Go ad-free with PLUS",
                            onClick = {
                                val activity = context.findComponentActivity()
                                when {
                                    activity == null -> toast("Couldn't open Google Play right now.")
                                    !Connectivity.isOnline(context) -> toast("You're offline. Connect to the internet to upgrade.")
                                    !app.billing.purchase(activity) ->
                                        toast("PLUS isn't available right now. It needs FLEXY installed from Google Play.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        NeonButton(
                            "Restore purchase",
                            {
                                if (Connectivity.isOnline(context)) {
                                    app.billing.connect()
                                    toast("Checking Google Play...")
                                } else toast("You're offline. Connect to the internet to restore.")
                            },
                            Modifier.fillMaxWidth(), filled = false
                        )
                        if (privacyOptions) {
                            Spacer(Modifier.height(8.dp))
                            NeonButton(
                                "Ad privacy options",
                                { context.findComponentActivity()?.let { app.ads.showPrivacyOptions(it) } },
                                Modifier.fillMaxWidth(), filled = false
                            )
                        }
                    }
                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.height(12.dp))
                        SwitchRow(
                            "Debug: simulate PLUS", "Only in debug builds, for testing both plans.",
                            app.plans.debugPlus
                        ) { app.plans.setDebugPlus(it) }
                    }
                }
            }
        }

        // ---- appearance ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Appearance")
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ChoiceChip(mode.label, settings.theme == mode, { app.settings.update { it.copy(theme = mode) } }, Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Accent color", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccentColor.entries.forEach { accent ->
                            val selected = settings.accent == accent
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accent.color)
                                    .border(if (selected) 3.dp else 0.dp, cs.onSurface, CircleShape)
                                    .flexyClickable { app.settings.update { it.copy(accent = accent) } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Icon(Icons.Rounded.Check, accent.label, tint = Color0)
                            }
                        }
                    }
                }
            }
        }

        // ---- feedback ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Feedback")
                GlassCard(Modifier.fillMaxWidth()) {
                    SwitchRow("Haptic feedback", "Small vibration when you tap buttons.", settings.haptics) {
                        app.settings.update { s -> s.copy(haptics = it) }
                    }
                    SwitchRow("Sound effects", "Plays Android's tap sound (if your phone's touch sounds are on).", settings.sounds) {
                        app.settings.update { s -> s.copy(sounds = it) }
                    }
                }
            }
        }

        // ---- voice ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Voice settings")
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Default effect intensity: ${(settings.voiceIntensity * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium, color = cs.onSurface
                    )
                    Slider(
                        value = settings.voiceIntensity,
                        onValueChange = { v -> app.settings.update { it.copy(voiceIntensity = v) } },
                        colors = sliderColors
                    )
                    Text(
                        "Default playback volume: ${(settings.voiceVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium, color = cs.onSurface
                    )
                    Slider(
                        value = settings.voiceVolume,
                        onValueChange = { v -> app.settings.update { it.copy(voiceVolume = v) } },
                        colors = sliderColors
                    )
                    InfoRow("Recording format", "44.1 kHz, mono, 16-bit WAV")
                    Text(
                        "Defaults apply the next time you open the Voice Changer.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
        }

        // ---- notifications ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Notifications")
                GlassCard(Modifier.fillMaxWidth()) {
                    SwitchRow(
                        "Gaming session notification",
                        "Shows a running timer in your notification shade while a session is active.",
                        settings.sessionNotification
                    ) { on ->
                        if (!on) {
                            app.settings.update { it.copy(sessionNotification = false) }
                            app.sessions.refreshNotification()
                        } else if (SessionNotifier.canPost(context)) {
                            app.settings.update { it.copy(sessionNotification = true) }
                            app.sessions.refreshNotification()
                        } else showNotifRationale = true
                    }
                    Spacer(Modifier.height(8.dp))
                    NeonButton(
                        "System notification settings",
                        { if (!SystemShortcuts.openNotificationSettings(context)) toast("Couldn't open settings") },
                        Modifier.fillMaxWidth(), filled = false
                    )
                }
            }
        }

        // ---- privacy ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Privacy")
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Voice recordings stay on your phone unless you tap Share. FLEXY never records in the background.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    NeonButton("Privacy information", onOpenPrivacy, Modifier.fillMaxWidth(), filled = false)
                    Spacer(Modifier.height(8.dp))
                    NeonButton("Delete all saved recordings", { confirmDelete = true }, Modifier.fillMaxWidth(), filled = false)
                }
            }
        }

        // ---- about ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("About")
                GlassCard(Modifier.fillMaxWidth()) {
                    InfoRow("App", "FLEXY")
                    InfoRow("Version", appVersion(context))
                    Spacer(Modifier.height(8.dp))
                    NeonButton("About FLEXY", { onNavigate(Route.ABOUT) }, Modifier.fillMaxWidth(), filled = false)
                }
            }
        }
    }

    if (showNotifRationale) {
        AlertDialog(
            onDismissRequest = { showNotifRationale = false },
            containerColor = cs.surfaceVariant,
            title = { Text("Allow notifications?") },
            text = { Text("FLEXY only uses notifications to show your running gaming-session timer. It never sends promotions.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotifRationale = false
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showNotifRationale = false }) { Text("Not now") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = cs.surfaceVariant,
            title = { Text("Delete all recordings?") },
            text = { Text("This permanently deletes every recording FLEXY saved on this phone. Copies you shared or exported are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Recordings.deleteAll(context)
                    toast("All saved recordings deleted")
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

private val Color0 = androidx.compose.ui.graphics.Color(0xFF031014)

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = cs.onPrimary, checkedTrackColor = cs.primary,
                uncheckedThumbColor = cs.onSurfaceVariant, uncheckedTrackColor = cs.surface,
                uncheckedBorderColor = cs.outline
            )
        )
    }
}
