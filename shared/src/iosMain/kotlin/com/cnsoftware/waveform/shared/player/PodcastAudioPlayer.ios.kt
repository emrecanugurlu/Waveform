package com.cnsoftware.waveform.shared.player

import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.PlayerState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.darwin.dispatch_get_main_queue

/**
 * Not: iOS'ta arka planda ses calismaya devam edebilmesi icin Xcode projesindeki Info.plist
 * dosyasinda UIBackgroundModes -> "audio" anahtarinin tanimli olmasi gerekir.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PodcastAudioPlayer actual constructor() : AudioPlayer {
    private val avPlayer = AVPlayer()
    private val _state = MutableStateFlow(PlayerState())
    actual override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var timeObserver: Any? = null
    private val notificationObservers = mutableListOf<Any>()
    private val remoteCommandTargets = mutableListOf<Pair<MPRemoteCommand, Any>>()

    init {
        setupRemoteCommands()

        // Konum, sure ve tamponlama bilgisini yarim saniyede bir yansit.
        timeObserver = avPlayer.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000),
            queue = dispatch_get_main_queue()
        ) { time ->
            val currentState = _state.value
            if (currentState.currentEpisode == null) return@addPeriodicTimeObserverForInterval

            val positionMs = secondsToMs(CMTimeGetSeconds(time))
            val durationMs = avPlayer.currentItem?.let { secondsToMs(CMTimeGetSeconds(it.duration)) }
            val buffering = avPlayer.timeControlStatus ==
                AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate

            // Tamponlama sonrasi baslatmada sistem hizi 1.0'a sifirladiysa secili hizi yeniden uygula.
            if (currentState.isPlaying &&
                avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying &&
                avPlayer.rate != currentState.playbackSpeed
            ) {
                avPlayer.rate = currentState.playbackSpeed
            }

            val durationChanged = durationMs != null && durationMs != currentState.durationMs
            _state.update {
                if (it.currentEpisode == null) {
                    it
                } else {
                    it.copy(
                        positionMs = positionMs ?: it.positionMs,
                        durationMs = durationMs ?: it.durationMs,
                        isBuffering = buffering && it.isPlaying
                    )
                }
            }
            if (durationChanged) updateNowPlayingInfo()
        }

        val center = NSNotificationCenter.defaultCenter
        notificationObservers += center.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            if (notification?.`object` !== avPlayer.currentItem) return@addObserverForName
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    isEnded = true,
                    positionMs = if (it.durationMs > 0L) it.durationMs else it.positionMs
                )
            }
            updateNowPlayingInfo()
        }
        notificationObservers += center.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            if (notification?.`object` !== avPlayer.currentItem) return@addObserverForName
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Bölüm oynatılamadı."
                )
            }
            updateNowPlayingInfo()
        }
    }

    private fun configureAudioSession() {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        }
    }

    private fun setupRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()

        fun register(command: MPRemoteCommand, block: (Any?) -> Long) {
            command.enabled = true
            val token = command.addTargetWithHandler { event -> block(event) }
            remoteCommandTargets += command to token
        }

        register(center.playCommand) {
            if (_state.value.currentEpisode == null) {
                MPRemoteCommandHandlerStatusCommandFailed
            } else {
                if (!_state.value.isPlaying) togglePlayPause()
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        register(center.pauseCommand) {
            if (_state.value.currentEpisode == null) {
                MPRemoteCommandHandlerStatusCommandFailed
            } else {
                if (_state.value.isPlaying) togglePlayPause()
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        register(center.togglePlayPauseCommand) {
            if (_state.value.currentEpisode == null) {
                MPRemoteCommandHandlerStatusCommandFailed
            } else {
                togglePlayPause()
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        center.skipBackwardCommand.preferredIntervals = listOf(10.0)
        register(center.skipBackwardCommand) {
            if (_state.value.currentEpisode == null) {
                MPRemoteCommandHandlerStatusCommandFailed
            } else {
                seekTo((_state.value.positionMs - 10_000L).coerceAtLeast(0L))
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        center.skipForwardCommand.preferredIntervals = listOf(30.0)
        register(center.skipForwardCommand) {
            if (_state.value.currentEpisode == null) {
                MPRemoteCommandHandlerStatusCommandFailed
            } else {
                val duration = _state.value.durationMs
                val target = _state.value.positionMs + 30_000L
                seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        register(center.changePlaybackPositionCommand) { event ->
            val positionEvent = event as? MPChangePlaybackPositionCommandEvent
                ?: return@register MPRemoteCommandHandlerStatusCommandFailed
            seekTo((positionEvent.positionTime * 1000.0).toLong())
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updateNowPlayingInfo() {
        val current = _state.value
        val episode = current.currentEpisode
        val center = MPNowPlayingInfoCenter.defaultCenter()
        if (episode == null) {
            center.nowPlayingInfo = null
            return
        }
        val info = mutableMapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to episode.title,
            MPMediaItemPropertyArtist to episode.podcastTitle,
            MPNowPlayingInfoPropertyElapsedPlaybackTime to (current.positionMs / 1000.0),
            MPNowPlayingInfoPropertyPlaybackRate to if (current.isPlaying) current.playbackSpeed.toDouble() else 0.0
        )
        if (current.durationMs > 0L) {
            info[MPMediaItemPropertyPlaybackDuration] = current.durationMs / 1000.0
        }
        center.nowPlayingInfo = info
    }

    actual override fun play(episode: Episode, startPositionMs: Long) {
        val url = NSURL.URLWithString(episode.audioUrl)
        if (url == null) {
            _state.update { it.copy(errorMessage = "Geçersiz ses adresi") }
            return
        }
        configureAudioSession()
        val item = AVPlayerItem.playerItemWithURL(url)
        avPlayer.replaceCurrentItemWithPlayerItem(item)
        val safeStart = startPositionMs.coerceAtLeast(0L)
        if (safeStart > 0L) {
            avPlayer.seekToTime(CMTimeMakeWithSeconds(safeStart / 1000.0, preferredTimescale = 1000))
        }
        avPlayer.play()
        avPlayer.rate = _state.value.playbackSpeed
        _state.update {
            it.copy(
                currentEpisode = episode,
                isPlaying = true,
                isBuffering = true,
                isEnded = false,
                positionMs = safeStart,
                durationMs = 0L,
                errorMessage = null
            )
        }
        updateNowPlayingInfo()
    }

    actual override fun togglePlayPause() {
        val current = _state.value
        if (current.currentEpisode == null) return
        if (current.isPlaying) {
            avPlayer.pause()
            _state.update { it.copy(isPlaying = false, isBuffering = false) }
        } else {
            configureAudioSession()
            // Bolum bittiyse basa sar, yoksa kaldigi yerden devam et.
            if (current.isEnded || (current.durationMs > 0L && current.positionMs >= current.durationMs)) {
                avPlayer.seekToTime(CMTimeMakeWithSeconds(0.0, preferredTimescale = 1000))
                _state.update { it.copy(positionMs = 0L) }
            }
            avPlayer.play()
            avPlayer.rate = current.playbackSpeed
            _state.update { it.copy(isPlaying = true, isEnded = false) }
        }
        updateNowPlayingInfo()
    }

    actual override fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        val seconds = target / 1000.0
        avPlayer.seekToTime(CMTimeMakeWithSeconds(seconds, preferredTimescale = 1000))
        _state.update { it.copy(positionMs = target, isEnded = false) }
        updateNowPlayingInfo()
    }

    actual override fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
        if (_state.value.isPlaying) {
            avPlayer.rate = speed
        }
        updateNowPlayingInfo()
    }

    actual override fun release() {
        avPlayer.pause()
        timeObserver?.let { avPlayer.removeTimeObserver(it) }
        timeObserver = null
        val center = NSNotificationCenter.defaultCenter
        notificationObservers.forEach { center.removeObserver(it) }
        notificationObservers.clear()
        remoteCommandTargets.forEach { (command, token) ->
            command.removeTarget(token)
        }
        remoteCommandTargets.clear()
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    /** NaN / sonsuz (sure henuz bilinmiyor) degerlerde null doner. */
    private fun secondsToMs(seconds: Double): Long? =
        if (seconds.isNaN() || seconds.isInfinite() || seconds < 0.0) null else (seconds * 1000).toLong()
}
