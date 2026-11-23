package com.cnsoftware.waveform.shared.data

/**
 * Bir bolumun sesini parca parca diske yazmak icin kullanilir. Veri once gecici bir dosyaya
 * yazilir; [commit] cagrilana kadar indirilmis sayilmaz, [abort] yarim dosyayi siler.
 */
interface AudioSink {
    fun write(buffer: ByteArray, length: Int)
    /** Dosyayi kalici hale getirir ve yerel URI'sini dondurur. */
    fun commit(): String
    fun abort()
}

interface EpisodeFileStore {
    fun getLocalFileUri(episodeId: String): String?
    fun openAudioSink(episodeId: String, audioUrl: String = ""): AudioSink
    fun deleteAudio(episodeId: String)
    fun listDownloadedIds(): Set<String>
}

internal val SUPPORTED_AUDIO_EXTENSIONS = listOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac")

internal fun safeEpisodeFileBase(episodeId: String): String {
    var hash = -3750763034362895579L // FNV-1a 64-bit offset basis
    for (byte in episodeId.encodeToByteArray()) {
        hash = hash xor (byte.toLong() and 0xffL)
        hash *= 1099511628211L
    }
    return hash.toULong().toString(16)
}

internal fun audioExtensionFromUrl(audioUrl: String): String {
    val path = audioUrl.substringBefore('?').substringBefore('#')
    val lastSegment = path.substringAfterLast('/')
    val ext = lastSegment.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return if (ext in SUPPORTED_AUDIO_EXTENSIONS) ext else "mp3"
}

internal fun safeEpisodeFileName(episodeId: String, audioUrl: String = ""): String =
    "${safeEpisodeFileBase(episodeId)}.${audioExtensionFromUrl(audioUrl)}"

expect class PlatformEpisodeFileStore() : EpisodeFileStore {
    override fun getLocalFileUri(episodeId: String): String?
    override fun openAudioSink(episodeId: String, audioUrl: String): AudioSink
    override fun deleteAudio(episodeId: String)
    override fun listDownloadedIds(): Set<String>
}
