package com.example.snakegame.data

// Defines all possible types of food/power-ups
enum class FoodType {
    NORMAL,
    SPEED_BOOST,
    SLOW_DOWN,
    GHOST,          // Combines Invincibility and Ghost Mode
    SHRINK,
    BOMB,
    DOUBLE_POINTS
}

// A new data class to hold the food's position and type
data class Food(
    val position: Position,
    val type: FoodType = FoodType.NORMAL
)

// ... (at the very bottom of the file)

fun FoodType.isTimed(): Boolean {
    // --- MODIFY THIS LINE ---
    return this != FoodType.NORMAL // This was: this == FoodType.SPEED_BOOST || this == FoodType.SLOW_DOWN
    // ------------------------
}

fun FoodType.isSpeedPowerUp(): Boolean {
    return this == FoodType.SPEED_BOOST || this == FoodType.SLOW_DOWN
}