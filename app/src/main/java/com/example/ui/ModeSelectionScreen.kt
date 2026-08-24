package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MediaRepository

/**
 * High-fidelity Mode Selection Launcher Screen matching the user provided visual reference.
 * Allows users to choose between Mobile Mode (Portrait/Touch) and Remote Mode (Landscape/TV D-Pad).
 * Displays live scrolling breaking news ticker that can be updated remotely via Admin Panel / Firebase RTDB.
 * The News Ticker bar is placed directly ABOVE the GET STARTED button as requested by user.
 */
@Composable
fun ModeSelectionScreen(
    repository: MediaRepository? = null,
    onSelectMobileMode: () -> Unit,
    onSelectRemoteMode: () -> Unit,
    onExitApp: () -> Unit
) {
    // Track focused or active selection
    var selectedMode by remember { mutableStateOf<AppUserMode?>(null) }
    var focusedMode by remember { mutableStateOf<AppUserMode?>(AppUserMode.MOBILE) }

    // Marquee scrolling ticker text state (fetched from repository & remote Firebase)
    var tickerText by remember {
        mutableStateOf(
            repository?.getMarqueeTickerText() ?: MediaRepository.DEFAULT_MARQUEE_TEXT
        )
    }

    LaunchedEffect(Unit) {
        if (repository != null) {
            val remoteTicker = repository.fetchMarqueeTickerFromFirebase()
            if (!remoteTicker.isNullOrBlank()) {
                tickerText = remoteTicker
            }
        }
    }

    // Intercept Back button to show exit dialog
    BackHandler {
        onExitApp()
    }

    // Ambient floating glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F081D),
                        Color(0xFF0B1021),
                        Color(0xFF030712)
                    )
                )
            )
    ) {
        // Decorative cosmic ambient glow orbs in background
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.35f * glowPulse),
                            Color(0xFF06B6D4).copy(alpha = 0.2f * glowPulse),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 60.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD946EF).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main Content Layout (Responsive Column)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section: Official App Logo + Title + Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Official NAFI TV24 App Logo with ambient glow & squircle styling
                Box(
                    modifier = Modifier.size(105.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient radial glow behind the logo
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .blur(18.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFEC4899)
                                )
                            )
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "NAFI TV24 Official App Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "NAFI TV24",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Choose Your Experience",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Center Cards: MOBILE MODE vs REMOTE MODE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. MOBILE MODE CARD (Neon Cyan)
                ModeOptionCard(
                    mode = AppUserMode.MOBILE,
                    title = "MOBILE MODE",
                    subtitle = "Stream on the go.",
                    accentColor = Color(0xFF00E5FF),
                    gradientColors = listOf(Color(0xFF03223D), Color(0xFF0B192C)),
                    isSelected = selectedMode == AppUserMode.MOBILE,
                    isFocused = focusedMode == AppUserMode.MOBILE,
                    onFocusChanged = { if (it) focusedMode = AppUserMode.MOBILE },
                    onSelect = {
                        selectedMode = AppUserMode.MOBILE
                        onSelectMobileMode()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 200.dp, max = 245.dp)
                ) {
                    MobileDeviceArtwork()
                }

                // 2. REMOTE MODE CARD (Neon Purple / Violet)
                ModeOptionCard(
                    mode = AppUserMode.REMOTE,
                    title = "REMOTE MODE",
                    subtitle = "Control the big screen.",
                    accentColor = Color(0xFFC084FC),
                    gradientColors = listOf(Color(0xFF2A0D45), Color(0xFF160826)),
                    isSelected = selectedMode == AppUserMode.REMOTE,
                    isFocused = focusedMode == AppUserMode.REMOTE,
                    onFocusChanged = { if (it) focusedMode = AppUserMode.REMOTE },
                    onSelect = {
                        selectedMode = AppUserMode.REMOTE
                        onSelectRemoteMode()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 200.dp, max = 245.dp)
                ) {
                    RemoteDeviceArtwork()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // NEWS MARQUEE BAR: Placed directly ABOVE the GET STARTED button as requested
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.8f),
                            Color(0xFF8B5CF6).copy(alpha = 0.9f),
                            Color(0xFFEC4899).copy(alpha = 0.8f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00E5FF), shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tickerText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE, velocity = 40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action: "GET STARTED" Button (Below the Marquee Bar)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                var isStartBtnFocused by remember { mutableStateOf(false) }
                val startBtnScale by animateFloatAsState(
                    targetValue = if (isStartBtnFocused) 1.08f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                    label = "startBtnScale"
                )

                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Transparent,
                    shadowElevation = if (isStartBtnFocused) 16.dp else 6.dp,
                    modifier = Modifier
                        .scale(startBtnScale)
                        .onFocusChanged { isStartBtnFocused = it.isFocused }
                        .focusable()
                        .clip(RoundedCornerShape(30.dp))
                        .clickable {
                            if (focusedMode == AppUserMode.REMOTE || selectedMode == AppUserMode.REMOTE) {
                                onSelectRemoteMode()
                            } else {
                                onSelectMobileMode()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2563EB),
                                        Color(0xFF1D4ED8),
                                        Color(0xFF3B82F6)
                                    )
                                )
                            )
                            .border(
                                width = if (isStartBtnFocused) 2.5.dp else 1.2.dp,
                                color = if (isStartBtnFocused) Color(0xFF00E5FF) else Color(0xFF93C5FD).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .padding(horizontal = 42.dp, vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GET STARTED",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

enum class AppUserMode {
    MOBILE,
    REMOTE
}

@Composable
private fun ModeOptionCard(
    mode: AppUserMode,
    title: String,
    subtitle: String,
    accentColor: Color,
    gradientColors: List<Color>,
    isSelected: Boolean,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isFocused || isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused || isSelected) 0.95f else 0.4f,
        label = "glowAlpha"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = if (isFocused || isSelected) 18.dp else 4.dp,
        modifier = modifier
            .scale(scale)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .focusable()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            gradientColors[0].copy(alpha = 0.95f),
                            gradientColors[1].copy(alpha = 0.98f),
                            Color(0xFF050B14)
                        )
                    )
                )
                .border(
                    width = if (isFocused || isSelected) 2.8.dp else 1.2.dp,
                    color = accentColor.copy(alpha = glowAlpha),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Artwork Center
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    artwork()
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Labels
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Stylized 3D Smartphone Artwork with glowing play screen and ambient cyan particles
 */
@Composable
private fun MobileDeviceArtwork() {
    Box(
        modifier = Modifier
            .size(width = 85.dp, height = 115.dp),
        contentAlignment = Alignment.Center
    ) {
        // Cyan ambient glow behind phone
        Box(
            modifier = Modifier
                .size(75.dp)
                .blur(20.dp)
                .background(Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape)
        )

        // Smartphone body
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(62.dp)
                .height(105.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0284C7).copy(alpha = 0.3f),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Top speaker notch
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(18.dp)
                        .height(3.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                )

                // Glowing Play Icon inside center circle
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom home indicator bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .width(22.dp)
                        .height(2.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

/**
 * Stylized 3D Remote & Big Screen TV + Gamepad Artwork with glowing purple ambient lighting
 */
@Composable
private fun RemoteDeviceArtwork() {
    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 115.dp),
        contentAlignment = Alignment.Center
    ) {
        // Purple ambient glow behind TV and remote
        Box(
            modifier = Modifier
                .size(80.dp)
                .blur(22.dp)
                .background(Color(0xFFC084FC).copy(alpha = 0.5f), CircleShape)
        )

        // Background TV Screen
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E1035),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)),
            modifier = Modifier
                .offset(x = (-8).dp, y = (-6).dp)
                .width(72.dp)
                .height(50.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4C1D95).copy(alpha = 0.6f), Color(0xFF1E1035))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tv,
                    contentDescription = null,
                    tint = Color(0xFFC084FC).copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Foreground Sleek Remote Controller
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFFC084FC)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .offset(x = 6.dp, y = 2.dp)
                .width(36.dp)
                .height(86.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF334155), Color(0xFF0F172A))
                        )
                    )
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Power LED
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color(0xFFEC4899), CircleShape)
                )

                // D-Pad Circle
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC)),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFC084FC), CircleShape)
                        )
                    }
                }

                // Remote buttons grid (2x3 dots)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFF94A3B8), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFF94A3B8), CircleShape))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFF94A3B8), CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(Color(0xFF94A3B8), CircleShape))
                    }
                }
            }
        }

        // Small Gamepad Icon badge in corner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE879F9)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Gamepad,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

