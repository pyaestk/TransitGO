package com.bangkoktransit.app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createTransitHttpClient(): HttpClient = HttpClient(OkHttp) {
    configureTransitClient()
}
