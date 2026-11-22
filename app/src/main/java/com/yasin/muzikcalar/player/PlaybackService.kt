package com.yasin.muzikcalar.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.yasin.muzikcalar.MainActivity
import com.yasin.muzikcalar.data.AppDatabase
import com.yasin.muzikcalar.data.TrackDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Arka planda çalmayı, bildirimi ve kilit ekranı kontrollerini sağlayan servis.
 * Her dosyada kalınan yeri ve son çalınma zamanını da burada kaydeder.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var trackDao: TrackDao
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Servis kapanırken de kayıt bitebilsin diye ayrı ve iptal edilmeyen kapsam.
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicSaveJob: Job? = null

    // Player.Listener Media3'ün kendi arayüzü; çalma olaylarını almak için zorunlu.
    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                markCurrentAsPlayed()
                startPeriodicSave()
            } else {
                periodicSaveJob?.cancel()
                saveCurrentPosition()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val oldId = oldPosition.mediaItem?.mediaId ?: return
            if (oldId == newPosition.mediaItem?.mediaId) {
                return
            }
            // Parça sonuna kadar dinlendiyse bir dahaki sefere baştan başlasın.
            val position = if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) 0L else oldPosition.positionMs
            savePosition(oldId, position)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem == null ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            ) {
                return
            }
            val player = mediaSession?.player ?: return
            if (player.playWhenReady) {
                markCurrentAsPlayed()
            }
            // Sıradaki parçaya geçince o parçada kalınan yerden devam et.
            mainScope.launch {
                val saved = trackDao.getPosition(mediaItem.mediaId) ?: 0L
                if (saved > 0 && player.currentMediaItem?.mediaId == mediaItem.mediaId) {
                    player.seekTo(saved)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        trackDao = AppDatabase.get(this).trackDao()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(playerListener)
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // MediaSession.Callback Media3'ün kendi arayüzü; uygulamadan gelen parçaların
        // dosya adresini geri koymak ve kalınan yerden başlatmak için zorunlu.
        val callback = object : MediaSession.Callback {
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                return Futures.immediateFuture(resolveUris(mediaItems))
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                // Yeni liste gelmeden önce çalan parçanın yerini kaydet.
                saveCurrentPosition()
                val resolved = resolveUris(mediaItems)
                return mainScope.future {
                    val index = startIndex.coerceIn(0, maxOf(resolved.size - 1, 0))
                    val position = resolved.getOrNull(index)?.let { trackDao.getPosition(it.mediaId) } ?: 0L
                    MediaSession.MediaItemsWithStartPosition(resolved, index, position)
                }
            }
        }
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .setSessionActivity(openAppIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveCurrentPosition()
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveCurrentPosition()
        mainScope.cancel()
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun resolveUris(mediaItems: List<MediaItem>): MutableList<MediaItem> {
        return mediaItems.map { item ->
            item.buildUpon().setUri(Uri.parse(item.mediaId)).build()
        }.toMutableList()
    }

    private fun startPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = mainScope.launch {
            while (isActive) {
                delay(10_000)
                saveCurrentPosition()
            }
        }
    }

    private fun saveCurrentPosition() {
        val player = mediaSession?.player ?: return
        val mediaId = player.currentMediaItem?.mediaId ?: return
        val position = if (player.playbackState == Player.STATE_ENDED) 0L else player.currentPosition
        savePosition(mediaId, position)
    }

    private fun savePosition(mediaId: String, positionMs: Long) {
        saveScope.launch {
            trackDao.savePosition(mediaId, positionMs.coerceAtLeast(0L))
        }
    }

    private fun markCurrentAsPlayed() {
        val mediaId = mediaSession?.player?.currentMediaItem?.mediaId ?: return
        val now = System.currentTimeMillis()
        saveScope.launch {
            trackDao.markPlayed(mediaId, now)
        }
    }
}
