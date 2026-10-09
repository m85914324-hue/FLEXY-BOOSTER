package com.flexy.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.flexy.app.ads.Plan
import com.flexy.app.flexy
import com.flexy.app.system.SessionNotifier
import com.flexy.app.system.SystemShortcuts
import com.flexy.app.system.appVersion
import com.flexy.app.ui.components.FlexyLogo
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.InfoRow
import com.flexy.app.ui.components.NeonButton
import com.flexy.app.ui.components.SectionTitle
import com.flexy.app.ui.components.StatusPill
import com.flexy.app.ui.components.rememberToast

@Composable
fun AboutScreen(onOpenPrivacy: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FlexyLogo(84.dp)
                Spacer(Modifier.height(14.dp))
                Text("FLEXY", style = MaterialTheme.typography.headlineLarge, color = cs.onSurface)
                Text("Version ${appVersion(context)}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A gaming utility: game booster dashboard, game launcher, performance monitor and voice changer.",
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("What FLEXY really does")
                GlassCard(Modifier.fillMaxWidth()) {
                    Bullet("Shows real RAM, storage, battery, heat and network readings from Android.")
                    Bullet("Finds your installed games and launches them with Android's normal launch mechanism.")
                    Bullet("Tracks a gaming session timer and per-game launch stats on your phone.")
                    Bullet("Opens the right Android settings for brightness, Battery Saver, Game Mode and sound.")
                    Bullet("Controls Do Not Disturb, once you allow Do Not Disturb access.")
                    Bullet("Records your voice and applies effects to the recording.")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("What FLEXY can't do (Android limitations)")
                GlassCard(Modifier.fillMaxWidth()) {
                    Bullet("It does not increase CPU/GPU speed, unlock FPS, or edit other apps' memory.")
                    Bullet("It does not force-close other apps. That needs root and Android discourages it.")
                    Bullet("It can't switch brightness, Battery Saver or another game's Game Mode itself.")
                    Bullet("It can't change your voice inside other games' voice chat.")
                    Bullet("Exact play time in other apps needs the special Usage Access permission, which FLEXY doesn't ask for.")
                }
            }
        }
        item {
            NeonButton("Privacy information", onOpenPrivacy, Modifier.fillMaxWidth(), filled = false)
        }
    }
}

@Composable
fun PrivacyScreen() {
    val context = LocalContext.current
    val app = context.flexy
    val cs = MaterialTheme.colorScheme
    val toast = rememberToast()
    val plan by app.plans.plan.collectAsState()

    fun mic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    var micOn by remember { mutableStateOf(mic()) }
    var notifOn by remember { mutableStateOf(SessionNotifier.canPost(context)) }
    var dndOn by remember { mutableStateOf(SystemShortcuts.hasDndAccess(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        micOn = mic()
        notifOn = SessionNotifier.canPost(context)
        dndOn = SystemShortcuts.hasDndAccess(context)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = true) {
                Text("Your privacy comes first", style = MaterialTheme.typography.titleLarge, color = cs.primary)
                Spacer(Modifier.height(6.dp))
                Bullet("Voice recordings are stored on your phone, in FLEXY's private folder, by default.")
                Bullet("Nothing is uploaded unless you tap Share or \"To Music folder\" yourself.")
                Bullet("The microphone is never used in the background or without a red recording indicator.")
                Bullet("FLEXY has no account, no analytics, and doesn't collect your name, contacts or location.")
                Bullet("Game detection happens on your phone. Your game list never leaves it.")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Advertising")
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (plan == Plan.PLUS) "FLEXY PLUS: ad-free" else "FLEXY FREE: one ad per app open",
                            style = MaterialTheme.typography.titleMedium, color = cs.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(if (plan == Plan.PLUS) "PLUS" else "FREE", plan == Plan.PLUS)
                    }
                    Spacer(Modifier.height(6.dp))
                    Bullet("PLUS never requests or shows ads, and FLEXY doesn't contact Google's ad services for PLUS users.")
                    Bullet("FREE uses Google AdMob to show one full-screen ad when you open the app. Not when you switch screens.")
                    Bullet("Where the law requires it (for example the EEA and UK), Google's consent form asks for your choice first. You can change it later in Settings > Ad privacy options.")
                    Bullet("AdMob may use your device's advertising ID. You can reset or delete it in Android settings.")
                    Bullet("When you're offline FLEXY doesn't try to load ads, and every feature still works.")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Permissions, and why")
                GlassCard(Modifier.fillMaxWidth()) {
                    PermissionRow("Microphone", "Only to record your voice after you tap record.", micOn)
                    PermissionRow("Notifications", "Only for the optional session timer notification (Android 13+).", notifOn)
                    PermissionRow("Do Not Disturb access", "Only to turn Do Not Disturb on or off when you tap it.", dndOn)
                    InfoRow("Internet", "Ads (FREE), consent form, PLUS purchase")
                    InfoRow("Network state", "Detect when you're offline")
                    Spacer(Modifier.height(8.dp))
                    NeonButton(
                        "Open Android app settings",
                        { if (!SystemShortcuts.openAppSettings(context)) toast("Couldn't open settings") },
                        Modifier.fillMaxWidth(), filled = false
                    )
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("•", color = cs.primary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PermissionRow(name: String, why: String, granted: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Text(why, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        StatusPill(if (granted) "Allowed" else "Not allowed", granted)
    }
}
