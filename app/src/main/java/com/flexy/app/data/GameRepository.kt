package com.flexy.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class GameItem(val packageName: String, val name: String, val icon: ImageBitmap?)

data class GameStats(
    val launchCount: Int = 0,
    val totalMillis: Long = 0L,
    val lastPlayedMs: Long = 0L
)

/**
 * Finds games using only normal Android APIs:
 *  - apps that declare the "Game" category (ApplicationInfo.CATEGORY_GAME)
 *  - plus any app the user adds by hand
 */
class GameRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("flexy_games", Context.MODE_PRIVATE)

    private val _games = MutableStateFlow<List<GameItem>>(emptyList())
    val games: StateFlow<List<GameItem>> = _games.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _stats = MutableStateFlow(loadStats())
    val stats: StateFlow<Map<String, GameStats>> = _stats.asStateFlow()

    suspend fun refresh() {
        _loading.value = true
        _games.value = withContext(Dispatchers.IO) { scan() }
        _loading.value = false
    }

    /** Every launchable app that is not already in the library (for the "Add game" list). */
    suspend fun candidates(): List<GameItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val already = _games.value.map { it.packageName }.toSet()
        pm.queryIntentActivities(launcherIntent(), 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .filter { it.activityInfo.packageName !in already }
            .map { toItem(it) }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun add(pkg: String) {
        editSet(KEY_MANUAL) { it.add(pkg) }
        editSet(KEY_HIDDEN) { it.remove(pkg) }
        refresh()
    }

    suspend fun remove(pkg: String) {
        editSet(KEY_MANUAL) { it.remove(pkg) }
        editSet(KEY_HIDDEN) { it.add(pkg) }
        refresh()
    }

    fun launch(pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun recordLaunch(pkg: String) = updateStats(pkg) {
        it.copy(launchCount = it.launchCount + 1, lastPlayedMs = System.currentTimeMillis())
    }

    fun addPlayTime(pkg: String, ms: Long) {
        if (ms <= 0) return
        updateStats(pkg) {
            it.copy(totalMillis = it.totalMillis + ms, lastPlayedMs = System.currentTimeMillis())
        }
    }

    // ---- internals ----

    private fun launcherIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    private fun scan(): List<GameItem> {
        val pm = context.packageManager
        val manual = stringSet(KEY_MANUAL)
        val hidden = stringSet(KEY_HIDDEN)
        return pm.queryIntentActivities(launcherIntent(), 0)
            .mapNotNull { info ->
                val app = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                val pkg = app.packageName
                if (pkg == context.packageName) return@mapNotNull null
                val detected = isGame(app) && pkg !in hidden
                if (!detected && pkg !in manual) return@mapNotNull null
                toItem(info)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }

    @Suppress("DEPRECATION")
    private fun isGame(app: ApplicationInfo): Boolean =
        app.category == ApplicationInfo.CATEGORY_GAME ||
            (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    private fun toItem(info: ResolveInfo): GameItem {
        val pm = context.packageManager
        val icon = runCatching { info.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        return GameItem(info.activityInfo.packageName, info.loadLabel(pm).toString(), icon)
    }

    private fun stringSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()) ?: emptySet()

    private fun editSet(key: String, block: (MutableSet<String>) -> Unit) {
        val set = stringSet(key).toMutableSet()
        block(set)
        prefs.edit().putStringSet(key, set).apply()
    }

    private fun updateStats(pkg: String, change: (GameStats) -> GameStats) {
        _stats.update { map -> map + (pkg to change(map[pkg] ?: GameStats())) }
        val s = _stats.value[pkg] ?: return
        prefs.edit().putString("stats_$pkg", "${s.launchCount},${s.totalMillis},${s.lastPlayedMs}").apply()
    }

    private fun loadStats(): Map<String, GameStats> =
        prefs.all.filterKeys { it.startsWith("stats_") }.mapNotNull { (key, value) ->
            val parts = (value as? String)?.split(",") ?: return@mapNotNull null
            if (parts.size != 3) return@mapNotNull null
            key.removePrefix("stats_") to GameStats(
                parts[0].toIntOrNull() ?: 0,
                parts[1].toLongOrNull() ?: 0L,
                parts[2].toLongOrNull() ?: 0L
            )
        }.toMap()

    private companion object {
        const val KEY_MANUAL = "manual_games"
        const val KEY_HIDDEN = "hidden_games"
    }
}
