package com.flexy.app.ads

import android.content.Context
import com.flexy.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Plan { FREE, PLUS }

/**
 * FREE or PLUS.
 * PLUS comes from a verified Google Play purchase (see BillingManager) and is cached,
 * so a PLUS user stays ad-free even when offline.
 * Debug builds also have a "Simulate PLUS" switch in Settings so you can test both plans.
 */
class PlanManager(context: Context) {
    private val prefs = context.getSharedPreferences("flexy_plan", Context.MODE_PRIVATE)

    private val _plan = MutableStateFlow(compute())
    val plan: StateFlow<Plan> = _plan.asStateFlow()

    /** True once Google Play has answered (or we gave up). Ads wait for this. */
    private val _resolved = MutableStateFlow(false)
    val resolved: StateFlow<Boolean> = _resolved.asStateFlow()

    val debugPlus: Boolean
        get() = BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG, false)

    fun setDebugPlus(on: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().putBoolean(KEY_DEBUG, on).apply()
        _plan.value = compute()
    }

    /** isPlus == null means "couldn't verify": keep whatever we had cached. */
    fun onBillingChecked(isPlus: Boolean?) {
        if (isPlus != null) prefs.edit().putBoolean(KEY_CACHED, isPlus).apply()
        _plan.value = compute()
        _resolved.value = true
    }

    private fun compute(): Plan =
        if (debugPlus || prefs.getBoolean(KEY_CACHED, false)) Plan.PLUS else Plan.FREE

    private companion object {
        const val KEY_CACHED = "plus_cached"
        const val KEY_DEBUG = "debug_plus"
    }
}
