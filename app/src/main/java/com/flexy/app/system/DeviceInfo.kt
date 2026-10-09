package com.flexy.app.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import java.io.File

/** One reading of the device. Every value is real; null means "Android won't tell us". */
data class DeviceSnapshot(
    val ramTotal: Long,
    val ramAvail: Long,
    val lowMemory: Boolean,
    val storageTotal: Long,
    val storageFree: Long,
    val batteryPercent: Int?,
    val batteryTempC: Float?,
    val charging: Boolean,
    val powerSave: Boolean,
    val thermalStatus: Int?,
    val netType: String,
    val netConnected: Boolean,
    val netDownMbps: Int?,
    val netUpMbps: Int?,
    val netMetered: Boolean?,
    val cpuCores: Int,
    val abi: String,
    val soc: String,
    val cpuFreqMhz: Int?,
    val model: String,
    val androidVersion: String
) {
    val ramUsed: Long get() = ramTotal - ramAvail
    val ramUsedFraction: Float get() = if (ramTotal > 0) ramUsed.toFloat() / ramTotal else 0f
    val storageUsedFraction: Float
        get() = if (storageTotal > 0) (storageTotal - storageFree).toFloat() / storageTotal else 0f
    val thermalLabel: String?
        get() = when (thermalStatus) {
            null -> null
            0 -> "Normal"
            1 -> "Light"
            2 -> "Moderate"
            3 -> "Severe"
            4 -> "Critical"
            5 -> "Emergency"
            6 -> "Shutdown"
            else -> null
        }
}

object DeviceInfo {
    fun snapshot(context: Context): DeviceSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        val stat = StatFs(Environment.getDataDirectory().path)

        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else null
        val tempRaw = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val tempC = if (tempRaw != Int.MIN_VALUE) tempRaw / 10f else null
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermal = if (Build.VERSION.SDK_INT >= 29) pm.currentThermalStatus else null

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val netType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Connected"
        }
        val down = caps?.linkDownstreamBandwidthKbps?.takeIf { it > 0 }?.div(1000)
        val up = caps?.linkUpstreamBandwidthKbps?.takeIf { it > 0 }?.div(1000)
        val metered = if (caps != null) cm.isActiveNetworkMetered else null

        val socName = if (Build.VERSION.SDK_INT >= 31) {
            listOf(Build.SOC_MANUFACTURER, Build.SOC_MODEL)
                .filter { it.isNotBlank() && it != Build.UNKNOWN }
                .joinToString(" ")
        } else ""

        return DeviceSnapshot(
            ramTotal = mem.totalMem,
            ramAvail = mem.availMem,
            lowMemory = mem.lowMemory,
            storageTotal = stat.totalBytes,
            storageFree = stat.availableBytes,
            batteryPercent = percent,
            batteryTempC = tempC,
            charging = charging,
            powerSave = pm.isPowerSaveMode,
            thermalStatus = thermal,
            netType = netType,
            netConnected = caps != null,
            netDownMbps = down,
            netUpMbps = up,
            netMetered = metered,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            soc = socName.ifBlank { Build.HARDWARE },
            cpuFreqMhz = readCpuFreqMhz(),
            model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }

    /** Android limitation: most phones block this file. Then we show "Not available". */
    private fun readCpuFreqMhz(): Int? = try {
        val f = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
        if (f.canRead()) f.readText().trim().toLongOrNull()?.let { (it / 1000).toInt() } else null
    } catch (e: Exception) {
        null
    }
}
