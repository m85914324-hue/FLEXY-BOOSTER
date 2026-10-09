package com.flexy.app.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Shortcuts to real Android settings screens.
 * Android limitation: apps cannot flip most of these switches themselves
 * (brightness, Battery Saver, Game Mode need system-level permissions).
 * So FLEXY opens the right settings page for you. The exception is
 * Do Not Disturb, which Android lets you control after you grant access.
 */
object SystemShortcuts {

    private fun Context.safeStart(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    fun openDisplaySettings(c: Context) = c.safeStart(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    fun openBatterySaverSettings(c: Context) = c.safeStart(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
    fun openWifiSettings(c: Context) = c.safeStart(Intent(Settings.ACTION_WIFI_SETTINGS))
    fun openSoundSettings(c: Context) = c.safeStart(Intent(Settings.ACTION_SOUND_SETTINGS))

    fun openAppSettings(c: Context) = c.safeStart(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", c.packageName, null))
    )

    fun openNotificationSettings(c: Context) = c.safeStart(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
    )

    /** Tries known manufacturer gaming hubs, then falls back to Display settings. */
    fun openGameTools(c: Context): String {
        val hubs = listOf(
            "com.samsung.android.game.gamehome" to "Samsung Game Launcher",
            "com.oplus.games" to "Game Space"
        )
        for ((pkg, label) in hubs) {
            val intent = c.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null && c.safeStart(intent)) return "Opened $label"
        }
        return if (openDisplaySettings(c)) "No gaming hub found, opened Display settings"
        else "Couldn't open settings on this device"
    }

    // ---- Do Not Disturb (needs "Do Not Disturb access" granted by the user) ----

    private fun nm(c: Context) = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasDndAccess(c: Context): Boolean = nm(c).isNotificationPolicyAccessGranted

    fun isDndActive(c: Context): Boolean {
        val f = nm(c).currentInterruptionFilter
        return f != NotificationManager.INTERRUPTION_FILTER_ALL &&
            f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    fun openDndAccessSettings(c: Context) =
        c.safeStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))

    fun setDnd(c: Context, on: Boolean): Boolean {
        val manager = nm(c)
        if (!manager.isNotificationPolicyAccessGranted) return false
        manager.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return true
    }
}
