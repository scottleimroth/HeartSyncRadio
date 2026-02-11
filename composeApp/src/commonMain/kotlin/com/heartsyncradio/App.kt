package com.heartsyncradio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.heartsyncradio.hrv.HrvMetrics
import com.heartsyncradio.model.ConnectionState
import com.heartsyncradio.model.HeartRateData
import com.heartsyncradio.model.PolarDeviceInfo
import com.heartsyncradio.music.SearchResult
import com.heartsyncradio.music.SessionPhase
import com.heartsyncradio.music.SongSessionResult
import com.heartsyncradio.music.TaggedSong
import com.heartsyncradio.ui.HomeScreen
import com.heartsyncradio.ui.SessionScreen

enum class AppScreen { HOME, SESSION }

@Composable
fun App(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    // Home parameters
    connectionState: ConnectionState,
    heartRateData: HeartRateData?,
    scannedDevices: List<PolarDeviceInfo>,
    isScanning: Boolean,
    batteryLevel: Int?,
    error: String?,
    permissionsGranted: Boolean,
    hrvMetrics: HrvMetrics?,
    selectedDeviceMode: String?,
    onSelectDeviceMode: (String) -> Unit,
    onChangeDeviceMode: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onClearError: () -> Unit,
    onRequestPermissions: () -> Unit,
    // Session parameters
    sessionPhase: SessionPhase = SessionPhase.NOT_STARTED,
    sessionCurrentSong: TaggedSong? = null,
    pendingSong: SearchResult? = null,
    sessionResults: List<SongSessionResult> = emptyList(),
    settleCountdownSec: Int = 0,
    recordingDurationSec: Int = 0,
    searchResults: List<SearchResult> = emptyList(),
    isSearching: Boolean = false,
    searchError: String? = null,
    totalSongCount: Long = 0,
    playlistCreated: String? = null,
    isCreatingPlaylist: Boolean = false,
    isMoving: Boolean = false,
    notificationListenerEnabled: Boolean = false,
    onStartSession: () -> Unit = {},
    onEndSession: () -> Unit = {},
    onSearchSongs: (String) -> Unit = {},
    onTagSong: (SearchResult) -> Unit = {},
    onCreatePlaylist: () -> Unit = {},
    onResetSession: () -> Unit = {},
    onClearSearchError: () -> Unit = {},
    onRequestNotificationListener: () -> Unit = {}
) {
    MaterialTheme {
        when (currentScreen) {
            AppScreen.HOME -> HomeScreen(
                connectionState = connectionState,
                heartRateData = heartRateData,
                scannedDevices = scannedDevices,
                isScanning = isScanning,
                batteryLevel = batteryLevel,
                error = error,
                permissionsGranted = permissionsGranted,
                hrvMetrics = hrvMetrics,
                selectedDeviceMode = selectedDeviceMode,
                onSelectDeviceMode = onSelectDeviceMode,
                onChangeDeviceMode = onChangeDeviceMode,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onConnectDevice = onConnectDevice,
                onDisconnect = onDisconnect,
                onClearError = onClearError,
                onRequestPermissions = onRequestPermissions,
                onStartSession = { onNavigate(AppScreen.SESSION) }
            )
            AppScreen.SESSION -> SessionScreen(
                sessionPhase = sessionPhase,
                currentSong = sessionCurrentSong,
                pendingSong = pendingSong,
                sessionResults = sessionResults,
                settleCountdownSec = settleCountdownSec,
                recordingDurationSec = recordingDurationSec,
                hrvMetrics = hrvMetrics,
                searchResults = searchResults,
                isSearching = isSearching,
                searchError = searchError,
                totalSongCount = totalSongCount,
                playlistCreated = playlistCreated,
                isCreatingPlaylist = isCreatingPlaylist,
                isMoving = isMoving,
                notificationListenerEnabled = notificationListenerEnabled,
                onStartSession = onStartSession,
                onEndSession = onEndSession,
                onSearchSongs = onSearchSongs,
                onTagSong = onTagSong,
                onCreatePlaylist = onCreatePlaylist,
                onResetSession = onResetSession,
                onClearSearchError = onClearSearchError,
                onRequestNotificationListener = onRequestNotificationListener,
                onBack = {
                    onResetSession()
                    onNavigate(AppScreen.HOME)
                }
            )
        }
    }
}
