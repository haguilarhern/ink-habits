package com.boox.atomic.habits.data.entity

import androidx.room.*

@Entity(tableName = "identity_goals")
data class IdentityGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val identityStatement: String,
    val icon: String = "📖",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
