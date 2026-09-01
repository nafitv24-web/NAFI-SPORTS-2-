package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun NafiLogoLoadingView(
    title: String = "লোডিং হচ্ছে...",
    subtitle: String = "অনুগ্রহ করে অপেক্ষা করুন, ডাটা প্রস্তুত হচ্ছে...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nafiLoading")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B).copy(alpha = 0.94f)),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = glowAlpha),
                        Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.8f),
                        Color(0xFFA855F7).copy(alpha = glowAlpha * 0.6f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .widthIn(max = 340.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Branded Pulsing Logo with Glowing Aura
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    // Soft Aura Halo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulseScale * 1.15f)
                            .alpha(glowAlpha * 0.45f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF2563EB), Color.Transparent)
                                )
                            )
                    )

                    // Sharp Logo
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "NAFI TV 24 Logo",
                        modifier = Modifier
                            .size(62.dp)
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                Color(0xFF00E5FF).copy(alpha = glowAlpha),
                                RoundedCornerShape(16.dp)
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                // Loading Title & Bengali Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }

                // Animated Gradient Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.65f)
                            .align(
                                when {
                                    shimmerOffset < 0f -> Alignment.CenterStart
                                    shimmerOffset > 1f -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                }
                            )
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF38BDF8),
                                        Color(0xFF818CF8),
                                        Color(0xFF00E5FF)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
