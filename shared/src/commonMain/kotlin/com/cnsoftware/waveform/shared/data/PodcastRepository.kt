package com.cnsoftware.waveform.shared.data

import com.cnsoftware.waveform.shared.model.Podcast
import com.cnsoftware.waveform.shared.model.SearchPodcastResult
import com.cnsoftware.waveform.shared.rss.RssParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PodcastRepository(
    private val httpClient: HttpClient = createHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun close() {
        httpClient.close()
    }

    suspend fun fetchPodcastByRss(rssUrl: String): Result<Podcast> = runCatching {
        val normalizedUrl = normalizeUrl(rssUrl)
        val response = httpClient.get(normalizedUrl)
        if (!response.status.isSuccess()) {
            error("RSS yüklenemedi (HTTP ${response.status.value})")
        }
        val xml = response.bodyAsText()
        // Regex tabanli ayristirma buyuk feed'lerde agir: ana is parcacigini dondurmasin.
        withContext(Dispatchers.Default) { RssParser.parseFeed(normalizedUrl, xml) }
    }

    suspend fun searchPodcasts(query: String): Result<List<SearchPodcastResult>> = runCatching {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@runCatching emptyList()

        val response = httpClient.get("https://itunes.apple.com/search") {
            parameter("media", "podcast")
            parameter("limit", "20")
            parameter("term", trimmed)
        }
        if (!response.status.isSuccess()) {
            error("Arama başarısız (HTTP ${response.status.value})")
        }
        val payload = json.decodeFromString<ItunesSearchResponse>(response.bodyAsText())
        payload.results.mapNotNull { item ->
            val feed = item.feedUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            SearchPodcastResult(
                title = item.collectionName ?: item.trackName ?: "İsimsiz Podcast",
                author = item.artistName ?: "",
                feedUrl = feed,
                artworkUrl = item.artworkUrl600 ?: item.artworkUrl100
            )
        }
    }

    /**
     * Sesi belleğe almadan parca parca [sink]'e yazar. [onProgress] (inen, toplam) ile cagrilir;
     * toplam boyut bilinmiyorsa null'dir. Basarida [AudioSink.commit] sonucu (yerel URI) doner.
     */
    suspend fun downloadAudio(
        audioUrl: String,
        sink: AudioSink,
        onProgress: (downloaded: Long, total: Long?) -> Unit
    ): Result<String> {
        var committed = false
        return try {
            try {
                httpClient.prepareGet(normalizeUrl(audioUrl)).execute { response ->
                    if (!response.status.isSuccess()) {
                        error("Bölüm indirilemedi (HTTP ${response.status.value})")
                    }
                    val total = response.contentLength()?.takeIf { it > 0L }
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(DOWNLOAD_CHUNK_SIZE)
                    var downloaded = 0L
                    onProgress(0L, total)
                    while (true) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read < 0) break
                        if (read == 0) continue
                        // Disk yazimi ana is parcacigini bloklamasin.
                        withContext(Dispatchers.Default) { sink.write(buffer, read) }
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                    if (total != null && downloaded < total) {
                        error("İndirme yarıda kesildi.")
                    }
                    if (downloaded == 0L) error("Boş dosya indirildi.")
                }
                Result.success(sink.commit().also { committed = true })
            } catch (cancelled: CancellationException) {
                // Kullanici iptali hata degildir: yutma, cagirana ilet (yarim dosya finally'de silinir).
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        } finally {
            // Hata ya da iptal durumunda yarim dosyayi temizle.
            if (!committed) sink.abort()
        }
    }

    private companion object {
        const val DOWNLOAD_CHUNK_SIZE = 64 * 1024
    }

    private fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotEmpty()) { "RSS adresi boş olamaz." }
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}

@Serializable
private data class ItunesSearchResponse(
    val results: List<ItunesPodcastItem> = emptyList()
)

@Serializable
private data class ItunesPodcastItem(
    val collectionName: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val feedUrl: String? = null,
    val artworkUrl100: String? = null,
    val artworkUrl600: String? = null
)
