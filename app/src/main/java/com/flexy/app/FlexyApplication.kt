package com.flexy.app

import android.app.Application
import android.content.Context
import com.flexy.app.ads.AdManager
import com.flexy.app.ads.BillingManager
import com.flexy.app.ads.PlanManager
import com.flexy.app.data.GameRepository
import com.flexy.app.data.SessionManager
import com.flexy.app.data.SettingsStore

/** Holds the few app-wide objects (settings, game library, session timer). */
class FlexyApplication : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var games: GameRepository
        private set
    lateinit var sessions: SessionManager
        private set
    lateinit var plans: PlanManager
        private set
    lateinit var billing: BillingManager
        private set
    lateinit var ads: AdManager
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        games = GameRepository(this)
        sessions = SessionManager(this, games, settings)
        plans = PlanManager(this)
        billing = BillingManager(this, plans)
        ads = AdManager(this, plans)
        billing.connect()
    }

    /** Launches a game with Android's normal package-launch intent and starts tracking. */
    fun launchGame(packageName: String, name: String?): Boolean {
        val ok = games.launch(packageName)
        if (ok) {
            games.recordLaunch(packageName)
            sessions.start(packageName, name)
        }
        return ok
    }
}

val Context.flexy: FlexyApplication
    get() = applicationContext as FlexyApplication
