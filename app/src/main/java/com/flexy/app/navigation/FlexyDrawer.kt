package com.flexy.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexy.app.ads.Plan
import com.flexy.app.system.appVersion
import com.flexy.app.ui.components.StatusPill
import com.flexy.app.ui.components.FlexyLogo

private data class DrawerEntry(val route: Route, val emoji: String, val label: String)

private val drawerEntries = listOf(
    DrawerEntry(Route.HOME, "🏠", "Home"),
    DrawerEntry(Route.BOOSTER, "🎮", "Game Booster"),
    DrawerEntry(Route.GAMES, "🎯", "Games"),
    DrawerEntry(Route.VOICE, "🎙️", "Voice Changer"),
    DrawerEntry(Route.PERFORMANCE, "📊", "Performance"),
    DrawerEntry(Route.SETTINGS, "⚙️", "Settings"),
    DrawerEntry(Route.ABOUT, "ℹ️", "About FLEXY")
)

/** The slide-out sidebar. Used by every screen through FlexyRoot. */
@Composable
fun FlexyDrawerContent(selected: Route, plan: Plan, onSelect: (Route) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.82f).widthIn(max = 330.dp),
        drawerContainerColor = cs.surface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlexyLogo(52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "FLEXY", color = cs.onSurface, fontSize = 26.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 4.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Game Booster", color = cs.primary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(if (plan == Plan.PLUS) "PLUS" else "FREE", plan == Plan.PLUS)
                    }
                }
            }
            HorizontalDivider(color = cs.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                drawerEntries.forEach { entry ->
                    val isSelected = entry.route == selected
                    val shape = RoundedCornerShape(16.dp)
                    val borderColor by animateColorAsState(
                        if (isSelected) cs.primary.copy(alpha = 0.6f) else Color.Transparent,
                        label = "drawerBorder"
                    )
                    NavigationDrawerItem(
                        label = {
                            Text(
                                entry.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selected = isSelected,
                        onClick = { onSelect(entry.route) },
                        icon = { Text(entry.emoji, fontSize = 22.sp) },
                        shape = shape,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = cs.primary.copy(alpha = 0.16f),
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = cs.primary,
                            unselectedTextColor = cs.onSurface,
                            selectedIconColor = cs.primary,
                            unselectedIconColor = cs.onSurface
                        ),
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .border(1.dp, borderColor, shape)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "FLEXY v${appVersion(context)}  •  Android gaming utility",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
