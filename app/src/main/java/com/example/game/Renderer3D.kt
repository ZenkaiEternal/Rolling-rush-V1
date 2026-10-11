package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Renderer3D {

    private val camera = Camera3D()

    fun render(
        drawScope: DrawScope,
        physics: PhysicsEngine,
        levelData: LevelData,
        selectedSkin: BallSkin,
        cameraMode: CameraMode = CameraMode.THIRD_PERSON,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val ballPos = physics.ballPos
        val theme = levelData.theme

        // Screen shake
        val shakeX = if (physics.screenShakeAmount > 0f) (Random.nextFloat() - 0.5f) * 16f * physics.screenShakeAmount else 0f
        val shakeY = if (physics.screenShakeAmount > 0f) (Random.nextFloat() - 0.5f) * 16f * physics.screenShakeAmount else 0f

        // Camera positioning based on mode
        when (cameraMode) {
            CameraMode.THIRD_PERSON -> {
                camera.fovDegrees = 65f
                camera.position = Vec3(
                    x = ballPos.x * 0.72f + shakeX,
                    y = maxOf(ballPos.y + 4.8f, 4.0f) + shakeY,
                    z = ballPos.z - 8.8f
                )
                camera.target = Vec3(
                    x = ballPos.x * 0.85f,
                    y = ballPos.y + 1.2f,
                    z = ballPos.z + 16f
                )
            }
            CameraMode.FIRST_PERSON -> {
                camera.fovDegrees = 78f
                // Positioned right at ball driver eye level
                camera.position = Vec3(
                    x = ballPos.x + shakeX,
                    y = ballPos.y + physics.ballRadius * 0.85f + shakeY,
                    z = ballPos.z + physics.ballRadius * 0.2f
                )
                camera.target = Vec3(
                    x = ballPos.x,
                    y = ballPos.y + physics.ballRadius * 0.75f,
                    z = ballPos.z + 30f
                )
            }
            CameraMode.FRONT_PERSON -> {
                camera.fovDegrees = 65f
                // Camera in front looking back at ball
                camera.position = Vec3(
                    x = ballPos.x * 0.75f + shakeX,
                    y = maxOf(ballPos.y + 4.0f, 3.2f) + shakeY,
                    z = ballPos.z + 9.5f
                )
                camera.target = Vec3(
                    x = ballPos.x * 0.85f,
                    y = ballPos.y + 1.0f,
                    z = ballPos.z - 8f
                )
            }
        }
        camera.updateVectors()

        // 1. Sky, Horizon & 3D Celestial Objects
        drawSky(drawScope, theme, screenWidth, screenHeight)

        // 2. 3D Track with Deep Extruded Curbs & Underglow
        drawTrack(drawScope, physics, levelData, screenWidth, screenHeight)

        // 3. 3D Collectibles with Glow Flares
        drawCollectibles(drawScope, levelData.collectibles, ballPos.z, screenWidth, screenHeight)

        // 4. 3D Obstacles with Directional Shading
        drawObstacles(drawScope, levelData.obstacles, ballPos.z, screenWidth, screenHeight)

        // 5. Large 3D Rolling Ball with Multi-Layer Rotation
        drawBall(drawScope, physics, selectedSkin, cameraMode, screenWidth, screenHeight)

        // 6. Particle Systems
        drawParticles(drawScope, physics.particles, screenWidth, screenHeight)

        // 7. First Person Cockpit HUD & Speed Streaks
        if (cameraMode == CameraMode.FIRST_PERSON) {
            drawFirstPersonOverlay(drawScope, physics, theme, screenWidth, screenHeight)
        }
    }

    private fun drawSky(
        scope: DrawScope,
        theme: ThemeStyle,
        w: Float,
        h: Float
    ) {
        // Deep atmosphere gradient
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(theme.skyTop, theme.skyBottom, theme.fogColor),
                startY = 0f,
                endY = h
            ),
            size = Size(w, h)
        )

        // Distant horizon neon sun / moon
        val sunCenter = Offset(w * 0.5f, h * 0.36f)
        val sunRadius = w * 0.20f
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.accentColor.copy(alpha = 0.85f),
                    theme.roadEdgeColor.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = sunCenter,
                radius = sunRadius * 1.6f
            ),
            radius = sunRadius * 1.6f,
            center = sunCenter
        )
        scope.drawCircle(
            color = theme.accentColor.copy(alpha = 0.65f),
            radius = sunRadius * 0.85f,
            center = sunCenter
        )

        // Distant floating 3D Cyber Pyramids in the sky
        for (i in -3..3) {
            val pyrX = i * 45f
            val pyrZ = camera.position.z + 140f
            val pyrY = 18f + (i % 2) * 8f
            val pTip = camera.project(Vec3(pyrX, pyrY + 12f, pyrZ), w, h)
            val pL = camera.project(Vec3(pyrX - 10f, pyrY, pyrZ), w, h)
            val pR = camera.project(Vec3(pyrX + 10f, pyrY, pyrZ), w, h)
            if (pTip != null && pL != null && pR != null) {
                val p = Path().apply {
                    moveTo(pTip.x, pTip.y)
                    lineTo(pR.x, pR.y)
                    lineTo(pL.x, pL.y)
                    close()
                }
                scope.drawPath(p, color = theme.accentColor.copy(alpha = 0.12f), style = Fill)
                scope.drawPath(p, color = theme.roadEdgeColor.copy(alpha = 0.3f), style = Stroke(width = 1.2f))
            }
        }
    }

    private fun drawTrack(
        scope: DrawScope,
        physics: PhysicsEngine,
        levelData: LevelData,
        w: Float,
        h: Float
    ) {
        val playerZ = physics.ballPos.z
        val minZ = playerZ - 12f
        val maxZ = playerZ + 120f

        val visibleSegments = levelData.segments.filter { it.endZ >= minZ && it.startZ <= maxZ }
        val sortedSegments = visibleSegments.sortedByDescending { it.startZ }

        val trackThickness = 1.8f // 3D block extrusion depth!

        for (seg in sortedSegments) {
            if (seg.isGap) {
                drawGapEdges(scope, seg, w, h)
                continue
            }

            val halfW = seg.width * 0.5f
            val cx = seg.centerX

            // Top 4 corners
            val pNearLeft = camera.project(Vec3(cx - halfW, seg.startY, seg.startZ), w, h)
            val pNearRight = camera.project(Vec3(cx + halfW, seg.startY, seg.startZ), w, h)
            val pFarLeft = camera.project(Vec3(cx - halfW, seg.endY, seg.endZ), w, h)
            val pFarRight = camera.project(Vec3(cx + halfW, seg.endY, seg.endZ), w, h)

            // Bottom 4 extruded corners for true 3D road depth!
            val pNearLeftBot = camera.project(Vec3(cx - halfW, seg.startY - trackThickness, seg.startZ), w, h)
            val pNearRightBot = camera.project(Vec3(cx + halfW, seg.startY - trackThickness, seg.startZ), w, h)
            val pFarLeftBot = camera.project(Vec3(cx - halfW, seg.endY - trackThickness, seg.endZ), w, h)
            val pFarRightBot = camera.project(Vec3(cx + halfW, seg.endY - trackThickness, seg.endZ), w, h)

            // 1. Left Side 3D Curb Extrusion
            if (pNearLeft != null && pFarLeft != null && pFarLeftBot != null && pNearLeftBot != null) {
                val leftWallPath = Path().apply {
                    moveTo(pNearLeft.x, pNearLeft.y)
                    lineTo(pFarLeft.x, pFarLeft.y)
                    lineTo(pFarLeftBot.x, pFarLeftBot.y)
                    lineTo(pNearLeftBot.x, pNearLeftBot.y)
                    close()
                }
                scope.drawPath(leftWallPath, color = Color(0xFF0C0E17), style = Fill)
                // Bottom glowing neon rail
                scope.drawLine(
                    color = levelData.theme.accentColor.copy(alpha = 0.5f),
                    start = Offset(pNearLeftBot.x, pNearLeftBot.y),
                    end = Offset(pFarLeftBot.x, pFarLeftBot.y),
                    strokeWidth = 2.0f
                )
            }

            // 2. Right Side 3D Curb Extrusion
            if (pNearRight != null && pFarRight != null && pFarRightBot != null && pNearRightBot != null) {
                val rightWallPath = Path().apply {
                    moveTo(pNearRight.x, pNearRight.y)
                    lineTo(pFarRight.x, pFarRight.y)
                    lineTo(pFarRightBot.x, pFarRightBot.y)
                    lineTo(pNearRightBot.x, pNearRightBot.y)
                    close()
                }
                scope.drawPath(rightWallPath, color = Color(0xFF0A0C14), style = Fill)
                scope.drawLine(
                    color = levelData.theme.accentColor.copy(alpha = 0.5f),
                    start = Offset(pNearRightBot.x, pNearRightBot.y),
                    end = Offset(pFarRightBot.x, pFarRightBot.y),
                    strokeWidth = 2.0f
                )
            }

            // 3. Top Road Surface Quad
            if (pNearLeft != null && pNearRight != null && pFarLeft != null && pFarRight != null) {
                val roadPath = Path().apply {
                    moveTo(pNearLeft.x, pNearLeft.y)
                    lineTo(pNearRight.x, pNearRight.y)
                    lineTo(pFarRight.x, pFarRight.y)
                    lineTo(pFarLeft.x, pFarLeft.y)
                    close()
                }

                // Road surface
                scope.drawPath(roadPath, color = levelData.theme.roadColor, style = Fill)

                // Neon glowing edge borders
                scope.drawLine(
                    color = levelData.theme.roadEdgeColor,
                    start = Offset(pNearLeft.x, pNearLeft.y),
                    end = Offset(pFarLeft.x, pFarLeft.y),
                    strokeWidth = 4.0f,
                    cap = StrokeCap.Round
                )
                scope.drawLine(
                    color = levelData.theme.roadEdgeColor,
                    start = Offset(pNearRight.x, pNearRight.y),
                    end = Offset(pFarRight.x, pFarRight.y),
                    strokeWidth = 4.0f,
                    cap = StrokeCap.Round
                )

                // Center dashed guideline
                val pNearCenter = camera.project(Vec3(cx, seg.startY, seg.startZ), w, h)
                val pFarCenter = camera.project(Vec3(cx, seg.endY, seg.endZ), w, h)
                if (pNearCenter != null && pFarCenter != null) {
                    scope.drawLine(
                        color = levelData.theme.gridColor.copy(alpha = 0.65f),
                        start = Offset(pNearCenter.x, pNearCenter.y),
                        end = Offset(pFarCenter.x, pFarCenter.y),
                        strokeWidth = 2.5f
                    )
                }

                // Pads
                if (seg.padType != PadType.NONE) {
                    drawPad(scope, seg, w, h)
                }
            }
        }

        // Draw Finish Line Arch
        drawFinishArch(scope, levelData, w, h)
    }

    private fun drawGapEdges(scope: DrawScope, seg: TrackSegment, w: Float, h: Float) {
        val halfW = seg.width * 0.5f
        val pStartLeft = camera.project(Vec3(-halfW, seg.startY, seg.startZ), w, h)
        val pStartRight = camera.project(Vec3(halfW, seg.startY, seg.startZ), w, h)
        if (pStartLeft != null && pStartRight != null) {
            scope.drawLine(
                color = Color(0xFFFF1744),
                start = Offset(pStartLeft.x, pStartLeft.y),
                end = Offset(pStartRight.x, pStartRight.y),
                strokeWidth = 5.0f
            )
        }
        val pEndLeft = camera.project(Vec3(-halfW, seg.endY, seg.endZ), w, h)
        val pEndRight = camera.project(Vec3(halfW, seg.endY, seg.endZ), w, h)
        if (pEndLeft != null && pEndRight != null) {
            scope.drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(pEndLeft.x, pEndLeft.y),
                end = Offset(pEndRight.x, pEndRight.y),
                strokeWidth = 5.0f
            )
        }
    }

    private fun drawPad(scope: DrawScope, seg: TrackSegment, w: Float, h: Float) {
        val padW = seg.width * 0.65f * 0.5f
        val midZ = (seg.startZ + seg.endZ) * 0.5f
        val midY = (seg.startY + seg.endY) * 0.5f + 0.03f
        val p0 = camera.project(Vec3(-padW, midY, midZ - 2.8f), w, h)
        val p1 = camera.project(Vec3(padW, midY, midZ - 2.8f), w, h)
        val p2 = camera.project(Vec3(padW, midY, midZ + 2.8f), w, h)
        val p3 = camera.project(Vec3(-padW, midY, midZ + 2.8f), w, h)

        if (p0 != null && p1 != null && p2 != null && p3 != null) {
            val padPath = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }
            scope.drawPath(padPath, color = seg.padColor.copy(alpha = 0.88f), style = Fill)
            scope.drawPath(padPath, color = Color.White, style = Stroke(width = 3.0f))
        }
    }

    private fun drawFinishArch(scope: DrawScope, levelData: LevelData, w: Float, h: Float) {
        val finishZ = levelData.totalLengthZ - 8f
        val seg = levelData.segments.lastOrNull() ?: return
        val finishY = seg.endY
        val halfW = seg.width * 0.58f

        val pLeftBase = camera.project(Vec3(-halfW, finishY, finishZ), w, h)
        val pRightBase = camera.project(Vec3(halfW, finishY, finishZ), w, h)
        val pLeftTop = camera.project(Vec3(-halfW, finishY + 6.0f, finishZ), w, h)
        val pRightTop = camera.project(Vec3(halfW, finishY + 6.0f, finishZ), w, h)

        if (pLeftBase != null && pRightBase != null && pLeftTop != null && pRightTop != null) {
            scope.drawLine(Color(0xFFFFEA00), Offset(pLeftBase.x, pLeftBase.y), Offset(pLeftTop.x, pLeftTop.y), 8f)
            scope.drawLine(Color(0xFFFFEA00), Offset(pRightBase.x, pRightBase.y), Offset(pRightTop.x, pRightTop.y), 8f)
            scope.drawLine(Color.White, Offset(pLeftTop.x, pLeftTop.y), Offset(pRightTop.x, pRightTop.y), 10f)

            val pBannerLeftBottom = camera.project(Vec3(-halfW, finishY + 4.5f, finishZ), w, h)
            val pBannerRightBottom = camera.project(Vec3(halfW, finishY + 4.5f, finishZ), w, h)
            if (pBannerLeftBottom != null && pBannerRightBottom != null) {
                val bannerPath = Path().apply {
                    moveTo(pLeftTop.x, pLeftTop.y)
                    lineTo(pRightTop.x, pRightTop.y)
                    lineTo(pBannerRightBottom.x, pBannerRightBottom.y)
                    lineTo(pBannerLeftBottom.x, pBannerLeftBottom.y)
                    close()
                }
                scope.drawPath(bannerPath, color = Color(0xFFFFD600).copy(alpha = 0.95f), style = Fill)
            }
        }
    }

    private fun drawCollectibles(
        scope: DrawScope,
        collectibles: List<Collectible>,
        playerZ: Float,
        w: Float,
        h: Float
    ) {
        val visible = collectibles.filter {
            !it.collected && it.position.z >= playerZ - 5f && it.position.z <= playerZ + 90f
        }.sortedByDescending { it.position.z }

        for (col in visible) {
            val proj = camera.project(col.position, w, h) ?: continue
            val scale = (150f / proj.depth).coerceIn(8f, 45f)

            val color = when (col.type) {
                CollectibleType.COIN -> Color(0xFFFFD700)
                CollectibleType.GEM -> Color(0xFF00E5FF)
                CollectibleType.SHIELD -> Color(0xFF00B0FF)
                CollectibleType.MAGNET -> Color(0xFFFF4081)
            }

            // Glow aura
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(proj.x, proj.y),
                    radius = scale * 2.0f
                ),
                radius = scale * 2.0f,
                center = Offset(proj.x, proj.y)
            )

            // Faceted diamond shape
            val diamond = Path().apply {
                moveTo(proj.x, proj.y - scale)
                lineTo(proj.x + scale * 0.8f, proj.y)
                lineTo(proj.x, proj.y + scale)
                lineTo(proj.x - scale * 0.8f, proj.y)
                close()
            }
            scope.drawPath(diamond, color = color, style = Fill)
            scope.drawPath(diamond, color = Color.White, style = Stroke(width = 2.0f))

            // Specular core
            scope.drawCircle(
                color = Color.White,
                radius = scale * 0.3f,
                center = Offset(proj.x - scale * 0.2f, proj.y - scale * 0.2f)
            )
        }
    }

    private fun drawObstacles(
        scope: DrawScope,
        obstacles: List<Obstacle>,
        playerZ: Float,
        w: Float,
        h: Float
    ) {
        val visible = obstacles.filter {
            it.isActive && it.position.z >= playerZ - 6f && it.position.z <= playerZ + 95f
        }.sortedByDescending { it.position.z }

        for (obs in visible) {
            when (obs.type) {
                ObstacleType.MOVING_BLOCK, ObstacleType.MOVING_WALL, ObstacleType.CRUSHING_PISTON -> {
                    drawBoxObstacle(scope, obs, w, h)
                }
                ObstacleType.SPINNING_BAR -> {
                    drawSpinningBar(scope, obs, w, h)
                }
                ObstacleType.SPIKE_ROW -> {
                    drawSpikes(scope, obs, w, h)
                }
                ObstacleType.SWINGING_HAMMER -> {
                    drawSwingingHammer(scope, obs, w, h)
                }
                ObstacleType.LASER_BARRIER -> {
                    drawLaserBarrier(scope, obs, w, h)
                }
                ObstacleType.ROLLING_BOULDER -> {
                    drawBoulder(scope, obs, w, h)
                }
                ObstacleType.FALLING_TILE -> {
                    drawBoxObstacle(scope, obs, w, h, baseColor = if (obs.isSteppedOn) Color(0xFFFF5252) else Color(0xFF78909C))
                }
            }
        }
    }

    private fun drawBoxObstacle(
        scope: DrawScope,
        obs: Obstacle,
        w: Float,
        h: Float,
        baseColor: Color = Color(0xFFFF3D00)
    ) {
        val hx = obs.size.x * 0.5f
        val hy = obs.size.y * 0.5f
        val hz = obs.size.z * 0.5f
        val p = obs.position

        val fbl = camera.project(Vec3(p.x - hx, p.y - hy, p.z - hz), w, h)
        val fbr = camera.project(Vec3(p.x + hx, p.y - hy, p.z - hz), w, h)
        val ftr = camera.project(Vec3(p.x + hx, p.y + hy, p.z - hz), w, h)
        val ftl = camera.project(Vec3(p.x - hx, p.y + hy, p.z - hz), w, h)

        val btl = camera.project(Vec3(p.x - hx, p.y + hy, p.z + hz), w, h)
        val btr = camera.project(Vec3(p.x + hx, p.y + hy, p.z + hz), w, h)

        if (ftl != null && ftr != null && btr != null && btl != null) {
            val topPath = Path().apply {
                moveTo(ftl.x, ftl.y)
                lineTo(ftr.x, ftr.y)
                lineTo(btr.x, btr.y)
                lineTo(btl.x, btl.y)
                close()
            }
            scope.drawPath(topPath, color = baseColor.copy(alpha = 0.95f), style = Fill)
            scope.drawPath(topPath, color = Color.White.copy(alpha = 0.6f), style = Stroke(width = 1.8f))
        }

        if (fbl != null && fbr != null && ftr != null && ftl != null) {
            val frontPath = Path().apply {
                moveTo(fbl.x, fbl.y)
                lineTo(fbr.x, fbr.y)
                lineTo(ftr.x, ftr.y)
                lineTo(ftl.x, ftl.y)
                close()
            }
            scope.drawPath(frontPath, color = baseColor, style = Fill)
            scope.drawPath(frontPath, color = Color(0xFFFFD700), style = Stroke(width = 2.5f))
        }
    }

    private fun drawSpinningBar(scope: DrawScope, obs: Obstacle, w: Float, h: Float) {
        val angleRad = Math.toRadians(obs.currentAngle.toDouble()).toFloat()
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val halfLen = obs.size.x * 0.5f

        val p1 = camera.project(Vec3(obs.position.x - cosA * halfLen, obs.position.y - sinA * halfLen, obs.position.z), w, h)
        val p2 = camera.project(Vec3(obs.position.x + cosA * halfLen, obs.position.y + sinA * halfLen, obs.position.z), w, h)
        val center = camera.project(obs.position, w, h)

        if (p1 != null && p2 != null && center != null) {
            val strokeW = (90f / center.depth).coerceIn(5f, 20f)
            scope.drawLine(Color(0xFFFF1744).copy(alpha = 0.5f), Offset(p1.x, p1.y), Offset(p2.x, p2.y), strokeW * 1.8f)
            scope.drawLine(Color(0xFFFF5252), Offset(p1.x, p1.y), Offset(p2.x, p2.y), strokeW, StrokeCap.Round)
            scope.drawCircle(Color.White, radius = strokeW * 0.8f, center = Offset(center.x, center.y))
        }
    }

    private fun drawSpikes(scope: DrawScope, obs: Obstacle, w: Float, h: Float) {
        val count = 4
        val stepX = obs.size.x / count
        val startX = obs.position.x - obs.size.x * 0.5f

        for (i in 0 until count) {
            val x = startX + i * stepX + stepX * 0.5f
            val baseLeft = camera.project(Vec3(x - stepX * 0.45f, obs.position.y - obs.size.y * 0.5f, obs.position.z), w, h)
            val baseRight = camera.project(Vec3(x + stepX * 0.45f, obs.position.y - obs.size.y * 0.5f, obs.position.z), w, h)
            val tip = camera.project(Vec3(x, obs.position.y + obs.size.y * 0.5f, obs.position.z), w, h)

            if (baseLeft != null && baseRight != null && tip != null) {
                val spikePath = Path().apply {
                    moveTo(baseLeft.x, baseLeft.y)
                    lineTo(tip.x, tip.y)
                    lineTo(baseRight.x, baseRight.y)
                    close()
                }
                scope.drawPath(spikePath, color = Color(0xFFFF3D00), style = Fill)
                scope.drawPath(spikePath, color = Color(0xFFFFD600), style = Stroke(width = 1.6f))
            }
        }
    }

    private fun drawSwingingHammer(scope: DrawScope, obs: Obstacle, w: Float, h: Float) {
        val pivot = camera.project(Vec3(obs.initialPosition.x, obs.initialPosition.y + 0.5f, obs.position.z), w, h)
        val head = camera.project(obs.position, w, h)

        if (pivot != null && head != null) {
            scope.drawLine(Color(0xFF78909C), Offset(pivot.x, pivot.y), Offset(head.x, head.y), 5.5f)
            val headRadius = (150f / head.depth).coerceIn(10f, 38f)
            scope.drawCircle(Color(0xFFD50000), radius = headRadius, center = Offset(head.x, head.y))
            scope.drawCircle(Color(0xFFFF8A80), radius = headRadius * 0.5f, center = Offset(head.x - headRadius * 0.2f, head.y - headRadius * 0.2f))
        }
    }

    private fun drawLaserBarrier(scope: DrawScope, obs: Obstacle, w: Float, h: Float) {
        val halfW = obs.size.x * 0.5f
        val leftPost = camera.project(Vec3(obs.position.x - halfW, obs.position.y, obs.position.z), w, h)
        val rightPost = camera.project(Vec3(obs.position.x + halfW, obs.position.y, obs.position.z), w, h)

        if (leftPost != null && rightPost != null) {
            val postRadius = (70f / leftPost.depth).coerceIn(5f, 15f)
            scope.drawCircle(Color(0xFF00E5FF), radius = postRadius, center = Offset(leftPost.x, leftPost.y))
            scope.drawCircle(Color(0xFF00E5FF), radius = postRadius, center = Offset(rightPost.x, rightPost.y))

            scope.drawLine(Color(0xFFFF1744).copy(alpha = 0.45f), Offset(leftPost.x, leftPost.y), Offset(rightPost.x, rightPost.y), postRadius * 2.2f)
            scope.drawLine(Color(0xFFFF5252), Offset(leftPost.x, leftPost.y), Offset(rightPost.x, rightPost.y), postRadius * 1.0f)
            scope.drawLine(Color.White, Offset(leftPost.x, leftPost.y), Offset(rightPost.x, rightPost.y), postRadius * 0.35f)
        }
    }

    private fun drawBoulder(scope: DrawScope, obs: Obstacle, w: Float, h: Float) {
        val proj = camera.project(obs.position, w, h) ?: return
        val r = (130f / proj.depth).coerceIn(10f, 44f)
        scope.drawCircle(Color(0xFF3E2723), radius = r, center = Offset(proj.x, proj.y))
        scope.drawCircle(Color(0xFFFF5722), radius = r * 0.85f, center = Offset(proj.x, proj.y))
        scope.drawCircle(Color(0xFFFFAB91), radius = r * 0.35f, center = Offset(proj.x - r * 0.3f, proj.y - r * 0.3f))
    }

    private fun drawBall(
        scope: DrawScope,
        physics: PhysicsEngine,
        skin: BallSkin,
        cameraMode: CameraMode,
        w: Float,
        h: Float
    ) {
        val ballPos = physics.ballPos
        val proj = camera.project(ballPos, w, h) ?: return
        val ballRadiusPx = (camera.project(Vec3(ballPos.x + physics.ballRadius, ballPos.y, ballPos.z), w, h)?.x ?: (proj.x + 55f)) - proj.x

        // Prominent, large arcade ball size
        val clampedRadius = ballRadiusPx.coerceIn(32f, 130f)

        // Drop shadow onto ground beneath ball
        val shadowY = ballPos.y - physics.ballRadius
        val shadowProj = camera.project(Vec3(ballPos.x, shadowY + 0.05f, ballPos.z), w, h)
        if (shadowProj != null) {
            val heightAboveGround = (ballPos.y - (shadowY + physics.ballRadius)).coerceAtLeast(0f)
            val shadowFactor = (1f - heightAboveGround * 0.12f).coerceIn(0.2f, 1f)
            val shadowWidth = clampedRadius * 1.5f * shadowFactor
            val shadowHeight = clampedRadius * 0.45f * shadowFactor
            scope.drawOval(
                color = Color.Black.copy(alpha = 0.55f * shadowFactor),
                topLeft = Offset(shadowProj.x - shadowWidth * 0.5f, shadowProj.y - shadowHeight * 0.5f),
                size = Size(shadowWidth, shadowHeight)
            )
        }

        // Speed trail streaks
        if (physics.speedMultiplier > 1.1f) {
            for (i in 1..5) {
                val trailZ = if (cameraMode == CameraMode.FRONT_PERSON) ballPos.z + i * 1.0f else ballPos.z - i * 1.0f
                val trailProj = camera.project(Vec3(ballPos.x, ballPos.y, trailZ), w, h)
                if (trailProj != null) {
                    val trailAlpha = 0.65f - i * 0.12f
                    scope.drawCircle(
                        color = skin.glowColor.copy(alpha = trailAlpha),
                        radius = clampedRadius * (1f - i * 0.14f),
                        center = Offset(trailProj.x, trailProj.y)
                    )
                }
            }
        }

        val center = Offset(proj.x, proj.y)

        // Outer glow
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(skin.glowColor.copy(alpha = 0.85f), Color.Transparent),
                center = center,
                radius = clampedRadius * 1.4f
            ),
            radius = clampedRadius * 1.4f,
            center = center
        )

        // 3D Sphere shading (Ambient + Diffuse + Rim Shadow)
        val lightOffset = Offset(center.x - clampedRadius * 0.35f, center.y - clampedRadius * 0.35f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    skin.glowColor,
                    skin.primaryColor,
                    skin.secondaryColor,
                    Color(0xFF060914)
                ),
                center = lightOffset,
                radius = clampedRadius * 1.15f
            ),
            radius = clampedRadius,
            center = center
        )

        // 3D BALL ROLLING ANIMATION:
        // Multiple rotating latitude and longitude rings & patterned crossbars that tumble with physics rotation!
        val rollRadX = Math.toRadians(physics.rollRotationX.toDouble()).toFloat()
        val tiltRadZ = Math.toRadians(physics.rollRotationZ.toDouble()).toFloat()

        // 1. Multiple Rotating Latitudes (-45°, 0°, 45°)
        val latOffsets = floatArrayOf(-0.55f, 0f, 0.55f)
        for (lat in latOffsets) {
            val rotAngle = rollRadX + lat * 1.5f
            val yOffset = sin(rotAngle) * clampedRadius * 0.72f
            val scaleX = cos(rotAngle)

            // Only draw if facing towards the camera
            if (cos(rotAngle + 0.3f) > -0.2f) {
                val ringCenter = Offset(center.x + sin(tiltRadZ) * 6f, center.y + yOffset)
                scope.drawOval(
                    color = Color.White.copy(alpha = 0.65f),
                    topLeft = Offset(ringCenter.x - clampedRadius * 0.88f, ringCenter.y - clampedRadius * 0.22f * scaleX),
                    size = Size(clampedRadius * 1.76f, clampedRadius * 0.44f * scaleX),
                    style = Stroke(width = 3.0f)
                )
            }
        }

        // 2. Rotating Longitude Meridian (Vertical ring spinning as ball steers)
        val lonAngle = tiltRadZ * 2.5f
        val lonScale = cos(lonAngle)
        scope.drawOval(
            color = skin.glowColor.copy(alpha = 0.5f),
            topLeft = Offset(center.x - clampedRadius * 0.25f * lonScale, center.y - clampedRadius * 0.9f),
            size = Size(clampedRadius * 0.5f * lonScale, clampedRadius * 1.8f),
            style = Stroke(width = 2.5f)
        )

        // 3. Dual-Tone Racing Arc Pattern (gives instant visual feedback of ball tumbling)
        val arcPhase = (physics.rollRotationX % 180f) / 180f
        val arcStartAngle = arcPhase * 360f
        scope.drawArc(
            color = Color.White.copy(alpha = 0.35f),
            startAngle = arcStartAngle,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - clampedRadius * 0.7f, center.y - clampedRadius * 0.7f),
            size = Size(clampedRadius * 1.4f, clampedRadius * 1.4f),
            style = Stroke(width = 4.5f)
        )

        // 4. Primary Specular Highlight (High-gloss glass reflection)
        scope.drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = clampedRadius * 0.28f,
            center = Offset(center.x - clampedRadius * 0.35f, center.y - clampedRadius * 0.35f)
        )
        scope.drawCircle(
            color = Color.White,
            radius = clampedRadius * 0.12f,
            center = Offset(center.x - clampedRadius * 0.32f, center.y - clampedRadius * 0.32f)
        )

        // 5. Shield Bubble (if active)
        if (physics.shieldActive) {
            scope.drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.28f),
                radius = clampedRadius * 1.5f,
                center = center
            )
            scope.drawCircle(
                color = Color(0xFF80D8FF),
                radius = clampedRadius * 1.5f,
                center = center,
                style = Stroke(width = 3.5f)
            )
        }
    }

    private fun drawFirstPersonOverlay(
        scope: DrawScope,
        physics: PhysicsEngine,
        theme: ThemeStyle,
        w: Float,
        h: Float
    ) {
        val cx = w * 0.5f
        val cy = h * 0.52f

        // High-tech reticle
        scope.drawCircle(
            color = theme.accentColor.copy(alpha = 0.4f),
            radius = 28f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.0f)
        )
        scope.drawLine(
            color = theme.accentColor.copy(alpha = 0.6f),
            start = Offset(cx - 38f, cy),
            end = Offset(cx - 14f, cy),
            strokeWidth = 2.0f
        )
        scope.drawLine(
            color = theme.accentColor.copy(alpha = 0.6f),
            start = Offset(cx + 14f, cy),
            end = Offset(cx + 38f, cy),
            strokeWidth = 2.0f
        )
        scope.drawLine(
            color = theme.accentColor.copy(alpha = 0.6f),
            start = Offset(cx, cy - 38f),
            end = Offset(cx, cy - 14f),
            strokeWidth = 2.0f
        )
        scope.drawLine(
            color = theme.accentColor.copy(alpha = 0.6f),
            start = Offset(cx, cy + 14f),
            end = Offset(cx, cy + 38f),
            strokeWidth = 2.0f
        )

        // Speed Lines on border
        if (physics.speedMultiplier > 1.1f) {
            for (i in 0 until 12) {
                val y = Random.nextFloat() * h
                val len = 40f + Random.nextFloat() * 120f
                scope.drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(len, y),
                    strokeWidth = 2.5f
                )
                scope.drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(w, y),
                    end = Offset(w - len, y),
                    strokeWidth = 2.5f
                )
            }
        }
    }

    private fun drawParticles(
        scope: DrawScope,
        particles: List<Particle>,
        w: Float,
        h: Float
    ) {
        for (p in particles) {
            val proj = camera.project(p.position, w, h) ?: continue
            val sizePx = (p.size * (90f / proj.depth)).coerceIn(3f, 30f)
            scope.drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = sizePx * 0.5f,
                center = Offset(proj.x, proj.y)
            )
        }
    }
}
