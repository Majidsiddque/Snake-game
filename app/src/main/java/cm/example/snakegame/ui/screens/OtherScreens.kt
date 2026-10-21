package com.example.snakegame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snakegame.managers.Difficulty

@Composable
fun DifficultyScreen(onDifficultySelected: (Difficulty) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Difficulty", fontSize = 32.sp)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onDifficultySelected(Difficulty.Easy) }, modifier = Modifier.width(200.dp)) { Text("Easy") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onDifficultySelected(Difficulty.Medium) }, modifier = Modifier.width(200.dp)) { Text("Medium") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onDifficultySelected(Difficulty.Hard) }, modifier = Modifier.width(200.dp)) { Text("Hard") }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onBack) { Text("Back to Menu") }
    }
}

@Composable
fun SettingsScreen(isSoundOn: Boolean, onSoundToggle: (Boolean) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            fontSize = 32.sp,
            color = Color.White // Or any color you want
        )
        Spacer(Modifier.height(32.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Sound", fontSize = 24.sp,color = Color.White)
            Spacer(Modifier.width(16.dp))
            Switch(checked = isSoundOn, onCheckedChange = onSoundToggle)
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onBack) { Text("Back to Menu") }
    }
}

@Composable
fun HighScoresScreen(scores: List<Int>, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("High Scores", fontSize = 32.sp,color = Color.White)
        Spacer(Modifier.height(32.dp))
        if (scores.isNotEmpty()) {
            scores.forEachIndexed { index, score ->
                Text("${index + 1}. $score", fontSize = 24.sp,color = Color.White)
                Spacer(Modifier.height(8.dp))
            }
        } else {
            Text("No Scores Yet!", fontSize = 24.sp)
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onBack) { Text("Back to Menu") }
    }
}
