package com.flexy.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flexy.app.flexy
import com.flexy.app.system.formatBytes
import com.flexy.app.system.formatDuration
import com.flexy.app.system.formatStorage
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.InfoRow
import com.flexy.app.ui.components.NeonBar
import com.flexy.app.ui.components.SectionTitle
import com.flexy.app.ui.components.rememberDeviceSnapshot
import com.flexy.app.ui.components.rememberSessionElapsed

/** Everything here is a real reading. If Android won't share it, it says "Not available". */
@Composable
fun PerformanceScreen() {
    val app = LocalContext.current.flexy
    val cs = MaterialTheme.colorScheme
    val snap by rememberDeviceSnapshot(1500L)
    val session by app.sessions.session.collectAsState()
    val elapsed = rememberSessionElapsed(session)
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(snap) {
        history.add(snap.ramUsedFraction)
        if (history.size > 40) history.removeAt(0)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = true) {
                Text("RAM in use (live)", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(
                    "${formatBytes(snap.ramUsed)} / ${formatBytes(snap.ramTotal)}",
                    style = MaterialTheme.typography.headlineMedium, color = cs.primary
                )
                Spacer(Modifier.height(10.dp))
                NeonBar(snap.ramUsedFraction)
                Spacer(Modifier.height(14.dp))
                MiniChart(history, Modifier.fillMaxWidth().height(90.dp))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Memory and storage")
                GlassCard(Modifier.fillMaxWidth()) {
                    InfoRow("RAM total", formatBytes(snap.ramTotal))
                    InfoRow("RAM available", formatBytes(snap.ramAvail))
                    InfoRow("Android low-memory flag", if (snap.lowMemory) "Yes" else "No")
                    InfoRow("Storage total", formatStorage(snap.storageTotal))
                    InfoRow("Storage free", formatStorage(snap.storageFree))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Battery and heat")
                GlassCard(Modifier.fillMaxWidth()) {
                    InfoRow("Battery level", snap.batteryPercent?.let { "$it%" })
                    InfoRow("Battery temperature", snap.batteryTempC?.let { "%.1f °C".format(it) })
                    InfoRow("Charging", if (snap.charging) "Yes" else "No")
                    InfoRow("Battery Saver", if (snap.powerSave) "On" else "Off")
                    InfoRow("Thermal status", snap.thermalLabel)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("CPU and device")
                GlassCard(Modifier.fillMaxWidth()) {
                    InfoRow("Device", snap.model)
                    InfoRow("Android", snap.androidVersion)
                    InfoRow("Chipset", snap.soc.ifBlank { null })
                    InfoRow("CPU cores", snap.cpuCores.toString())
                    InfoRow("Architecture", snap.abi)
                    InfoRow("Current CPU speed", snap.cpuFreqMhz?.let { "$it MHz" })
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Network")
                GlassCard(Modifier.fillMaxWidth()) {
                    InfoRow("Connection", snap.netType)
                    InfoRow("Estimated download link", snap.netDownMbps?.let { "~$it Mbps" })
                    InfoRow("Estimated upload link", snap.netUpMbps?.let { "~$it Mbps" })
                    InfoRow("Metered connection", snap.netMetered?.let { if (it) "Yes" else "No" })
                    InfoRow("Ping / latency", null)
                    Text(
                        "Link speeds are Android's estimates, not a speed test. Ping isn't measured, because FLEXY doesn't make test connections.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Gaming session")
                GlassCard(Modifier.fillMaxWidth()) {
                    InfoRow("Status", if (session != null) "Running" else "Not started")
                    InfoRow("Duration", formatDuration(elapsed))
                    InfoRow("Game", session?.gameName)
                }
            }
        }
    }
}

@Composable
private fun MiniChart(values: List<Float>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val line = cs.primary
    val grid = cs.outline.copy(alpha = 0.35f)
    Canvas(modifier) {
        for (i in 0..2) {
            val y = size.height * i / 2f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height * (1f - v.coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}
