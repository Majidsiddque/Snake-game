package com.example.snakegame.data

import com.example.snakegame.managers.GameMode

data class GameState(
    // Player 1
    val snakeBody: List<Position> = listOf(Position(5, 5)),
    val direction: Direction = Direction.Right,
    val score: Int = 0,

    // Player 2
    val snakeBody2: List<Position> = listOf(Position(15, 15)),
    val direction2: Direction = Direction.Left,
    val score2: Int = 0,

    // Shared State
    val obstacles: List<Position> = emptyList(),
    val currentFood: Food = Food(Position(10, 10), FoodType.NORMAL),
    val foodLifespan: Int = -1,
    val gameSpeedDelay: Long = DEFAULT_SPEED,
    val isGamePaused: Boolean = false,
    val isGameOver: Boolean = false,

    // Tracks the current game mode
    val gameMode: GameMode = GameMode.Solo,

    // Tracks active power-ups (now shared)
    val activePowerUps: Map<FoodType, Int> = emptyMap()
) {
    companion object {
        const val BOARD_SIZE = 20
        const val DEFAULT_SPEED = 200L
        const val POWER_UP_DURATION = 50
        const val FOOD_LIFESPAN_TICKS = 50
    }
}