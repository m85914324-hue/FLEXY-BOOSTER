package com.flexy.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.flexy.app.flexy
import com.flexy.app.system.formatDuration
import com.flexy.app.ui.components.FlexyIconButton
import com.flexy.app.ui.components.FlexyLogo
import com.flexy.app.ui.components.LocalSnackbar
import com.flexy.app.ui.components.flexyClickable
import com.flexy.app.ui.components.rememberSessionElapsed
import com.flexy.app.ui.screens.AboutScreen
import com.flexy.app.ui.screens.BoosterScreen
import com.flexy.app.ui.screens.GamesScreen
import com.flexy.app.ui.screens.HomeScreen
import com.flexy.app.ui.screens.PerformanceScreen
import com.flexy.app.ui.screens.PrivacyScreen
import com.flexy.app.ui.screens.SettingsScreen
import com.flexy.app.ui.screens.VoiceScreen
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * App shell: ONE sidebar + top bar + bottom bar shared by every screen,
 * so the sidebar behaves the same everywhere.
 */
@Composable
fun FlexyRoot() {
    val context = LocalContext.current
    val app = context.flexy
    val cs = MaterialTheme.colorScheme
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val backEntry by navController.currentBackStackEntryAsState()
    val current = Route.fromPath(backEntry?.destination?.route) ?: Route.HOME
    val highlighted = if (current == Route.PRIVACY) Route.SETTINGS else current
    val plan by app.plans.plan.collectAsState()

    // Re-scan games whenever the app comes to the front (catches new installs).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { app.games.refresh() }
    }

    fun navigateTo(route: Route) {
        if (navController.currentDestination?.route == route.path) return
        navController.navigate(route.path) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    // Back button closes the sidebar first.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Closed: the edge-swipe below opens it. Open: swipe left or tap the dim area to close.
            gesturesEnabled = drawerState.isOpen,
            scrimColor = Color.Black.copy(alpha = 0.55f),
            drawerContent = {
                FlexyDrawerContent(
                    selected = highlighted,
                    plan = plan,
                    onSelect = { route ->
                        scope.launch { drawerState.close() }
                        navigateTo(route)
                    }
                )
            }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .edgeSwipeToOpen(drawerState) { scope.launch { drawerState.open() } }
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        FlexyTopBar(
                            route = current,
                            onMenu = openDrawer,
                            onSession = { navigateTo(Route.BOOSTER) }
                        )
                    },
                    bottomBar = { FlexyBottomBar(highlighted) { navigateTo(it) } },
                    snackbarHost = {
                        SnackbarHost(snackbar) { data ->
                            Snackbar(
                                snackbarData = data,
                                shape = RoundedCornerShape(16.dp),
                                containerColor = cs.surfaceVariant,
                                contentColor = cs.onSurface,
                                actionColor = cs.primary
                            )
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Route.HOME.path,
                        modifier = Modifier.padding(padding),
                        enterTransition = { fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 20 } },
                        exitTransition = { fadeOut(tween(150)) },
                        popEnterTransition = { fadeIn(tween(250)) },
                        popExitTransition = { fadeOut(tween(150)) }
                    ) {
                        composable(Route.HOME.path) { HomeScreen { navigateTo(it) } }
                        composable(Route.BOOSTER.path) { BoosterScreen { navigateTo(it) } }
                        composable(Route.GAMES.path) { GamesScreen() }
                        composable(Route.VOICE.path) { VoiceScreen() }
                        composable(Route.PERFORMANCE.path) { PerformanceScreen() }
                        composable(Route.SETTINGS.path) {
                            SettingsScreen(
                                onNavigate = { navigateTo(it) },
                                onOpenPrivacy = { navController.navigate(Route.PRIVACY.path) { launchSingleTop = true } }
                            )
                        }
                        composable(Route.ABOUT.path) {
                            AboutScreen(
                                onOpenPrivacy = { navController.navigate(Route.PRIVACY.path) { launchSingleTop = true } }
                            )
                        }
                        composable(Route.PRIVACY.path) { PrivacyScreen() }
                    }
                }
            }
        }
    }
}

/**
 * Opens the sidebar when a finger that starts within ~36dp of the LEFT edge
 * drags to the right. It only watches (never consumes) touches, so taps and
 * scrolling on the screen keep working normally.
 *
 * Android limitation: with gesture navigation, the system "Back" swipe also
 * starts at the very edge. If the sidebar doesn't open, start the swipe a
 * little further in from the edge, or tap the menu button.
 */
private fun Modifier.edgeSwipeToOpen(drawerState: DrawerState, onOpen: () -> Unit): Modifier =
    pointerInput(drawerState) {
        val edgePx = 36.dp.toPx()
        val triggerPx = 44.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x > edgePx || drawerState.isOpen) return@awaitEachGesture
            var dx = 0f
            var dy = 0f
            var opened = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val delta = change.positionChange()
                dx += delta.x
                dy += delta.y
                if (!opened && dx > triggerPx && dx > abs(dy) * 1.5f) {
                    opened = true
                    onOpen()
                }
            }
        }
    }

@Composable
private fun FlexyTopBar(route: Route, onMenu: () -> Unit, onSession: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val app = LocalContext.current.flexy
    val session by app.sessions.session.collectAsState()
    val elapsed = rememberSessionElapsed(session)
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlexyIconButton(Icons.Rounded.Menu, "Open menu", onMenu)
        Spacer(Modifier.width(4.dp))
        if (route == Route.HOME) {
            FlexyLogo(30.dp)
            Spacer(Modifier.width(10.dp))
            Text("FLEXY", color = cs.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        } else {
            Text(route.title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
        }
        Spacer(Modifier.weight(1f))
        if (session != null) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(cs.primary.copy(alpha = 0.14f))
                    .border(1.dp, cs.primary.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .flexyClickable(onSession)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(cs.primary))
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDuration(elapsed), color = cs.primary,
                    style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun FlexyBottomBar(selected: Route, onSelect: (Route) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        Triple(Route.HOME, Icons.Rounded.Home, "Home"),
        Triple(Route.GAMES, Icons.Rounded.SportsEsports, "Games"),
        Triple(Route.VOICE, Icons.Rounded.Mic, "Voice"),
        Triple(Route.SETTINGS, Icons.Rounded.Settings, "Settings")
    )
    NavigationBar(containerColor = cs.surface.copy(alpha = 0.95f), tonalElevation = 0.dp) {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = selected == route,
                onClick = { onSelect(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = cs.onPrimary,
                    selectedTextColor = cs.primary,
                    indicatorColor = cs.primary,
                    unselectedIconColor = cs.onSurfaceVariant,
                    unselectedTextColor = cs.onSurfaceVariant
                )
            )
        }
    }
}
