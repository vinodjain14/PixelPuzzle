package com.example.pixelpuzzle

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages Google AdMob integration for rewarded ads
 * Used for free hint system
 */
class AdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    companion object {
        private const val TAG = "AdManager"

        // Test Ad Unit IDs - REPLACE WITH YOUR OWN IN PRODUCTION
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3490366920456093~4483944914" // Test ID

        @Volatile
        private var instance: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Initialize AdMob SDK
     * Call this in Application onCreate or MainActivity onCreate
     */
    fun initialize() {
        MobileAds.initialize(context) { initializationStatus ->
            DebugConfig.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }
    }

    /**
     * Load a rewarded ad
     */
    fun loadRewardedAd(onAdLoaded: () -> Unit = {}, onAdFailedToLoad: (String) -> Unit = {}) {
        if (isLoading) {
            DebugConfig.d(TAG, "Ad already loading")
            return
        }

        if (rewardedAd != null) {
            DebugConfig.d(TAG, "Ad already loaded")
            onAdLoaded()
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    DebugConfig.d(TAG, "Rewarded ad loaded successfully")
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    val error = "Ad failed to load: ${loadAdError.message}"
                    DebugConfig.e(TAG, error)
                    onAdFailedToLoad(error)
                }
            }
        )
    }

    /**
     * Show the rewarded ad
     * @param activity The activity to show the ad in
     * @param onUserEarnedReward Called when user completes watching the ad
     * @param onAdDismissed Called when ad is dismissed (whether completed or not)
     * @param onAdFailedToShow Called if ad fails to show
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissed: () -> Unit = {},
        onAdFailedToShow: (String) -> Unit = {}
    ) {
        val ad = rewardedAd

        if (ad == null) {
            val error = "Ad not ready. Please try again."
            DebugConfig.w(TAG, error)
            onAdFailedToShow(error)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                DebugConfig.d(TAG, "Ad dismissed")
                rewardedAd = null
                onAdDismissed()
                // Preload next ad
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                val error = "Ad failed to show: ${adError.message}"
                DebugConfig.e(TAG, error)
                rewardedAd = null
                onAdFailedToShow(error)
                // Try to load another ad
                loadRewardedAd()
            }

            override fun onAdShowedFullScreenContent() {
                DebugConfig.d(TAG, "Ad showed")
            }
        }

        ad.show(activity) { rewardItem ->
            val rewardAmount = rewardItem.amount
            val rewardType = rewardItem.type
            DebugConfig.d(TAG, "User earned reward: $rewardAmount $rewardType")
            onUserEarnedReward()
        }
    }

    /**
     * Check if ad is ready to show
     */
    fun isAdReady(): Boolean {
        return rewardedAd != null
    }

    /**
     * Preload ad (call at app start or after showing an ad)
     */
    fun preloadAd() {
        loadRewardedAd()
    }
}

/**
 * Test ad helper - shows test ads for development
 * In production, replace test IDs with real ones from AdMob console
 */
object AdTestHelper {
    /**
     * Enable test ads for development
     * Add your device ID to see test ads
     */
    fun enableTestMode(context: Context) {
        val testDeviceIds = listOf(
            AdRequest.DEVICE_ID_EMULATOR,
            // Add your device ID here for testing
            // "YOUR_DEVICE_ID_HERE"
        )

        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()

        MobileAds.setRequestConfiguration(configuration)
        DebugConfig.d("AdTestHelper", "Test mode enabled for devices: $testDeviceIds")
    }
}