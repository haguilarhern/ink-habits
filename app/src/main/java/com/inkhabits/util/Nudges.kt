package com.inkhabits.util

/**
 * Short, proactive "it's time — go" lines in a friend/coach voice, used to make the
 * scheduled habit reminders feel alive instead of repeating one static subtitle.
 * Kept short and sweet so they fit a single notification line. See also [Comebacks]
 * (the recovery voice used after a miss) and [Quotes] (the daily home-screen quote).
 */
object Nudges {
    private val lines = listOf(
        "Now's the moment. You've got this.",
        "Two minutes to start — that's all it takes.",
        "Future you will thank you for this one.",
        "Small step, big momentum. Let's go.",
        "Show up for yourself right now.",
        "This is a vote for who you're becoming.",
        "Don't think, just begin.",
        "One rep — that's the whole job right now.",
        "Make today count. Start now.",
        "The best time is right now.",
        "You always feel better after. Go.",
        "Keep the streak breathing — do it now.",
        "Progress is one action away.",
        "Be the person who follows through.",
        "A little effort now beats regret later.",
        "Let's keep the chain alive.",
        "You don't need motivation, just movement.",
        "Start small, but start now.",
        "This is your moment to shine.",
        "A little now saves a lot later.",
        "Show up. That's the win.",
        "One step closer to your best self.",
        "Consistency is calling — answer it.",
        "You've done harder things. Go.",
        "Momentum loves a quick start.",
        "Make it happen while it's fresh.",
        "Today's rep, right now.",
        "Tiny action, big identity. Let's move."
    )

    /** A random proactive reminder line. */
    fun random(): String = lines.random()
}
