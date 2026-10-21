package com.example.snakegame.ui

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snakegame.R
import com.example.snakegame.data.*
import com.example.snakegame.game.GameViewModel
import com.example.snakegame.managers.*
import com.example.snakegame.ui.screens.*
import com.example.snakegame.ui.theme.SnakeGameTheme
import kotlinx.coroutines.delay
import com.example.snakegame.data.isTimed
import com.example.snakegame.data.isSpeedPowerUp
import androidx.compose.runtime.snapshotFlow

class SnakeGame : ComponentActivity() {
    private val viewModel by viewModels<GameViewModel>()
    private var soundPool: SoundPool? = null
    private var eatSoundId: Int = 0
    private var gameOverSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ... (Your SoundPool setup code remains exactly the same) ...
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
        eatSoundId = soundPool!!.load(this, R.raw.eat, 1)
        gameOverSoundId = soundPool!!.load(this, R.raw.game_over1, 1)


        setContent {
            SnakeGameTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    var currentScreen by remember { mutableStateOf(Screen.MainMenu) }
                    val context = LocalContext.current
                    val gameState by viewModel.gameState.collectAsState()

                    // --- Game Loop ---
                    LaunchedEffect(Unit) {
                        while (true) {
                            val currentState = viewModel.gameState.value
                            if (!currentState.isGameOver && !currentState.isGamePaused) {
                                viewModel.updateGame()
                                delay(currentState.gameSpeedDelay)
                            } else {
                                delay(100)
                            }
                        }
                    }

                    // --- Sound Effects ---
                    LaunchedEffect(gameState.isGameOver) {
                        if (gameState.isGameOver && SettingsManager.isSoundEnabled(context)) {
                            soundPool?.play(gameOverSoundId, 1f, 1f, 0, 0, 1f)
                        }
                    }
                    LaunchedEffect(Unit) {
                        snapshotFlow { gameState.score + gameState.score2 }
                            .collect { totalScore ->
                                if (totalScore > 0 && SettingsManager.isSoundEnabled(context)) {
                                    soundPool?.play(eatSoundId, 1f, 1f, 0, 0, 1f)
                                }
                            }
                    }


                    // --- Screen Navigator ---
                    when (currentScreen) {
                        Screen.MainMenu -> MainMenuScreen(
                            onNewGame = {
                                val difficulty = SettingsManager.getDifficulty(context)
                                viewModel.newGame(difficulty, GameMode.Solo) // Default to Solo
                                currentScreen = Screen.Game
                            },
                            // --- NEW 2-PLAYER BUTTON ---
                            onNewGame2Player = {
                                val difficulty = SettingsManager.getDifficulty(context)
                                viewModel.newGame(difficulty, GameMode.TwoPlayer) // Start 2-Player
                                currentScreen = Screen.Game
                            },
                            onContinue = {
                                viewModel.resumeGame()
                                currentScreen = Screen.Game
                            },
                            onDifficulty = { currentScreen = Screen.Difficulty },
                            onSettings = { currentScreen = Screen.Settings },
                            onHighScores = { currentScreen = Screen.HighScores },
                            isContinueEnabled = viewModel.isGameInProgress()
                        )
                        Screen.Game -> SnakeGameScreen(
                            gameState = gameState,
                            onDirectionChange = { viewModel.onDirectionChange(it) },
                            onDirectionChange2 = { viewModel.onDirectionChange2(it) }, // Pass P2 controls
                            onPause = {
                                viewModel.pauseGame()
                                currentScreen = Screen.MainMenu
                            },
                            onResetGame = {
                                val difficulty = SettingsManager.getDifficulty(context)
                                // Reset with the same game mode
                                viewModel.newGame(difficulty, gameState.gameMode)
                            }
                        )
                        Screen.Difficulty -> DifficultyScreen(
                            onDifficultySelected = {
                                SettingsManager.setDifficulty(context, it)
                                currentScreen = Screen.MainMenu
                            },
                            onBack = { currentScreen = Screen.MainMenu }
                        )
                        Screen.Settings -> {
                            var isSoundOn by remember { mutableStateOf(SettingsManager.isSoundEnabled(context)) }
                            SettingsScreen(
                                isSoundOn = isSoundOn,
                                onSoundToggle = {
                                    isSoundOn = it
                                    SettingsManager.setSoundEnabled(context, it)
                                },
                                onBack = { currentScreen = Screen.MainMenu }
                            )
                        }
                        Screen.HighScores -> HighScoresScreen(
                            scores = HighScoreManager.getHighScores(context),
                            onBack = { currentScreen = Screen.MainMenu }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }
}

// --- UPDATED UI COMPOSABLES ---

@Composable
fun SnakeGameScreen(
    gameState: GameState,
    onDirectionChange: (Direction) -> Unit,
    onDirectionChange2: (Direction) -> Unit, // New input
    onPause: () -> Unit,
    onResetGame: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- SCOREBOARD ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // P1 Score
            Text(
                "P1: ${gameState.score}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00C853) // P1 Color
            )

            // P2 Score
            if (gameState.gameMode == GameMode.TwoPlayer) {
                Text(
                    "P2: ${gameState.score2}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D47A1) // P2 Color
                )
            }

            if (!gameState.isGameOver) {
                Button(onClick = onPause) { Text("Pause") }
            }
        }

        // --- TIMERS ROW ---
        // We've moved the timers to their own row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- FOOD TIMER (Time to eat) ---
            if (gameState.foodLifespan > 0) {
                val remainingSeconds = (gameState.foodLifespan * GameState.DEFAULT_SPEED) / 1000f
                Text(
                    text = "Food: ${"%.1f".format(remainingSeconds)}s", // e.g., "Food: 9.8s"
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow
                )
            }

            // --- ACTIVE POWER-UP TIMER (Time remaining) ---
            if (gameState.activePowerUps.isNotEmpty()) {
                // Get the first active power-up to display
                val (powerUp, duration) = gameState.activePowerUps.entries.first()

                // Calculate time based on default speed for a consistent real-world timer
                val remainingSeconds = (duration * GameState.DEFAULT_SPEED) / 1000f

                val powerUpName = when (powerUp) {
                    FoodType.GHOST -> "Ghost"
                    FoodType.SPEED_BOOST -> "Speed"
                    FoodType.SLOW_DOWN -> "Slow"
                    FoodType.DOUBLE_POINTS -> "2x Pts"
                    else -> ""
                }

                if (powerUpName.isNotEmpty()) {
                    Text(
                        text = "$powerUpName: ${"%.1f".format(remainingSeconds)}s", // e.g., "Ghost: 9.8s"
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Cyan // Use a different color to distinguish
                    )
                }
            }
        }

