package com.example.poco.data.pleos

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/** SDK callbacks cannot be cancelled; late/duplicate replies must not resume twice. */
suspend fun <T> awaitPleos(request: ((T) -> Unit, (Exception) -> Unit) -> Unit): T =
    withTimeout(8_000) {
        suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            request(
                { value ->
                    if (completed.compareAndSet(false, true) &&
                        continuation.isActive
                    ) {
                        continuation.resume(value)
                    }
                },
                { error ->
                    if (completed.compareAndSet(false, true) &&
                        continuation.isActive
                    ) {
                        continuation.resumeWithException(error)
                    }
                }
            )
        }
    }
