package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.math.sin

object LevelGenerator {

    val THEMES = listOf(
        ThemeStyle(
            name = "Cyber Neon",
            skyTop = Color(0xFF0A0518),
            skyBottom = Color(0xFF1E1035),
            roadColor = Color(0xFF161B2B),
            roadEdgeColor = Color(0xFF00E5FF),
            railColor = Color(0xFF00B0FF),
            gridColor = Color(0xFF26314A),
            fogColor = Color(0xFF120B20),
            accentColor = Color(0xFF00E5FF)
        ),
        ThemeStyle(
            name = "Synthwave Sunset",
            skyTop = Color(0xFF170224),
            skyBottom = Color(0xFF4A0E3C),
            roadColor = Color(0xFF240D30),
            roadEdgeColor = Color(0xFFFF2A85),
            railColor = Color(0xFFFF7700),
            gridColor = Color(0xFF421550),
            fogColor = Color(0xFF2B082E),
            accentColor = Color(0xFFFF007F)
        ),
        ThemeStyle(
            name = "Emerald Matrix",
            skyTop = Color(0xFF03140F),
            skyBottom = Color(0xFF082B20),
            roadColor = Color(0xFF0B1E17),
            roadEdgeColor = Color(0xFF00E676),
            railColor = Color(0xFF00BFA5),
            gridColor = Color(0xFF14382B),
            fogColor = Color(0xFF051711),
            accentColor = Color(0xFF69F0AE)
        ),
        ThemeStyle(
            name = "Magma Canyon",
            skyTop = Color(0xFF190703),
            skyBottom = Color(0xFF3D1306),
            roadColor = Color(0xFF22110D),
            roadEdgeColor = Color(0xFFFF5722),
            railColor = Color(0xFFFF9100),
            gridColor = Color(0xFF381C15),
            fogColor = Color(0xFF1C0A06),
            accentColor = Color(0xFFFFAB00)
        ),
        ThemeStyle(
            name = "Quantum Cosmic",
            skyTop = Color(0xFF040817),
            skyBottom = Color(0xFF0E1A38),
            roadColor = Color(0xFF10172D),
            roadEdgeColor = Color(0xFF7C4DFF),
            railColor = Color(0xFF651FFF),
            gridColor = Color(0xFF1E2B4E),
            fogColor = Color(0xFF070F22),
            accentColor = Color(0xFFB388FF)
        )
    )

    val SKINS = listOf(
        BallSkin("cyber_blue", "Cyber Sphere", Color(0xFF00E5FF), Color(0xFF0077C2), Color(0xFF80D8FF), 0),
        BallSkin("synth_pink", "Neon Pulse", Color(0xFFFF2A85), Color(0xFF9C27B0), Color(0xFFFF80AB), 100),
        BallSkin("fire_core", "Magma Core", Color(0xFFFF3D00), Color(0xFFFF9100), Color(0xFFFFAB40), 250),
        BallSkin("matrix_green", "Matrix Runner", Color(0xFF00E676), Color(0xFF00BFA5), Color(0xFFB9F6CA), 400),
        BallSkin("solar_gold", "Royal Gold", Color(0xFFFFD700), Color(0xFFFF8F00), Color(0xFFFFF176), 600),
        BallSkin("quantum_violet", "Void Walker", Color(0xFF7C4DFF), Color(0xFF311B92), Color(0xFFD1C4E9), 850),
        BallSkin("rainbow_light", "Prism Nova", Color(0xFF00E5FF), Color(0xFFFF2A85), Color(0xFFFFFFFF), 1200)
    )

    private val LEVEL_NAMES = listOf(
        "First Roll", "Jump Horizon", "Sliding Blocks", "Spinning Blades",
        "Elevated Ramps", "Spike Gauntlet", "Swinging Pendulums", "Laser Corridor",
        "Crumbling Road", "Skyway Dash", "Rolling Hazards", "Crusher Canyon",
        "Neon Slalom", "The Citadel", "Quantum Leap", "Laser Grid",
        "Hammer Fortress", "Vertigo Peaks", "Chaos Highway", "The Grandmaster"
    )

