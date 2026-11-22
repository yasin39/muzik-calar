package com.yasin.muzikcalar.player

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.yasin.muzikcalar.data.TrackItem

/** Ekranlar ile arka plandaki çalma servisi arasındaki bağlantı. */
class PlayerConnection(context: Context) {

    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    var currentTitle by mutableStateOf<String?>(null)
        private set
    var currentFolder by mutableStateOf("")
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var hasNext by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var isConnected by mutableStateOf(false)
        private set

    val positionMs: Long
        get() = controller?.currentPosition ?: 0L

    val durationMs: Long
        get() = controller?.duration?.takeIf { it > 0 } ?: 0L

    // Player.Listener Media3'ün kendi arayüzü; durum değişikliklerini almak için zorunlu.
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState()
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture.addListener(
            {
                val connected = controllerFuture.get()
                controller = connected
                connected.addListener(listener)
                isConnected = true
                updateState()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    /** Listeyi verilen parçadan başlatır; başlangıç yeri serviste kalınan yerden belirlenir. */
    fun play(tracks: List<TrackItem>, index: Int) {
        val player = controller ?: return
        player.setMediaItems(toMediaItems(tracks), index, C.TIME_UNSET)
        player.prepare()
        player.play()
    }

    /** Uygulama açılışında son dinlenen parçayı çalmadan hazırlar (çalar boşsa). */
    fun prepareIfIdle(tracks: List<TrackItem>, index: Int) {
        val player = controller ?: return
        if (player.mediaItemCount > 0) {
            return
        }
        player.setMediaItems(toMediaItems(tracks), index, C.TIME_UNSET)
        player.prepare()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun changeSpeed(newSpeed: Float) {
        controller?.setPlaybackSpeed(newSpeed)
    }

    fun release() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun toMediaItems(tracks: List<TrackItem>): List<MediaItem> {
        return tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.folderName)
                        .build()
                )
                .build()
        }
    }

    private fun updateState() {
        val player = controller ?: return
        val metadata = player.currentMediaItem?.mediaMetadata
        currentTitle = metadata?.title?.toString()
        currentFolder = metadata?.artist?.toString() ?: ""
        isPlaying = player.isPlaying
        hasNext = player.hasNextMediaItem()
        speed = player.playbackParameters.speed
    }
}
