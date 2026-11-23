package com.cnsoftware.waveform.shared.ui

import com.cnsoftware.waveform.shared.model.Episode

/** Calma sirasi (kuyruk) uzerindeki saf islemler; durumu degistirmez, yeni liste dondurur. */
internal object PlayQueue {

    /** Zaten siradaysa listeyi degistirmez. */
    fun addLast(queue: List<Episode>, episode: Episode): List<Episode> =
        if (queue.any { it.id == episode.id }) queue else queue + episode

    fun remove(queue: List<Episode>, episodeId: String): List<Episode> =
        queue.filterNot { it.id == episodeId }

    /** Bolumu bir asagi tasir; zaten sondaysa ya da listede yoksa listeyi degistirmez. */
    fun moveDown(queue: List<Episode>, episodeId: String): List<Episode> {
        val index = queue.indexOfFirst { it.id == episodeId }
        if (index < 0 || index >= queue.lastIndex) return queue
        return queue.toMutableList().also { it.add(index + 1, it.removeAt(index)) }
    }

    /** Bolumu bir yukari tasir; zaten basta ya da listede yoksa listeyi degistirmez. */
    fun moveUp(queue: List<Episode>, episodeId: String): List<Episode> {
        val index = queue.indexOfFirst { it.id == episodeId }
        if (index <= 0) return queue
        return queue.toMutableList().also { it.add(index - 1, it.removeAt(index)) }
    }
}
