package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.BallSkin
import com.example.game.CameraMode
import com.example.game.LevelData
import com.example.game.PhysicsEngine
import com.example.game.Renderer3D
import com.example.ui.GameUiState
import com.example.ui.PlayState

@Composable
fun GameScreen(
    uiState: GameUiState,
    levelData: LevelData?,
    physics: PhysicsEngine?,
    renderer: Renderer3D,
    currentSkin: BallSkin,
    onDrag: (Float, Float, Float) -> Unit,
    onReleaseTouch: () -> Unit,
    onToggleCamera: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onNextLevel: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler {
        if (uiState.playState == PlayState.PLAYING) {
            onPause()
        } else {
            onExit()
        }
    }

    var screenWidthPx by remember { mutableStateOf(1080f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Canvas with drag gesture detection
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.playState) {
                    if (uiState.playState == PlayState.PLAYING) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y, screenWidthPx)
                            },
                            onDragEnd = {
                                onReleaseTouch()
                            },
                            onDragCancel = {
                                onReleaseTouch()
                            }
                        )
                    }
                }
        ) {
            screenWidthPx = size.width
            if (physics != null && levelData != null) {
                renderer.render(
                    drawScope = this,
                    physics = physics,
                    levelData = levelData,
                    selectedSkin = currentSkin,
                    cameraMode = uiState.cameraMode,
                    screenWidth = size.width,
                    screenHeight = size.height
                )
            }
        }

        // HUD Overlay
        if (levelData != null) {
            GameHudOverlay(
                levelData = levelData,
                uiState = uiState,
                onToggleCamera = onToggleCamera,
                onPause = onPause
            )
        }

        // Pause Dialog
        if (uiState.playState == PlayState.PAUSED) {
            PauseOverlay(
                onResume = onResume,
                onRestart = onRestart,
                onExit = onExit
            )
        }

        // Game Over Dialog
        if (uiState.playState == PlayState.GAME_OVER) {
            GameOverOverlay(
                reason = uiState.deathReason,
                score = uiState.currentScore,
                coins = uiState.currentCoins,
                onRestart = onRestart,
                onExit = onExit
            )
        }

        // Level Complete Dialog
        if (uiState.playState == PlayState.LEVEL_COMPLETE) {
            LevelCompleteOverlay(
                levelNumber = uiState.selectedLevelNumber,
                stars = uiState.earnedStars,
                score = uiState.currentScore,
                coins = uiState.currentCoins,
                onNextLevel = onNextLevel,
                onRestart = onRestart,
                onExit = onExit
            )
        }
    }
}

@Composable
fun GameHudOverlay(
    levelData: LevelData,
    uiState: GameUiState,
    onToggleCamera: () -> Unit,
    onPause: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level Title, Score & Camera Mode Badge
                Column {
                    Text(
                        text = "LVL ${levelData.number}: ${levelData.title.uppercase()}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SCORE: ${uiState.currentScore}",
                            color = levelData.theme.accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Camera Mode Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x6600E5FF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleCamera() }
                        ) {
                            Text(
                                text = when (uiState.cameraMode) {
                                    CameraMode.THIRD_PERSON -> "3RD PERSON"
                                    CameraMode.FIRST_PERSON -> "1ST PERSON"
                                    CameraMode.FRONT_PERSON -> "FRONT VIEW"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF070B18),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Camera Toggle, Coins & Pause
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier
                            .background(Color(0x66000000), CircleShape)
                            .testTag("toggle_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Switch Camera Mode",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x66000000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${uiState.currentCoins}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .background(Color(0x66000000), CircleShape)
                            .testTag("pause_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Track Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.currentProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = levelData.theme.accentColor,
                        trackColor = Color(0x44FFFFFF),
                        strokeCap = StrokeCap.Round
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Flag, contentDescription = "Finish", tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
            }
        }

        // Active Boosts & Floating Indicators
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.speedBoostActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xCC00E676),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Boost", tint = Color(0xFF0A0F1D), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "SPEED BOOST!", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF0A0F1D))
                    }
                }
            }

            if (uiState.shieldActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xCC00E5FF),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Shield", tint = Color(0xFF0A0F1D), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "SHIELD ACTIVE", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF0A0F1D))
                    }
                }
            }

            // Controls Hint for first 15% of track
            if (uiState.currentProgress < 0.15f && levelData.number <= 2) {
                Text(
                    text = "◀ DRAG FINGER TO STEER ▶",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .background(Color(0x66000000), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA080415)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14122C))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "PAUSED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 2.sp
                )

                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF0A0F1D))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "RESUME", color = Color(0xFF0A0F1D), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restart_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "RESTART", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("exit_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A85)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.ViewModule, contentDescription = "Levels", tint = Color(0xFFFF2A85))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "LEVEL SELECT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    reason: String,
    score: Int,
    coins: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB18040C)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.5.dp, Color(0xFFFF1744), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF200E1C))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "GAME OVER",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF1744),
                    letterSpacing = 2.sp
                )

                Text(
                    text = reason.ifEmpty { "Out of bounds!" },
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                // Stats Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SCORE", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(text = "$score", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "COINS", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(text = "$coins", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                    }
                }

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("retry_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "TRY AGAIN", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("game_over_exit_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = "LEVEL SELECT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LevelCompleteOverlay(
    levelNumber: Int,
    stars: Int,
    score: Int,
    coins: Int,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC051219)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2228))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "LEVEL $levelNumber COMPLETE!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                // 3 Stars Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val earned = stars >= i
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (earned) Color(0xFFFFD700) else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Stats Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33000000),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SCORE", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(text = "$score", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "COINS EARNED", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(text = "+$coins", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                    }
                }

                if (levelNumber < 20) {
                    Button(
                        onClick = onNextLevel,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("next_level_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text(text = "NEXT LEVEL", color = Color(0xFF0A0F1D), fontWeight = FontWeight.ExtraBold)
                    }
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("level_complete_replay_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = "REPLAY", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("level_complete_exit_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = "LEVEL SELECT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
