package com.example.game

import androidx.compose.ui.graphics.Color

data class ThemeStyle(
    val name: String,
    val skyTop: Color,
    val skyBottom: Color,
    val roadColor: Color,
    val roadEdgeColor: Color,
    val railColor: Color,
    val gridColor: Color,
    val fogColor: Color,
    val accentColor: Color
)

enum class CameraMode {
    THIRD_PERSON,
    FIRST_PERSON,
    FRONT_PERSON
}

enum class ObstacleType {
    MOVING_BLOCK,
    SPINNING_BAR,
    SPIKE_ROW,
    SWINGING_HAMMER,
    LASER_BARRIER,
    FALLING_TILE,
    MOVING_WALL,
    CRUSHING_PISTON,
    ROLLING_BOULDER
}

data class Obstacle(
    val id: Int,
    val type: ObstacleType,
    var position: Vec3,
    val initialPosition: Vec3 = position,
    val size: Vec3 = Vec3(1.5f, 1.5f, 1.5f),
    val speed: Float = 2.0f,
    val phase: Float = 0f,
    val amplitude: Float = 3.0f,
    var currentAngle: Float = 0f,
    var fallVelocity: Float = 0f,
    var isSteppedOn: Boolean = false,
    var isActive: Boolean = true
)

enum class CollectibleType {
    COIN,
    GEM,
    SHIELD,
    MAGNET
}

data class Collectible(
    val id: Int,
    val type: CollectibleType,
    val position: Vec3,
    var collected: Boolean = false,
    val value: Int = when (type) {
        CollectibleType.COIN -> 10
        CollectibleType.GEM -> 50
        CollectibleType.SHIELD -> 20
        CollectibleType.MAGNET -> 20
    }
)

enum class PadType {
    NONE,
    JUMP_PAD,
    SPEED_BOOST
}

data class TrackSegment(
    val index: Int,
    val startZ: Float,
    val endZ: Float,
    val startY: Float,
    val endY: Float,
    val width: Float = 7.0f,
    val centerX: Float = 0f,
    val isGap: Boolean = false,
    val padType: PadType = PadType.NONE,
    val padColor: Color = when (padType) {
        PadType.JUMP_PAD -> Color(0xFFFF9800)
        PadType.SPEED_BOOST -> Color(0xFF00E676)
        PadType.NONE -> Color.Transparent
    }
)

data class BallSkin(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val costCoins: Int
)

data class LevelData(
    val number: Int,
    val title: String,
    val theme: ThemeStyle,
    val baseSpeed: Float,
    val totalLengthZ: Float,
    val segments: List<TrackSegment>,
    val obstacles: List<Obstacle>,
    val collectibles: List<Collectible>,
    val targetScore3Stars: Int = 500
)

data class Particle(
    var position: Vec3,
    var velocity: Vec3,
    var color: Color,
    var size: Float,
    var alpha: Float = 1.0f,
    var life: Float = 1.0f,
    val maxLife: Float = 1.0f
)
