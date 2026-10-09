package com.flexy.app.ads

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.flexy.app.BuildConfig
import com.flexy.app.system.Connectivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class AdGateResult { GRANTED, OFFLINE, UNAVAILABLE, NOT_COMPLETED }

/**
 * All advertising lives here.
 *
 * Rules enforced in code:
 *  - PLUS: nothing in this class talks to Google. No consent request, no SDK init, no ad request.
 *  - FREE: at most ONE interstitial per "app open" (cold start, or coming back after 30+ min away),
 *    never while navigating between screens, and never more than once per MIN_INTERVAL_MS.
 *  - Offline: no ad is requested and nothing is forced. The app keeps working.
 *  - Consent (Google UMP) is gathered before the Ads SDK is initialised or any ad is requested.
 *  - Only official Google ads are shown (test ads until you add your own AdMob ids).
 */
class AdManager(private val context: Context, private val plans: PlanManager) {
    private val prefs = context.getSharedPreferences("flexy_ads", Context.MODE_PRIVATE)

    private var sdkReady = false
    private var openHandled = false
    private var openRunning = false
    private var lastStopMs = 0L

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** Settings shows a "Privacy options" button when Google's consent library says it must. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    // ---------- the one launch ad ----------

    fun onActivityStarted(activity: ComponentActivity) {
        val now = System.currentTimeMillis()
        val firstOpen = !openHandled
        val returnedAfterLongAbsence = lastStopMs > 0 && now - lastStopMs >= NEW_OPEN_AFTER_MS
        if ((firstOpen || returnedAfterLongAbsence) && !openRunning) {
            openHandled = true
            openRunning = true
            activity.lifecycleScope.launch {
                try {
                    runOpenAd(activity)
                } finally {
                    openRunning = false
                }
            }
        }
    }

    fun onActivityStopped() {
        lastStopMs = System.currentTimeMillis()
    }

    private suspend fun runOpenAd(activity: ComponentActivity) {
        // Wait briefly for Google Play to confirm FREE vs PLUS before touching any ad code.
        withTimeoutOrNull(PLAN_WAIT_MS) { plans.resolved.first { it } }
        if (plans.plan.value == Plan.PLUS) return
        if (!Connectivity.isOnline(context)) return

        val canRequest = gatherConsent(activity)
        refreshPrivacyOptions(activity)
        if (!canRequest || plans.plan.value == Plan.PLUS) return

        val sinceLast = System.currentTimeMillis() - prefs.getLong(KEY_LAST_SHOWN, 0L)
        if (sinceLast < MIN_INTERVAL_MS) return

        initSdk()
        if (!Connectivity.isOnline(context) || plans.plan.value == Plan.PLUS) return

        val ad = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine<InterstitialAd?> { cont ->
                InterstitialAd.load(
                    context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            if (cont.isActive) cont.resume(ad)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        } ?: return

        // Only show if the user is actually looking at FLEXY right now.
        if (activity.isFinishing || activity.isDestroyed ||
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
            plans.plan.value == Plan.PLUS
        ) return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                prefs.edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {}
            override fun onAdDismissedFullScreenContent() {}
        }
        ad.show(activity)
    }

    // ---------- optional: features that need a rewarded ad ----------

    /**
     * Use this to gate a feature behind a rewarded ad.
     *  GRANTED         -> unlock the feature (PLUS users get this instantly, with no ad)
     *  OFFLINE         -> show OFFLINE_MESSAGE; do NOT unlock; everything else keeps working
     *  UNAVAILABLE     -> no ad right now (no fill, or consent not given); try again later
     *  NOT_COMPLETED   -> user closed the ad early; no reward
     */
    suspend fun showRewarded(activity: ComponentActivity): AdGateResult {
        if (plans.plan.value == Plan.PLUS) return AdGateResult.GRANTED
        if (!Connectivity.isOnline(context)) return AdGateResult.OFFLINE
        if (!gatherConsent(activity)) return AdGateResult.UNAVAILABLE
        refreshPrivacyOptions(activity)
        initSdk()

        val ad = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine<RewardedAd?> { cont ->
                RewardedAd.load(
                    context, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            if (cont.isActive) cont.resume(ad)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        } ?: return AdGateResult.UNAVAILABLE

        return suspendCancellableCoroutine { cont ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) cont.resume(if (earned) AdGateResult.GRANTED else AdGateResult.NOT_COMPLETED)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(AdGateResult.UNAVAILABLE)
                }
            }
            ad.show(activity) { earned = true }
        }
    }

    // ---------- consent (Google User Messaging Platform) ----------

    /** Shows Google's consent form if (and only if) it is required. Returns canRequestAds(). */
    private suspend fun gatherConsent(activity: ComponentActivity): Boolean =
        suspendCancellableCoroutine { cont ->
            val info = UserMessagingPlatform.getConsentInformation(activity)
            val params = ConsentRequestParameters.Builder().build()
            info.requestConsentInfoUpdate(
                activity, params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        if (cont.isActive) cont.resume(info.canRequestAds())
                    }
                },
                {
                    // Couldn't reach Google: fall back to any consent we already stored.
                    if (cont.isActive) cont.resume(info.canRequestAds())
                }
            )
        }

    private fun refreshPrivacyOptions(activity: ComponentActivity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        _privacyOptionsRequired.value = info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /** "Privacy options" button in Settings (lets users change their consent choice). */
    fun showPrivacyOptions(activity: ComponentActivity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { /* nothing to do */ }
    }

    private suspend fun initSdk() {
        if (sdkReady) return
        suspendCancellableCoroutine<Unit> { cont ->
            MobileAds.initialize(context) { if (cont.isActive) cont.resume(Unit) }
        }
        sdkReady = true
    }

    companion object {
        const val OFFLINE_MESSAGE = "Internet connection required to load an advertisement."
        private const val KEY_LAST_SHOWN = "last_interstitial_ms"
        private const val PLAN_WAIT_MS = 3_000L
        private const val LOAD_TIMEOUT_MS = 8_000L
        private const val MIN_INTERVAL_MS = 5 * 60_000L
        private const val NEW_OPEN_AFTER_MS = 30 * 60_000L
    }
}
