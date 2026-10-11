package com.example.reeltracker

/**
 * Mood states. The mood depends on total Instagram + YouTube time today.
 * Change the minute limits below to match your own goals.
 */
object Moods {

    data class State(val upToMinutes: Int, val label: String, val message: String)

    val states = listOf(
        State(30, "Happy", "Great balance today. Keep it up!"),
        State(60, "Content", "Doing well. Stay mindful while scrolling."),
        State(90, "Neutral", "Getting a bit long. Consider a short break."),
        State(120, "Uneasy", "You've been scrolling quite a lot."),
        State(150, "Worried", "Time to take a break soon."),
        State(210, "Sad", "That's a lot of scrolling today."),
        State(Int.MAX_VALUE, "Crying", "Please step away and rest your eyes.")
    )

    fun indexFor(minutes: Long): Int =
        states.indexOfFirst { minutes < it.upToMinutes }
}