        Board(gameState = gameState)

        // --- CONTROLS / GAME OVER ---
        if (gameState.isGameOver) {
            val highScores = HighScoreManager.getHighScores(LocalContext.current)
            GameOverDialog(
                score = gameState.score,
                score2 = gameState.score2,
                gameMode = gameState.gameMode,
                highScores = highScores,
                onResetGame = onResetGame
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                Controls(onDirectionChange, Color(0xFF00C853)) // P1 Controls

                if (gameState.gameMode == GameMode.TwoPlayer) {
                    Controls(onDirectionChange2, Color(0xFF0D47A1)) // P2 Controls
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    score2: Int,
    gameMode: GameMode,
    highScores: List<Int>,
    onResetGame: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Game Over!", fontSize = 32.sp, color = Color.Red, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if(gameMode == GameMode.Solo) {
            Text("Your Score: $score", fontSize = 24.sp, color = Color.White)
        } else {
            Text("P1 Score: $score", fontSize = 24.sp, color = Color(0xFF00C853))
            Text("P2 Score: $score2", fontSize = 24.sp, color = Color(0xFF0D47A1))
        }

        Spacer(Modifier.height(8.dp))
        Text("High Scores: ${highScores.joinToString(", ")}", fontSize = 20.sp, color = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onResetGame) { Text("Play Again") }
    }
}

@Composable
fun Board(gameState: GameState) {
    var boxWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .background(Color(0xFF2c2c2c))
            .border(2.dp, Color.LightGray)
            .onSizeChanged { size: IntSize ->
                boxWidthPx = size.width
            }
    ) {
        if (boxWidthPx > 0) {
            val cellSizeDp = with(density) { (boxWidthPx.toFloat() / GameState.BOARD_SIZE).toDp() }

            // ... (Obstacles and Food drawing code is exactly the same as before) ...
            gameState.obstacles.forEach { obstacle ->
                Box(modifier = Modifier.offset(x = cellSizeDp * obstacle.x, y = cellSizeDp * obstacle.y).size(cellSizeDp).background(Color.Gray, RoundedCornerShape(2.dp)))
            }
            val food = gameState.currentFood
            var foodColor = when (food.type) {
                FoodType.NORMAL -> Color.Red
                FoodType.SPEED_BOOST -> Color.Cyan
                FoodType.SLOW_DOWN -> Color(0xFFFFA500) // Orange
                FoodType.GHOST -> Color.White
                FoodType.SHRINK -> Color.Yellow
                FoodType.BOMB -> Color(0xFF404040)
                FoodType.DOUBLE_POINTS -> Color.Magenta
            }
            val isTimedFood = food.type.isTimed()
            if (isTimedFood && gameState.foodLifespan < (GameState.FOOD_LIFESPAN_TICKS / 2)) {
                if (gameState.foodLifespan % 4 < 2) { foodColor = Color.Transparent }
            }
            Box(modifier = Modifier.offset(x = cellSizeDp * food.position.x, y = cellSizeDp * food.position.y).size(cellSizeDp).background(foodColor, CircleShape).border(width = if (food.type == FoodType.BOMB) 1.dp else 0.dp, color = Color.White, shape = CircleShape))

            // --- P1 SNAKE (GREEN) ---
            val (headColor1, bodyColor1) = getSnakeColors(gameState.activePowerUps, Color(0xFF00C853), Color(0xFF008E3C))
            gameState.snakeBody.forEachIndexed { index, part ->
                Box(
                    modifier = Modifier
                        .offset(x = cellSizeDp * part.x, y = cellSizeDp * part.y)
                        .size(cellSizeDp)
                        .background(
                            color = if (index == 0) headColor1 else bodyColor1,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }

            // --- P2 SNAKE (BLUE) ---
            if (gameState.gameMode == GameMode.TwoPlayer) {
                val (headColor2, bodyColor2) = getSnakeColors(gameState.activePowerUps, Color(0xFF0D47A1), Color(0xFF1976D2))
                gameState.snakeBody2.forEachIndexed { index, part ->
                    Box(
                        modifier = Modifier
                            .offset(x = cellSizeDp * part.x, y = cellSizeDp * part.y)
                            .size(cellSizeDp)
                            .background(
                                color = if (index == 0) headColor2 else bodyColor2,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

// Helper to de-duplicate snake color logic
@Composable
private fun getSnakeColors(activePowerUps: Map<FoodType, Int>, defaultHead: Color, defaultBody: Color): Pair<Color, Color> {
    val headColor = when {
        activePowerUps.containsKey(FoodType.GHOST) -> Color.White.copy(alpha = 0.7f)
        activePowerUps.containsKey(FoodType.DOUBLE_POINTS) -> Color.Magenta
        activePowerUps.containsKey(FoodType.SPEED_BOOST) -> Color.Cyan
        else -> defaultHead
    }
    val bodyColor = if (activePowerUps.containsKey(FoodType.GHOST)) {
        Color.White.copy(alpha = 0.5f)
    } else {
        defaultBody
    }
    return Pair(headColor, bodyColor)
}


@Composable
fun Controls(onDirectionChange: (Direction) -> Unit, color: Color) {
    // Smaller controls to fit two
    val buttonSize = 48.dp
    val spacerSize = 52.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { onDirectionChange(Direction.Up) }, modifier = Modifier.size(buttonSize), colors = ButtonDefaults.buttonColors(containerColor = color)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
        }
        Row {
            Button(onClick = { onDirectionChange(Direction.Left) }, modifier = Modifier.size(buttonSize), colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
            }
            Spacer(Modifier.width(spacerSize))
            Button(onClick = { onDirectionChange(Direction.Right) }, modifier = Modifier.size(buttonSize), colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
            }
        }
        Button(onClick = { onDirectionChange(Direction.Down) }, modifier = Modifier.size(buttonSize), colors = ButtonDefaults.buttonColors(containerColor = color)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
        }
    }
}

// --- Finally, update MainMenuScreen.kt ---
// Open ui/screens/MainMenuScreen.kt and replace its composable
@Composable
fun MainMenuScreen(
    onNewGame: () -> Unit,
    onNewGame2Player: () -> Unit, // Add this
    onContinue: () -> Unit,
    onDifficulty: () -> Unit,
    onSettings: () -> Unit,
    onHighScores: () -> Unit,
    isContinueEnabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Snake Game by unaib",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00C853)
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNewGame, modifier = Modifier.width(200.dp)) { Text("1 Player") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNewGame2Player, modifier = Modifier.width(200.dp)) { Text("2 Player") } // Add this
        Spacer(Modifier.height(16.dp))
        Button(onClick = onContinue, enabled = isContinueEnabled, modifier = Modifier.width(200.dp)) { Text("Continue") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDifficulty, modifier = Modifier.width(200.dp)) { Text("Difficulty") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSettings, modifier = Modifier.width(200.dp)) { Text("Settings") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onHighScores, modifier = Modifier.width(200.dp)) { Text("High Scores") }
    }
}