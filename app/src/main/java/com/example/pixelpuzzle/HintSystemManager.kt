package com.example.pixelpuzzle

import android.content.Context

/**
 * Manages the tiered hint system with free (ad-based) and premium hints
 */
object HintSystemManager {

    private const val KEY_FREE_HINTS_REMAINING = "free_hints_remaining"
    private const val KEY_PREMIUM_HINTS_COUNT = "premium_hints_count"
    private const val KEY_MASTER_REVEAL_COUNT = "master_reveal_count"
    private const val KEY_LAST_FREE_HINT_RESET = "last_free_hint_reset"
    private const val KEY_TOTAL_HINTS_USED = "total_hints_used"

    private const val TAG = "HintSystemManager"

    // Free hints reset daily
    private const val FREE_HINTS_PER_DAY = 3

    // Premium hint costs (in coins)
    private const val PREMIUM_HINT_COST = 50
    private const val MASTER_REVEAL_COST = 200

    /**
     * Get current hint availability
     */
    fun getHintAvailability(context: Context): HintAvailability {
        val prefs = GamePreferences.getPrefs(context)
        val today = java.time.LocalDate.now().toString()
        val lastReset = prefs.getString(KEY_LAST_FREE_HINT_RESET, "")

        // Reset free hints if new day
        val freeHints = if (lastReset != today) {
            prefs.edit().apply {
                putInt(KEY_FREE_HINTS_REMAINING, FREE_HINTS_PER_DAY)
                putString(KEY_LAST_FREE_HINT_RESET, today)
                apply()
            }
            FREE_HINTS_PER_DAY
        } else {
            prefs.getInt(KEY_FREE_HINTS_REMAINING, FREE_HINTS_PER_DAY)
        }

        val premiumHints = prefs.getInt(KEY_PREMIUM_HINTS_COUNT, 0)
        val masterReveals = prefs.getInt(KEY_MASTER_REVEAL_COUNT, 0)
        val totalCoins = GamePreferences.getTotalPoints(context)

        return HintAvailability(
            freeHintsRemaining = freeHints,
            premiumHintsOwned = premiumHints,
            masterRevealsOwned = masterReveals,
            canAffordPremiumHint = totalCoins >= PREMIUM_HINT_COST,
            canAffordMasterReveal = totalCoins >= MASTER_REVEAL_COST
        )
    }

    /**
     * Use a free hint (requires watching ad)
     */
    fun useFreeHint(context: Context): Boolean {
        val prefs = GamePreferences.getPrefs(context)
        val freeHints = prefs.getInt(KEY_FREE_HINTS_REMAINING, 0)

        if (freeHints <= 0) {
            DebugConfig.w(TAG, "No free hints remaining")
            return false
        }

        prefs.edit().apply {
            putInt(KEY_FREE_HINTS_REMAINING, freeHints - 1)
            apply()
        }

        incrementTotalHintsUsed(context)
        DebugConfig.d(TAG, "Free hint used. Remaining: ${freeHints - 1}")
        return true
    }

    /**
     * Purchase premium hint with coins
     */
    fun purchasePremiumHint(context: Context): Boolean {
        val totalCoins = GamePreferences.getTotalPoints(context)

        if (totalCoins < PREMIUM_HINT_COST) {
            DebugConfig.w(TAG, "Insufficient coins for premium hint")
            return false
        }

        // Deduct coins
        val prefs = GamePreferences.getPrefs(context)
        val newTotal = totalCoins - PREMIUM_HINT_COST
        prefs.edit().putInt("total_points", newTotal).apply()

        // Add premium hint
        val premiumHints = prefs.getInt(KEY_PREMIUM_HINTS_COUNT, 0)
        prefs.edit().putInt(KEY_PREMIUM_HINTS_COUNT, premiumHints + 1).apply()

        DebugConfig.d(TAG, "Premium hint purchased. Coins: $totalCoins -> $newTotal")
        return true
    }

    /**
     * Use a premium hint (already owned)
     */
    fun usePremiumHint(context: Context): Boolean {
        val prefs = GamePreferences.getPrefs(context)
        val premiumHints = prefs.getInt(KEY_PREMIUM_HINTS_COUNT, 0)

        if (premiumHints <= 0) {
            DebugConfig.w(TAG, "No premium hints owned")
            return false
        }

        prefs.edit().putInt(KEY_PREMIUM_HINTS_COUNT, premiumHints - 1).apply()

        incrementTotalHintsUsed(context)
        DebugConfig.d(TAG, "Premium hint used. Remaining: ${premiumHints - 1}")
        return true
    }

