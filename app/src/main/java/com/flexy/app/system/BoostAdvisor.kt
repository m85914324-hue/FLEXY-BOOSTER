package com.flexy.app.system

data class Recommendation(val title: String, val detail: String, val good: Boolean = false)

/** Turns REAL readings into plain-language advice. It never claims to change CPU/GPU speed. */
object BoostAdvisor {
    fun build(s: DeviceSnapshot, dndActive: Boolean): List<Recommendation> {
        val out = mutableListOf<Recommendation>()

        val pct = s.batteryPercent
        if (pct != null && pct <= 20 && !s.charging) {
            out += Recommendation("Battery is at $pct%", "Plug in a charger before a long session.")
        }
        if (s.powerSave) {
            out += Recommendation(
                "Battery Saver is on",
                "Android may limit performance. Turn it off in Battery Saver settings if your game stutters."
            )
        }
        val hot = (s.thermalStatus ?: 0) >= 2 || (s.batteryTempC ?: 0f) >= 40f
        if (hot) {
            out += Recommendation("Phone is warm", "Take a short break or remove the case. Hot phones slow themselves down.")
        }
        if (s.lowMemory || s.ramAvail.toFloat() / s.ramTotal.coerceAtLeast(1) < 0.15f) {
            out += Recommendation(
                "Free RAM is low (${formatBytes(s.ramAvail)})",
                "Close apps you don't need from the Recents screen. FLEXY doesn't force-close apps; Android manages memory itself."
            )
        }
        if (s.storageFree < 2_000_000_000L || s.storageFree.toFloat() / s.storageTotal.coerceAtLeast(1) < 0.10f) {
            out += Recommendation("Storage is almost full", "Only ${formatStorage(s.storageFree)} free. Free some space so games can update and save.")
        }
        when {
            !s.netConnected -> out += Recommendation("No internet connection", "Online games need a connection.")
            s.netType == "Mobile data" -> out += Recommendation("On mobile data", "Wi-Fi is usually more stable for online games.")
        }
        if (!dndActive) {
            out += Recommendation("Notifications can interrupt you", "Turn on Do Not Disturb from the Game Booster screen.")
        }
        if (out.isEmpty() || out.all { it.title.startsWith("Notifications") }) {
            out.add(0, Recommendation("Everything looks good", "No problems found. You're ready to play.", good = true))
        }
        return out
    }
}
