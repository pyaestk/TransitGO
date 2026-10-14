package com.bangkoktransit.app.data.local

import com.transitgo.shared.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
suspend fun readBundledStations(): String =
    Res.readBytes("files/stations_fallback.json").decodeToString()
