package com.ilyne.helloszigetkmp.core.auth

import com.ilyne.helloszigetkmp.domain.model.User

/**
 * Holds the signed-in user for the lifetime of the session. Populated exactly once, either right
 * after a fresh login (LoginViewModel, once the profile is synced to the DB) or during session
 * restore (SzigetAuthService.restoreSession, from the already-synced local DB) — both of which
 * complete before the Main nav graph becomes reachable. This lets authenticated ViewModels read
 * [currentUser] directly instead of each re-fetching it from the DB and juggling a nullable/
 * lateinit field for the brief window before that fetch resolves.
 */
class CurrentUserProvider {
    private var user: User? = null

    /**
     * Throws if accessed before a session is established. Safe to use unguarded within the
     * Main nav graph, which SzigetAuthService guarantees is only reachable after [set] has
     * been called (see App.kt's LaunchedEffect). A ViewModel that can be constructed mid-
     * navigation concurrently with a [clear] (e.g. a session invalidation racing the screen
     * navigation that's about to be torn down anyway) should read [currentUserOrNull] instead
     * and degrade gracefully rather than crash on construction.
     */
    val currentUser: User
        get() = user ?: error("CurrentUserProvider accessed before a session was established")

    val currentUserOrNull: User?
        get() = user

    fun set(user: User) {
        this.user = user
    }

    fun clear() {
        user = null
    }
}