    fun getLevel(levelNumber: Int): LevelData {
        val clampedLevel = levelNumber.coerceIn(1, 20)
        val name = LEVEL_NAMES[clampedLevel - 1]
        val themeIndex = ((clampedLevel - 1) / 4) % THEMES.size
        val theme = THEMES[themeIndex]

        val baseSpeed = 16f + (clampedLevel - 1) * 0.95f
        val trackLength = 160f + (clampedLevel - 1) * 14f

        val segments = mutableListOf<TrackSegment>()
        val obstacles = mutableListOf<Obstacle>()
        val collectibles = mutableListOf<Collectible>()

        var currentZ = 0f
        var currentY = 0f
        var segIndex = 0
        var obstacleId = 0
        var collectibleId = 0

        // Track width narrows slightly on higher levels
        val trackWidth = (7.5f - (clampedLevel * 0.12f)).coerceAtLeast(4.2f)

        // Starting runway (safe zone)
        segments.add(
            TrackSegment(
                index = segIndex++,
                startZ = currentZ,
                endZ = currentZ + 25f,
                startY = currentY,
                endY = currentY,
                width = trackWidth + 1.5f,
                centerX = 0f
            )
        )
        currentZ += 25f

        // Initial welcome coins
        for (i in 1..3) {
            collectibles.add(
                Collectible(
                    id = collectibleId++,
                    type = CollectibleType.COIN,
                    position = Vec3(0f, currentY + 0.6f, currentZ - 18f + i * 5f)
                )
            )
        }

        while (currentZ < trackLength - 30f) {
            val stepType = (segIndex + clampedLevel) % 7
            val segLen = 18f + (segIndex % 3) * 6f

            when {
                // Gap with jump pad
                (clampedLevel >= 2 && stepType == 2 && currentZ > 35f) -> {
                    // Pre-gap launch pad
                    segments.add(
                        TrackSegment(
                            index = segIndex++,
                            startZ = currentZ,
                            endZ = currentZ + 12f,
                            startY = currentY,
                            endY = currentY + 0.8f,
                            width = trackWidth,
                            padType = PadType.JUMP_PAD
                        )
                    )
                    currentZ += 12f
                    currentY += 0.8f

                    // The Void Gap
                    val gapSize = (8f + (clampedLevel * 0.35f)).coerceAtMost(16f)
                    segments.add(
                        TrackSegment(
                            index = segIndex++,
                            startZ = currentZ,
                            endZ = currentZ + gapSize,
                            startY = currentY,
                            endY = currentY,
                            width = trackWidth,
                            isGap = true
                        )
                    )
                    // Mid-air collectible
                    collectibles.add(
                        Collectible(
                            id = collectibleId++,
                            type = if (segIndex % 2 == 0) CollectibleType.GEM else CollectibleType.COIN,
                            position = Vec3(0f, currentY + 3.0f, currentZ + gapSize * 0.5f)
                        )
                    )
                    currentZ += gapSize

                    // Landing runway
                    segments.add(
                        TrackSegment(
                            index = segIndex++,
                            startZ = currentZ,
                            endZ = currentZ + 16f,
                            startY = currentY,
                            endY = currentY,
                            width = trackWidth + 1.0f
                        )
                    )
                    currentZ += 16f
                }

                // Ramps up and down
                (clampedLevel >= 5 && stepType == 4) -> {
                    val rampElevation = 3.5f
                    // Ramp Up with speed pad
                    segments.add(
                        TrackSegment(
                            index = segIndex++,
                            startZ = currentZ,
                            endZ = currentZ + segLen,
                            startY = currentY,
                            endY = currentY + rampElevation,
                            width = trackWidth,
                            padType = PadType.SPEED_BOOST
                        )
                    )
                    currentZ += segLen
                    currentY += rampElevation

                    // Elevated flat segment
                    segments.add(
                        TrackSegment(
                            index = segIndex++,
                            startZ = currentZ,
                            endZ = currentZ + segLen,
                            startY = currentY,
                            endY = currentY,
                            width = trackWidth
                        )
                    )
                    // Ramp Down
                    currentZ += segLen
                    segments.add(
                        TrackSegment(
                            index = segIndex++,
                            startZ = currentZ,
                            endZ = currentZ + segLen,
                            startY = currentY,
                            endY = currentY - rampElevation,
                            width = trackWidth
                        )
                    )
                    currentZ += segLen
                    currentY -= rampElevation
                }

                else -> {
                    // Regular track segment
                    segments.add(
                        TrackSegment(
                            index = segIndex++,
                            startZ = currentZ,
                            endZ = currentZ + segLen,
                            startY = currentY,
                            endY = currentY,
                            width = trackWidth
                        )
                    )

                    // Add obstacles based on level progression
                    val obstacleZ = currentZ + segLen * 0.5f
                    addObstaclesForLevel(
                        clampedLevel,
                        obstacleId,
                        obstacles,
                        obstacleZ,
                        currentY,
                        trackWidth
                    )
                    obstacleId = obstacles.size

                    // Add collectibles
                    if (segIndex % 2 == 0) {
                        val sideX = if (segIndex % 4 == 0) -trackWidth * 0.3f else trackWidth * 0.3f
                        collectibles.add(
                            Collectible(
                                id = collectibleId++,
                                type = CollectibleType.COIN,
                                position = Vec3(sideX, currentY + 0.6f, obstacleZ - 4f)
                            )
                        )
                        collectibles.add(
                            Collectible(
                                id = collectibleId++,
                                type = if (segIndex % 6 == 0) CollectibleType.GEM else CollectibleType.COIN,
                                position = Vec3(-sideX, currentY + 0.6f, obstacleZ + 4f)
                            )
                        )
                    }

                    currentZ += segLen
                }
            }
        }

        // Finish Runway and Victory Arch
        segments.add(
            TrackSegment(
                index = segIndex++,
                startZ = currentZ,
                endZ = currentZ + 30f,
                startY = currentY,
                endY = currentY,
                width = trackWidth + 2.0f
            )
        )
        currentZ += 30f

        val finalLength = currentZ
        val targetScore = 200 + clampedLevel * 80

        return LevelData(
            number = clampedLevel,
            title = name,
            theme = theme,
            baseSpeed = baseSpeed,
            totalLengthZ = finalLength,
            segments = segments,
            obstacles = obstacles,
            collectibles = collectibles,
            targetScore3Stars = targetScore
        )
    }

