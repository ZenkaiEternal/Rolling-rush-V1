package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSettingsEntity
import com.example.game.BallSkin
import com.example.game.LevelGenerator
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkinsShopScreen(
    settings: GameSettingsEntity,
    onSelectSkin: (String) -> Unit,
    onPurchaseSkin: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val unlockedList = settings.unlockedSkins.split(",").map { it.trim() }

    val transition = rememberInfiniteTransition(label = "skins_anim")
    val spinAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070414),
                        Color(0xFF140726),
                        Color(0xFF0D051A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header
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
                    text = "BALL SKINS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                // Coin Balance
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
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${settings.totalCoins}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Skins Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(LevelGenerator.SKINS) { skin ->
                    val isUnlocked = skin.id in unlockedList
                    val isEquipped = settings.selectedSkinId == skin.id

                    SkinCard(
                        skin = skin,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        canAfford = settings.totalCoins >= skin.costCoins,
                        spinAngle = spinAngle,
                        onSelect = { onSelectSkin(skin.id) },
                        onPurchase = { onPurchaseSkin(skin.id, skin.costCoins) }
                    )
                }
            }
        }
    }
}

@Composable
fun SkinCard(
    skin: BallSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    spinAngle: Float,
    onSelect: () -> Unit,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                color = if (isEquipped) skin.glowColor else Color(0x33FFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("skin_card_${skin.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16152D)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ball Canvas preview
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                    val r = size.width * 0.38f

                    // Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(skin.glowColor.copy(alpha = 0.6f), Color.Transparent),
                            center = center,
                            radius = r * 1.3f
                        ),
                        radius = r * 1.3f,
                        center = center
                    )

                    // Sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                skin.glowColor,
                                skin.primaryColor,
                                skin.secondaryColor,
                                Color(0xFF090D1A)
                            ),
                            center = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
                            radius = r * 1.1f
                        ),
                        radius = r,
                        center = center
                    )

                    // Rotating ring
                    val rad = Math.toRadians(spinAngle.toDouble()).toFloat()
                    val ringOffset = sin(rad) * r * 0.45f
                    val ringScale = cos(rad)
                    drawOval(
                        color = Color.White.copy(alpha = 0.6f),
                        topLeft = Offset(center.x - r * 0.85f, center.y + ringOffset - r * 0.15f * ringScale),
                        size = Size(r * 1.7f, r * 0.3f * ringScale),
                        style = Stroke(width = 2.5f)
                    )

                    // Shine
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = r * 0.25f,
                        center = Offset(center.x - r * 0.35f, center.y - r * 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = skin.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isEquipped -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = skin.glowColor.copy(alpha = 0.25f),
                            disabledContentColor = skin.glowColor
                        ),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Equipped", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "EQUIPPED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                isUnlocked -> {
                    Button(
                        onClick = onSelect,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0A0F1D)
                        ),
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("select_skin_${skin.id}")
                    ) {
                        Text(text = "USE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = onPurchase,
                        enabled = canAfford,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color(0xFF0A0F1D)
                        ),
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("buy_skin_${skin.id}")
                    ) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Price", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "${skin.costCoins}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
