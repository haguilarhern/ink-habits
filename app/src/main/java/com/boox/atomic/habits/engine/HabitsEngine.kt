package com.boox.atomic.habits.engine

import com.boox.atomic.habits.data.repository.HabitRepository

class HabitsEngine(
    private val habitRepository: HabitRepository
) {

    suspend fun getStreak(
        habitId: Long,
        frequencyType: String,
        intervalDays: Int = 1,
        daysOfWeek: String = ""
    ): Int {
        return habitRepository.getStreak(habitId, frequencyType, intervalDays, daysOfWeek)
    }

    suspend fun toggleHabit(habitId: Long) {
        habitRepository.toggleCompletion(habitId)
    }

    fun getStreakMessage(streak: Int): String {
        return when {
            streak == 0 -> "Start your streak today! Every journey begins with a single step."
            streak in 1..2 -> "Great start! You're building momentum. Keep it going!"
            streak in 3..6 -> "You're on a roll! Consistency is the key to lasting change."
            streak in 7..29 -> "Impressive! You're turning this into a habit. Stay strong!"
            streak >= 30 -> "Outstanding! You've cemented a new identity. This is who you are now!"
            else -> "Keep pushing forward!"
        }
    }
}