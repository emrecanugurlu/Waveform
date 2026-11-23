package com.cnsoftware.waveform.shared.player

import com.cnsoftware.waveform.shared.model.Episode
import com.cnsoftware.waveform.shared.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val state: StateFlow<PlayerState>
    fun play(episode: Episode, startPositionMs: Long = 0L)
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun release()
}

expect class PodcastAudioPlayer() : AudioPlayer {
    override val state: StateFlow<PlayerState>
    override fun play(episode: Episode, startPositionMs: Long)
    override fun togglePlayPause()
    override fun seekTo(positionMs: Long)
    override fun setPlaybackSpeed(speed: Float)
    override fun release()
}
