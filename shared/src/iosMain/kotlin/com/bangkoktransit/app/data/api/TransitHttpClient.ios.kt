package com.bangkoktransit.app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createTransitHttpClient(): HttpClient = HttpClient(Darwin) {
    configureTransitClient()
}
