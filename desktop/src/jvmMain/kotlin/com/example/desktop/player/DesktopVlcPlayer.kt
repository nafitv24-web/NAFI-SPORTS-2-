package com.example.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.BorderLayout
import java.awt.Desktop
import java.net.URI
import javax.swing.JPanel

/**
 * Universal Native Desktop Video Player for NAFI TV 24
 * 1. Checks if internal VLC engine is present.
 * 2. If present, plays seamlessly inside Compose Desktop SwingPanel.
 * 3. If VLC runtime is not detected, provides an instant 1-Click "পিসির নিজস্ব প্লেয়ার / ব্রাউজারে চালান"
 *    and direct system launcher button so user can watch with ANY video player (Windows Media Player, PotPlayer, MPV, or Edge/Chrome) WITHOUT needing VLC!
 */
@Composable
fun DesktopVlcPlayer(
    title: String,
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    var vlcLoaded by remember(streamUrl) { mutableStateOf<Boolean?>(null) }
    var vlcComponent by remember(streamUrl) { mutableStateOf<Any?>(null) }

    // Try initializing embedded VLC engine dynamically without hard crash
    LaunchedEffect(streamUrl) {
        try {
            val vlcClass = Class.forName("uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent")
            val instance = vlcClass.getDeclaredConstructor().newInstance()
            val playerMethod = vlcClass.getMethod("mediaPlayer")
            val player = playerMethod.invoke(instance)

            val mediaMethod = player.javaClass.getMethod("media")
            val mediaObj = mediaMethod.invoke(player)
            val playMethod = mediaObj.javaClass.getMethod("play", String::class.java)
            playMethod.invoke(mediaObj, streamUrl)

            vlcComponent = instance
            vlcLoaded = true
        } catch (_: Throwable) {
            vlcComponent = null
            vlcLoaded = false
        }
    }

    DisposableEffect(streamUrl) {
        onDispose {
            try {
                vlcComponent?.let { comp ->
                    val playerMethod = comp.javaClass.getMethod("mediaPlayer")
                    val player = playerMethod.invoke(comp)
                    val controlsMethod = player.javaClass.getMethod("controls")
                    val controls = controlsMethod.invoke(player)
                    controls.javaClass.getMethod("stop").invoke(controls)

                    comp.javaClass.getMethod("release").invoke(comp)
                }
            } catch (_: Throwable) {}
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (vlcLoaded == true && vlcComponent is java.awt.Component) {
            SwingPanel(
                factory = {
                    JPanel().apply {
                        layout = BorderLayout()
                        background = java.awt.Color.BLACK
                        add(vlcComponent as java.awt.Component, BorderLayout.CENTER)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Standalone Direct Player View - Works with 100% of Windows PCs out of the box!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "VLC ছাড়াও আপনার কম্পিউটারে যেকোনো প্লেয়ার বা ব্রাউজারে সরাসরি ফুলস্ক্রিন চলবে",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Button 1: Play via Default Windows Media Player / Default Stream Player
                    Button(
                        onClick = {
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI(streamUrl))
                                } else {
                                    Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", streamUrl))
                                }
                            } catch (e: Exception) {
                                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", streamUrl))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ভিডিও চালু করুন (সরাসরি প্লেয়ারে)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Button 2: Copy Stream URL
                    OutlinedButton(
                        onClick = {
                            try {
                                val stringSelection = java.awt.datatransfer.StringSelection(streamUrl)
                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, null)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("লিংক কপি করুন", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = streamUrl,
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
