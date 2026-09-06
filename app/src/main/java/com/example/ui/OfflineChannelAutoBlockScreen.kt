package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BlockableChannel
import com.example.model.ChannelStatus
import com.example.model.MediaItem
import com.example.util.OfflineChannelAutoBlockManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Filter tab options for the channel list
 */
enum class ChannelListFilter(val label: String) {
    ALL("সবগুলো"),
    ONLINE("অনলাইন"),
    OFFLINE("অফলাইন"),
    BLOCKED("ব্লকড"),
    UNKNOWN("যাচাইহীন")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineChannelAutoBlockScreen(
    initialMediaItems: List<MediaItem>,
    onBack: () -> Unit,
    onChannelsUpdated: ((List<MediaItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val manager = remember { OfflineChannelAutoBlockManager.getInstance(context) }

    // Channel list state
    var channelList by remember {
        mutableStateOf(
            if (initialMediaItems.isNotEmpty()) {
                initialMediaItems.map { item ->
                    val isBlocked = manager.isChannelBlocked(item.id)
                    BlockableChannel.fromMediaItem(item, isBlocked = isBlocked)
                }
            } else {
                manager.loadFromLocalStorage()
            }
        )
    }

    var selectedFilter by remember { mutableStateOf(ChannelListFilter.ALL) }
    var rawSearchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }

    // Settings
    var autoBlockOffline by remember { mutableStateOf(manager.isAutoBlockEnabled()) }
    var activeScanJob by remember { mutableStateOf<Job?>(null) }
    var singleCheckingId by remember { mutableStateOf<String?>(null) }

    // Export dialog
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("JSON") } // "JSON" or "CSV"

    val isScanning by manager.isScanning.collectAsState()
    val scanProgress by manager.scanProgress.collectAsState()
    val scanStatusMessage by manager.scanStatusMessage.collectAsState()

    // 4. Debounce input typing (Performance Optimization 3)
    LaunchedEffect(rawSearchQuery) {
        delay(250)
        debouncedSearchQuery = rawSearchQuery.trim()
    }

    // Load persisted data if empty
    LaunchedEffect(Unit) {
        if (channelList.isEmpty()) {
            val loaded = manager.loadFromLocalStorage()
            if (loaded.isNotEmpty()) {
                channelList = loaded
            }
        }
    }

    // Compute statistics reactively
    val statistics by remember(channelList) {
        derivedStateOf { manager.getStatistics(channelList) }
    }

    // Filtered list with virtualized support
    val displayedChannels by remember(channelList, selectedFilter, debouncedSearchQuery) {
        derivedStateOf {
            val filteredByStatus = when (selectedFilter) {
                ChannelListFilter.ALL -> channelList
                ChannelListFilter.ONLINE -> channelList.filter { it.status == ChannelStatus.ONLINE && !it.isBlocked }
                ChannelListFilter.OFFLINE -> channelList.filter { it.status == ChannelStatus.OFFLINE && !it.isBlocked }
                ChannelListFilter.BLOCKED -> channelList.filter { it.isBlocked }
                ChannelListFilter.UNKNOWN -> channelList.filter { it.status == ChannelStatus.UNKNOWN && !it.isBlocked }
            }

            if (debouncedSearchQuery.isBlank()) {
                filteredByStatus
            } else {
                filteredByStatus.filter {
                    it.name.contains(debouncedSearchQuery, ignoreCase = true) ||
                            (it.group != null && it.group.contains(debouncedSearchQuery, ignoreCase = true)) ||
                            it.url.contains(debouncedSearchQuery, ignoreCase = true)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // =====================================================================
        // TOP APP BAR
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                activeScanJob?.cancel()
                onBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF00E5FF)
                )
            }

            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = "অফলাইন চ্যানেল অটো-ব্লক",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "M3U লাইভ স্ট্রিম চেকার ও অটোমেটিক ব্লকার",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            // Export Button
            IconButton(
                onClick = { showExportDialog = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Export Blocked",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // =====================================================================
        // AUTO-BLOCK TOGGLE & NOTICE
        // =====================================================================
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.8f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (autoBlockOffline) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF334155).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = if (autoBlockOffline) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "অটো-ব্লক মোড (Auto-Block Offline)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (autoBlockOffline) "অফলাইন পেলে সাথে সাথে ব্লক লিস্টে যোগ হবে" else "শুধু স্ট্যাটাস দেখাবে, ব্লক করবে না",
                            color = if (autoBlockOffline) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = autoBlockOffline,
                    onCheckedChange = {
                        autoBlockOffline = it
                        manager.setAutoBlockEnabled(it)
                    },
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFEF4444),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }
        }

