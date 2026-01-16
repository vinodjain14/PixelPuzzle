package com.example.pixelpuzzle

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Manages daily streaks and rewards for consecutive play days
 */
object DailyStreakManager {

    private const val KEY_LAST_PLAY_DATE = "last_play_date"
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_TOTAL_PLAY_DAYS = "total_play_days"
    private const val KEY_STREAK_REWARDS_CLAIMED = "streak_rewards_claimed"

    private const val TAG = "DailyStreakManager"

    /**
     * Update streak when user plays the game
     * Returns the current streak count
     */
    fun updateDailyStreak(context: Context): StreakInfo {
        val prefs = GamePreferences.getPrefs(context)
        val today = LocalDate.now()
        val todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val lastPlayDateString = prefs.getString(KEY_LAST_PLAY_DATE, null)
        val currentStreak = prefs.getInt(KEY_STREAK_COUNT, 0)
        val longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)
        val totalPlayDays = prefs.getInt(KEY_TOTAL_PLAY_DAYS, 0)

        val newStreak = when {
            lastPlayDateString == null -> {
                // First time playing
                DebugConfig.d(TAG, "First time playing - Starting streak at 1")
                1
            }
            lastPlayDateString == todayString -> {
                // Already played today
                DebugConfig.d(TAG, "Already played today - Streak remains at $currentStreak")
                currentStreak
            }
            else -> {
                val lastPlayDate = LocalDate.parse(lastPlayDateString, DateTimeFormatter.ISO_LOCAL_DATE)
                val daysSinceLastPlay = java.time.temporal.ChronoUnit.DAYS.between(lastPlayDate, today)

                when {
                    daysSinceLastPlay == 1L -> {
                        // Consecutive day - increment streak
                        val newCount = currentStreak + 1
                        DebugConfig.d(TAG, "Consecutive day! Streak: $currentStreak → $newCount")
                        newCount
                    }
                    else -> {
                        // Streak broken - reset to 1
                        DebugConfig.d(TAG, "Streak broken after $daysSinceLastPlay days. Resetting to 1")
                        1
                    }
                }
            }
        }

        val newLongestStreak = maxOf(newStreak, longestStreak)
        val newTotalPlayDays = if (lastPlayDateString != todayString) totalPlayDays + 1 else totalPlayDays

        // Save updated values
        prefs.edit().apply {
            putString(KEY_LAST_PLAY_DATE, todayString)
            putInt(KEY_STREAK_COUNT, newStreak)
            putInt(KEY_LONGEST_STREAK, newLongestStreak)
            putInt(KEY_TOTAL_PLAY_DAYS, newTotalPlayDays)
            apply()
        }

        DebugConfig.d(TAG, "Streak updated: $newStreak days (Longest: $newLongestStreak, Total play days: $newTotalPlayDays)")

