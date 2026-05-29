package com.inkhabits.ui.widget

import com.inkhabits.R

/**
 * Minimal line icons (Lucide, ISC-licensed) for identities/habits. Stored by KEY
 * on [com.inkhabits.data.entity.IdentityGoal.icon]; rendered as tintable vectors.
 */
object HabitIcons {

    const val DEFAULT = "star"

    /** Picker order — common habits first. */
    val keys = listOf(
        "water", "hydrate", "eat", "apple", "coffee",
        "run", "workout", "bike",
        "read", "write", "music", "focus", "meditate",
        "health", "sleep", "morning", "night",
        "nature", "grow", "goal", "star"
    )

    private val res = mapOf(
        "water" to R.drawable.ic_hb_water,
        "hydrate" to R.drawable.ic_hb_hydrate,
        "eat" to R.drawable.ic_hb_eat,
        "apple" to R.drawable.ic_hb_apple,
        "coffee" to R.drawable.ic_hb_coffee,
        "run" to R.drawable.ic_hb_run,
        "workout" to R.drawable.ic_hb_workout,
        "bike" to R.drawable.ic_hb_bike,
        "read" to R.drawable.ic_hb_read,
        "write" to R.drawable.ic_hb_write,
        "music" to R.drawable.ic_hb_music,
        "focus" to R.drawable.ic_hb_focus,
        "meditate" to R.drawable.ic_hb_meditate,
        "health" to R.drawable.ic_hb_health,
        "sleep" to R.drawable.ic_hb_sleep,
        "morning" to R.drawable.ic_hb_morning,
        "night" to R.drawable.ic_hb_night,
        "nature" to R.drawable.ic_hb_nature,
        "grow" to R.drawable.ic_hb_grow,
        "goal" to R.drawable.ic_hb_goal,
        "star" to R.drawable.ic_hb_star,
    )

    /** Drawable resource for a key, falling back to the default (also covers legacy emoji values). */
    fun resFor(key: String?): Int = res[key] ?: res[DEFAULT]!!
}
