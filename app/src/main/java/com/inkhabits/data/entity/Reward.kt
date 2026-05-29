package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A self-reward the user promises themselves, unlocked when any habit reaches a
 * target streak — a positive feedback loop to reinforce consistency.
 * Title may be typed text or handwritten ink.
 */
@Entity(tableName = "rewards")
data class Reward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val titleStrokes: String = "",
    val targetStreak: Int = 7,
    val unlocked: Boolean = false,
    val unlockedAt: Long = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
