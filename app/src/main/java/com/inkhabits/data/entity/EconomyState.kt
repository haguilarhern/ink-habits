package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row ([id] == 1) wallet for the aura economy.
 *
 * Aura *earned* is derived purely from completion history (see [com.inkhabits.util.Economy])
 * so it can never be farmed by toggling completions on/off. This row only persists what
 * can't be recomputed: total aura spent, and the current totem inventory.
 *
 *   balance = Economy.earnedAura(history) - [auraSpent]
 */
@Entity(tableName = "economy")
data class EconomyState(
    @PrimaryKey val id: Long = 1,
    /** Total aura ever spent on totems. */
    val auraSpent: Long = 0,
    /** Habit totems currently owned (bought minus auto-consumed). */
    val habitTotems: Int = 0,
    /** Identity totems currently owned (bought minus auto-consumed). */
    val identityTotems: Int = 0
)
