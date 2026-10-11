package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelProgressEntity
import com.example.game.LevelGenerator

@Composable
fun LevelSelectScreen(
    levels: List<LevelProgressEntity>,
    onSelectLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val totalStars = levels.sumOf { it.stars }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0418),
                        Color(0xFF140827),
                        Color(0xFF0F061E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "SELECT LEVEL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                // Stars tally
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x33000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Total Stars",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalStars / 60",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 20 Level Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val fullLevels = (1..20).map { lvlNum ->
                    levels.firstOrNull { it.levelNumber == lvlNum } ?: LevelProgressEntity(
                        levelNumber = lvlNum,
                        unlocked = (lvlNum == 1)
                    )
                }

                items(fullLevels) { progress ->
                    LevelCard(
                        progress = progress,
                        onClick = {
                            if (progress.unlocked) {
                                onSelectLevel(progress.levelNumber)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelCard(
    progress: LevelProgressEntity,
    onClick: () -> Unit
) {
    val levelData = LevelGenerator.getLevel(progress.levelNumber)
    val isUnlocked = progress.unlocked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .border(
                width = if (isUnlocked) 1.5.dp else 1.dp,
                color = if (isUnlocked) levelData.theme.accentColor.copy(alpha = 0.7f) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("level_card_${progress.levelNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color(0xFF1B1A35) else Color(0xFF10101F).copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Number badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isUnlocked) levelData.theme.accentColor.copy(alpha = 0.25f) else Color(0x22FFFFFF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Text(
                            text = String.format("%02d", progress.levelNumber),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = levelData.theme.accentColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = levelData.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Theme Tag
                Text(
                    text = levelData.theme.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isUnlocked) levelData.theme.accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Star Rating
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val earned = isUnlocked && progress.stars >= i
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (earned) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isUnlocked && progress.highScore > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Best: ${progress.highScore}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
