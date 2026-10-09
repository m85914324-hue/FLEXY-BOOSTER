package com.flexy.app.data

import android.content.Context
import com.flexy.app.system.SessionNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameSession(
    val startMs: Long,
    val segmentStartMs: Long,
    val packageName: String?,
    val gameName: String?
)

/**
 * The gaming-session timer.
 * Android limitation: FLEXY cannot see when another app closes unless you grant
 * "Usage access". So play time = time between launching a game from FLEXY and
 * ending the session (or launching another game).
 */
class SessionManager(
    private val context: Context,
    private val games: GameRepository,
    private val settings: SettingsStore
) {
    private val prefs = context.getSharedPreferences("flexy_session", Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(load())
    val session: StateFlow<GameSession?> = _session.asStateFlow()

    fun start(packageName: String? = null, gameName: String? = null) {
        val now = System.currentTimeMillis()
        val cur = _session.value
        val next = when {
            cur == null -> GameSession(now, now, packageName, gameName)
            packageName == null || packageName == cur.packageName -> cur
            else -> {
                cur.packageName?.let { games.addPlayTime(it, now - cur.segmentStartMs) }
                cur.copy(segmentStartMs = now, packageName = packageName, gameName = gameName)
            }
        }
        if (next != cur) {
            _session.value = next
            save(next)
            refreshNotification()
        }
    }

    fun end() {
        val cur = _session.value ?: return
        cur.packageName?.let { games.addPlayTime(it, System.currentTimeMillis() - cur.segmentStartMs) }
        _session.value = null
        save(null)
        SessionNotifier.cancel(context)
    }

    fun refreshNotification() {
        val s = _session.value
        if (s != null && settings.state.value.sessionNotification) SessionNotifier.show(context, s)
        else SessionNotifier.cancel(context)
    }

    private fun save(s: GameSession?) {
        prefs.edit().apply {
            if (s == null) clear() else {
                putLong("start", s.startMs)
                putLong("segment", s.segmentStartMs)
                putString("pkg", s.packageName)
                putString("name", s.gameName)
            }
        }.apply()
    }

    private fun load(): GameSession? {
        val start = prefs.getLong("start", 0L)
        if (start == 0L) return null
        return GameSession(start, prefs.getLong("segment", start), prefs.getString("pkg", null), prefs.getString("name", null))
    }
}
