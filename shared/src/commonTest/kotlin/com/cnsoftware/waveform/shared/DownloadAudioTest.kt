package com.cnsoftware.waveform.shared

import com.cnsoftware.waveform.shared.data.AudioSink
import com.cnsoftware.waveform.shared.data.PodcastRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class RecordingSink : AudioSink {
    var bytes = 0
    var committed = false
    var aborted = false

    override fun write(buffer: ByteArray, length: Int) {
        bytes += length
    }

    override fun commit(): String {
        committed = true
        return "file://kayit"
    }

    override fun abort() {
        aborted = true
    }
}

class DownloadAudioTest {

    private fun repository(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
        PodcastRepository(HttpClient(MockEngine(handler)))

    @Test
    fun writesAllChunksAndCommitsOnSuccess() = runTest {
        val body = ByteArray(200_000) { it.toByte() }
        val repo = repository {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, body.size.toString())
            )
        }
        val sink = RecordingSink()
        val progress = mutableListOf<Long>()

        val result = repo.downloadAudio("https://example.com/a.mp3", sink) { done, total ->
            progress += done
            assertEquals(body.size.toLong(), total)
        }

        assertEquals("file://kayit", result.getOrNull())
        assertEquals(body.size, sink.bytes)
        assertTrue(sink.committed)
        assertFalse(sink.aborted)
        assertEquals(body.size.toLong(), progress.last())
    }

    @Test
    fun abortsPartialFileOnHttpError() = runTest {
        val repo = repository { respond(content = "yok", status = HttpStatusCode.NotFound) }
        val sink = RecordingSink()

        val result = repo.downloadAudio("https://example.com/a.mp3", sink) { _, _ -> }

        assertTrue(result.isFailure)
        assertTrue(sink.aborted)
        assertFalse(sink.committed)
    }

    @Test
    fun abortsWhenConnectionEndsEarly() = runTest {
        // Sunucu 1000 bayt vaat edip 400 gonderiyor.
        val repo = repository {
            respond(
                content = ByteReadChannel(ByteArray(400)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, "1000")
            )
        }
        val sink = RecordingSink()

        val result = repo.downloadAudio("https://example.com/a.mp3", sink) { _, _ -> }

        assertTrue(result.isFailure)
        assertTrue(sink.aborted)
        assertFalse(sink.committed)
    }

    @Test
    fun cancellingMidDownloadAbortsAndPropagatesCancellation() = runTest {
        val channel = ByteChannel(autoFlush = true)
        val repo = repository {
            respond(
                content = channel,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, "1000000")
            )
        }
        val sink = RecordingSink()
        var cancellation: CancellationException? = null

        val job = launch {
            try {
                repo.downloadAudio("https://example.com/a.mp3", sink) { _, _ -> }
            } catch (e: CancellationException) {
                cancellation = e
                throw e
            }
        }

        // Ilk parca diske yazilana kadar bekle (gercek zamanda; yazim Default'ta yapilir).
        channel.writeFully(ByteArray(50_000))
        withContext(Dispatchers.Default) {
            repeat(200) {
                if (sink.bytes > 0) return@withContext
                delay(10)
            }
        }
        assertTrue(sink.bytes > 0, "Ilk parca yazilmadi")

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertTrue(cancellation != null, "Iptal, hata sonucuna cevrilmeden iletilmeli")
        assertTrue(sink.aborted, "Yarim dosya silinmeli")
        assertFalse(sink.committed)
    }
}
