package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A self-reward the user promises themselves, unlocked when its target streak is
 * reached — a positive feedback loop to reinforce consistency.
 * Title may be typed text or handwritten ink.
 *
 * The target is one of three bases (see [habitId] / [identityId]):
 *  - both 0           → "Any habit": longest current streak across all habits.
 *  - [habitId] > 0    → a specific habit's current streak.
 *  - [identityId] > 0 → a specific identity's current perfect-day streak.
 */
@Entity(tableName = "rewards")
data class Reward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val titleStrokes: String = "",
    val targetStreak: Int = 7,
    /** When > 0, the reward tracks this single habit's streak. */
    val habitId: Long = 0,
    /** When > 0, the reward tracks this identity's perfect-day streak. */
    val identityId: Long = 0,
    val unlocked: Boolean = false,
    val unlockedAt: Long = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
