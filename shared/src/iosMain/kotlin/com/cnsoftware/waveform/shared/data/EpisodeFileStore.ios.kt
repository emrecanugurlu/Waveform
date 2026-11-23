package com.cnsoftware.waveform.shared.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.synchronizeFile
import platform.Foundation.writeData
import platform.Foundation.writeToFile

private val episodeIndexLock = NSLock()

private inline fun <T> withEpisodeIndexLock(block: () -> T): T {
    episodeIndexLock.lock()
    return try {
        block()
    } finally {
        episodeIndexLock.unlock()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class PlatformEpisodeFileStore actual constructor() : EpisodeFileStore {
    private val episodesDir: String by lazy {
        val docs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull()?.toString() ?: ""
        val dir = "$docs/episodes"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        dir
    }

    private val indexPath: String get() = "$episodesDir/downloaded_ids.txt"

    actual override fun getLocalFileUri(episodeId: String): String? {
        val base = safeEpisodeFileBase(episodeId)
        val fileManager = NSFileManager.defaultManager
        for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
            val path = "$episodesDir/$base.$ext"
            if (fileManager.fileExistsAtPath(path)) {
                return NSURL.fileURLWithPath(path).absoluteString
            }
        }
        return null
    }

    actual override fun openAudioSink(episodeId: String, audioUrl: String): AudioSink {
        val base = safeEpisodeFileBase(episodeId)
        val targetName = safeEpisodeFileName(episodeId, audioUrl)
        val path = "$episodesDir/$targetName"
        val tempPath = "$path.part"
        val fileManager = NSFileManager.defaultManager
        fileManager.removeItemAtPath(tempPath, error = null)
        check(fileManager.createFileAtPath(tempPath, contents = null, attributes = null)) {
            "Geçici indirme dosyası oluşturulamadı."
        }
        val handle = checkNotNull(NSFileHandle.fileHandleForWritingAtPath(tempPath)) {
            "Geçici indirme dosyası açılamadı."
        }
        return object : AudioSink {
            override fun write(buffer: ByteArray, length: Int) {
                if (length <= 0) return
                val data = buffer.usePinned { pinned ->
                    NSData.create(bytes = pinned.addressOf(0), length = length.toULong())
                }
                handle.writeData(data)
            }

            override fun commit(): String {
                handle.synchronizeFile()
                handle.closeFile()
                fileManager.removeItemAtPath(path, error = null)
                check(fileManager.moveItemAtPath(tempPath, toPath = path, error = null)) {
                    "İndirilen dosya kaydedilemedi."
                }
                for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
                    val siblingName = "$base.$ext"
                    if (siblingName != targetName) {
                        fileManager.removeItemAtPath("$episodesDir/$siblingName", error = null)
                    }
                }
                withEpisodeIndexLock {
                    writeIndexLocked(readIndexLocked() + episodeId)
                }
                return NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
            }

            override fun abort() {
                handle.closeFile()
                fileManager.removeItemAtPath(tempPath, error = null)
            }
        }
    }

    actual override fun deleteAudio(episodeId: String) {
        val base = safeEpisodeFileBase(episodeId)
        val fileManager = NSFileManager.defaultManager
        for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
            fileManager.removeItemAtPath("$episodesDir/$base.$ext", error = null)
        }
        withEpisodeIndexLock {
            writeIndexLocked(readIndexLocked() - episodeId)
        }
    }

    actual override fun listDownloadedIds(): Set<String> = withEpisodeIndexLock {
        readIndexLocked()
    }

    private fun readIndexLocked(): Set<String> {
        if (!NSFileManager.defaultManager.fileExistsAtPath(indexPath)) return emptySet()
        val content = NSString.stringWithContentsOfFile(
            path = indexPath,
            encoding = NSUTF8StringEncoding,
            error = null
        ) ?: return emptySet()
        return content.lines().filter { it.isNotBlank() }.toSet()
    }

    private fun writeIndexLocked(ids: Set<String>) {
        NSString.create(string = ids.joinToString("\n")).writeToFile(
            path = indexPath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    }
}
