package com.flexy.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.flexy.app.flexy
import com.flexy.app.navigation.Route
import com.flexy.app.system.BoostAdvisor
import com.flexy.app.system.SystemShortcuts
import com.flexy.app.system.formatBytes
import com.flexy.app.system.formatDuration
import com.flexy.app.ui.components.GameIcon
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.NeonButton
import com.flexy.app.ui.components.QuickTile
import com.flexy.app.ui.components.SectionTitle
import com.flexy.app.ui.components.StatTile
import com.flexy.app.ui.components.flexyClickable
import com.flexy.app.ui.components.rememberDeviceSnapshot
import com.flexy.app.ui.components.rememberSessionElapsed
import com.flexy.app.ui.components.rememberToast

@Composable
fun BoosterScreen(onNavigate: (Route) -> Unit) {
    val context = LocalContext.current
    val app = context.flexy
    val cs = MaterialTheme.colorScheme
    val snap by rememberDeviceSnapshot()
    val session by app.sessions.session.collectAsState()
    val elapsed = rememberSessionElapsed(session)
    val games by app.games.games.collectAsState()
    val toast = rememberToast()
    val active = session

    var dndOn by remember { mutableStateOf(SystemShortcuts.isDndActive(context)) }
    var showDndDialog by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { dndOn = SystemShortcuts.isDndActive(context) }

    val recs = remember(snap, dndOn) { BoostAdvisor.build(snap, dndOn) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- session ----
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = active != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Gaming session", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Text(
                            formatDuration(elapsed), fontSize = 34.sp, fontFamily = FontFamily.Monospace,
                            color = if (active != null) cs.primary else cs.onSurfaceVariant
                        )
                        Text(
                            active?.gameName?.let { "Playing $it" } ?: if (active != null) "Session running" else "Not started",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (active == null) {
                    NeonButton("Start session", { app.sessions.start() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Timer)
                } else {
                    NeonButton("End session", { app.sessions.end() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Stop, filled = false)
                }
            }
        }

        // ---- live performance dashboard ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Performance dashboard")
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        Icons.Rounded.Memory, "RAM", "${formatBytes(snap.ramAvail)} free",
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = "of ${formatBytes(snap.ramTotal)}", progress = snap.ramUsedFraction
                    )
                    StatTile(
                        Icons.Rounded.BatteryFull, "Battery", snap.batteryPercent?.let { "$it%" } ?: "Not available",
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = buildString {
                            append(if (snap.charging) "Charging" else "On battery")
                            snap.batteryTempC?.let { append(" • ${"%.1f".format(it)}°C") }
                        },
                        progress = (snap.batteryPercent ?: 0) / 100f
                    )
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        Icons.Rounded.Wifi, "Network", snap.netType,
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = snap.netDownMbps?.let { "~$it Mbps link (estimate)" } ?: "Speed not available"
                    )
                    StatTile(
                        Icons.Rounded.Whatshot, "Heat", snap.thermalLabel ?: "Not available",
                        Modifier.weight(1f).fillMaxHeight(),
                        sub = "Android thermal status"
                    )
                }
            }
        }

        // ---- quick settings ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Quick settings for gaming")
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(
                        Icons.Rounded.DoNotDisturbOn, "Do Not Disturb",
                        when {
                            !SystemShortcuts.hasDndAccess(context) -> "Tap to allow access"
                            dndOn -> "On, tap to turn off"
                            else -> "Off, tap to turn on"
                        },
                        onClick = {
                            if (!SystemShortcuts.hasDndAccess(context)) showDndDialog = true
                            else {
                                SystemShortcuts.setDnd(context, !dndOn)
                                dndOn = SystemShortcuts.isDndActive(context)
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(), active = dndOn
                    )
                    QuickTile(
                        Icons.Rounded.Brightness6, "Brightness", "Opens Display settings",
                        onClick = { if (!SystemShortcuts.openDisplaySettings(context)) toast("Couldn't open settings") },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(
                        Icons.Rounded.BatteryFull, "Battery Saver",
                        if (snap.powerSave) "On, tap to review" else "Off, opens settings",
                        onClick = { if (!SystemShortcuts.openBatterySaverSettings(context)) toast("Couldn't open settings") },
                        modifier = Modifier.weight(1f).fillMaxHeight(), active = snap.powerSave
                    )
                    QuickTile(
                        Icons.Rounded.SportsEsports, "Game Mode", "Opens gaming tools",
                        onClick = { toast(SystemShortcuts.openGameTools(context)) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(
                        Icons.Rounded.Wifi, "Network", "Opens Wi-Fi settings",
                        onClick = { if (!SystemShortcuts.openWifiSettings(context)) toast("Couldn't open settings") },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    QuickTile(
                        Icons.Rounded.VolumeUp, "Sound", "Opens Sound settings",
                        onClick = { if (!SystemShortcuts.openSoundSettings(context)) toast("Couldn't open settings") },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Row {
                        Icon(Icons.Rounded.Info, null, tint = cs.tertiary, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Android limitation: apps can't change brightness, Battery Saver or another game's Game Mode directly. FLEXY opens the right settings page for you. Do Not Disturb works in-app once you allow access.",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---- game launcher row ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Launch a game")
                if (games.isEmpty()) {
                    GlassCard(Modifier.fillMaxWidth(), onClick = { onNavigate(Route.GAMES) }) {
                        Text("No games in your library yet", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Text("Tap to scan or add games.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(games, key = { it.packageName }) { game ->
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

        // ---- recommendations from real readings ----
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Gaming recommendations", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Spacer(Modifier.height(8.dp))
                recs.forEach { rec ->
                    Column(Modifier.padding(vertical = 5.dp)) {
                        Text(rec.title, style = MaterialTheme.typography.bodyMedium, color = if (rec.good) cs.primary else cs.onSurface)
                        Text(rec.detail, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showDndDialog) {
        AlertDialog(
            onDismissRequest = { showDndDialog = false },
            containerColor = cs.surfaceVariant,
            title = { Text("Allow Do Not Disturb access") },
            text = {
                Text(
                    "Android needs you to give FLEXY \"Do Not Disturb access\" before it can silence notifications during a game. " +
                        "FLEXY only changes the Do Not Disturb switch. It can't read your notifications."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDndDialog = false
                    if (!SystemShortcuts.openDndAccessSettings(context)) toast("Couldn't open settings")
                }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = { showDndDialog = false }) { Text("Not now") } }
        )
    }
}
