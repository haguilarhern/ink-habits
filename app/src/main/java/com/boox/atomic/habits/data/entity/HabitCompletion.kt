package com.boox.atomic.habits.data.entity

import androidx.room.*

@Entity(
    tableName = "habit_completions",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String,
    val completedAt: Long = System.currentTimeMillis()
)
