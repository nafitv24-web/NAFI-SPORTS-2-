package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItem
import com.example.util.SportComment
import com.example.util.SportsInteractionManager

@Composable
fun SportsMatchCommentsAndReactionsView(
    sport: MediaItem,
    isTvMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(sport.id) {
        SportsInteractionManager.init(context)
    }

    val updateTick by SportsInteractionManager.updateTick.collectAsState()

    val reactions = remember(sport.id, updateTick) {
        SportsInteractionManager.getReactions(sport.id)
    }

    val comments = remember(sport.id, updateTick) {
        SportsInteractionManager.getComments(sport.id)
    }

    val playlistSource = remember(sport.id, sport.category, sport.tournament, sport.isAdminAdded) {
        SportsInteractionManager.getPlaylistSource(sport.category, sport.tournament, sport.isAdminAdded, sport.id)
    }

    var commentInput by remember { mutableStateOf("") }
    var userNameInput by remember { mutableStateOf("") }
    var lastReactedEmoji by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B132B))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // MATCH & PLAYLIST SOURCE HEADER
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LIVE FAN ZONE TAG
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Text(
                            text = "লাইভ ফ্যান জোন (LIVE FAN ZONE)",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // PLAYLIST SOURCE BADGE (Prominently shows which playlist the match is from!)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistPlay,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "উৎস প্লেলিস্ট: $playlistSource",
                                color = Color(0xFFBAE6FD),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Match Title
                Text(
                    text = sport.title.ifBlank { "${sport.team1 ?: "Team 1"} vs ${sport.team2 ?: "Team 2"}" },
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // LIVE EMOJI REACTIONS BAR
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚡ খেলায় রিয়েক্ট দিন:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ট্যাপ করে রিয়েক্ট করুন",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 6 Emoji Reaction Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SportsInteractionManager.EMOJI_LIST.forEach { emoji ->
                        val count = reactions[emoji] ?: 0
                        val isRecent = lastReactedEmoji == emoji
                        val reactionScale by animateFloatAsState(
                            targetValue = if (isRecent) 1.2f else 1.0f,
                            animationSpec = tween(150, easing = FastOutSlowInEasing),
                            label = "reactionScale"
                        )
                        var isFocused by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFocused) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isFocused) 1.5.dp else 1.dp,
                                if (isFocused) Color(0xFF38BDF8) else Color(0xFF334155).copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .scale(reactionScale)
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .clickable {
                                    lastReactedEmoji = emoji
                                    SportsInteractionManager.addReaction(sport.id, emoji)
                                    Toast.makeText(context, "$emoji রিয়েক্ট যোগ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = emoji, fontSize = 16.sp)
                                Text(
                                    text = "$count",
                                    color = if (count > 0) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // COMMENTS STREAM HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Forum,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "💬 ফ্যান কমেন্ট ও প্রতিক্রিয়া (${comments.size})",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "লাইভ চ্যাট",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // COMMENTS LIST (Scrollable feed)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(comments, key = { it.id }) { comment ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131D33),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // User Avatar Initial
                        val avatarColor = try {
                            Color(android.graphics.Color.parseColor(comment.avatarBgColorHex))
                        } catch (_: Exception) {
                            Color(0xFF0284C7)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = comment.userName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Comment Details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = comment.userName,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = comment.timestamp,
                                    color = Color(0xFF64748B),
                                    fontSize = 9.5.sp
                                )
                            }
                            Text(
                                text = comment.text,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // COMMENT INPUT BOX (For posting comments)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Optional Fan Name field (quick optional tag)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        placeholder = {
                            Text("আপনার নাম (ঐচ্ছিক)", color = Color(0xFF64748B), fontSize = 11.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )
                }

                // Comment Text Field & Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = {
                            Text("ম্যাচ নিয়ে আপনার কমেন্ট লিখুন...", color = Color(0xFF64748B), fontSize = 11.5.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = false,
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commentInput.isNotBlank()) {
                                    val name = userNameInput.ifBlank { "ফ্যান" }
                                    SportsInteractionManager.addComment(sport.id, name, commentInput)
                                    commentInput = ""
                                    focusManager.clearFocus()
                                    Toast.makeText(context, "কমেন্ট সফলভাবে পোস্ট হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 64.dp)
                    )

                    // Post / Send Button
                    var isSendFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (commentInput.isNotBlank()) {
                                val name = userNameInput.ifBlank { "ফ্যান" }
                                SportsInteractionManager.addComment(sport.id, name, commentInput)
                                commentInput = ""
                                focusManager.clearFocus()
                                Toast.makeText(context, "কমেন্ট সফলভাবে পোস্ট হয়েছে!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "অনুগ্রহ করে কিছু লিখুন", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .onFocusChanged { isSendFocused = it.isFocused }
                            .focusable()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("পাঠান", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
