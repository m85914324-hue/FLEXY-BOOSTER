package com.flexy.app.ui.components

import android.view.SoundEffectConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexy.app.ui.theme.LocalFeedback
import kotlinx.coroutines.launch

val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState not provided")
}

/** Returns a function that shows a short message at the bottom of the screen. */
@Composable
fun rememberToast(): (String) -> Unit {
    val host = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { host.showSnackbar(msg) } }
    return toast
}

/** Press animation + optional haptic/click sound (both controlled in Settings). */
fun Modifier.flexyClickable(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
            if (feedback.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (feedback.sounds) view.playSoundEffect(SoundEffectConstants.CLICK)
            onClick()
        }
}

@Composable
fun FlexyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxSize()
            .background(cs.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(cs.primary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.02f),
                        radius = size.width * 0.95f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(cs.tertiary.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.98f),
                        radius = size.width * 0.9f
                    )
                )
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val borderBrush = Brush.linearGradient(
        listOf(
            cs.primary.copy(alpha = if (glow) 0.85f else 0.35f),
            cs.outline.copy(alpha = 0.3f),
            cs.tertiary.copy(alpha = if (glow) 0.65f else 0.25f)
        )
    )
    val fill = Brush.verticalGradient(listOf(cs.surfaceVariant, cs.surfaceVariant.copy(alpha = 0.82f)))
    var m = modifier
    if (glow) m = m.shadow(14.dp, shape, ambientColor = cs.primary, spotColor = cs.primary)
    if (onClick != null) m = m.flexyClickable(onClick)
    Column(
        modifier = m
            .clip(shape)
            .background(fill)
            .border(1.dp, borderBrush, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    enabled: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val bg = if (filled) {
        Modifier.background(Brush.horizontalGradient(listOf(cs.primary, cs.tertiary)), shape)
    } else {
        Modifier
            .border(1.5.dp, cs.primary.copy(alpha = 0.7f), shape)
            .background(cs.primary.copy(alpha = 0.08f), shape)
    }
    val contentColor = if (filled) cs.onPrimary else cs.primary
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (enabled) Modifier.flexyClickable(onClick) else Modifier)
            .then(bg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun FlexyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .flexyClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint)
    }
}

@Composable
fun NeonBar(progress: Float, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(cs.outline.copy(alpha = 0.35f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(cs.primary, cs.tertiary)))
        )
    }
}

@Composable
fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    progress: Float? = null
) {
    val cs = MaterialTheme.colorScheme
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(cs.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = cs.primary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            value, style = MaterialTheme.typography.titleLarge, color = cs.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (sub != null) {
            Text(
                sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            NeonBar(progress)
        }
    }
}

@Composable
fun QuickTile(
    icon: ImageVector,
    title: String,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    GlassCard(modifier, onClick = onClick, glow = active) {
        Icon(icon, null, tint = if (active) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        Text(status, style = MaterialTheme.typography.bodySmall, color = if (active) cs.primary else cs.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.padding(start = 4.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value ?: "Not available",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (value == null) cs.onSurfaceVariant.copy(alpha = 0.6f) else cs.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) cs.primary.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (selected) cs.primary else cs.outline, shape)
            .flexyClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge,
            color = if (selected) cs.primary else cs.onSurfaceVariant
        )
    }
}

@Composable
fun StatusPill(text: String, on: Boolean) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Text(
        text,
        modifier = Modifier
            .clip(shape)
            .background(if (on) cs.primary.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (on) cs.primary else cs.outline, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (on) cs.primary else cs.onSurfaceVariant
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(cs.primary.copy(alpha = 0.28f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = cs.primary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            NeonButton(actionLabel, onAction)
        }
    }
}

@Composable
fun FlexyLogo(size: Dp = 40.dp) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(size * 0.3f), ambientColor = cs.primary, spotColor = cs.primary)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(cs.primary, cs.tertiary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Bolt, null, tint = cs.onPrimary, modifier = Modifier.size(size * 0.62f))
    }
}

@Composable
fun GameIcon(icon: ImageBitmap?, name: String, size: Dp) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.24f)
    if (icon != null) {
        Image(bitmap = icon, contentDescription = name, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(
            Modifier
                .size(size)
                .clip(shape)
                .background(Brush.linearGradient(listOf(cs.primary.copy(alpha = 0.5f), cs.tertiary.copy(alpha = 0.5f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Black, color = cs.onSurface)
        }
    }
}

/** Red dot that pulses while the microphone is recording. */
@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot"
    )
    Box(modifier.size(12.dp).alpha(a).clip(CircleShape).background(Color(0xFFFF3B4F)))
}

@Composable
fun WaveformView(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    idleColor: Color = MaterialTheme.colorScheme.outline
) {
    Canvas(modifier) {
        if (levels.isEmpty()) return@Canvas
        val n = levels.size
        val gap = 3.dp.toPx()
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        val mid = size.height / 2f
        levels.forEachIndexed { i, lv ->
            val h = (lv.coerceIn(0f, 1f) * size.height).coerceAtLeast(4.dp.toPx())
            val color = if ((i + 0.5f) / n <= progress) activeColor else idleColor
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barW + gap), mid - h / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}
