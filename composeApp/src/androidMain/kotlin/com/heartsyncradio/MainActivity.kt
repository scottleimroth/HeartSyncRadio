package com.heartsyncradio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heartsyncradio.di.AppModule
import com.heartsyncradio.di.DeviceMode
import com.heartsyncradio.permission.BlePermissionHandler
import com.heartsyncradio.viewmodel.HomeViewModel
import com.heartsyncradio.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var sessionViewModel: SessionViewModel
    private var currentScreen by mutableStateOf(AppScreen.HOME)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onPermissionsResult(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this,
            AppModule.provideHomeViewModelFactory(this)
        )[HomeViewModel::class.java]

        sessionViewModel = ViewModelProvider(
            this,
            AppModule.provideSessionViewModelFactory(this)
        )[SessionViewModel::class.java]

        if (BlePermissionHandler.hasAllPermissions(this)) {
            viewModel.onPermissionsResult(true)
        }

        setContent {
            // Home state
            val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
            val heartRateData by viewModel.heartRateData.collectAsStateWithLifecycle()
            val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
            val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
            val batteryLevel by viewModel.batteryLevel.collectAsStateWithLifecycle()
            val error by viewModel.error.collectAsStateWithLifecycle()
            val permissionsGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()
            val hrvMetrics by viewModel.hrvMetrics.collectAsStateWithLifecycle()
            val selectedDeviceMode by viewModel.selectedDeviceMode.collectAsStateWithLifecycle()

            // Session state
            val sessionPhase by sessionViewModel.sessionPhase.collectAsStateWithLifecycle()
            val sessionCurrentSong by sessionViewModel.currentSong.collectAsStateWithLifecycle()
            val sessionResults by sessionViewModel.sessionResults.collectAsStateWithLifecycle()
            val settleCountdownSec by sessionViewModel.settleCountdownSec.collectAsStateWithLifecycle()
            val recordingDurationSec by sessionViewModel.recordingDurationSec.collectAsStateWithLifecycle()
            val searchResults by sessionViewModel.searchResults.collectAsStateWithLifecycle()
            val isSearching by sessionViewModel.isSearching.collectAsStateWithLifecycle()
            val searchError by sessionViewModel.searchError.collectAsStateWithLifecycle()
            val totalSongCount by sessionViewModel.totalSongCount.collectAsStateWithLifecycle()
            val playlistCreated by sessionViewModel.playlistCreated.collectAsStateWithLifecycle()
            val isCreatingPlaylist by sessionViewModel.isCreatingPlaylist.collectAsStateWithLifecycle()

            App(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
                // Home parameters
                connectionState = connectionState,
                heartRateData = heartRateData,
                scannedDevices = scannedDevices,
                isScanning = isScanning,
                batteryLevel = batteryLevel,
                error = error,
                permissionsGranted = permissionsGranted,
                hrvMetrics = hrvMetrics,
                selectedDeviceMode = selectedDeviceMode,
                onSelectDeviceMode = { mode ->
                    val deviceMode = when (mode) {
                        "polar" -> DeviceMode.POLAR
                        else -> DeviceMode.GENERIC_BLE
                    }
                    val manager = AppModule.switchMode(this, deviceMode)
                    viewModel.switchManager(manager, mode)
                },
                onChangeDeviceMode = {
                    viewModel.clearDeviceSelection()
                },
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onConnectDevice = viewModel::connectToDevice,
                onDisconnect = viewModel::disconnectFromDevice,
                onClearError = viewModel::clearError,
                onRequestPermissions = {
                    permissionLauncher.launch(
                        BlePermissionHandler.requiredPermissions().toTypedArray()
                    )
                },
                // Session parameters
                sessionPhase = sessionPhase,
                sessionCurrentSong = sessionCurrentSong,
                sessionResults = sessionResults,
                settleCountdownSec = settleCountdownSec,
                recordingDurationSec = recordingDurationSec,
                searchResults = searchResults,
                isSearching = isSearching,
                searchError = searchError,
                totalSongCount = totalSongCount,
                playlistCreated = playlistCreated,
                isCreatingPlaylist = isCreatingPlaylist,
                onStartSession = sessionViewModel::startSession,
                onEndSession = sessionViewModel::endSession,
                onSearchSongs = sessionViewModel::searchSongs,
                onTagSong = { result ->
                    sessionViewModel.tagSong(result)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/watch?v=${result.videoId}"))
                    startActivity(intent)
                },
                onCreatePlaylist = sessionViewModel::createPlaylist,
                onResetSession = sessionViewModel::resetSession,
                onClearSearchError = sessionViewModel::clearSearchError
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AppModule.shutDown()
        }
    }
}
