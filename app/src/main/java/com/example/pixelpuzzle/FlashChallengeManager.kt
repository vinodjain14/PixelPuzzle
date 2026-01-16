package com.example.pixelpuzzle

import android.content.Context
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * Manages timed flash challenges with exclusive rewards
 */
object FlashChallengeManager {

    private const val KEY_ACTIVE_CHALLENGE = "active_challenge"
    private const val KEY_CHALLENGE_EXPIRES_AT = "challenge_expires_at"
    private const val KEY_CHALLENGE_LEVEL = "challenge_level"
    private const val KEY_CHALLENGE_TIME_LIMIT = "challenge_time_limit"
    private const val KEY_CHALLENGE_REWARD_TYPE = "challenge_reward_type"
    private const val KEY_LAST_CHALLENGE_DATE = "last_challenge_date"
    private const val KEY_COMPLETED_CHALLENGES = "completed_challenges_count"

    private const val TAG = "FlashChallengeManager"
    private const val CHALLENGE_DURATION_HOURS = 6 // Challenge available for 6 hours

    /**
     * Get current active challenge or generate new one
     */
    fun getActiveChallenge(context: Context): FlashChallenge? {
        val prefs = GamePreferences.getPrefs(context)
        val now = System.currentTimeMillis()

        val expiresAt = prefs.getLong(KEY_CHALLENGE_EXPIRES_AT, 0L)

        // Check if there's an active challenge that hasn't expired
        if (expiresAt > now) {
            val level = prefs.getInt(KEY_CHALLENGE_LEVEL, 1)
            val timeLimit = prefs.getInt(KEY_CHALLENGE_TIME_LIMIT, 300)
            val rewardType = prefs.getString(KEY_CHALLENGE_REWARD_TYPE, "POINTS")

            DebugConfig.d(TAG, "Active challenge found: Level $level, ${(expiresAt - now) / 1000}s remaining")

            return FlashChallenge(
                id = "flash_${level}_${expiresAt}",
                level = level,
                timeLimit = timeLimit,
                reward = createReward(rewardType ?: "POINTS"),
                expiresAt = expiresAt
            )
        }

        // Check if we should generate a new challenge (once per day)
        val lastChallengeDate = prefs.getString(KEY_LAST_CHALLENGE_DATE, "")
        val today = LocalDateTime.now().toLocalDate().toString()

        if (lastChallengeDate != today) {
            return generateNewChallenge(context)
        }

        DebugConfig.d(TAG, "No active challenge available")
        return null
    }

    /**
     * Generate a new daily flash challenge
     */
    private fun generateNewChallenge(context: Context): FlashChallenge {
        val prefs = GamePreferences.getPrefs(context)
        val unlockedLevels = GamePreferences.getUnlockedLevels(context)

        // Select a random unlocked level (prefer recent levels)
        val level = if (unlockedLevels > 5) {
            Random.nextInt(maxOf(1, unlockedLevels - 5), unlockedLevels)
        } else {
            Random.nextInt(1, maxOf(2, unlockedLevels))
        }

        // Time limit based on difficulty
        val difficulty = getDifficultyForLevel(level)
        val timeLimit = when (difficulty) {
            GridDifficulty.SUPER_EASY -> 180 // 3 minutes
            GridDifficulty.EASY -> 240        // 4 minutes
            GridDifficulty.MEDIUM -> 300      // 5 minutes
            GridDifficulty.HARD -> 360        // 6 minutes
            GridDifficulty.SUPER_HARD -> 420  // 7 minutes
        }

        // Random reward type
        val rewardTypes = listOf("POINTS", "GOLDEN_BRUSH", "PALETTE")
        val rewardType = rewardTypes.random()

        // Challenge expires in 6 hours
        val expiresAt = System.currentTimeMillis() + (CHALLENGE_DURATION_HOURS * 60 * 60 * 1000)

        // Save challenge
        val today = LocalDateTime.now().toLocalDate().toString()
        prefs.edit().apply {
            putLong(KEY_CHALLENGE_EXPIRES_AT, expiresAt)
            putInt(KEY_CHALLENGE_LEVEL, level)
            putInt(KEY_CHALLENGE_TIME_LIMIT, timeLimit)
            putString(KEY_CHALLENGE_REWARD_TYPE, rewardType)
            putString(KEY_LAST_CHALLENGE_DATE, today)
            apply()
        }

        DebugConfig.d(TAG, "New challenge generated: Level $level, ${timeLimit}s limit, Reward: $rewardType")

        return FlashChallenge(
            id = "flash_${level}_${expiresAt}",
            level = level,
            timeLimit = timeLimit,
            reward = createReward(rewardType),
            expiresAt = expiresAt
        )
    }

