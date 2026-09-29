package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaRepository
import com.example.model.MatchComment
import com.example.model.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveMatchCommentSection(
    mediaItem: MediaItem,
    repository: MediaRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("nafitv_comments_prefs", Context.MODE_PRIVATE) }

    var userName by remember {
        val saved = prefs.getString("user_display_name", "")
        mutableStateOf(if (!saved.isNullOrBlank()) saved else "ফ্যান_${(100..999).random()}")
    }
    var isEditingName by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf(userName) }

    var commentText by remember { mutableStateOf("") }
    var commentsList by remember { mutableStateOf<List<MatchComment>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }

    val quickReactions = listOf("🏏 ছক্কা!", "⚽ গোল!", "🔥 আগুন খেলা!", "🇧🇩 বাংলাদেশ!", "👏 সাবাশ!", "🏆 কাপ আমাদের!", "❤️ লাভ ইট", "💥 দুর্দান্ত!")

    // Real-time live comments listener / poller (updates every 5 seconds)
    LaunchedEffect(mediaItem.id) {
        while (isActive) {
            try {
                val fetched = repository.fetchMatchComments(mediaItem.id)
                if (fetched.isNotEmpty() || commentsList.isEmpty()) {
                    commentsList = fetched
                }
            } catch (_: Exception) {}
            delay(5000L)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "লাইভ ফ্যান কমেন্ট (Live Chat)",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${commentsList.size} টি কমেন্ট • সরাসরি সবার সাথে শেয়ার করুন",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Edit Display Name Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable {
                        tempNameInput = userName
                        isEditingName = !isEditingName
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.People, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userName.take(12),
                            color = Color(0xFF38BDF8),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                    }
                }
            }

            // Edit Name Dropdown Field
            AnimatedVisibility(visible = isEditingName) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tempNameInput,
                        onValueChange = { tempNameInput = it },
                        label = { Text("আপনার নাম লিখুন", fontSize = 11.sp) },
                        placeholder = { Text("যেমন: সাকিব, রাকিব", color = Color(0xFF64748B), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (tempNameInput.isNotBlank()) {
                                userName = tempNameInput.trim()
                                prefs.edit().putString("user_display_name", userName).apply()
                                isEditingName = false
                                Toast.makeText(context, "নাম সেভ হয়েছে: $userName", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("সেভ", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Quick Emoji Reaction Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickReactions) { reaction ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                val success = repository.postMatchComment(mediaItem.id, userName, reaction)
                                if (success) {
                                    commentsList = repository.fetchMatchComments(mediaItem.id)
                                }
                            }
                        }
                    ) {
                        Text(
                            text = reaction,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Input Field & Send Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("ম্যাচ নিয়ে কমেন্ট করুন...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        val textToSend = commentText.trim()
                        if (textToSend.isNotBlank() && !isSending) {
                            isSending = true
                            commentText = ""
                            coroutineScope.launch {
                                val success = repository.postMatchComment(mediaItem.id, userName, textToSend)
                                if (success) {
                                    commentsList = repository.fetchMatchComments(mediaItem.id)
                                    Toast.makeText(context, "কমেন্ট সফলভাবে পোস্ট হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E5FF))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Comments List Area
            if (commentsList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "এখনো কোনো কমেন্ট নেই। প্রথম কমেন্টটি আপনিই করুন!",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commentsList, key = { it.id }) { comment ->
                        CommentItemRow(comment = comment)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItemRow(comment: MatchComment) {
    val formattedTime = remember(comment.timestamp) {
        val diff = System.currentTimeMillis() - comment.timestamp
        when {
            diff < 60_000L -> "এইমাত্র"
            diff < 3600_000L -> "${diff / 60_000L} মিনিট আগে"
            diff < 86400_000L -> "${diff / 3600_000L} ঘণ্টা আগে"
            else -> {
                val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
                sdf.format(Date(comment.timestamp))
            }
        }
    }

    val avatarGradient = remember(comment.userName) {
        val hash = Math.abs(comment.userName.hashCode())
        val colors = listOf(
            listOf(Color(0xFF00E5FF), Color(0xFF2563EB)),
            listOf(Color(0xFF10B981), Color(0xFF059669)),
            listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
            listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
        )
        colors[hash % colors.size]
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(avatarGradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.userName,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        color = Color(0xFF64748B),
                        fontSize = 9.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.text,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
