package com.flexy.app.system

import android.content.Context
import java.util.Date
import java.util.Locale

/** RAM style (1 GB = 1024 MB). */
fun formatBytes(bytes: Long): String {
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb)
    else String.format(Locale.US, "%d MB", bytes / 1024 / 1024)
}

/** Storage style (1 GB = 1000 MB), like Android's own Settings app. */
fun formatStorage(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb)
    else String.format(Locale.US, "%d MB", bytes / 1_000_000)
}

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60)
}

fun formatClip(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

fun formatPlaytime(ms: Long): String {
    val min = ms / 60_000
    return when {
        min < 1 -> "0 min"
        min < 60 -> "$min min"
        else -> "${min / 60} h ${min % 60} min"
    }
}

fun relativeTime(ms: Long): String {
    if (ms <= 0) return "Never"
    val diff = System.currentTimeMillis() - ms
    val min = diff / 60_000
    return when {
        diff < 60_000 -> "Just now"
        min < 60 -> "$min min ago"
        min < 60 * 24 -> "${min / 60} h ago"
        min < 60 * 24 * 7 -> "${min / (60 * 24)} d ago"
        else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(Date(ms))
    }
}

fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "1.0.0"
