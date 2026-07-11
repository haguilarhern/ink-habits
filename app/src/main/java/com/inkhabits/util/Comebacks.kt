package com.inkhabits.util

/**
 * Short, warm "get back on it" lines — the voice of a friend or coach in your corner.
 * Used by the streak-protected notification when a freeze saves a streak, to nudge the
 * user back rather than just report the save. Kept short and sweet so they read well in
 * a single notification line.
 */
object Comebacks {
    private val lines = listOf(
        "Everyone slips. Champions just start again.",
        "One missed day doesn't erase you — get back up.",
        "I've got your back. Now go show up today.",
        "This is the comeback, not the collapse.",
        "You're one small step away from being back.",
        "Proud of how far you've come. Keep going.",
        "Missing once is human. Twice is a habit — not today.",
        "Dust yourself off. You've still got this.",
        "The streak's alive — now let's get you moving.",
        "A stumble isn't a fall. Keep walking.",
        "Small step today. That's all it takes.",
        "Your future self is counting on this one.",
        "Back on track starts with a single check.",
        "You didn't come this far to stop now.",
        "Rest happens. Quitting doesn't. Let's go.",
        "Show up today and the momentum returns.",
        "Great runs have off days. Restart the run.",
        "You're still in this. Prove it today.",
        "Consistency beats perfection — get back to it.",
        "The bounce-back is the best part.",
        "Today's the day you keep the promise.",
        "Winners are just people who restarted.",
        "You saved the streak — now honor it.",
        "Don't overthink it. Just begin again.",
        "Your habits missed you. Welcome back.",
        "One good choice restarts everything.",
        "Keep the chain alive — you're so close.",
        "Fall down seven, get up eight."
    )

    /** A random encouraging line. */
    fun random(): String = lines.random()
}
