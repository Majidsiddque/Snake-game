package com.example.snakegame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

@Composable
fun MainMenuScreen(
    onNewGame: () -> Unit,
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
            text = "Snake_Game", // <-- CHANGED THIS LINE
            fontSize = 50.sp,     // <-- CHANGED THIS LINE (Optional, but recommended)
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00C853)
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNewGame, modifier = Modifier.width(200.dp)) { Text("New Game") }
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
