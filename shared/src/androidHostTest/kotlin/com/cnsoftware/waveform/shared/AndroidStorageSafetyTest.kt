package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.data.DirectoryEpisodeFileStore
import com.cnsoftware.waveform.shared.data.audioExtensionFromUrl
import com.cnsoftware.waveform.shared.data.writeTextAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidStorageSafetyTest {

    private val tempRoot: File = createTempDirectory("waveform-storage-test").toFile()

    @AfterTest
    fun cleanup() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun writeTextAtomicallyWritesOverwritesUtf8AndLeavesNoTempFiles() {
        val target = File(tempRoot, "nested/subscriptions.json")

        writeTextAtomically(target, """{"title":"İlk kayıt"}""")
        assertTrue(target.exists())
        assertEquals("""{"title":"İlk kayıt"}""", target.readText(Charsets.UTF_8))

        writeTextAtomically(target, """{"title":"Güncel içerik — şğıüöç"}""")
        assertEquals("""{"title":"Güncel içerik — şğıüöç"}""", target.readText(Charsets.UTF_8))

        val leftoverTmp = target.parentFile?.listFiles()?.filter { it.name.endsWith(".tmp") }.orEmpty()
        assertTrue(leftoverTmp.isEmpty(), "Gecici .tmp dosyalari temizlenmeli: $leftoverTmp")
    }

    @Test
    fun directoryEpisodeFileStoreCommitDeleteAndAbortWorkSafely() {
        val store = DirectoryEpisodeFileStore(File(tempRoot, "episodes"))

        val sink1 = store.openAudioSink("ep-1")
        val bytes = "ses-verisi-123".toByteArray()
        sink1.write(bytes, bytes.size)
        val uri = sink1.commit()

        assertTrue(uri.startsWith("file:"))
        assertEquals(uri, store.getLocalFileUri("ep-1"))
        assertEquals(setOf("ep-1"), store.listDownloadedIds())

        val sinkAbort = store.openAudioSink("ep-abort")
        sinkAbort.write(bytes, bytes.size)
        sinkAbort.abort()

        assertNull(store.getLocalFileUri("ep-abort"))
        assertEquals(setOf("ep-1"), store.listDownloadedIds())
        val leftoverPart = File(tempRoot, "episodes").listFiles()?.filter { it.name.endsWith(".part") }.orEmpty()
        assertTrue(leftoverPart.isEmpty(), "Iptal edilen .part dosyalari kalmamali")

        store.deleteAudio("ep-1")
        assertNull(store.getLocalFileUri("ep-1"))
        assertFalse(store.listDownloadedIds().contains("ep-1"))
    }

    @Test
    fun concurrentCommitsPreserveEveryEpisodeIdInIndex() = runTest {
        val store = DirectoryEpisodeFileStore(File(tempRoot, "concurrent-episodes"))
        val episodeIds = (1..20).map { "ep-$it" }

        episodeIds.map { id ->
            async(Dispatchers.Default) {
                val sink = store.openAudioSink(id)
                val data = "payload-$id".toByteArray()
                sink.write(data, data.size)
                sink.commit()
            }
        }.awaitAll()

        assertEquals(episodeIds.toSet(), store.listDownloadedIds())
        episodeIds.forEach { id ->
            assertNotNull(store.getLocalFileUri(id), "$id dosyasi diskte bulunmali")
        }
    }

    @Test
    fun audioExtensionDetectionAndM4aStorageWorkCleanly() {
        assertEquals("m4a", audioExtensionFromUrl("https://cdn.example.com/ep1.M4A?token=123#t=10"))
        assertEquals("ogg", audioExtensionFromUrl("https://cdn.example.com/audio.ogg"))
        assertEquals("mp3", audioExtensionFromUrl("https://cdn.example.com/stream"))
        assertEquals("mp3", audioExtensionFromUrl("https://cdn.example.com/file.unknown"))

        val store = DirectoryEpisodeFileStore(File(tempRoot, "ext-episodes"))
        val sink = store.openAudioSink("ep-m4a", "https://cdn.example.com/show/ep1.m4a?sig=abc")
        val payload = "m4a-data".toByteArray()
        sink.write(payload, payload.size)
        val uri = sink.commit()

        assertTrue(uri.endsWith(".m4a"), "Kaydedilen dosya .m4a uzantisini korumali: $uri")
        assertEquals(uri, store.getLocalFileUri("ep-m4a"))
        assertEquals(setOf("ep-m4a"), store.listDownloadedIds())

        store.deleteAudio("ep-m4a")
        assertNull(store.getLocalFileUri("ep-m4a"))
        assertFalse(store.listDownloadedIds().contains("ep-m4a"))
    }
}
