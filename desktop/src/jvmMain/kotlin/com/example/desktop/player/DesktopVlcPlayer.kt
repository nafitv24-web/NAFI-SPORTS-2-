package com.example.desktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.BorderLayout
import java.awt.Canvas
import javax.swing.JPanel

@Composable
fun DesktopVlcPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    var vlcAvailable = true
    val mediaPlayerComponent = remember {
        try {
            EmbeddedMediaPlayerComponent()
        } catch (e: Throwable) {
            vlcAvailable = false
            null
        }
    }

    if (mediaPlayerComponent != null) {
        DisposableEffect(streamUrl) {
            try {
                val player = mediaPlayerComponent.mediaPlayer()
                player.media().play(streamUrl)
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            onDispose {
                try {
                    mediaPlayerComponent.mediaPlayer().controls().stop()
                    mediaPlayerComponent.release()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        SwingPanel(
            factory = {
                JPanel().apply {
                    layout = BorderLayout()
                    background = java.awt.Color.BLACK
                    add(mediaPlayerComponent, BorderLayout.CENTER)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
        // Fallback UI if VLC runtime is not installed
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ভিডিও প্লে করার জন্য আপনার পিসিতে 64-bit VLC Media Player ইনস্টল থাকা প্রয়োজন।\nলিংক: $streamUrl",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }
    }
}
