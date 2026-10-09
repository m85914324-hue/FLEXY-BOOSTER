package com.flexy.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flexy.app.data.GameItem
import com.flexy.app.data.GameStats
import com.flexy.app.flexy
import com.flexy.app.system.formatPlaytime
import com.flexy.app.system.relativeTime
import com.flexy.app.ui.components.EmptyState
import com.flexy.app.ui.components.FlexyIconButton
import com.flexy.app.ui.components.GameIcon
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.LocalSnackbar
import com.flexy.app.ui.components.NeonButton
import com.flexy.app.ui.components.flexyClickable
import com.flexy.app.ui.components.rememberToast
import kotlinx.coroutines.launch

@Composable
fun GamesScreen() {
    val context = LocalContext.current
    val app = context.flexy
    val cs = MaterialTheme.colorScheme
    val games by app.games.games.collectAsState()
    val loading by app.games.loading.collectAsState()
    val stats by app.games.stats.collectAsState()
    val session by app.sessions.session.collectAsState()
    val scope = rememberCoroutineScope()
    val toast = rememberToast()
    val snackbar = LocalSnackbar.current
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Game library", style = MaterialTheme.typography.headlineMedium, color = cs.onSurface)
                    Text(
                        if (games.size == 1) "1 game" else "${games.size} games",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant
                    )
                }
                FlexyIconButton(Icons.Rounded.Refresh, "Scan again", { scope.launch { app.games.refresh() } })
                FlexyIconButton(Icons.Rounded.Add, "Add a game", { showAdd = true }, tint = cs.primary)
            }
        }

        when {
            loading && games.isEmpty() -> items(3) { SkeletonCard() }
            games.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Rounded.SportsEsports,
                    title = "No games found yet",
                    message = "FLEXY finds games that Android marks as games. If yours isn't listed, add it by hand.",
                    actionLabel = "Add a game",
                    onAction = { showAdd = true }
                )
            }
            else -> items(games, key = { it.packageName }) { game ->
                GameCard(
                    game = game,
                    stats = stats[game.packageName] ?: GameStats(),
                    active = session?.packageName == game.packageName,
                    onPlay = { launchGameWithToast(context, game, toast) },
                    onRemove = {
                        scope.launch {
                            app.games.remove(game.packageName)
                            val result = snackbar.showSnackbar("${game.name} removed", actionLabel = "Undo")
                            if (result == SnackbarResult.ActionPerformed) app.games.add(game.packageName)
                        }
                    }
                )
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row {
                    Icon(Icons.Rounded.Info, null, tint = cs.tertiary, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Android limitation: there's no perfect \"is this a game?\" flag, so FLEXY lists apps that declare the Game category. " +
                            "Play time is the time between pressing Play and ending the session, because Android only shares exact app usage if you grant a special \"Usage access\" permission.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddGameDialog(
            onDismiss = { showAdd = false },
            onPick = { picked ->
                showAdd = false
                scope.launch {
                    app.games.add(picked.packageName)
                    toast("${picked.name} added")
                }
            }
        )
    }
}

@Composable
private fun GameCard(
    game: GameItem,
    stats: GameStats,
    active: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth(), glow = active) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameIcon(game.icon, game.name, 60.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    game.name, style = MaterialTheme.typography.titleMedium, color = cs.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Last played: ${relativeTime(stats.lastPlayedMs)}",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                )
                Text(
                    "${stats.launchCount} launches, ${formatPlaytime(stats.totalMillis)} tracked",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                )
            }
            Box {
                FlexyIconButton(Icons.Rounded.MoreVert, "More options", { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove from library") },
                        onClick = {
                            menu = false
                            onRemove()
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        NeonButton(if (active) "Resume playing" else "Play", onPlay, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
    }
}

@Composable
private fun SkeletonCard() {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "skeleton")
    val a by t.animateFloat(
        0.25f, 0.6f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "alpha"
    )
    GlassCard(Modifier.fillMaxWidth().alpha(a)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(cs.outline))
            Spacer(Modifier.width(12.dp))
            Column {
                Box(Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(cs.outline))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(cs.outline))
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(cs.outline))
    }
}

@Composable
private fun AddGameDialog(onDismiss: () -> Unit, onPick: (GameItem) -> Unit) {
    val app = LocalContext.current.flexy
    val cs = MaterialTheme.colorScheme
    val candidates by produceState<List<GameItem>?>(null) { value = app.games.candidates() }
    var query by remember { mutableStateOf("") }
    val filtered = candidates?.filter { it.name.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceVariant,
        title = { Text("Add a game") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Search your apps") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                when {
                    filtered == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary)
                    }
                    filtered.isEmpty() -> Text(
                        "No matching apps.", color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                    else -> LazyColumn(Modifier.heightIn(max = 340.dp)) {
                        items(filtered, key = { it.packageName }) { candidate ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .flexyClickable { onPick(candidate) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GameIcon(candidate.icon, candidate.name, 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(candidate.name, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
