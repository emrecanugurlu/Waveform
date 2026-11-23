package com.cnsoftware.waveform.shared.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PlatformContextHolder {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getOrNull(): Context? = if (::appContext.isInitialized) appContext else null

    fun get(): Context = appContext
}

actual class PodcastAudioPlayer actual constructor() : AudioPlayer {
    private companion object {
        const val EXTRA_PUB_DATE = "pubDate"
        const val EXTRA_DURATION_TEXT = "durationText"
        // Oturum meta verisi (Binder) boyut sinirina takilmasin.
        const val MAX_DESCRIPTION_CHARS = 2000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    actual override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingPlayAction: ((MediaController) -> Unit)? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val context = PlatformContextHolder.get()
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PodcastPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { controller ->
                        mediaController = controller
                        attachListener(controller)
                        // Uygulama kapatilip yeniden acildiginda servis hala calabilir: durumu ondan al.
                        if (pendingPlayAction == null) restoreFromController(controller)
                        pendingPlayAction?.invoke(controller)
                        pendingPlayAction = null
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                errorMessage = error.localizedMessage ?: "Oynatma servisine bağlanılamadı"
                            )
                        }
                    }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    /** Servisteki mevcut oynatmayi (varsa) arayuz durumuna yansitir. */
    private fun restoreFromController(controller: MediaController) {
        if (_state.value.currentEpisode != null) return
        val item = controller.currentMediaItem ?: return
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        val audioUrl = item.localConfiguration?.uri?.toString().orEmpty()
        val episode = Episode(
            id = item.mediaId,
            title = metadata.title?.toString().orEmpty(),
            description = metadata.description?.toString().orEmpty(),
            audioUrl = audioUrl,
            pubDate = extras?.getString(EXTRA_PUB_DATE).orEmpty(),
            durationText = extras?.getString(EXTRA_DURATION_TEXT).orEmpty(),
            imageUrl = metadata.artworkUri?.toString(),
            podcastTitle = metadata.artist?.toString().orEmpty()
        )
        if (episode.id.isEmpty()) return
        _state.update {
            it.copy(
                currentEpisode = episode,
                isPlaying = controller.isPlaying,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                isEnded = controller.playbackState == Player.STATE_ENDED,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.coerceAtLeast(0L),
                playbackSpeed = controller.playbackParameters.speed
            )
        }
        if (controller.isPlaying) startProgressLoop()
    }

    private fun attachListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressLoop() else stopProgressLoop()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val ended = playbackState == Player.STATE_ENDED
                _state.update {
                    val duration = controller.duration.coerceAtLeast(0L)
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        isEnded = ended,
                        durationMs = duration,
                        // Ilerleme dongusu durdugu icin son konumu burada kesinlestir.
                        positionMs = if (ended && duration > 0L) duration else it.positionMs
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "Ses oynatılamadı"
                    )
                }
            }
        })
    }

    actual override fun play(episode: Episode, startPositionMs: Long) {
        val safeStart = startPositionMs.coerceAtLeast(0L)
        _state.update {
            it.copy(
                currentEpisode = episode,
                isBuffering = true,
                isEnded = false,
                errorMessage = null,
                positionMs = safeStart,
                durationMs = 0L
            )
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(episode.podcastTitle)
            .setDescription(episode.description.take(MAX_DESCRIPTION_CHARS))
            .setExtras(
                Bundle().apply {
                    putString(EXTRA_PUB_DATE, episode.pubDate)
                    putString(EXTRA_DURATION_TEXT, episode.durationText)
                }
            )
            .apply {
                episode.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    setArtworkUri(Uri.parse(url))
                }
            }
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(episode.audioUrl)
            .setMediaMetadata(metadata)
            .build()

        val action: (MediaController) -> Unit = { controller ->
            controller.setMediaItem(mediaItem, safeStart)
            controller.setPlaybackSpeed(_state.value.playbackSpeed)
            controller.prepare()
            controller.play()
        }

        val controller = mediaController
        if (controller != null) {
            action(controller)
        } else {
            pendingPlayAction = action
        }
    }

    actual override fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else if (_state.value.currentEpisode != null) {
            when (controller.playbackState) {
                Player.STATE_ENDED -> {
                    controller.seekTo(0L)
                    _state.update { it.copy(positionMs = 0L, isEnded = false, errorMessage = null) }
                }
                Player.STATE_IDLE -> {
                    controller.prepare()
                    _state.update { it.copy(isBuffering = true, errorMessage = null) }
                }
            }
            controller.play()
        }
    }

    actual override fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val target = positionMs.coerceAtLeast(0L)
        controller.seekTo(target)
        _state.update { it.copy(positionMs = target, isEnded = false) }
    }

    actual override fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    actual override fun release() {
        stopProgressLoop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        scope.cancel()
    }

    private fun startProgressLoop() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    _state.update {
                        it.copy(
                            positionMs = controller.currentPosition.coerceAtLeast(0L),
                            durationMs = controller.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }
}
