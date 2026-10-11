package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSettingsEntity
import com.example.data.LevelProgressEntity
import com.example.game.BallSkin
import com.example.ui.ScreenState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MainMenuScreen(
    levels: List<LevelProgressEntity>,
    settings: GameSettingsEntity,
    currentSkin: BallSkin,
    onNavigate: (ScreenState) -> Unit,
    onPlayNext: () -> Unit
) {
    val totalStars = levels.sumOf { it.stars }
    val nextPlayableLevel = levels.firstOrNull { it.unlocked && !it.completed }?.levelNumber
        ?: (levels.lastOrNull { it.unlocked }?.levelNumber ?: 1)

    val transition = rememberInfiniteTransition(label = "menu_anim")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ball_spin"
    )
    val floatOffset by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ball_bob"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0418),
                        Color(0xFF1B0B33),
                        Color(0xFF0F0A1E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x33000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${settings.totalCoins}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Stars Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x33000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Stars",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$totalStars / 60",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Title & 3D Rolling Sphere Preview
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ROLLING RUSH",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "3D ARCADE RUNNER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF2A85),
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Animated 3D Ball Showcase Canvas
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width * 0.5f, size.height * 0.5f + floatOffset)
                        val radius = size.width * 0.38f

                        // Floor shadow
                        drawOval(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(center.x - radius * 1.1f, size.height * 0.88f),
                            size = Size(radius * 2.2f, radius * 0.4f)
                        )

                        // Outer glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(currentSkin.glowColor.copy(alpha = 0.7f), Color.Transparent),
                                center = center,
                                radius = radius * 1.4f
                            ),
                            radius = radius * 1.4f,
                            center = center
                        )

                        // 3D Sphere body
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    currentSkin.glowColor,
                                    currentSkin.primaryColor,
                                    currentSkin.secondaryColor,
                                    Color(0xFF070B18)
                                ),
                                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                                radius = radius * 1.1f
                            ),
                            radius = radius,
                            center = center
                        )

                        // Rotating equator ring
                        val rad = Math.toRadians(rotation.toDouble()).toFloat()
                        val ringOffset = sin(rad) * radius * 0.5f
                        val ringScale = cos(rad)
                        drawOval(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(center.x - radius * 0.9f, center.y + ringOffset - radius * 0.18f * ringScale),
                            size = Size(radius * 1.8f, radius * 0.36f * ringScale),
                            style = Stroke(width = 3.5f)
                        )

                        // Specular shine
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = radius * 0.28f,
                            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
                        )
                    }
                }

                Text(
                    text = currentSkin.name,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play Button
                Button(
                    onClick = onPlayNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("play_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF0A0F1D),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY LEVEL $nextPlayableLevel",
                        color = Color(0xFF0A0F1D),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Levels Button
                OutlinedButton(
                    onClick = { onNavigate(ScreenState.LEVEL_SELECT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("levels_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF2A85)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewModule,
                        contentDescription = "Levels",
                        tint = Color(0xFFFF2A85),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELECT LEVEL (20)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Skins and Settings Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigate(ScreenState.SKINS_SHOP) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("skins_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Skins",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SKINS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigate(ScreenState.SETTINGS) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("settings_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB0BEC5)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFB0BEC5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SETTINGS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
