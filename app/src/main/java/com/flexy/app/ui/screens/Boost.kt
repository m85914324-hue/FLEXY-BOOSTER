package com.flexy.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flexy.app.flexy
import com.flexy.app.navigation.Route
import com.flexy.app.system.BoostAdvisor
import com.flexy.app.system.DeviceInfo
import com.flexy.app.system.DeviceSnapshot
import com.flexy.app.system.Recommendation
import com.flexy.app.system.SystemShortcuts
import com.flexy.app.system.formatBytes
import com.flexy.app.system.formatStorage
import com.flexy.app.ui.components.GlassCard
import com.flexy.app.ui.components.NeonButton
import com.flexy.app.ui.components.flexyClickable
import com.flexy.app.ui.components.rememberToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The big glowing BOOST NOW button. */
@Composable
fun BoostButton(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val accent = cs.primary
    val purple = cs.tertiary
    val transition = rememberInfiniteTransition(label = "boost")
    val pulse by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "pulse"
    )
    val rot by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "rot"
    )
    Box(Modifier.size(240.dp).flexyClickable(onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(240.dp)) {
            val c = center
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.32f), Color.Transparent), c, r), radius = r)
            for (k in 0..1) {
                val p = (pulse + k * 0.5f) % 1f
                drawCircle(
                    accent.copy(alpha = (1f - p) * 0.4f),
                    radius = r * (0.66f + 0.34f * p),
                    style = Stroke(2.dp.toPx())
                )
            }
            val ringR = r * 0.68f
            rotate(rot, c) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(accent, purple, Color.Transparent, accent), c),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(c.x - ringR, c.y - ringR),
                    size = Size(ringR * 2, ringR * 2),
                    style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Box(
            Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent, purple))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Bolt, null, tint = cs.onPrimary, modifier = Modifier.size(46.dp))
                Text("BOOST NOW", color = cs.onPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

/**
 * Boost = start a session, read real device status, give advice.
 * It does NOT kill apps, raise CPU/GPU speed, or free RAM.
 */
@Composable
fun BoostDialog(onDismiss: () -> Unit, onNavigate: (Route) -> Unit) {
    val context = LocalContext.current
    val app = context.flexy
    val cs = MaterialTheme.colorScheme
    val toast = rememberToast()
    val steps = remember {
        listOf(
            "Starting your gaming session",
            "Reading memory and storage",
            "Checking battery and temperature",
            "Checking your network",
            "Preparing recommendations"
        )
    }
    var done by remember { mutableIntStateOf(0) }
    var snap by remember { mutableStateOf<DeviceSnapshot?>(null) }
    var recs by remember { mutableStateOf<List<Recommendation>?>(null) }

    LaunchedEffect(Unit) {
        app.sessions.start()
        for (i in 1..steps.size) {
            delay(600)
            done = i
        }
        val s = withContext(Dispatchers.IO) { DeviceInfo.snapshot(context) }
        snap = s
        recs = BoostAdvisor.build(s, SystemShortcuts.isDndActive(context))
    }

    val finished = recs != null
    val sweep by animateFloatAsState(done / steps.size.toFloat() * 360f, tween(500), label = "sweep")
    val spin = rememberInfiniteTransition(label = "spin")
    val spinAngle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "spinAngle"
    )

    Dialog(
        onDismissRequest = { if (finished) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = finished, dismissOnClickOutside = false)
    ) {
        GlassCard(Modifier.fillMaxWidth(), glow = true) {
            Column(
                Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(120.dp)) {
                        val stroke = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
                        val inset = 8.dp.toPx()
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                        drawArc(
                            cs.outline.copy(alpha = 0.4f), 0f, 360f, false,
                            topLeft = Offset(inset, inset), size = arcSize, style = stroke
                        )
                        rotate(if (finished) 0f else spinAngle, center) {
                            drawArc(
                                Brush.sweepGradient(listOf(cs.primary, cs.tertiary, cs.primary), center),
                                -90f, sweep.coerceAtLeast(12f), false,
                                topLeft = Offset(inset, inset), size = arcSize, style = stroke
                            )
                        }
                    }
                    Icon(
                        if (finished) Icons.Rounded.Check else Icons.Rounded.Bolt, null,
                        tint = cs.primary, modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (finished) "Ready to play" else "Getting ready...",
                    style = MaterialTheme.typography.headlineMedium, color = cs.onSurface
                )
                Spacer(Modifier.height(14.dp))

                steps.forEachIndexed { i, label ->
                    Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                            when {
                                i < done -> Icon(Icons.Rounded.Check, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                                i == done -> CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = cs.primary
                                )
                                else -> Box(Modifier.size(8.dp).clip(CircleShape).background(cs.outline))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            label, style = MaterialTheme.typography.bodyMedium,
                            color = if (i <= done) cs.onSurface else cs.onSurfaceVariant
                        )
                    }
                }

                val s = snap
                val r = recs
                if (s != null && r != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Free RAM", formatBytes(s.ramAvail))
                        MiniStat("Free storage", formatStorage(s.storageFree))
                        MiniStat("Battery", s.batteryPercent?.let { "$it%" } ?: "N/A")
                    }
                    Spacer(Modifier.height(14.dp))
                    r.forEach { rec ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Icon(
                                if (rec.good) Icons.Rounded.Check else Icons.Rounded.Info, null,
                                tint = if (rec.good) cs.primary else cs.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(rec.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                                Text(rec.detail, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "FLEXY started your session and checked your phone. It did not close other apps or change CPU/GPU speed. Android doesn't allow that without root.",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    NeonButton("Open my games", { onNavigate(Route.GAMES) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.SportsEsports)
                    Spacer(Modifier.height(10.dp))
                    NeonButton(
                        "Game Mode settings",
                        { toast(SystemShortcuts.openGameTools(context)) },
                        Modifier.fillMaxWidth(), icon = Icons.Rounded.Tune, filled = false
                    )
                    TextButton(onClick = onDismiss) { Text("Close", color = cs.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = cs.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}