        // =====================================================================
        // স্ট্যাটাস কাউন্টার (Status Counters: Total, Online, Offline, Blocked, Unknown)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Total
            StatMetricBadge(
                label = "মোট",
                count = statistics.total,
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
            // Online
            StatMetricBadge(
                label = "অনলাইন",
                count = statistics.online,
                percent = statistics.onlinePercentage,
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1.1f)
            )
            // Offline
            StatMetricBadge(
                label = "অফলাইন",
                count = statistics.offline,
                percent = statistics.offlinePercentage,
                accentColor = Color(0xFFF87171),
                modifier = Modifier.weight(1.1f)
            )
            // Blocked
            StatMetricBadge(
                label = "ব্লকড",
                count = statistics.blocked,
                percent = statistics.blockedPercentage,
                accentColor = Color(0xFFFB923C),
                modifier = Modifier.weight(1.1f)
            )
        }

        // =====================================================================
        // প্রোগ্রেস বার (Linear Progress Bar when Scanning)
        // =====================================================================
        AnimatedVisibility(
            visible = isScanning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scanStatusMessage.ifBlank { "চ্যানেল স্ক্যান চলছে..." },
                        color = Color(0xFF00E5FF),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(scanProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF1E293B)
                )
            }
        }

        // =====================================================================
        // ACTION BUTTONS ROW (১. স্ক্যান বাটন, ৬. রি-চেক বাটন, ৭. ব্লক অফলাইন বাটন)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ১. স্ক্যান বাটন (Start / Stop Scan)
            Button(
                onClick = {
                    if (isScanning) {
                        activeScanJob?.cancel()
                        Toast.makeText(context, "স্ক্যান থামানো হয়েছে", Toast.LENGTH_SHORT).show()
                    } else {
                        activeScanJob = coroutineScope.launch {
                            val updated = manager.scanAndAutoBlock(
                                channels = channelList,
                                autoBlockOffline = autoBlockOffline
                            ) { cur, tot, updatedChannel ->
                                // Update item in real time without lagging
                                val index = channelList.indexOfFirst { it.id == updatedChannel.id }
                                if (index != -1) {
                                    val copy = channelList.toMutableList()
                                    copy[index] = updatedChannel
                                    channelList = copy
                                }
                            }
                            channelList = updated
                            onChannelsUpdated?.invoke(updated.map { it.toMediaItem() })
                        }
                    }
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Color(0xFFDC2626) else Color(0xFF0284C7)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isScanning) "থামান" else "সব স্ক্যান",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ৬. রি-চেক বাটন (Recheck only blocked channels)
            Button(
                onClick = {
                    if (statistics.blocked == 0) {
                        Toast.makeText(context, "কোনো ব্লক করা চ্যানেল নেই", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    activeScanJob = coroutineScope.launch {
                        Toast.makeText(context, "ব্লকড চ্যানেল রি-চেক শুরু হচ্ছে...", Toast.LENGTH_SHORT).show()
                        val updated = manager.recheckBlockedChannels(channelList) { cur, tot, res ->
                            // Progress update
                        }
                        channelList = updated
                        onChannelsUpdated?.invoke(updated.map { it.toMediaItem() })
                        Toast.makeText(context, "রি-চেক সম্পন্ন!", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isScanning && statistics.blocked > 0,
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155),
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = if (statistics.blocked > 0) Color(0xFF00E5FF) else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "রি-চেক (${statistics.blocked})",
                    color = if (statistics.blocked > 0) Color.White else Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ব্লক অফলাইন বাটন (Block All Currently Offline)
            OutlinedButton(
                onClick = {
                    val updated = manager.blockOfflineChannels(channelList)
                    channelList = updated
                    onChannelsUpdated?.invoke(updated.map { it.toMediaItem() })
                    Toast.makeText(context, "অফলাইন চ্যানেলগুলো ব্লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                },
                enabled = statistics.offline > 0,
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (statistics.offline > 0) Color(0xFFEF4444) else Color(0xFF334155)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    tint = if (statistics.offline > 0) Color(0xFFEF4444) else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ব্লক অফলাইন",
                    color = if (statistics.offline > 0) Color(0xFFFCA5A5) else Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // =====================================================================
        // SEARCH & FILTER CHIPS
        // =====================================================================
        OutlinedTextField(
            value = rawSearchQuery,
            onValueChange = { rawSearchQuery = it },
            placeholder = {
                Text("চ্যানেলের নাম, গ্রুপ বা URL খুঁজুন...", color = Color(0xFF64748B), fontSize = 12.sp)
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (rawSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { rawSearchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A)
            ),
            singleLine = true
        )

        // ৫. ফিল্টার অপশন (Filter chips: All, Online, Offline, Blocked, Unknown)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(ChannelListFilter.values()) { filter ->
                val isSelected = selectedFilter == filter
                val count = when (filter) {
                    ChannelListFilter.ALL -> statistics.total
                    ChannelListFilter.ONLINE -> statistics.online
                    ChannelListFilter.OFFLINE -> statistics.offline
                    ChannelListFilter.BLOCKED -> statistics.blocked
                    ChannelListFilter.UNKNOWN -> statistics.unknown
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0F172A),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
                    ),
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "($count)",
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF64748B),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // ৪. ভার্চুয়াল স্ক্রল চ্যানেল লিস্ট (Virtual Scroll with LazyColumn)
        // =====================================================================
        if (displayedChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = if (rawSearchQuery.isNotBlank()) "খোঁজের সাথে মিল পাওয়া যায়নি" else "কোনো চ্যানেল পাওয়া যায়নি",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "চ্যানেল স্ক্যান করতে উপরের 'সব স্ক্যান' বাটনে ট্যাপ করুন",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedChannels, key = { it.id }) { channel ->
                    val isCheckingThis = singleCheckingId == channel.id

                    ChannelAutoBlockCard(
                        channel = channel,
                        isChecking = isCheckingThis,
                        onRecheckSingle = {
                            if (isCheckingThis || isScanning) return@ChannelAutoBlockCard
                            singleCheckingId = channel.id
                            coroutineScope.launch {
                                val res = manager.checkChannelStatus(channel.url)
                                val updated = if (res.status == ChannelStatus.ONLINE) {
                                    manager.unblockChannelId(channel.id)
                                    channel.copy(
                                        status = ChannelStatus.ONLINE,
                                        isBlocked = false,
                                        blockReason = null,
                                        responseTime = res.responseTime,
                                        httpCode = res.code,
                                        lastChecked = BlockableChannel.currentIsoDate()
                                    )
                                } else {
                                    if (autoBlockOffline) {
                                        channel.copy(
                                            status = ChannelStatus.BLOCKED,
                                            isBlocked = true,
                                            blockReason = res.reason,
                                            responseTime = res.responseTime,
                                            httpCode = res.code,
                                            lastChecked = BlockableChannel.currentIsoDate(),
                                            blockedAt = channel.blockedAt ?: BlockableChannel.currentIsoDate()
                                        )
                                    } else {
                                        channel.copy(
                                            status = ChannelStatus.OFFLINE,
                                            responseTime = res.responseTime,
                                            httpCode = res.code,
                                            lastChecked = BlockableChannel.currentIsoDate()
                                        )
                                    }
                                }

                                val listCopy = channelList.toMutableList()
                                val idx = listCopy.indexOfFirst { it.id == channel.id }
                                if (idx != -1) {
                                    listCopy[idx] = updated
                                    channelList = listCopy
                                    manager.triggerAutoSave(listCopy)
                                }
                                singleCheckingId = null
                            }
                        },
                        onToggleBlock = {
                            if (channel.isBlocked) {
                                // ৫. unblockChannel
                                val updated = manager.unblockChannel(channelList, channel.id)
                                channelList = updated
                                onChannelsUpdated?.invoke(updated.map { it.toMediaItem() })
                                Toast.makeText(context, "${channel.name} আনব্লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                            } else {
                                // Block directly
                                val blockedItem = manager.blockChannelDirect(channel)
                                val listCopy = channelList.toMutableList()
                                val idx = listCopy.indexOfFirst { it.id == channel.id }
                                if (idx != -1) {
                                    listCopy[idx] = blockedItem
                                    channelList = listCopy
                                    manager.triggerAutoSave(listCopy)
                                    onChannelsUpdated?.invoke(listCopy.map { it.toMediaItem() })
                                }
                                Toast.makeText(context, "${channel.name} ব্লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // ৭. এক্সপোর্ট বাটন ডায়ালগ (Export Dialog: JSON / CSV)
    // =========================================================================
    if (showExportDialog) {
        val blockedOnly = manager.filterBlockedChannels(channelList)
        val exportText = remember(exportFormat, channelList) {
            if (exportFormat == "CSV") {
                manager.exportBlockedChannelsCsv(channelList)
            } else {
                manager.exportBlockedChannelsJson(channelList)
            }
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                    Text(
                        text = "ব্লকড চ্যানেল এক্সপোর্ট",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "মোট ${blockedOnly.size}টি ব্লক করা চ্যানেল এক্সপোর্ট করতে প্রস্তুত।",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )

                    // Format selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (exportFormat == "JSON") Color(0xFF00E5FF) else Color(0xFF1E293B),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { exportFormat = "JSON" }
                        ) {
                            Text(
                                text = "JSON ফরম্যাট",
                                color = if (exportFormat == "JSON") Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (exportFormat == "CSV") Color(0xFF00E5FF) else Color(0xFF1E293B),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { exportFormat = "CSV" }
                        ) {
                            Text(
                                text = "CSV (Excel)",
                                color = if (exportFormat == "CSV") Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF020617))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = exportText.take(600) + if (exportText.length > 600) "\n..." else "",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Blocked Channels ($exportFormat)", exportText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "$exportFormat ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কপি করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    IconButton(onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Blocked Channels Export ($exportFormat)")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color(0xFF94A3B8))
                    }
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("বন্ধ করুন", color = Color(0xFF94A3B8))
                    }
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Metric summary badge
 */
@Composable
private fun StatMetricBadge(
    label: String,
    count: Int,
    percent: Float? = null,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count",
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (percent != null && percent > 0f) {
                Text(
                    text = "${percent.toInt()}%",
                    color = accentColor.copy(alpha = 0.8f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Individual channel card with status badges, response times, and unblock action
 */
@Composable
private fun ChannelAutoBlockCard(
    channel: BlockableChannel,
    isChecking: Boolean,
    onRecheckSingle: () -> Unit,
    onToggleBlock: () -> Unit
) {
    val cardBg = when {
        channel.isBlocked -> Color(0xFF1E1B2E)
        channel.status == ChannelStatus.OFFLINE -> Color(0xFF1F1A24)
        channel.status == ChannelStatus.ONLINE -> Color(0xFF0E2320)
        else -> Color(0xFF0F172A)
    }

    val borderColor = when {
        channel.isBlocked -> Color(0xFFEF4444).copy(alpha = 0.6f)
        channel.status == ChannelStatus.OFFLINE -> Color(0xFFF87171).copy(alpha = 0.4f)
        channel.status == ChannelStatus.ONLINE -> Color(0xFF10B981).copy(alpha = 0.4f)
        else -> Color(0xFF1E293B)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Logo
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF020617)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = channel.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name, group, URL
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (!channel.group.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = channel.group,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = channel.url,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Badge
                StatusBadge(
                    status = channel.status,
                    isBlocked = channel.isBlocked,
                    isChecking = isChecking
                )
            }

            // Bottom metadata & Action Buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info: response time, error code, block reason
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (channel.responseTime > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                            Text(
                                text = "${channel.responseTime}ms",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (channel.httpCode > 0) {
                        Text(
                            text = "HTTP ${channel.httpCode}",
                            color = if (channel.httpCode in 200..399) Color(0xFF34D399) else Color(0xFFF87171),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!channel.blockReason.isNullOrBlank()) {
                        Text(
                            text = "• ${channel.blockReason}",
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Single Recheck Button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.clickable { onRecheckSingle() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00E5FF),
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(10.dp)
                                )
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                            }
                            Text(text = "চেক", color = Color(0xFF00E5FF), fontSize = 10.5.sp)
                        }
                    }

                    // Block / Unblock Button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (channel.isBlocked) Color(0xFF065F46) else Color(0xFF7F1D1D),
                        modifier = Modifier.clickable { onToggleBlock() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (channel.isBlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (channel.isBlocked) "আনব্লক" else "ব্লক",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Status Chip with icon and label
 */
@Composable
private fun StatusBadge(
    status: ChannelStatus,
    isBlocked: Boolean,
    isChecking: Boolean
) {
    if (isChecking) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0284C7).copy(alpha = 0.2f),
            border = BorderStroke(1.dp, Color(0xFF38BDF8))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(10.dp)
                )
                Text("চেক হচ্ছে...", color = Color(0xFF38BDF8), fontSize = 10.sp)
            }
        }
        return
    }

    if (isBlocked) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFEF4444).copy(alpha = 0.2f),
            border = BorderStroke(1.dp, Color(0xFFEF4444))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                Text("ব্লকড", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    when (status) {
        ChannelStatus.ONLINE -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF10B981).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Text("অনলাইন", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        ChannelStatus.OFFLINE -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF87171).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFF87171))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(12.dp))
                    Text("অফলাইন", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        else -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF64748B).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Rounded.HourglassEmpty, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                    Text("যাচাইহীন", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }
        }
    }
}
