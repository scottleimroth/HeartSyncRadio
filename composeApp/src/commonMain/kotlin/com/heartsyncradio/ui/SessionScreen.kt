package com.heartsyncradio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.heartsyncradio.hrv.HrvMetrics
import com.heartsyncradio.music.SearchResult
import com.heartsyncradio.music.SessionPhase
import com.heartsyncradio.music.SongSessionResult
import com.heartsyncradio.music.TaggedSong
import com.heartsyncradio.ui.components.SongResultCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    sessionPhase: SessionPhase,
    currentSong: TaggedSong?,
    pendingSong: SearchResult?,
    sessionResults: List<SongSessionResult>,
    settleCountdownSec: Int,
    recordingDurationSec: Int,
    hrvMetrics: HrvMetrics?,
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    searchError: String?,
    totalSongCount: Long,
    playlistCreated: String?,
    isCreatingPlaylist: Boolean,
    notificationListenerEnabled: Boolean,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    onSearchSongs: (String) -> Unit,
    onTagSong: (SearchResult) -> Unit,
    onCreatePlaylist: () -> Unit,
    onResetSession: () -> Unit,
    onClearSearchError: () -> Unit,
    onRequestNotificationListener: () -> Unit,
    onBack: () -> Unit
) {
    var showSearchSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (sessionPhase) {
                SessionPhase.NOT_STARTED -> {
                    NotStartedContent(
                        hrvMetrics = hrvMetrics,
                        notificationListenerEnabled = notificationListenerEnabled,
                        onStartSession = onStartSession,
                        onRequestNotificationListener = onRequestNotificationListener
                    )
                }

                SessionPhase.ACTIVE_NO_SONG -> {
                    CoherenceHeader(hrvMetrics)
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Search for a song and tap it to open in YouTube Music",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "We'll detect playback automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { showSearchSheet = true }) {
                                Text("Search Songs")
                            }
                        }
                    }
                    if (sessionResults.isNotEmpty()) {
                        SessionResultsList(
                            results = sessionResults,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    EndSessionButton(onEndSession)
                }

                SessionPhase.ACTIVE_WAITING_PLAYBACK -> {
                    CoherenceHeader(hrvMetrics)
                    Spacer(modifier = Modifier.height(16.dp))
                    WaitingForPlaybackCard(pendingSong)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (sessionResults.isNotEmpty()) {
                        SessionResultsList(
                            results = sessionResults,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    EndSessionButton(onEndSession)
                }

                SessionPhase.ACTIVE_SETTLING -> {
                    CoherenceHeader(hrvMetrics)
                    Spacer(modifier = Modifier.height(16.dp))
                    CurrentSongCard(
                        song = currentSong,
                        statusText = "Calibrating... ${settleCountdownSec}s",
                        isSettling = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CalibratingNotice()
                    Spacer(modifier = Modifier.height(16.dp))
                    SessionResultsList(
                        results = sessionResults,
                        modifier = Modifier.weight(1f)
                    )
                    EndSessionButton(onEndSession)
                }

                SessionPhase.ACTIVE_RECORDING -> {
                    CoherenceHeader(hrvMetrics)
                    Spacer(modifier = Modifier.height(16.dp))
                    CurrentSongCard(
                        song = currentSong,
                        statusText = "Recording ${recordingDurationSec}s",
                        isSettling = false,
                        coherence = hrvMetrics?.coherenceScore
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (recordingDurationSec < SessionManager_MIN_RECORDING_SEC) {
                        Text(
                            text = "Stay still — ${SessionManager_MIN_RECORDING_SEC - recordingDurationSec}s until valid reading",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Valid reading collected. Continue for more accuracy, or end session.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SessionResultsList(
                        results = sessionResults,
                        modifier = Modifier.weight(1f)
                    )
                    EndSessionButton(onEndSession)
                }

                SessionPhase.ENDED -> {
                    EndedContent(
                        sessionResults = sessionResults,
                        totalSongCount = totalSongCount,
                        playlistCreated = playlistCreated,
                        isCreatingPlaylist = isCreatingPlaylist,
                        onCreatePlaylist = onCreatePlaylist,
                        onNewSession = onResetSession
                    )
                }
            }
        }

        // Search bottom sheet
        if (showSearchSheet) {
            SongSearchBottomSheet(
                searchResults = searchResults,
                isSearching = isSearching,
                searchError = searchError,
                onSearch = onSearchSongs,
                onSelect = { result ->
                    onTagSong(result)
                    showSearchSheet = false
                },
                onDismiss = { showSearchSheet = false },
                onClearError = onClearSearchError
            )
        }
    }
}

// Constant mirrored from SessionManager for UI display (commonMain can't reference androidMain)
private const val SessionManager_MIN_RECORDING_SEC = 60

@Composable
private fun NotStartedContent(
    hrvMetrics: HrvMetrics?,
    notificationListenerEnabled: Boolean,
    onStartSession: () -> Unit,
    onRequestNotificationListener: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Coherence Playlist Session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Search for a song, tap it, and it opens in YouTube Music. We detect playback automatically and track your cardiac coherence.\n\nStay still and listen for at least 60 seconds per song to log a valid reading.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (hrvMetrics != null) {
                Text(
                    text = "Coherence: ${(hrvMetrics.coherenceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Collecting HRV data...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!notificationListenerEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Notification access required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "HeartSyncRadio needs notification access to detect what's playing in YouTube Music.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRequestNotificationListener) {
                            Text("Grant Access")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Requires YouTube Music app installed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStartSession,
                enabled = notificationListenerEnabled
            ) {
                Text("Start Session")
            }
        }
    }
}

@Composable
private fun WaitingForPlaybackCard(pendingSong: SearchResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pendingSong != null) {
                Text(
                    text = pendingSong.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = pendingSong.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = "Waiting for YouTube Music playback...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CalibratingNotice() {
    Text(
        text = "Stay still — calibrating before recording",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CoherenceHeader(hrvMetrics: HrvMetrics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Coherence",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = if (hrvMetrics != null)
                        "${(hrvMetrics.coherenceScore * 100).toInt()}%"
                    else "---",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (hrvMetrics != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RMSSD",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${hrvMetrics.rmssd.toInt()} ms",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentSongCard(
    song: TaggedSong?,
    statusText: String,
    isSettling: Boolean,
    coherence: Double? = null
) {
    if (song == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSettling)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = song.searchResult.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = song.searchResult.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSettling) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (coherence != null && !isSettling) {
                    Text(
                        text = "${(coherence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (isSettling) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SessionResultsList(
    results: List<SongSessionResult>,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Completed songs will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(results) { result ->
                SongResultCard(result = result)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EndSessionButton(onEndSession: () -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(
        onClick = onEndSession,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "End Session",
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EndedContent(
    sessionResults: List<SongSessionResult>,
    totalSongCount: Long,
    playlistCreated: String?,
    isCreatingPlaylist: Boolean,
    onCreatePlaylist: () -> Unit,
    onNewSession: () -> Unit
) {
    val validResults = sessionResults.filter { it.isValid }
    val bestSong = validResults.maxByOrNull { it.avgCoherence }
    val avgCoherence = if (validResults.isNotEmpty())
        validResults.map { it.avgCoherence }.average() else 0.0

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Session Complete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (validResults.isEmpty() && sessionResults.isEmpty()) {
            // No songs recorded at all — early end
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Session ended early",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No data recorded. Listen to a song for at least 60 seconds to log a valid coherence reading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Songs Recorded", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${validResults.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Coherence", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${(avgCoherence * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (bestSong != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Best Song", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${bestSong.searchResult.title} — ${(bestSong.avgCoherence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Invalid songs notice
            val invalidCount = sessionResults.count { !it.isValid }
            if (invalidCount > 0) {
                Text(
                    text = "$invalidCount song(s) had less than 60s of data — not counted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Song results list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sessionResults) { result ->
                    SongResultCard(result = result)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playlist creation
            if (playlistCreated != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "Playlist created on YouTube Music!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (totalSongCount >= 3) {
                Button(
                    onClick = onCreatePlaylist,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreatingPlaylist
                ) {
                    if (isCreatingPlaylist) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text("Create Coherence Playlist")
                }
            } else {
                Text(
                    text = "${totalSongCount}/3 songs — listen to ${3 - totalSongCount} more for your first playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onNewSession,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New Session")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongSearchBottomSheet(
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    searchError: String?,
    onSearch: (String) -> Unit,
    onSelect: (SearchResult) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Search YouTube Music",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Song or artist") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { if (query.isNotBlank()) onSearch(query) },
                modifier = Modifier.fillMaxWidth(),
                enabled = query.isNotBlank() && !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text("Search")
            }

            searchError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (searchResults.isEmpty() && !isSearching) {
                Text(
                    text = "Search for a song to tag it to your session",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn {
                    items(searchResults) { result ->
                        Card(
                            onClick = { onSelect(result) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = result.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = result.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                result.duration?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
