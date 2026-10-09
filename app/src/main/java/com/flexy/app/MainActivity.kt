package com.flexy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.flexy.app.navigation.FlexyRoot
import com.flexy.app.ui.components.FlexyBackground
import com.flexy.app.ui.theme.FlexyTheme

class MainActivity : ComponentActivity() {
    private val flexyApp get() = application as FlexyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = (application as FlexyApplication).settings
        setContent {
            val settings by store.state.collectAsState()
            FlexyTheme(settings) {
                FlexyBackground { FlexyRoot() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        flexyApp.billing.connect()          // re-checks the PLUS purchase
        flexyApp.ads.onActivityStarted(this) // FREE only: at most one ad per app open
    }

    override fun onStop() {
        flexyApp.ads.onActivityStopped()
        super.onStop()
    }
}
