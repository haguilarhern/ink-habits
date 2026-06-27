package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A consumed "totem" that protects a streak on a single missed day. Created
 * automatically by the overnight reconcile when a scheduled occurrence is missed
 * and the user owns a matching totem (Duolingo-style streak freeze).
 *
 *  - [habitId] > 0    → protects that habit's missed occurrence on [date].
 *  - [identityId] > 0 → protects that identity's perfect-day on [date].
 *
 * Streak math treats a frozen [date] as if it had been completed.
 */
@Entity(
    tableName = "streak_freezes",
    indices = [Index("date"), Index("habitId"), Index("identityId")]
)
data class StreakFreeze(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long = 0,
    val identityId: Long = 0,
    /** ISO yyyy-MM-dd of the protected day. */
    val date: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