    private fun createReward(rewardType: String): ChallengeReward {
        return when (rewardType) {
            "POINTS" -> ChallengeReward.BonusPoints(100)
            "GOLDEN_BRUSH" -> ChallengeReward.SpecialTool(ToolType.GOLDEN_BRUSH)
            "PALETTE" -> ChallengeReward.ExclusivePalette("flash_palette_${Random.nextInt(1, 10)}")
            else -> ChallengeReward.BonusPoints(100)
        }
    }

    /**
     * Complete a flash challenge
     */
    fun completeChallenge(context: Context, challenge: FlashChallenge, completedInTime: Boolean): Boolean {
        if (!completedInTime) {
            DebugConfig.d(TAG, "Challenge failed: Time limit exceeded")
            return false
        }

        val prefs = GamePreferences.getPrefs(context)
        val now = System.currentTimeMillis()

        // Verify challenge hasn't expired
        if (now > challenge.expiresAt) {
            DebugConfig.d(TAG, "Challenge failed: Expired")
            return false
        }

        // Award reward
        when (val reward = challenge.reward) {
            is ChallengeReward.BonusPoints -> {
                GamePreferences.addPoints(context, reward.amount)
                DebugConfig.d(TAG, "Awarded ${reward.amount} bonus points")
            }
            is ChallengeReward.SpecialTool -> {
                // Will implement with tools system
                DebugConfig.d(TAG, "Unlocked special tool: ${reward.type}")
            }
            is ChallengeReward.ExclusivePalette -> {
                // Will implement with palette system
                DebugConfig.d(TAG, "Unlocked exclusive palette: ${reward.paletteId}")
            }
        }

        // Increment completed challenges counter
        val completedCount = prefs.getInt(KEY_COMPLETED_CHALLENGES, 0)
        prefs.edit().putInt(KEY_COMPLETED_CHALLENGES, completedCount + 1).apply()

        // Clear active challenge
        clearActiveChallenge(context)

        DebugConfig.d(TAG, "Challenge completed successfully! Total: ${completedCount + 1}")
        return true
    }

    /**
     * Clear active challenge
     */
    private fun clearActiveChallenge(context: Context) {
        val prefs = GamePreferences.getPrefs(context)
        prefs.edit().apply {
            remove(KEY_CHALLENGE_EXPIRES_AT)
            remove(KEY_CHALLENGE_LEVEL)
            remove(KEY_CHALLENGE_TIME_LIMIT)
            remove(KEY_CHALLENGE_REWARD_TYPE)
            apply()
        }
    }

    /**
     * Get total completed challenges count
     */
    fun getCompletedChallengesCount(context: Context): Int {
        val prefs = GamePreferences.getPrefs(context)
        return prefs.getInt(KEY_COMPLETED_CHALLENGES, 0)
    }

    /**
     * Check if challenge is still valid
     */
    fun isChallengeValid(challenge: FlashChallenge): Boolean {
        return System.currentTimeMillis() < challenge.expiresAt
    }

    /**
     * Get time remaining for challenge in seconds
     */
    fun getTimeRemaining(challenge: FlashChallenge): Long {
        val remaining = (challenge.expiresAt - System.currentTimeMillis()) / 1000
        return maxOf(0, remaining)
    }

    /**
     * Force generate new challenge (for testing)
     */
    fun forceNewChallenge(context: Context): FlashChallenge {
        val prefs = GamePreferences.getPrefs(context)
        prefs.edit().remove(KEY_LAST_CHALLENGE_DATE).apply()
        return generateNewChallenge(context)
    }
}

/**
 * Flash challenge definition
 */
data class FlashChallenge(
    val id: String,
    val level: Int,
    val timeLimit: Int, // seconds
    val reward: ChallengeReward,
    val expiresAt: Long // timestamp
)

/**
 * Challenge rewards
 */
sealed class ChallengeReward {
    data class SpecialTool(val type: ToolType) : ChallengeReward()
    data class BonusPoints(val amount: Int) : ChallengeReward()
    data class ExclusivePalette(val paletteId: String) : ChallengeReward()
}

enum class ToolType {
    GOLDEN_BRUSH,    // Fill 10 pixels at once
    MAGIC_WAND,      // Auto-complete a section
    TIME_FREEZE      // Pause timer for 30 seconds
}