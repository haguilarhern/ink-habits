package com.boox.atomic.habits.data.entity

import androidx.room.*

@Entity(
    tableName = "habits",
    foreignKeys = [ForeignKey(
        entity = IdentityGoal::class,
        parentColumns = ["id"],
        childColumns = ["identityGoalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("identityGoalId")]
)
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val identityGoalId: Long,
    val name: String,
    val frequencyType: String = "daily",
    val intervalDays: Int = 1,
    val daysOfWeek: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
