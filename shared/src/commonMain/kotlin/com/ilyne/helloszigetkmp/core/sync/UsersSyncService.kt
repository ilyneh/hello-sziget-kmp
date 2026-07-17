package com.ilyne.helloszigetkmp.core.sync

import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Background fetch of all users. `fetchAllUsers` is fire-and-forget and safe to call
 * repeatedly (e.g. on every login); a call is dropped if a fetch is already in flight
 * rather than being queued.
 */
class UsersSyncService(
    private val userRepository: UserRepository,
) {
    sealed class SyncStatus {
        data object Idle : SyncStatus()

        data object InProgress : SyncStatus()

        data object Success : SyncStatus()

        data class Failure(
            val error: Throwable,
        ) : SyncStatus()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun fetchAllUsers(api: SzigetApiService) {
        if (!mutex.tryLock()) return

        _status.value = SyncStatus.InProgress
        scope.launch {
            try {
                userRepository.refresh(api)
                _status.value = SyncStatus.Success
            } catch (e: Throwable) {
                _status.value = SyncStatus.Failure(e)
            } finally {
                mutex.unlock()
            }
        }
    }

    /** Suspends until the current or most recent fetch finishes, returning true on success. */
    suspend fun awaitSuccessfulSync(): Boolean = status.first { it is SyncStatus.Success || it is SyncStatus.Failure } is SyncStatus.Success
}
