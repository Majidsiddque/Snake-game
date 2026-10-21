package com.example.snakegame.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.snakegame.data.*
import com.example.snakegame.managers.Difficulty
import com.example.snakegame.managers.GameMode
import com.example.snakegame.managers.HighScoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import com.example.snakegame.data.isTimed
import com.example.snakegame.data.isSpeedPowerUp

class GameViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private var isGameInProgress = false
    fun isGameInProgress(): Boolean = isGameInProgress && !_gameState.value.isGameOver

    fun newGame(difficulty: Difficulty, gameMode: GameMode) {
        val newObstacles = generateObstacles(difficulty)
        val snake1 = listOf(Position(5, 5))
        // Position snake 2 only if in 2-player mode
        val snake2 = if (gameMode == GameMode.TwoPlayer) listOf(Position(15, 15)) else emptyList()
        val allSnakes = snake1 + snake2

        val newFood = generateFood(allSnakes, newObstacles)
        val newLifespan = if (newFood.type.isTimed()) GameState.FOOD_LIFESPAN_TICKS else -1

        _gameState.value = GameState(
            obstacles = newObstacles,
            currentFood = newFood,
            foodLifespan = newLifespan,
            gameMode = gameMode,
            snakeBody = snake1, // Reset P1
            snakeBody2 = snake2, // Reset P2
            direction = Direction.Right, // Reset P1 dir
            direction2 = Direction.Left, // Reset P2 dir
            score = 0,
            score2 = 0,
            isGameOver = false,
            isGamePaused = false,
            activePowerUps = emptyMap()
        )
        isGameInProgress = true
    }

    fun pauseGame() {
        if (isGameInProgress) _gameState.value = _gameState.value.copy(isGamePaused = true)
    }

    fun resumeGame() {
        if (isGameInProgress) _gameState.value = _gameState.value.copy(isGamePaused = false)
    }

    private fun endGame() {
        // Save high scores for both players
        HighScoreManager.saveScore(application, _gameState.value.score)
        if (_gameState.value.gameMode == GameMode.TwoPlayer) {
            HighScoreManager.saveScore(application, _gameState.value.score2)
        }
        isGameInProgress = false
        _gameState.value = _gameState.value.copy(isGameOver = true)
    }

    // --- INPUTS for PLAYER 1 and 2 ---
    fun onDirectionChange(newDirection: Direction) {
        val current = _gameState.value
        val invalidMove = when (current.direction) {
            Direction.Up -> newDirection == Direction.Down
            Direction.Down -> newDirection == Direction.Up
            Direction.Left -> newDirection == Direction.Right
            Direction.Right -> newDirection == Direction.Left
        }
        if (!invalidMove) {
            _gameState.value = current.copy(direction = newDirection)
        }
    }

    fun onDirectionChange2(newDirection: Direction) {
        val current = _gameState.value
        if (current.gameMode == GameMode.Solo) return // Ignore P2 input in solo

        val invalidMove = when (current.direction2) {
            Direction.Up -> newDirection == Direction.Down
            Direction.Down -> newDirection == Direction.Up
            Direction.Left -> newDirection == Direction.Right
            Direction.Right -> newDirection == Direction.Left
        }
        if (!invalidMove) {
            _gameState.value = current.copy(direction2 = newDirection)
        }
    }

    fun updateGame() {
        if (_gameState.value.isGameOver || _gameState.value.isGamePaused) return

        processPowerUpTicks()
        processFoodLifespan()

        // Get the state *after* processing ticks
        var state = _gameState.value

        // --- PLAYER 1 UPDATE ---
        var (newHead1, newBody1) = getUpdatedSnake(state.snakeBody, state.direction)

        // --- PLAYER 2 UPDATE (only if 2-player) ---
        var newHead2 = Position(-1, -1) // Dummy position
        var newBody2 = state.snakeBody2
        if (state.gameMode == GameMode.TwoPlayer) {
            val (head2, body2) = getUpdatedSnake(state.snakeBody2, state.direction2)
            newHead2 = head2
            newBody2 = body2
        }

        // --- COLLISION CHECKS ---
        // P1 collides with walls, obstacles, self, OR P2
        if (checkCollision(newHead1, newBody1, state.obstacles, newBody2)) {
            endGame()
            return
        }
        // P2 collides with walls, obstacles, self, OR P1
        if (state.gameMode == GameMode.TwoPlayer && checkCollision(newHead2, newBody2, state.obstacles, newBody1)) {
            endGame()
            return
        }

        // --- FOOD EATING ---
        var foodEaten = false
        var p1Ate = newHead1 == state.currentFood.position
        var p2Ate = state.gameMode == GameMode.TwoPlayer && newHead2 == state.currentFood.position

        // Handle head-on collision for food (P1 wins tie)
        if (p1Ate) {
            foodEaten = true
            state = handleFoodEaten(state, newHead1, newBody1, player = 1)
        } else if (p2Ate) {
            foodEaten = true
            state = handleFoodEaten(state, newHead2, newBody2, player = 2)
        }

        // --- STATE UPDATE ---
        if (!foodEaten) {
            // No one ate, just move snakes
            _gameState.value = state.copy(
                snakeBody = newBody1,
                snakeBody2 = newBody2
            )
        } else {
            // Food was eaten, state was already updated.
            // Generate new food and set its timer
            val allSnakes = state.snakeBody + state.snakeBody2
            val newFood = generateFood(allSnakes, state.obstacles)
            val newLifespan = if (newFood.type.isTimed()) GameState.FOOD_LIFESPAN_TICKS else -1

            _gameState.value = state.copy(
                currentFood = newFood,
                foodLifespan = newLifespan
            )
        }
    }

    // Helper to get next snake position
    private fun getUpdatedSnake(body: List<Position>, dir: Direction): Pair<Position, List<Position>> {
        val newHead = getNextPosition(body.first(), dir)
        val newBody = listOf(newHead) + body.dropLast(1)
        return Pair(newHead, newBody)
    }

    private fun handleFoodEaten(state: GameState, newHead: Position, newBody: List<Position>, player: Int): GameState {
        val foodType = state.currentFood.type
        val scoreIncrease = if (state.activePowerUps.containsKey(FoodType.DOUBLE_POINTS)) 2 else 1
        val currentPowerUps = state.activePowerUps.toMutableMap()

        var snakeBodyP1 = if (player == 1) listOf(newHead) + state.snakeBody else state.snakeBody
        var snakeBodyP2 = if (player == 2) listOf(newHead) + state.snakeBody2 else state.snakeBody2
        var newObstacles = state.obstacles
        var newGameSpeed = state.gameSpeedDelay

        when (foodType) {
            FoodType.NORMAL -> { /* Just grow (handled by default) */ }
            FoodType.SPEED_BOOST -> {
                newGameSpeed = 50L // Fast
                currentPowerUps[FoodType.SPEED_BOOST] = GameState.POWER_UP_DURATION
            }
            FoodType.SLOW_DOWN -> {
                newGameSpeed = 350L // Slow
                currentPowerUps[FoodType.SLOW_DOWN] = GameState.POWER_UP_DURATION
            }
            FoodType.GHOST -> {
                currentPowerUps[FoodType.GHOST] = GameState.POWER_UP_DURATION
            }
            FoodType.SHRINK -> {
                if (player == 1) {
                    snakeBodyP1 = listOf(newHead) + state.snakeBody.dropLast(min(3, state.snakeBody.size - 1))
                } else {
                    snakeBodyP2 = listOf(newHead) + state.snakeBody2.dropLast(min(3, state.snakeBody2.size - 1))
                }
            }
            FoodType.BOMB -> {
                newObstacles = state.obstacles.filterNot {
                    abs(it.x - newHead.x) <= 2 && abs(it.y - newHead.y) <= 2
                }
            }
            FoodType.DOUBLE_POINTS -> {
                currentPowerUps[FoodType.DOUBLE_POINTS] = GameState.POWER_UP_DURATION
            }
        }

        return state.copy(
            snakeBody = snakeBodyP1,
            snakeBody2 = snakeBodyP2,
            score = if (player == 1) state.score + scoreIncrease else state.score,
            score2 = if (player == 2) state.score2 + scoreIncrease else state.score2,
            activePowerUps = currentPowerUps,
            gameSpeedDelay = newGameSpeed,
            obstacles = newObstacles
        )
    }

    // --- UPDATED HELPER FUNCTIONS ---

    private fun processFoodLifespan() {
        val state = _gameState.value
        if (state.foodLifespan > 0) {
            val newLifespan = state.foodLifespan - 1
            if (newLifespan == 0) {
                // Timer expired! Replace food
                val allSnakes = state.snakeBody + state.snakeBody2
                _gameState.value = state.copy(
                    currentFood = generateFood(allSnakes, state.obstacles, forceNormal = true),
                    foodLifespan = -1
                )
            } else {
                _gameState.value = state.copy(foodLifespan = newLifespan)
            }
        }
    }

    private fun processPowerUpTicks() {
        val state = _gameState.value
        if (state.activePowerUps.isEmpty()) return
        val updatedPowerUps = mutableMapOf<FoodType, Int>()
        var newGameSpeed = state.gameSpeedDelay
        for ((powerUp, duration) in state.activePowerUps) {
            if (duration - 1 > 0) {
                updatedPowerUps[powerUp] = duration - 1
            } else {
                if (powerUp.isSpeedPowerUp()) newGameSpeed = GameState.DEFAULT_SPEED
            }
        }
        _gameState.value = state.copy(activePowerUps = updatedPowerUps, gameSpeedDelay = newGameSpeed)
    }

    // Now checks against other snake
    private fun checkCollision(
        head: Position,
        ownBody: List<Position>,
        obstacles: List<Position>,
        otherSnakeBody: List<Position>
    ): Boolean {
        if (_gameState.value.activePowerUps.containsKey(FoodType.GHOST)) return false

        if (head.x !in 0 until GameState.BOARD_SIZE || head.y !in 0 until GameState.BOARD_SIZE) return true // Wall
        if (ownBody.drop(1).contains(head)) return true // Self
        if (obstacles.contains(head)) return true // Obstacle
        if (otherSnakeBody.isNotEmpty() && otherSnakeBody.contains(head)) return true // Other snake

        return false
    }

    private fun generateFood(
        snakeBodies: List<Position>, // Combined list of all snake parts
        obstacles: List<Position>,
        forceNormal: Boolean = false
    ): Food {
        val foodType = if (forceNormal || Random.nextInt(100) >= 20) {
            FoodType.NORMAL
        } else {
            FoodType.values().filter { it != FoodType.NORMAL }.random()
        }
        var newFoodPosition: Position
        do {
            newFoodPosition = Position(Random.nextInt(GameState.BOARD_SIZE), Random.nextInt(GameState.BOARD_SIZE))
        } while (snakeBodies.contains(newFoodPosition) || obstacles.contains(newFoodPosition))

        return Food(newFoodPosition, foodType)
    }

    private fun getNextPosition(head: Position, direction: Direction): Position {
        return when (direction) {
            Direction.Up -> Position(head.x, head.y - 1)
            Direction.Down -> Position(head.x, head.y + 1)
            Direction.Left -> Position(head.x - 1, head.y)
            Direction.Right -> Position(head.x + 1, head.y)
        }
    }

    private fun generateObstacles(difficulty: Difficulty): List<Position> {
        val newObstacles = mutableListOf<Position>()
        val obstacleCount = when (difficulty) {
            Difficulty.Easy -> 0
            Difficulty.Medium -> 10
            Difficulty.Hard -> 25
        }
        // Safe zones for both players
        val safeZone = listOf(
            Position(5, 5), Position(6, 5), Position(7, 5), // P1 start
            Position(15, 15), Position(14, 15), Position(13, 15) // P2 start
        )
        repeat(obstacleCount) {
            var obstaclePosition: Position
            do {
                obstaclePosition = Position(Random.nextInt(GameState.BOARD_SIZE), Random.nextInt(GameState.BOARD_SIZE))
            } while (safeZone.contains(obstaclePosition))
            newObstacles.add(obstaclePosition)
        }
        return newObstacles
    }
}

// --- ADD THESE HELPER FUNCTIONS AT THE BOTTOM OF Food.kt ---
// Open data/Food.kt and add these outside the class/enum

fun FoodType.isTimed(): Boolean {
    return this == FoodType.SPEED_BOOST || this == FoodType.SLOW_DOWN
}

fun FoodType.isSpeedPowerUp(): Boolean {
    return this == FoodType.SPEED_BOOST || this == FoodType.SLOW_DOWN
}
