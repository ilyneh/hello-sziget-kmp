package com.ilyne.helloszigetkmp.core.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coalesces concurrent calls to [run] into a single in-flight execution, so callers that
 * invoke it while a call is already in progress share its result instead of triggering a
 * redundant duplicate (e.g. a second network fetch + DB write racing the first).
 */
class SingleFlight {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var inFlight: Deferred<Unit>? = null

    suspend fun run(block: suspend () -> Unit) {
        val deferred = mutex.withLock {
            inFlight?.takeIf { it.isActive } ?: scope.async { block() }.also { inFlight = it }
        }
        try {
            deferred.await()
        } finally {
            mutex.withLock { if (inFlight === deferred) inFlight = null }
        }
    }
}
