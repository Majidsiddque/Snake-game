package com.example.snakegame.managers

import android.content.Context

object HighScoreManager {
    private const val PREFS_NAME = "SnakeGamePrefs"
    private const val HIGH_SCORES_KEY = "high_scores"

    // Returns a sorted list of top scores (up to 2)
    fun getHighScores(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(HIGH_SCORES_KEY, "")
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.map { it.toInt() }
            ?.sortedDescending()
            ?: emptyList()
    }

    // Adds a new score and keeps the list sorted and trimmed to the top 2
    fun saveScore(context: Context, score: Int) {
        if (score == 0) return // Don't save zero scores
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentScores = getHighScores(context).toMutableList()
        currentScores.add(score)

        val newHighScores = currentScores.sortedDescending().take(2).joinToString(",")
        prefs.edit().putString(HIGH_SCORES_KEY, newHighScores).apply()
    }
}
