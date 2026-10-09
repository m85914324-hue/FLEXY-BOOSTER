package com.flexy.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.flexy.app.flexy
import com.flexy.app.navigation.Route
import com.flexy.app.system.SystemShortcuts
import com.flexy.app.system.formatBytes
import com.flexy.app.system.formatDuration
import com.flexy.app.system.formatStorage
import com.flexy.app.ui.components.GameIcon
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.NeonButton
import com.flexy.app.ui.components.SectionTitle
import com.flexy.app.ui.components.StatTile
import com.flexy.app.ui.components.StatusPill
import com.flexy.app.ui.components.flexyClickable
import com.flexy.app.ui.components.rememberDeviceSnapshot
import com.flexy.app.ui.components.rememberSessionElapsed
import com.flexy.app.ui.components.rememberToast

@Composable
fun HomeScreen(onNavigate: (Route) -> Unit) {
    val context = LocalContext.current
    val app = context.flexy
    val cs = MaterialTheme.colorScheme
    val snap by rememberDeviceSnapshot()
    val session by app.sessions.session.collectAsState()
    val elapsed = rememberSessionElapsed(session)
    val games by app.games.games.collectAsState()
    val stats by app.games.stats.collectAsState()
    val toast = rememberToast()
    var showBoost by remember { mutableStateOf(false) }
    var dndOn by remember { mutableStateOf(SystemShortcuts.isDndActive(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { dndOn = SystemShortcuts.isDndActive(context) }

    val recent = remember(games, stats) {
        games.filter { (stats[it.packageName]?.lastPlayedMs ?: 0L) > 0L }
            .sortedByDescending { stats[it.packageName]?.lastPlayedMs ?: 0L }
            .take(8)
    }
    val active = session

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Game Booster", style = MaterialTheme.typography.headlineLarge, color = cs.onSurface)
                Text(
                    "Check your phone, start a session, and jump into a game.",
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BoostButton { showBoost = true }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = active != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Current gaming mode", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Text(
                            if (active != null) "Gaming session" else "Standard",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (active != null) cs.primary else cs.onSurface
                        )
                        if (active?.gameName != null) {
                            Text("Playing ${active.gameName}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Session timer", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Text(
                            formatDuration(elapsed), fontSize = 26.sp, fontFamily = FontFamily.Monospace,
                            color = if (active != null) cs.primary else cs.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("Do Not Disturb", dndOn)
                    StatusPill("Battery Saver", snap.powerSave)
                    snap.thermalLabel?.let { StatusPill("Heat: $it", (snap.thermalStatus ?: 0) >= 2) }
                }
                Spacer(Modifier.height(14.dp))
                if (active == null) {
                    NeonButton("Start session", { app.sessions.start() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Timer)
                } else {
                    NeonButton("End session", { app.sessions.end() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Stop, filled = false)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        Icons.Rounded.Memory, "RAM", formatBytes(snap.ramUsed),
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = "of ${formatBytes(snap.ramTotal)} in use",
                        progress = snap.ramUsedFraction
                    )
                    StatTile(
                        Icons.Rounded.BatteryFull, "Battery",
                        snap.batteryPercent?.let { "$it%" } ?: "Not available",
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = if (snap.charging) "Charging" else "On battery",
                        progress = (snap.batteryPercent ?: 0) / 100f
                    )
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        Icons.Rounded.Storage, "Storage", "${formatStorage(snap.storageFree)} free",
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = "of ${formatStorage(snap.storageTotal)}",
                        progress = snap.storageUsedFraction
                    )
                    StatTile(
                        Icons.Rounded.PhoneAndroid, "Device", snap.model,
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = "${snap.cpuCores} cores, ${snap.abi}"
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Recently launched")
                if (recent.isEmpty()) {
                    GlassCard(Modifier.fillMaxWidth(), onClick = { onNavigate(Route.GAMES) }) {
                        Text("No games launched yet", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Text(
                            "Games you start from FLEXY show up here. Tap to open your library.",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(recent, key = { it.packageName }) { game ->
                            Column(
                                Modifier.width(76.dp).flexyClickable { launchGameWithToast(context, game, toast) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                GameIcon(game.icon, game.name, 60.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    game.name, style = MaterialTheme.typography.bodySmall, color = cs.onSurface,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBoost) {
        BoostDialog(
            onDismiss = { showBoost = false },
            onNavigate = { showBoost = false; onNavigate(it) }
        )
    }
}
