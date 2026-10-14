package com.bangkoktransit.app.util

import kotlinx.coroutines.CancellationException

internal suspend fun <T> cancellableRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
