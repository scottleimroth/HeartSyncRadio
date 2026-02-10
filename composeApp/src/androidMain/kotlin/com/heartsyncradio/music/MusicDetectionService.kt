package com.heartsyncradio.music

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectedTrack(
    val title: String,
    val artist: String
)

class MusicDetectionService : NotificationListenerService() {

    companion object {
        private const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"

        private val _currentTrack = MutableStateFlow<DetectedTrack?>(null)
        val currentTrack: StateFlow<DetectedTrack?> = _currentTrack.asStateFlow()

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        private var controllerRef: MediaController? = null

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat?.contains(context.packageName) == true
        }

        fun pausePlayback() {
            controllerRef?.transportControls?.pause()
        }
    }

    private var ytMusicController: MediaController? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let { readMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            _isPlaying.value = state?.state == PlaybackState.STATE_PLAYING
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            connectToYouTubeMusic(controllers)
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MusicDetectionService::class.java)
        manager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
        connectToYouTubeMusic(manager.getActiveSessions(componentName))
    }

    override fun onListenerDisconnected() {
        ytMusicController?.unregisterCallback(mediaCallback)
        ytMusicController = null
        controllerRef = null
        _currentTrack.value = null
        _isPlaying.value = false
        super.onListenerDisconnected()
    }

    private fun connectToYouTubeMusic(controllers: List<MediaController>?) {
        ytMusicController?.unregisterCallback(mediaCallback)
        ytMusicController = controllers?.find { it.packageName == YT_MUSIC_PACKAGE }
        controllerRef = ytMusicController

        ytMusicController?.let { controller ->
            controller.registerCallback(mediaCallback)
            controller.metadata?.let { readMetadata(it) }
            _isPlaying.value =
                controller.playbackState?.state == PlaybackState.STATE_PLAYING
        }
    }

    private fun readMetadata(metadata: MediaMetadata) {
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        _currentTrack.value = DetectedTrack(title, artist)
    }
}
