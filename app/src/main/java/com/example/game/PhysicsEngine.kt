package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class PhysicsEngine(
    val levelData: LevelData,
    private val soundManager: SoundManager,
    var sensitivity: Float = 1.0f
) {
    val ballRadius = 0.95f

    var ballPos = Vec3(0f, ballRadius + 0.1f, 2f)
    var ballVel = Vec3(0f, 0f, levelData.baseSpeed)
    var rollRotationX = 0f
    var rollRotationZ = 0f

    var isGrounded = true
    var isDead = false
    var isVictory = false
    var deathReason = ""

    var score = 0
    var coinsCollected = 0
    var speedMultiplier = 1.0f
    var speedBoostTimer = 0f
    var shieldActive = false
    var magnetActive = false
    var magnetTimer = 0f

    var screenShakeAmount = 0f

    val particles = mutableListOf<Particle>()

    private var targetX = 0f
    private var totalTime = 0f
    private var inputNudgeY = 0f

    fun handleDrag(deltaX: Float, deltaY: Float, screenWidth: Float, isCameraFront: Boolean = false) {
        if (isDead || isVictory) return
        val direction = if (isCameraFront) -1f else 1f
        val normalizedDeltaX = (deltaX / (screenWidth * 0.35f)) * 5.5f * sensitivity * direction
        targetX = (targetX + normalizedDeltaX).coerceIn(-4.2f, 4.2f)

        inputNudgeY = (-deltaY * 0.015f).coerceIn(-0.3f, 0.4f)
    }

    fun releaseTouch() {
        inputNudgeY = 0f
    }

    fun update(dt: Float) {
        if (dt <= 0f) return
        val clampedDt = dt.coerceIn(0.001f, 0.05f)
        totalTime += clampedDt

        // Decay screen shake
        if (screenShakeAmount > 0f) {
            screenShakeAmount = (screenShakeAmount - clampedDt * 8f).coerceAtLeast(0f)
        }

        // Speed boost timer
        if (speedBoostTimer > 0f) {
            speedBoostTimer -= clampedDt
            if (speedBoostTimer <= 0f) {
                speedMultiplier = 1.0f
            }
        }

        // Magnet timer
        if (magnetTimer > 0f) {
            magnetTimer -= clampedDt
            if (magnetTimer <= 0f) magnetActive = false
        }

        // Update particles
        val particleIter = particles.iterator()
        while (particleIter.hasNext()) {
            val p = particleIter.next()
            p.life -= clampedDt
            p.position += p.velocity * clampedDt
            p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (p.life <= 0f) {
                particleIter.remove()
            }
        }

        if (isDead || isVictory) return

        // Forward and lateral movement
        val currentSpeed = (levelData.baseSpeed * speedMultiplier) * (1.0f + inputNudgeY * 0.3f)
        ballVel = Vec3(ballVel.x, ballVel.y, currentSpeed)

        // Smooth steering towards targetX
        val lateralError = targetX - ballPos.x
        val steeringForce = lateralError * 14f
        val newVx = (ballVel.x + steeringForce * clampedDt).coerceIn(-12f, 12f)
        ballVel = Vec3(newVx, ballVel.y, ballVel.z)

        // Find active track segment
        val currentSeg = findSegmentAt(ballPos.z)
        val trackY: Float
        val trackWidth: Float
        val hasTrack: Boolean

        if (currentSeg != null && !currentSeg.isGap) {
            val segLen = currentSeg.endZ - currentSeg.startZ
            val progress = if (segLen > 0.001f) ((ballPos.z - currentSeg.startZ) / segLen).coerceIn(0f, 1f) else 0f
            trackY = currentSeg.startY + progress * (currentSeg.endY - currentSeg.startY)
            trackWidth = currentSeg.width
            hasTrack = true
        } else {
            trackY = -100f
            trackWidth = 0f
            hasTrack = false
        }

        // Gravity
        val gravity = 32f
        if (!isGrounded || !hasTrack) {
            ballVel = Vec3(ballVel.x, ballVel.y - gravity * clampedDt, ballVel.z)
        }

        // Integrate position
        ballPos += ballVel * clampedDt

        // Rolling rotation
        val forwardDistance = ballVel.z * clampedDt
        rollRotationX += (forwardDistance / ballRadius) * 57.2958f // to degrees
        rollRotationZ += (ballVel.x * clampedDt / ballRadius) * 57.2958f

        // Track collision / Ground check
        if (hasTrack && abs(ballPos.x) <= trackWidth * 0.52f) {
            if (ballPos.y - ballRadius <= trackY + 0.1f && ballVel.y <= 0f) {
                // Landed on track
                if (!isGrounded && ballVel.y < -3f) {
                    spawnLandingDust(ballPos)
                    soundManager.vibrate(18)
                }
                ballPos = Vec3(ballPos.x, trackY + ballRadius, ballPos.z)
                ballVel = Vec3(ballVel.x, 0f, ballVel.z)
                isGrounded = true

                // Check pads
                if (currentSeg != null) {
                    when (currentSeg.padType) {
                        PadType.JUMP_PAD -> {
                            ballVel = Vec3(ballVel.x, 14.5f, ballVel.z)
                            isGrounded = false
                            soundManager.playJump()
                            screenShakeAmount = 0.35f
                            spawnBoostParticles(ballPos, Color(0xFFFF9800))
                        }
                        PadType.SPEED_BOOST -> {
                            speedMultiplier = 1.45f
                            speedBoostTimer = 2.2f
                            soundManager.playBoost()
                            screenShakeAmount = 0.25f
                            spawnBoostParticles(ballPos, Color(0xFF00E676))
                        }
                        PadType.NONE -> {}
                    }
                }
            }
        } else {
            // Ball is off track
            isGrounded = false
            if (ballPos.y < trackY - 8f || ballPos.y < -15f) {
                // Fallen into the abyss!
                triggerDeath("Fell into the abyss!")
                return
            }
        }

        // Animate obstacles and check collision
        updateObstacles(clampedDt)

        // Check collectibles
        updateCollectibles()

        // Check Victory
        if (ballPos.z >= levelData.totalLengthZ - 6f) {
            isVictory = true
            soundManager.playLevelComplete()
            spawnVictoryConfetti(ballPos)
        }
    }

    private fun findSegmentAt(z: Float): TrackSegment? {
        return levelData.segments.firstOrNull { z >= it.startZ && z <= it.endZ }
    }

    private fun updateObstacles(dt: Float) {
        val playerZ = ballPos.z

        for (obs in levelData.obstacles) {
            // Only update obstacles reasonably close to ball
            if (obs.position.z < playerZ - 10f || obs.position.z > playerZ + 90f) continue

            when (obs.type) {
                ObstacleType.MOVING_BLOCK, ObstacleType.MOVING_WALL -> {
                    val newX = sin(totalTime * obs.speed + obs.phase) * obs.amplitude
                    obs.position = Vec3(newX, obs.position.y, obs.position.z)
                }
                ObstacleType.SPINNING_BAR -> {
                    obs.currentAngle += obs.speed * dt * 180f
                }
                ObstacleType.SWINGING_HAMMER -> {
                    obs.currentAngle = sin(totalTime * obs.speed) * obs.amplitude
                    val swingX = sin(obs.currentAngle) * 3.5f
                    val swingY = obs.initialPosition.y - cos(obs.currentAngle) * 2.8f
                    obs.position = Vec3(swingX, swingY, obs.position.z)
                }
                ObstacleType.FALLING_TILE -> {
                    if (abs(playerZ - obs.position.z) < 6f) {
                        obs.isSteppedOn = true
                    }
                    if (obs.isSteppedOn) {
                        obs.fallVelocity += 30f * dt
                        obs.position = Vec3(
                            obs.position.x + (Random.nextFloat() - 0.5f) * 0.05f,
                            obs.position.y - obs.fallVelocity * dt,
                            obs.position.z
                        )
                    }
                }
                ObstacleType.CRUSHING_PISTON -> {
                    val cycle = (sin(totalTime * obs.speed) + 1f) * 0.5f // 0..1
                    val dropY = if (cycle > 0.65f) obs.initialPosition.y - 1.8f else obs.initialPosition.y
                    obs.position = Vec3(obs.position.x, dropY, obs.position.z)
                }
                ObstacleType.ROLLING_BOULDER -> {
                    val rollZ = obs.position.z - obs.speed * dt * 2.0f
                    val rollX = sin(totalTime * 2f) * obs.amplitude
                    obs.position = Vec3(rollX, obs.position.y, rollZ)
                }
                else -> {}
            }

            // Check collision with player ball
            if (checkCollision(obs)) {
                if (shieldActive) {
                    shieldActive = false
                    soundManager.playBoost()
                    screenShakeAmount = 0.5f
                    spawnExplosion(obs.position, Color(0xFF00E5FF))
                    obs.isActive = false
                } else {
                    triggerDeath("Collided with ${obs.type.name.replace("_", " ").lowercase()}!")
                    return
                }
            }
        }
    }

    private fun checkCollision(obs: Obstacle): Boolean {
        if (!obs.isActive) return false

        // Simple bounding checks based on obstacle type
        val dx = abs(ballPos.x - obs.position.x)
        val dy = abs(ballPos.y - obs.position.y)
        val dz = abs(ballPos.z - obs.position.z)

        return when (obs.type) {
            ObstacleType.MOVING_BLOCK, ObstacleType.MOVING_WALL, ObstacleType.CRUSHING_PISTON -> {
                dx < (obs.size.x * 0.5f + ballRadius * 0.8f) &&
                        dy < (obs.size.y * 0.5f + ballRadius * 0.8f) &&
                        dz < (obs.size.z * 0.5f + ballRadius * 0.8f)
            }
            ObstacleType.SPIKE_ROW -> {
                dx < (obs.size.x * 0.5f + ballRadius * 0.7f) &&
                        dy < (obs.size.y * 0.5f + ballRadius * 0.6f) &&
                        dz < (obs.size.z * 0.5f + ballRadius * 0.6f)
            }
            ObstacleType.SWINGING_HAMMER, ObstacleType.ROLLING_BOULDER -> {
                val dist = (ballPos - obs.position).length()
                dist < (ballRadius + obs.size.x * 0.5f)
            }
            ObstacleType.SPINNING_BAR -> {
                if (dz < 0.6f && dy < 0.6f) {
                    val barAngleRad = Math.toRadians(obs.currentAngle.toDouble()).toFloat()
                    val barCos = cos(barAngleRad)
                    val barSin = sin(barAngleRad)
                    val barHalf = obs.size.x * 0.5f
                    // Point on bar closest to ball
                    val proj = ((ballPos.x - obs.position.x) * barCos + (ballPos.y - obs.position.y) * barSin).coerceIn(-barHalf, barHalf)
                    val closeX = obs.position.x + proj * barCos
                    val closeY = obs.position.y + proj * barSin
                    val d = sqrt((ballPos.x - closeX) * (ballPos.x - closeX) + (ballPos.y - closeY) * (ballPos.y - closeY))
                    d < (ballRadius + 0.25f)
                } else false
            }
            ObstacleType.LASER_BARRIER -> {
                val isPulsingOn = sin(totalTime * obs.speed) > -0.15f
                if (isPulsingOn && dz < 0.45f && dy < 1.4f) {
                    dx < (obs.size.x * 0.5f)
                } else false
            }
            ObstacleType.FALLING_TILE -> {
                false // Falling tile makes player fall by removing ground
            }
        }
    }

    private fun updateCollectibles() {
        val magnetRange = if (magnetActive) 5.5f else 1.25f
        for (col in levelData.collectibles) {
            if (col.collected) continue
            val dist = (ballPos - col.position).length()
            if (dist < magnetRange) {
                col.collected = true
                score += col.value
                when (col.type) {
                    CollectibleType.COIN -> {
                        coinsCollected++
                        soundManager.playCoin()
                        spawnCoinSparkles(col.position, Color(0xFFFFD700))
                    }
                    CollectibleType.GEM -> {
                        coinsCollected += 3
                        soundManager.playCoin()
                        spawnCoinSparkles(col.position, Color(0xFF00E5FF))
                    }
                    CollectibleType.SHIELD -> {
                        shieldActive = true
                        soundManager.playBoost()
                        spawnCoinSparkles(col.position, Color(0xFF00B0FF))
                    }
                    CollectibleType.MAGNET -> {
                        magnetActive = true
                        magnetTimer = 8.0f
                        soundManager.playBoost()
                        spawnCoinSparkles(col.position, Color(0xFFFF4081))
                    }
                }
            }
        }
    }

    private fun triggerDeath(reason: String) {
        if (isDead) return
        isDead = true
        deathReason = reason
        screenShakeAmount = 0.65f
        soundManager.playCollision()
        spawnExplosion(ballPos, Color(0xFFFF5252))
    }

    private fun spawnLandingDust(pos: Vec3) {
        for (i in 0 until 12) {
            val angle = Random.nextFloat() * 2f * 3.14159f
            val speed = 1.5f + Random.nextFloat() * 2.5f
            particles.add(
                Particle(
                    position = Vec3(pos.x, pos.y - ballRadius * 0.9f, pos.z),
                    velocity = Vec3(cos(angle) * speed, 0.5f + Random.nextFloat(), sin(angle) * speed),
                    color = Color.White.copy(alpha = 0.7f),
                    size = 4f + Random.nextFloat() * 5f,
                    life = 0.35f,
                    maxLife = 0.35f
                )
            )
        }
    }

    private fun spawnBoostParticles(pos: Vec3, color: Color) {
        for (i in 0 until 18) {
            particles.add(
                Particle(
                    position = Vec3(
                        pos.x + (Random.nextFloat() - 0.5f) * 1.5f,
                        pos.y + (Random.nextFloat() - 0.5f) * 0.8f,
                        pos.z - Random.nextFloat() * 1.5f
                    ),
                    velocity = Vec3((Random.nextFloat() - 0.5f) * 4f, 2f + Random.nextFloat() * 4f, -ballVel.z * 0.6f),
                    color = color,
                    size = 5f + Random.nextFloat() * 6f,
                    life = 0.5f,
                    maxLife = 0.5f
                )
            )
        }
    }

    private fun spawnCoinSparkles(pos: Vec3, color: Color) {
        for (i in 0 until 14) {
            val angle = Random.nextFloat() * 2f * 3.14159f
            val elev = (Random.nextFloat() - 0.5f) * 2f
            particles.add(
                Particle(
                    position = pos,
                    velocity = Vec3(cos(angle) * 3.5f, 2.0f + elev * 2f, sin(angle) * 3.5f),
                    color = color,
                    size = 4.5f + Random.nextFloat() * 5f,
                    life = 0.45f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun spawnExplosion(pos: Vec3, color: Color) {
        for (i in 0 until 35) {
            val angle = Random.nextFloat() * 2f * 3.14159f
            val elevation = (Random.nextFloat() - 0.5f) * 2f
            val speed = 3f + Random.nextFloat() * 7f
            particles.add(
                Particle(
                    position = pos,
                    velocity = Vec3(cos(angle) * speed, elevation * speed + 2f, sin(angle) * speed),
                    color = if (i % 2 == 0) color else Color(0xFFFFEB3B),
                    size = 6f + Random.nextFloat() * 8f,
                    life = 0.7f,
                    maxLife = 0.7f
                )
            )
        }
    }

    private fun spawnVictoryConfetti(pos: Vec3) {
        val confettiColors = listOf(
            Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676),
            Color(0xFF00E5FF), Color(0xFFD500F9), Color(0xFFFF9100)
        )
        for (i in 0 until 60) {
            val angle = Random.nextFloat() * 2f * 3.14159f
            val speed = 2f + Random.nextFloat() * 8f
            particles.add(
                Particle(
                    position = Vec3(pos.x + (Random.nextFloat() - 0.5f) * 3f, pos.y + 1f, pos.z + Random.nextFloat() * 4f),
                    velocity = Vec3(cos(angle) * speed, 5f + Random.nextFloat() * 7f, sin(angle) * speed),
                    color = confettiColors[i % confettiColors.size],
                    size = 6f + Random.nextFloat() * 6f,
                    life = 1.4f,
                    maxLife = 1.4f
                )
            )
        }
    }
}
