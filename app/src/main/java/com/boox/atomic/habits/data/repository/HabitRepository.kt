package com.boox.atomic.habits.data.repository

import com.boox.atomic.habits.data.dao.DashboardItem
import com.boox.atomic.habits.data.dao.HabitCompletionDao
import com.boox.atomic.habits.data.dao.HabitDao
import com.boox.atomic.habits.data.entity.Habit
import com.boox.atomic.habits.data.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao
) {

    fun getFullDashboard(): Flow<List<DashboardItem>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return habitDao.getFullDashboard(today)
    }

    suspend fun createHabit(habit: Habit): Long {
        return habitDao.insert(habit)
    }

    suspend fun toggleCompletion(habitId: Long) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val isCompleted = habitCompletionDao.isCompleted(habitId, today)
        if (isCompleted > 0) {
            habitCompletionDao.removeCompletion(habitId, today)
        } else {
            habitCompletionDao.insert(
                HabitCompletion(habitId = habitId, date = today)
            )
        }
    }

    suspend fun getStreak(
        habitId: Long,
        frequencyType: String,
        intervalDays: Int = 1,
        daysOfWeek: String = ""
    ): Int {
        val today = LocalDate.now()
        // Fetch up to 365 days of completion history
        val sinceDate = today.minusDays(365)
        val dates = habitCompletionDao.getStreakWindow(
            habitId,
            sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        val completionSet = dates.toHashSet()

        var streak = 0
        var currentDate = today

        while (true) {
            val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Skip days the habit is not scheduled for (based on frequency)
            if (!isScheduledDay(currentDate, frequencyType, intervalDays, daysOfWeek)) {
                currentDate = currentDate.minusDays(1)
                continue
            }

            if (completionSet.contains(dateStr)) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    private fun isScheduledDay(
        date: LocalDate,
        frequencyType: String,
        intervalDays: Int,
        daysOfWeek: String
    ): Boolean {
        return when (frequencyType) {
            "daily" -> true
            "interval" -> {
                // For interval frequency, count days from a reference point
                // Every N days is a scheduled day (relative to today for streak counting)
                // A habit is "scheduled" today if the epoch-day modulo intervalDays matches
                val daysSinceEpoch = date.toEpochDay()
                daysSinceEpoch % intervalDays == 0L
            }
            "daysofweek" -> {
                if (daysOfWeek.isBlank()) return false
                val dayNames = daysOfWeek.split(",").map { it.trim().lowercase() }
                val dayName = date.dayOfWeek.name.lowercase().take(3)
                dayNames.contains(dayName)
            }
            else -> true
        }
    }
}