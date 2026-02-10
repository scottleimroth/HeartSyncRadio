package com.heartsyncradio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartsyncradio.ble.HrDeviceManager
import com.heartsyncradio.music.DetectedTrack
import com.heartsyncradio.music.MusicApiClient
import com.heartsyncradio.music.MusicDetectionService
import com.heartsyncradio.music.SearchResult
import com.heartsyncradio.music.SessionManager
import com.heartsyncradio.music.SessionPhase
import com.heartsyncradio.music.SongCoherenceRepository
import com.heartsyncradio.music.SongSessionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val deviceManagerProvider: () -> HrDeviceManager,
    private val musicApiClient: MusicApiClient,
    private val repository: SongCoherenceRepository
) : ViewModel() {

    private val sessionManager = SessionManager()

    val sessionPhase: StateFlow<SessionPhase> = sessionManager.phase
    val currentSong = sessionManager.currentSong
    val pendingSong = sessionManager.pendingSong
    val sessionResults: StateFlow<List<SongSessionResult>> = sessionManager.results
    val settleCountdownSec = sessionManager.settleCountdownSec
    val recordingDurationSec = sessionManager.recordingDurationSec

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _playlistCreated = MutableStateFlow<String?>(null)
    val playlistCreated: StateFlow<String?> = _playlistCreated.asStateFlow()

    private val _isCreatingPlaylist = MutableStateFlow(false)
    val isCreatingPlaylist: StateFlow<Boolean> = _isCreatingPlaylist.asStateFlow()

    val totalSongCount = repository.songCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val topSongs = repository.topCoherenceSongs(20)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var metricsCollectionJob: kotlinx.coroutines.Job? = null
    private var playbackMonitorJob: kotlinx.coroutines.Job? = null

    fun startSession() {
        sessionManager.startSession()
        startMetricsCollection()
        startPlaybackMonitoring()
    }

    private fun startMetricsCollection() {
        metricsCollectionJob?.cancel()
        metricsCollectionJob = viewModelScope.launch {
            deviceManagerProvider().hrvMetrics.collect { metrics ->
                if (metrics != null && sessionPhase.value in listOf(
                        SessionPhase.ACTIVE_SETTLING,
                        SessionPhase.ACTIVE_RECORDING
                    )
                ) {
                    sessionManager.recordMetrics(metrics, System.currentTimeMillis())
                }
            }
        }
    }

    private fun startPlaybackMonitoring() {
        playbackMonitorJob?.cancel()

        // Snapshot current state so we ignore whatever is already playing.
        // We only react to changes AFTER the user selects a song via search.
        val baselineTrack = MusicDetectionService.currentTrack.value
        var lastTrack: DetectedTrack? = baselineTrack
        var hasUserSelectedSong = false

        playbackMonitorJob = viewModelScope.launch {
            // Watch for user selecting a song (phase moves to WAITING_PLAYBACK)
            launch {
                sessionManager.phase.collect { phase ->
                    if (phase == SessionPhase.ACTIVE_WAITING_PLAYBACK) {
                        hasUserSelectedSong = true
                    }
                }
            }
            // Monitor track changes — only react after user has selected a song
            launch {
                MusicDetectionService.currentTrack.drop(1).collect { track ->
                    if (!hasUserSelectedSong) return@collect
                    val isPlaying = MusicDetectionService.isPlaying.value
                    val now = System.currentTimeMillis()

                    if (track != null && isPlaying && track != lastTrack) {
                        if (sessionPhase.value == SessionPhase.ACTIVE_WAITING_PLAYBACK ||
                            sessionPhase.value == SessionPhase.ACTIVE_NO_SONG
                        ) {
                            sessionManager.onPlaybackDetected(
                                track.title, track.artist, now
                            )
                        } else {
                            sessionManager.onSongChanged(
                                track.title, track.artist, now
                            )
                        }
                    }
                    lastTrack = track
                }
            }
            // Monitor play/pause state
            launch {
                MusicDetectionService.isPlaying.drop(1).collect { playing ->
                    if (!hasUserSelectedSong) return@collect
                    val track = MusicDetectionService.currentTrack.value
                    val now = System.currentTimeMillis()

                    if (!playing) {
                        sessionManager.onPlaybackStopped(now)
                    } else if (playing && track != null && track != baselineTrack) {
                        if (sessionPhase.value == SessionPhase.ACTIVE_WAITING_PLAYBACK ||
                            sessionPhase.value == SessionPhase.ACTIVE_NO_SONG
                        ) {
                            sessionManager.onPlaybackDetected(
                                track.title, track.artist, now
                            )
                            lastTrack = track
                        }
                    }
                }
            }
        }
    }

    fun searchSongs(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                _searchResults.value = musicApiClient.search(query)
            } catch (e: Exception) {
                _searchError.value = "Search failed. Check your connection."
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun selectSong(result: SearchResult) {
        sessionManager.selectSong(result)
        _searchResults.value = emptyList()
    }

    fun endSession() {
        val now = System.currentTimeMillis()
        sessionManager.endSession(now)
        metricsCollectionJob?.cancel()
        playbackMonitorJob?.cancel()
        MusicDetectionService.pausePlayback()

        // Save valid results to database
        viewModelScope.launch {
            for (result in sessionManager.results.value) {
                if (result.isValid) {
                    repository.insertSong(
                        videoId = result.searchResult.videoId,
                        title = result.searchResult.title,
                        artist = result.searchResult.artist,
                        avgCoherence = result.avgCoherence,
                        avgRmssd = result.avgRmssd,
                        meanHr = result.meanHr,
                        durationListenedSec = result.durationListenedSec.toLong(),
                        sessionDate = now
                    )
                }
            }
        }
    }

    fun createPlaylist() {
        viewModelScope.launch {
            _isCreatingPlaylist.value = true
            try {
                val songs = topSongs.value
                if (songs.isEmpty()) return@launch

                val songIds = songs.map { it.video_id }
                val playlistId = musicApiClient.createPlaylist(
                    title = "HrvXo Coherence Playlist",
                    description = "Songs ranked by cardiac coherence response",
                    songIds = songIds
                )
                _playlistCreated.value = playlistId
            } catch (e: Exception) {
                _searchError.value = "Failed to create playlist. Try again."
            } finally {
                _isCreatingPlaylist.value = false
            }
        }
    }

    fun resetSession() {
        metricsCollectionJob?.cancel()
        playbackMonitorJob?.cancel()
        sessionManager.reset()
        _searchResults.value = emptyList()
        _playlistCreated.value = null
        _searchError.value = null
    }

    fun clearSearchError() {
        _searchError.value = null
    }
}