    private fun addObstaclesForLevel(
        level: Int,
        startId: Int,
        obstacles: MutableList<Obstacle>,
        z: Float,
        y: Float,
        trackWidth: Float
    ) {
        var id = startId
        val maxAmp = (trackWidth * 0.38f).coerceAtLeast(1.2f)

        when {
            // Level 1: A few easy moving blocks
            level == 1 -> {
                if (z > 40f && z % 28f < 14f) {
                    obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.MOVING_BLOCK,
                            position = Vec3(0f, y + 0.8f, z),
                            size = Vec3(1.6f, 1.4f, 1.2f),
                            speed = 1.6f,
                            amplitude = maxAmp,
                            phase = z
                        )
                    )
                }
            }

            // Level 3 & 4: Moving blocks and spinning bars
            level in 2..4 -> {
                val opt = (z.toInt() / 20) % 2
                if (opt == 0) {
                    obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.MOVING_BLOCK,
                            position = Vec3(0f, y + 0.8f, z),
                            size = Vec3(1.8f, 1.5f, 1.2f),
                            speed = 2.2f + level * 0.2f,
                            amplitude = maxAmp,
                            phase = z
                        )
                    )
                } else if (level >= 4) {
                    obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.SPINNING_BAR,
                            position = Vec3(0f, y + 0.9f, z),
                            size = Vec3(trackWidth * 0.75f, 0.4f, 0.4f),
                            speed = 2.5f
                        )
                    )
                }
            }

            // Level 6 & 7: Spikes and swinging hammers
            level in 5..7 -> {
                val mod = (z.toInt() / 22) % 3
                when (mod) {
                    0 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.SPIKE_ROW,
                            position = Vec3(0f, y + 0.5f, z),
                            size = Vec3(trackWidth * 0.5f, 0.8f, 1.2f)
                        )
                    )
                    1 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.SWINGING_HAMMER,
                            position = Vec3(0f, y + 3.8f, z),
                            size = Vec3(1.8f, 1.8f, 1.2f),
                            speed = 2.8f,
                            amplitude = 1.1f
                        )
                    )
                    2 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.MOVING_BLOCK,
                            position = Vec3(0f, y + 0.8f, z),
                            size = Vec3(1.8f, 1.5f, 1.2f),
                            speed = 2.6f,
                            amplitude = maxAmp
                        )
                    )
                }
            }

            // Level 8 & 9: Laser barriers and falling platforms
            level in 8..10 -> {
                val mod = (z.toInt() / 24) % 4
                when (mod) {
                    0 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.LASER_BARRIER,
                            position = Vec3(0f, y + 0.8f, z),
                            size = Vec3(trackWidth * 0.8f, 1.4f, 0.4f),
                            speed = 2.4f
                        )
                    )
                    1 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.FALLING_TILE,
                            position = Vec3(0f, y + 0.1f, z),
                            size = Vec3(trackWidth * 0.85f, 0.4f, 8.0f)
                        )
                    )
                    2 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.SPINNING_BAR,
                            position = Vec3(0f, y + 0.9f, z),
                            size = Vec3(trackWidth * 0.8f, 0.4f, 0.4f),
                            speed = 3.0f
                        )
                    )
                    else -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.SWINGING_HAMMER,
                            position = Vec3(0f, y + 4.0f, z),
                            size = Vec3(1.8f, 1.8f, 1.2f),
                            speed = 3.0f,
                            amplitude = 1.2f
                        )
                    )
                }
            }

            // Level 11 to 14: Rolling boulders & crushing pistons & moving walls
            level in 11..14 -> {
                val mod = (z.toInt() / 25) % 4
                when (mod) {
                    0 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.ROLLING_BOULDER,
                            position = Vec3(0f, y + 1.0f, z),
                            size = Vec3(1.6f, 1.6f, 1.6f),
                            speed = 3.2f,
                            amplitude = maxAmp
                        )
                    )
                    1 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.CRUSHING_PISTON,
                            position = Vec3(0f, y + 3.0f, z),
                            size = Vec3(trackWidth * 0.6f, 1.8f, 1.4f),
                            speed = 3.5f
                        )
                    )
                    2 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.MOVING_WALL,
                            position = Vec3(0f, y + 1.2f, z),
                            size = Vec3(2.4f, 2.5f, 1.0f),
                            speed = 3.0f,
                            amplitude = maxAmp
                        )
                    )
                    else -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.LASER_BARRIER,
                            position = Vec3(0f, y + 0.8f, z),
                            size = Vec3(trackWidth * 0.85f, 1.4f, 0.4f),
                            speed = 3.0f
                        )
                    )
                }
            }

            // Level 15 to 20: Advanced Gauntlet combinations
            else -> {
                val mod = (z.toInt() / 22) % 6
                when (mod) {
                    0 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.SWINGING_HAMMER,
                            position = Vec3(0f, y + 4.0f, z),
                            size = Vec3(2.0f, 2.0f, 1.3f),
                            speed = 3.4f,
                            amplitude = 1.3f
                        )
                    )
                    1 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.CRUSHING_PISTON,
                            position = Vec3(0f, y + 3.0f, z),
                            size = Vec3(trackWidth * 0.7f, 2.0f, 1.4f),
                            speed = 4.0f
                        )
                    )
                    2 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.LASER_BARRIER,
                            position = Vec3(0f, y + 0.9f, z),
                            size = Vec3(trackWidth * 0.9f, 1.5f, 0.5f),
                            speed = 3.5f
                        )
                    )
                    3 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.SPINNING_BAR,
                            position = Vec3(0f, y + 1.0f, z),
                            size = Vec3(trackWidth * 0.85f, 0.45f, 0.45f),
                            speed = 3.8f
                        )
                    )
                    4 -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.FALLING_TILE,
                            position = Vec3(0f, y + 0.1f, z),
                            size = Vec3(trackWidth * 0.9f, 0.4f, 9.0f)
                        )
                    )
                    else -> obstacles.add(
                        Obstacle(
                            id = id++,
                            type = ObstacleType.MOVING_BLOCK,
                            position = Vec3(0f, y + 0.9f, z),
                            size = Vec3(2.2f, 1.8f, 1.4f),
                            speed = 3.5f,
                            amplitude = maxAmp
                        )
                    )
                }
            }
        }
    }
}
