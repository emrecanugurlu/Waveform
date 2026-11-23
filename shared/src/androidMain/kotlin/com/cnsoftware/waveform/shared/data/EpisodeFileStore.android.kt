package com.cnsoftware.waveform.shared.data

import com.cnsoftware.waveform.shared.player.PlatformContextHolder
import java.io.File
import java.io.FileOutputStream

internal class DirectoryEpisodeFileStore(private val episodesDir: File) : EpisodeFileStore {
    private val indexFile: File = File(episodesDir, "downloaded_ids.txt")

    init {
        episodesDir.mkdirs()
    }

    private fun readIndexLocked(): Set<String> {
        if (!indexFile.exists()) return emptySet()
        return indexFile.readLines().filter { it.isNotBlank() }.toSet()
    }

    private fun findExistingFile(episodeId: String): File? {
        val base = safeEpisodeFileBase(episodeId)
        for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
            val candidate = File(episodesDir, "$base.$ext")
            if (candidate.exists() && candidate.length() > 0L) return candidate
        }
        return null
    }

    override fun getLocalFileUri(episodeId: String): String? =
        findExistingFile(episodeId)?.toURI()?.toString()

    override fun openAudioSink(episodeId: String, audioUrl: String): AudioSink {
        episodesDir.mkdirs()
        val base = safeEpisodeFileBase(episodeId)
        val target = File(episodesDir, safeEpisodeFileName(episodeId, audioUrl))
        val temp = File.createTempFile("${target.name}.", ".part", episodesDir)
        val out = FileOutputStream(temp)
        return object : AudioSink {
            override fun write(buffer: ByteArray, length: Int) {
                out.write(buffer, 0, length)
            }

            override fun commit(): String {
                var renamed = false
                try {
                    out.use { stream ->
                        stream.flush()
                        stream.fd.sync()
                    }
                    if (!temp.renameTo(target)) {
                        if (!(target.exists() && target.delete() && temp.renameTo(target))) {
                            error("İndirilen dosya kaydedilemedi.")
                        }
                    }
                    renamed = true
                    for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
                        val sibling = File(episodesDir, "$base.$ext")
                        if (sibling.name != target.name && sibling.exists()) {
                            sibling.delete()
                        }
                    }
                    synchronized(FileLocks.forPath(indexFile)) {
                        writeTextAtomically(indexFile, (readIndexLocked() + episodeId).joinToString("\n"))
                    }
                    return target.toURI().toString()
                } finally {
                    if (!renamed) {
                        runCatching { out.close() }
                        temp.delete()
                    }
                }
            }

            override fun abort() {
                runCatching { out.close() }
                temp.delete()
            }
        }
    }

    override fun deleteAudio(episodeId: String) {
        val base = safeEpisodeFileBase(episodeId)
        for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
            val file = File(episodesDir, "$base.$ext")
            if (file.exists()) file.delete()
        }
        synchronized(FileLocks.forPath(indexFile)) {
            writeTextAtomically(indexFile, (readIndexLocked() - episodeId).joinToString("\n"))
        }
    }

    override fun listDownloadedIds(): Set<String> =
        synchronized(FileLocks.forPath(indexFile)) { readIndexLocked() }
}

actual class PlatformEpisodeFileStore actual constructor() : EpisodeFileStore {
    private val delegate: DirectoryEpisodeFileStore by lazy {
        DirectoryEpisodeFileStore(File(PlatformContextHolder.get().filesDir, "episodes"))
    }

    actual override fun getLocalFileUri(episodeId: String): String? = delegate.getLocalFileUri(episodeId)

    actual override fun openAudioSink(episodeId: String, audioUrl: String): AudioSink =
        delegate.openAudioSink(episodeId, audioUrl)

    actual override fun deleteAudio(episodeId: String) = delegate.deleteAudio(episodeId)

    actual override fun listDownloadedIds(): Set<String> = delegate.listDownloadedIds()
}
