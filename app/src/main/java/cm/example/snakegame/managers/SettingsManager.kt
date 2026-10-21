package com.example.snakegame.managers

import android.content.Context

// Enum to navigate between different app screens
enum class Screen {
    MainMenu, Game, Difficulty, Settings, HighScores
}

// Enum for game difficulty levels
enum class Difficulty {
    Easy, Medium, Hard
}

enum class GameMode {
    Solo,
    TwoPlayer
}
// Object to manage saving and loading user preferences
object SettingsManager {
    private const val PREFS_NAME = "SnakeGamePrefs"
    private const val SOUND_KEY = "sound_enabled"
    private const val DIFFICULTY_KEY = "difficulty"

    fun isSoundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SOUND_KEY, true) // Sound is on by default
    }

    fun setSoundEnabled(context: Context, isEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(SOUND_KEY, isEnabled).apply()
    }

    fun getDifficulty(context: Context): Difficulty {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val difficultyName = prefs.getString(DIFFICULTY_KEY, Difficulty.Easy.name)
        return Difficulty.valueOf(difficultyName ?: Difficulty.Easy.name)
    }

    fun setDifficulty(context: Context, difficulty: Difficulty) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(DIFFICULTY_KEY, difficulty.name).apply()
    }
}