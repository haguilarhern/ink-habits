package com.inkhabits.util

import java.time.LocalDate

/**
 * Bundled motivational quotes (Atomic Habits flavored). One is chosen
 * deterministically per day so the app and widget agree.
 */
object Quotes {
    private val quotes = listOf(
        "You do not rise to the level of your goals. You fall to the level of your systems.",
        "Every action is a vote for the type of person you wish to become.",
        "Habits are the compound interest of self-improvement.",
        "You should be far more concerned with your current trajectory than your current results.",
        "Never miss twice. Missing once is an accident; missing twice is the start of a new habit.",
        "The goal is not to read a book, the goal is to become a reader.",
        "Small habits don't add up. They compound.",
        "Be the designer of your world and not merely the consumer of it.",
        "Every action you take is a vote for the person you wish to become.",
        "Success is the product of daily habits, not once-in-a-lifetime transformations.",
        "What is the smallest step I can take toward who I want to be today?",
        "Make it obvious. Make it attractive. Make it easy. Make it satisfying.",
        "The most effective way to change your habits is to focus on who you wish to become.",
        "Don't break the chain.",
        "You don't have to be the victim of your environment. You can be the architect of it."
    )

    fun forToday(date: LocalDate = LocalDate.now()): String {
        val idx = (date.toEpochDay() % quotes.size).toInt()
        return quotes[if (idx < 0) idx + quotes.size else idx]
    }
}
