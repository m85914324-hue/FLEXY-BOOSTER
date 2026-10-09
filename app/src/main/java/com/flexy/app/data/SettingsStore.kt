package com.flexy.app.data

import android.content.Context
import com.flexy.app.ui.theme.AccentColor
import com.flexy.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FlexySettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val accent: AccentColor = AccentColor.CYAN,
    val haptics: Boolean = true,
    val sounds: Boolean = true,
    val voiceVolume: Float = 0.9f,
    val voiceIntensity: Float = 0.6f,
    val sessionNotification: Boolean = false
)

/** Tiny settings store on top of SharedPreferences. Everything stays on the phone. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("flexy_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<FlexySettings> = _state.asStateFlow()

    fun update(change: (FlexySettings) -> FlexySettings) {
        val next = change(_state.value)
        _state.value = next
        prefs.edit()
            .putString("theme", next.theme.name)
            .putString("accent", next.accent.name)
            .putBoolean("haptics", next.haptics)
            .putBoolean("sounds", next.sounds)
            .putFloat("voiceVolume", next.voiceVolume)
            .putFloat("voiceIntensity", next.voiceIntensity)
            .putBoolean("sessionNotification", next.sessionNotification)
            .apply()
    }

    private fun load() = FlexySettings(
        theme = enumOr(prefs.getString("theme", null), ThemeMode.DARK),
        accent = enumOr(prefs.getString("accent", null), AccentColor.CYAN),
        haptics = prefs.getBoolean("haptics", true),
        sounds = prefs.getBoolean("sounds", true),
        voiceVolume = prefs.getFloat("voiceVolume", 0.9f),
        voiceIntensity = prefs.getFloat("voiceIntensity", 0.6f),
        sessionNotification = prefs.getBoolean("sessionNotification", false)
    )

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name ?: "") }.getOrDefault(default)
}
