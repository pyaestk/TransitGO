package com.bangkoktransit.app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

internal expect fun createTransitHttpClient(): HttpClient

internal fun HttpClientConfig<*>.configureTransitClient() {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
        requestTimeoutMillis = 15_000
    }
}
