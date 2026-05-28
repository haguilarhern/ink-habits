package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An identity the user wants to embody (e.g. "I am a Reader").
 * Habits are grouped under an identity. The name may be typed text,
 * handwritten ink (serialized strokes), or both.
 */
@Entity(tableName = "identity_goals")
data class IdentityGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val nameStrokes: String = "",
    val icon: String = "★",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
