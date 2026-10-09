package com.flexy.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.flexy.app.data.GameSession
import com.flexy.app.system.DeviceInfo
import com.flexy.app.system.DeviceSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Real device readings, refreshed only while the app is on screen. */
@Composable
fun rememberDeviceSnapshot(intervalMs: Long = 2000L): State<DeviceSnapshot> {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(DeviceInfo.snapshot(context)) }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                state.value = withContext(Dispatchers.IO) { DeviceInfo.snapshot(context) }
                delay(intervalMs)
            }
        }
    }
    return state
}

/** Milliseconds since the session started (ticks once per second). */
@Composable
fun rememberSessionElapsed(session: GameSession?): Long {
    val owner = LocalLifecycleOwner.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session?.startMs, owner) {
        if (session != null) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(1000)
                }
            }
        }
    }
    return if (session == null) 0L else (now - session.startMs).coerceAtLeast(0L)
}