        return StreakInfo(
            currentStreak = newStreak,
            longestStreak = newLongestStreak,
            totalPlayDays = newTotalPlayDays,
            playedToday = true,
            isNewStreak = newStreak > currentStreak,
            isNewRecord = newStreak > longestStreak
        )
    }

    /**
     * Get current streak information without updating
     */
    fun getStreakInfo(context: Context): StreakInfo {
        val prefs = GamePreferences.getPrefs(context)
        val today = LocalDate.now()
        val todayString = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val lastPlayDateString = prefs.getString(KEY_LAST_PLAY_DATE, null)
        val currentStreak = prefs.getInt(KEY_STREAK_COUNT, 0)
        val longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)
        val totalPlayDays = prefs.getInt(KEY_TOTAL_PLAY_DAYS, 0)

        val playedToday = lastPlayDateString == todayString

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalPlayDays = totalPlayDays,
            playedToday = playedToday,
            isNewStreak = false,
            isNewRecord = false
        )
    }

    /**
     * Get available streak rewards
     */
    fun getAvailableRewards(context: Context): List<StreakReward> {
        val streak = getStreakInfo(context)
        val claimedRewards = getClaimedRewards(context)

        return StreakReward.allRewards.filter { reward ->
            streak.currentStreak >= reward.requiredDays && !claimedRewards.contains(reward.id)
        }
    }

    /**
     * Claim a streak reward
     */
    fun claimReward(context: Context, reward: StreakReward): Boolean {
        val availableRewards = getAvailableRewards(context)

        if (!availableRewards.contains(reward)) {
            DebugConfig.w(TAG, "Reward ${reward.id} not available to claim")
            return false
        }

        // Apply reward
        when (reward.type) {
            RewardType.BONUS_POINTS -> {
                GamePreferences.addPoints(context, reward.value)
                DebugConfig.d(TAG, "Claimed ${reward.value} bonus points")
            }
            RewardType.UNLOCK_PALETTE -> {
                // Will implement with palette system
                DebugConfig.d(TAG, "Unlocked palette: ${reward.name}")
            }
            RewardType.SPECIAL_TOOL -> {
                // Will implement with tools system
                DebugConfig.d(TAG, "Unlocked special tool: ${reward.name}")
            }
        }

        // Mark reward as claimed
        val claimedRewards = getClaimedRewards(context).toMutableSet()
        claimedRewards.add(reward.id)

        val prefs = GamePreferences.getPrefs(context)
        prefs.edit().putStringSet(KEY_STREAK_REWARDS_CLAIMED, claimedRewards).apply()

        DebugConfig.d(TAG, "Reward ${reward.id} claimed successfully")
        return true
    }

    private fun getClaimedRewards(context: Context): Set<String> {
        val prefs = GamePreferences.getPrefs(context)
        return prefs.getStringSet(KEY_STREAK_REWARDS_CLAIMED, emptySet()) ?: emptySet()
    }

    /**
     * Reset streak (for testing purposes)
     */
    fun resetStreak(context: Context) {
        val prefs = GamePreferences.getPrefs(context)
        prefs.edit().apply {
            remove(KEY_LAST_PLAY_DATE)
            remove(KEY_STREAK_COUNT)
            remove(KEY_STREAK_REWARDS_CLAIMED)
            apply()
        }
        DebugConfig.d(TAG, "Streak reset")
    }
}

/**
 * Streak information data class
 */
data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalPlayDays: Int,
    val playedToday: Boolean,
    val isNewStreak: Boolean,
    val isNewRecord: Boolean
)

/**
 * Streak reward definition
 */
data class StreakReward(
    val id: String,
    val name: String,
    val description: String,
    val requiredDays: Int,
    val type: RewardType,
    val value: Int,
    val icon: String
) {
    companion object {
        val allRewards = listOf(
            StreakReward(
                id = "streak_3_days",
                name = "3-Day Warrior",
                description = "Play 3 days in a row",
                requiredDays = 3,
                type = RewardType.BONUS_POINTS,
                value = 50,
                icon = "🔥"
            ),
            StreakReward(
                id = "streak_7_days",
                name = "Weekly Champion",
                description = "Play 7 days in a row",
                requiredDays = 7,
                type = RewardType.BONUS_POINTS,
                value = 150,
                icon = "⭐"
            ),
            StreakReward(
                id = "streak_7_palette",
                name = "Mystery Palette",
                description = "Unlock exclusive color palette",
                requiredDays = 7,
                type = RewardType.UNLOCK_PALETTE,
                value = 1,
                icon = "🎨"
            ),
            StreakReward(
                id = "streak_14_days",
                name = "Fortnight Master",
                description = "Play 14 days in a row",
                requiredDays = 14,
                type = RewardType.BONUS_POINTS,
                value = 300,
                icon = "💎"
            ),
            StreakReward(
                id = "streak_30_days",
                name = "Monthly Legend",
                description = "Play 30 days in a row",
                requiredDays = 30,
                type = RewardType.BONUS_POINTS,
                value = 1000,
                icon = "👑"
            ),
            StreakReward(
                id = "streak_30_tool",
                name = "Golden Brush",
                description = "Fill 10 pixels at once",
                requiredDays = 30,
                type = RewardType.SPECIAL_TOOL,
                value = 1,
                icon = "🖌️"
            )
        )
    }
}

enum class RewardType {
    BONUS_POINTS,
    UNLOCK_PALETTE,
    SPECIAL_TOOL
}