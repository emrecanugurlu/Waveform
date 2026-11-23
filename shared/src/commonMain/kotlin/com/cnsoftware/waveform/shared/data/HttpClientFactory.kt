package com.cnsoftware.waveform.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

internal const val APP_USER_AGENT = "Waveform/1.0 (Podcast Player)"

internal fun HttpClientConfig<*>.configureWaveformClient() {
    followRedirects = true
    defaultRequest {
        header(HttpHeaders.UserAgent, APP_USER_AGENT)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000L
        socketTimeoutMillis = 30_000L
    }
}

expect fun createHttpClient(): HttpClient