    /**
     * Purchase master reveal with coins
     */
    fun purchaseMasterReveal(context: Context): Boolean {
        val totalCoins = GamePreferences.getTotalPoints(context)

        if (totalCoins < MASTER_REVEAL_COST) {
            DebugConfig.w(TAG, "Insufficient coins for master reveal")
            return false
        }

        // Deduct coins
        val prefs = GamePreferences.getPrefs(context)
        val newTotal = totalCoins - MASTER_REVEAL_COST
        prefs.edit().putInt("total_points", newTotal).apply()

        // Add master reveal
        val masterReveals = prefs.getInt(KEY_MASTER_REVEAL_COUNT, 0)
        prefs.edit().putInt(KEY_MASTER_REVEAL_COUNT, masterReveals + 1).apply()

        DebugConfig.d(TAG, "Master reveal purchased. Coins: $totalCoins -> $newTotal")
        return true
    }

    /**
     * Use a master reveal (already owned)
     */
    fun useMasterReveal(context: Context): Boolean {
        val prefs = GamePreferences.getPrefs(context)
        val masterReveals = prefs.getInt(KEY_MASTER_REVEAL_COUNT, 0)

        if (masterReveals <= 0) {
            DebugConfig.w(TAG, "No master reveals owned")
            return false
        }

        prefs.edit().putInt(KEY_MASTER_REVEAL_COUNT, masterReveals - 1).apply()

        incrementTotalHintsUsed(context)
        DebugConfig.d(TAG, "Master reveal used. Remaining: ${masterReveals - 1}")
        return true
    }

    /**
     * Add premium hints (e.g., from in-app purchase)
     */
    fun addPremiumHints(context: Context, count: Int) {
        val prefs = GamePreferences.getPrefs(context)
        val current = prefs.getInt(KEY_PREMIUM_HINTS_COUNT, 0)
        prefs.edit().putInt(KEY_PREMIUM_HINTS_COUNT, current + count).apply()
        DebugConfig.d(TAG, "Added $count premium hints. Total: ${current + count}")
    }

    /**
     * Add master reveals (e.g., from in-app purchase)
     */
    fun addMasterReveals(context: Context, count: Int) {
        val prefs = GamePreferences.getPrefs(context)
        val current = prefs.getInt(KEY_MASTER_REVEAL_COUNT, 0)
        prefs.edit().putInt(KEY_MASTER_REVEAL_COUNT, current + count).apply()
        DebugConfig.d(TAG, "Added $count master reveals. Total: ${current + count}")
    }

    /**
     * Get total hints used (statistics)
     */
    fun getTotalHintsUsed(context: Context): Int {
        val prefs = GamePreferences.getPrefs(context)
        return prefs.getInt(KEY_TOTAL_HINTS_USED, 0)
    }

    private fun incrementTotalHintsUsed(context: Context) {
        val prefs = GamePreferences.getPrefs(context)
        val current = prefs.getInt(KEY_TOTAL_HINTS_USED, 0)
        prefs.edit().putInt(KEY_TOTAL_HINTS_USED, current + 1).apply()
    }

    /**
     * Get hint costs
     */
    fun getHintCosts(): HintCosts {
        return HintCosts(
            premiumHintCost = PREMIUM_HINT_COST,
            masterRevealCost = MASTER_REVEAL_COST
        )
    }
}

/**
 * Current hint availability
 */
data class HintAvailability(
    val freeHintsRemaining: Int,
    val premiumHintsOwned: Int,
    val masterRevealsOwned: Int,
    val canAffordPremiumHint: Boolean,
    val canAffordMasterReveal: Boolean
)

/**
 * Hint costs
 */
data class HintCosts(
    val premiumHintCost: Int,
    val masterRevealCost: Int
)

/**
 * Types of hints available
 */
sealed class HintType {
    object FreeHint : HintType()        // Watch ad to reveal 1 correct piece
    object PremiumHint : HintType()     // Reveal 3x3 section (50 coins or owned)
    object MasterReveal : HintType()    // Reveal entire quadrant (200 coins or owned)
}

/**
 * Result of applying a hint
 */
data class HintResult(
    val success: Boolean,
    val piecesRevealed: List<Int>,     // List of piece IDs revealed
    val message: String
)