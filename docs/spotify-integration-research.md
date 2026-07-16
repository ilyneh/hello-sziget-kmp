# Spotify Integration Research

Status: research only, no implementation. Written against the codebase as of `main` @ `983a95d`.

## Goal

Two related asks tend to get bundled together under "add Spotify" and are worth separating up front:

1. Let a user **link their Spotify account** to their Sziget profile.
2. On the artist details screen, show and **play an artist's top 5-10 tracks**.

(1) is a straightforward OAuth integration that mirrors what this app already does for Google.
(2) has two independent halves — *fetching* track metadata (easy) and *playing audio* (genuinely constrained by what Spotify allows a third-party mobile app to do) — and the doc treats them separately in Section 2.

---

## 0. How the existing Google Sign-In flow works (for reference)

Grounding in the current pattern, since a Spotify integration should mirror it where it makes sense and deviate deliberately where Spotify's constraints differ:

- **`GoogleAuthProviding`** (`shared/src/commonMain/.../core/auth/GoogleAuthProvider.kt`) is a small commonMain interface (`signIn(): AuthUser`, `signOut()`, `getCurrentUser(): AuthUser?`). The concrete `expect class GoogleAuthProvider` is implemented per platform:
  - **Android** (`GoogleAuthProvider.android.kt`): uses `androidx.credentials.CredentialManager` + Google's `GoogleIdTokenCredential`, entirely native, no browser redirect.
  - **iOS** (`GoogleAuthProvider.ios.kt`): has *no* native Kotlin/Native Google SDK binding. Instead it exposes package-level callback hooks (`googleSignInHandler`, `googleSignOutHandler`, `googleCurrentUserHandler`) that the Swift host app (`ContentView.swift`) wires up before the Compose root launches, then bridges into a `suspendCancellableCoroutine`. This split exists because Kotlin/Native's generated Objective-C header can't expose a `suspend` lambda in a way Swift can satisfy directly — a plain completion-handler shape is used instead.
- **`SzigetAuthService.signIn()`** calls `authProvider.signIn()` to get a Google ID token, then exchanges it server-side via `SzigetAuthApiService.googleLogin(googleToken)` for the app's *own* `TokenDto` (Sziget access + refresh token). The Google token itself is never persisted client-side past that exchange.
- **`TokenStorage`** persists only the app's own `TokenDto` (access token, refresh token, token type) as JSON in `multiplatform-settings`' `Settings` — not Spotify- or Google-specific, just "the current session's bearer tokens."
- **`createApiHttpClient`** (`core/network/HttpClients.kt`) installs Ktor's `Auth { bearer { ... } }` plugin, with `refreshTokens` calling `POST $baseUrl/auth/refresh` and calling `onSessionInvalidated()` (→ `SzigetAuthService.invalidateSession()`) if that fails.
- **`createAuthenticatedApiModule`** (`di/AuthenticatedApiModule.kt`) is a *session-scoped* Koin module built only after login succeeds (needs `accessToken`/`refreshToken`/`baseUrl` at construction time) — loaded via `loadKoinModules(apiModule)` in `SzigetAuthService.loadAuthenticatedModules`, unloaded on `invalidateSession()`.

The key takeaway: **the app already delegates all Google-specific OAuth mechanics to a thin per-platform provider, and immediately trades the third-party token for the app's own backend-issued token.** The backend, not the client, is the source of truth for "is this user logged in." A Spotify integration should follow the same shape: Spotify's token is a *secondary* credential the backend holds on the user's behalf, not a replacement for the Sziget session.

---

## 1. Account linking (OAuth)

### Flow

Spotify's Web API auth uses **OAuth 2.0 Authorization Code Flow with PKCE** — this is Spotify's own recommended flow for mobile/native apps because it doesn't require embedding a client secret in the app binary (the Authorization Code flow without PKCE does require a secret, which is not safe to ship in a mobile client). Concretely:

1. App generates a PKCE `code_verifier` + `code_challenge` (S256).
2. App opens a browser/`ASWebAuthenticationSession` (iOS) / Custom Tabs (Android) to `https://accounts.spotify.com/authorize` with `client_id`, `code_challenge`, `redirect_uri` (a custom URI scheme or Universal/App Link back into the app, e.g. `helloszigetkmp://spotify-callback`), and the requested `scope`s.
3. User logs into Spotify and approves. Spotify redirects back to the app's registered redirect URI with an authorization `code`.
4. App (or, per the recommendation below, the backend) exchanges `code` + `code_verifier` for an `access_token` + `refresh_token` at `https://accounts.spotify.com/api/token`.

### Scopes needed

| Scope | Why |
|---|---|
| `user-top-read` | Not actually needed for *artist* top tracks (see Section 2 — that endpoint is a public/app-token call), but relevant if a future feature wants the *user's own* top tracks/artists. |
| `user-read-email`, `user-read-private` | Only needed if displaying the linked Spotify profile (display name, email, avatar) back to the user, analogous to what `AuthUser` captures for Google. Optional for a v1 that's just "show me the artist's top tracks." |

Note: fetching an *artist's* top tracks (Section 2) does **not** require any user-scoped permission at all — it just needs a valid Spotify access token (app-level `client_credentials` grant is sufficient). So if the only goal is "play/show artist top tracks," **Spotify account linking may not be required at all** for the fetch — only for playback control (App Remote SDK requires the user to be signed into Spotify, but that's a Spotify-app-level sign-in, not necessarily an OAuth token the Sziget backend needs to see). This changes the shape of Phase 1 significantly — see Section 4.

### Where a `SpotifyAuthProvider` would fit

Following the `GoogleAuthProviding` pattern:

```kotlin
// commonMain
interface SpotifyAuthProviding {
    suspend fun signIn(): SpotifyAuthResult   // returns an authorization code (+ verifier), not tokens
    fun signOut()
}

data class SpotifyAuthResult(val authorizationCode: String, val codeVerifier: String, val redirectUri: String)

expect class SpotifyAuthProvider() : SpotifyAuthProviding
```

Unlike Google, Spotify has no Android Credential Manager integration and no purely-native "one tap" sign-in — it's fundamentally a web redirect flow (`accounts.spotify.com`). Two implementation paths per platform:

- **Spotify's Android Auth SDK / iOS Auth SDK** (`spotify-auth` / `SpotifyiOS.framework`) wrap the browser redirect + PKCE + redirect-URI handling in a native SDK, and *can* also hand back a token that unlocks the App Remote SDK for in-app playback control (see Section 2). This is the more integrated option if playback via the Spotify app is in scope at all.
- **Plain web-based OAuth redirect** — `ASWebAuthenticationSession` on iOS, Chrome Custom Tabs + an `AppAuth`-style redirect on Android — talking directly to `accounts.spotify.com` and Spotify's Web API, with no Spotify SDK dependency. Simpler to reason about, no proprietary SDK version-pinning, but doesn't get you App Remote playback for free.

Recommendation: since Spotify's Auth SDK is effectively a prerequisite for the App Remote SDK later (Section 2 recommends App Remote as the playback path), it's worth adopting the **Spotify Auth SDK** for both platforms from the start, even for Phase 1's "just link + fetch" scope, rather than doing a plain web-redirect now and swapping later.

The `expect`/`actual` split would look like Google's: Android using `com.spotify.sdk.android.auth.AuthorizationClient`, iOS bridging through Swift (`SPTSessionManager` from `SpotifyiOS.framework`) via the same callback-property pattern `GoogleAuthProvider.ios.kt` uses, since Kotlin/Native has the same suspend-fn-to-ObjC limitation here too.

### Where it plugs into `SzigetAuthService`

Not into `SzigetAuthService` itself — that owns the *primary* Sziget session (signIn/logout/restoreSession), and Spotify linking is an optional, independent, secondary link a user opts into after they're already logged in. A new `SpotifyLinkService` (or a method added to a small new `core/spotify/` package) makes more sense:

```kotlin
class SpotifyLinkService(
    private val spotifyAuthProvider: SpotifyAuthProviding,
    private val szigetApiService: SzigetApiService, // authenticated client, see Section 3
) {
    suspend fun link() {
        val result = spotifyAuthProvider.signIn()
        szigetApiService.linkSpotifyAccount(result.authorizationCode, result.codeVerifier, result.redirectUri)
    }
    suspend fun unlink() = szigetApiService.unlinkSpotifyAccount()
}
```

This only requires the *already-authenticated* `SzigetApiService` (Section 3), so it naturally belongs in the session-scoped Koin graph (`PresentationModule.kt` territory) rather than the pre-login `appModule`.

---

## 2. Playing top tracks on the artist details screen

This is two separate Spotify capabilities. They are commonly conflated and should not be — one is trivial, the other has real device/subscription constraints.

### 2a. Fetching an artist's top tracks (easy)

`GET https://api.spotify.com/v1/artists/{id}/top-tracks?market={market}` returns up to 10 tracks with title, album art, duration, and (historically) a `preview_url`. This needs:

- A Spotify artist ID per Sziget artist — **new mapping problem**: the backend's `ArtistDto` has no Spotify ID today, so either the backend stores a manually-curated or search-matched `spotify_artist_id` per artist, or the client does a `GET /v1/search?type=artist&q={name}` lookup at read time (name-matching festival lineup artists to Spotify is fuzzy — expect a curation/admin step rather than fully automatic matching).
- A valid Spotify access token. For this endpoint specifically, a **`client_credentials` app token is sufficient** — no per-user OAuth needed. That token is trivially obtainable server-side and should be, so client secret material never ships in the app.

### 2b. Actually playing audio (constrained — read carefully)

This is where the real design decision is. As of **November 2024, Spotify deprecated the `preview_url` (30-second clip) field for most new API integrations** — apps that didn't already have extended API access before the policy change generally don't get `preview_url` populated anymore. That closes off what used to be the easy path ("just stream the 30s preview clip in an in-app `MediaPlayer`, no Spotify app or Premium required"). Assume that path is **not available** for a new integration built today; a first step in Phase 1 should be confirming this directly against the developer dashboard for whatever app registration is used, since Spotify has made ad hoc exceptions, but it should not be the plan of record.

That leaves three realistic options for actual playback:

| Option | Requires | UX | Notes |
|---|---|---|---|
| **A. Spotify App Remote SDK / iOS SDK, in-app playback control** | Spotify app installed on device + user has a **Spotify Premium** account (Free-tier accounts can't be driven via App Remote for on-demand track playback — Free tier is shuffle-only/ad-supported and App Remote doesn't unlock that) | Track plays through the Spotify app process in the background while the Sziget app shows a mini-player-style UI and sends play/pause/skip commands. Feels close to "playing in-app" but is really remote-controlling the Spotify app. | Real integration work: separate Android SDK (`spotify-app-remote`) and iOS SDK (`SpotifyiOS.framework`), connection/session lifecycle to manage, and it silently doesn't work for non-Premium or Spotify-not-installed users — needs a fallback for both. |
| **B. Deep-link out to the Spotify app** (`spotify:track:{id}` URI or `https://open.spotify.com/track/{id}` universal link) | Nothing beyond a track ID — works for Free and Premium users, works whether or not Spotify is installed (falls back to web player / App Store) | User leaves the Sziget app entirely; Spotify (app or web) opens and plays the track there. | Minimal engineering: one `Intent`/`UIApplication.open` call, no SDK, no token needed beyond having the track ID. No Premium requirement — Spotify's own now-playing UI handles the free/ad-supported case. |
| **C. No playback at all — show track list + "Open in Spotify" as pure metadata/link-out** | Nothing (already covered by 2a) | User sees a ranked track list with cover art; tapping a row is the same deep link as (B), or nothing at all if even that's out of scope. | Cheapest option; arguably indistinguishable from (B) from an engineering standpoint — (B) is really just (C) with one row-tap action wired up. |

**Recommendation for this app specifically:** this is a **festival companion app**, not a music player — its job is schedule/lineup/friends, not competing with Spotify's own player UX. Building and maintaining the App Remote SDK integration (Option A) is a meaningfully larger, higher-maintenance surface (SDK lifecycle, Premium-gating UX, "Spotify not installed" fallback UX, two more native dependencies) for a feature that's adjacent to the app's core value, not central to it. **Option B (deep link to open the track in Spotify) is the right default**: it requires no Spotify app-remote integration, works for all users regardless of subscription tier, and still gets the user to "hear the song" in one tap. Treat Option A as a possible Phase 2 if user data later shows people specifically want in-app playback and are bouncing off the deep-link handoff — see Section 4.

---

## 3. Backend / API changes

The mobile client never talks to `api.spotify.com` with a *user* token directly in the recommended design below — everything Spotify-related that needs a stored, refreshable token goes through the Sziget backend, mirroring how Google's ID token is exchanged server-side today rather than the client holding a long-lived Google session.

### 3a. Storage

Per-user table/columns, analogous to what `TokenStorage` holds client-side but server-side and encrypted at rest:

```
spotify_account (
    user_id                 FK -> users.id, unique
    spotify_user_id          text
    spotify_access_token      text  -- encrypted at rest
    spotify_refresh_token     text  -- encrypted at rest
    spotify_token_expires_at  timestamptz
    scopes                    text  -- space-delimited, as granted
    linked_at                 timestamptz
)
```

Same sensitivity class as the app's own access/refresh tokens — encrypt at rest (e.g. KMS-backed column encryption or an application-layer encryption library), never log them (the existing `HttpClients.kt` already takes care to disable Ktor request/response logging in release builds for this exact reason — the backend needs the equivalent discipline).

### 3b. Client-side vs. server-side token refresh

Two options:

- **Client-side** (mirrors Google today): mobile app holds the Spotify access+refresh token itself (in the same `Settings`-backed local storage `TokenStorage` uses, or a sibling `SpotifySettings`), refreshes directly against `accounts.spotify.com/api/token` using PKCE (no client secret required, since PKCE flow doesn't need one). Simpler backend, but the backend can't make any Spotify API call on the user's behalf (no background jobs, no server-initiated top-tracks pre-fetch/caching), and the token material lives on-device where it's one more thing subject to device compromise/backup extraction.
- **Server-side proxying** (recommended): mobile app sends the PKCE authorization code once, at link time; backend does the code→token exchange and all subsequent refreshes, storing the result server-side per 3a. The mobile client only ever asks *its own* backend for Spotify data — it never needs a raw Spotify token client-side at all for the fetch use case (2a).

**Recommendation: server-side.** Even though PKCE removes the *hard requirement* for a client secret, proxying still wins here because: (1) it lets the backend do the artist-ID-to-Spotify-ID mapping and top-tracks fetch centrally, cacheable across all users instead of N duplicate client-side calls; (2) it keeps the Spotify credential off-device, consistent with how the Sziget session token is already the only thing the client persists; (3) it enables future server-initiated use cases (e.g. a backend job matching lineup artists to Spotify IDs) without needing a signed-in client. The tradeoff is that the backend now owns Spotify's rate limits and must handle refresh failures (revoked access, expired refresh token) by surfacing an "unlink and relink" state to the client, similar to `onSessionInvalidated` today.

### 3c. New endpoints

```
POST   /users/me/spotify/link
  body: { authorization_code, code_verifier, redirect_uri }
  → backend exchanges code for tokens, stores per 3a, returns linked Spotify profile summary (or 204)

DELETE /users/me/spotify/unlink
  → backend revokes/discards stored tokens

GET    /artists/{id}/spotify-top-tracks
  → backend resolves artist_id -> spotify_artist_id (curated mapping, see 2a),
    calls Spotify Web API with an app-level client_credentials token (not the user's),
    returns a small DTO: [{ spotify_track_id, name, album_art_url, duration_ms, spotify_url }]
    — cacheable server-side (e.g. 24h) since it's not user-specific.
```

Note `GET /artists/{id}/spotify-top-tracks` doesn't need the *user's* Spotify link at all — it only needs the backend's own app credentials — so this endpoint could ship even before account linking exists, decoupling Section 1 and Section 2a as independently shippable pieces of work (see Phase 1 in Section 4).

### 3d. Security / operational notes

- Encrypt `spotify_access_token`/`spotify_refresh_token` at rest; scope access to the service role that performs the proxy calls.
- Rate limits: Spotify's Web API enforces per-app rate limits (rolling 30s window, exact quota tied to the app's extended-access tier). A shared server-side cache for `spotify-top-tracks` (keyed by artist, not by user) avoids the N-users-hit-the-same-endpoint amplification a naive client-side-direct-call design would cause.
- Revocation handling: if Spotify returns 401 on a refresh attempt (revoked by user from their Spotify account settings), the backend should mark the link as invalid and the client should show a "reconnect Spotify" affordance rather than silently failing — same shape as `onSessionInvalidated` for the primary session.

---

## 4. Scope estimate and phased approach

Rough sizing (client + backend combined, order-of-magnitude, assuming familiarity with the existing codebase patterns):

| Piece | Rough size |
|---|---|
| Backend: `spotify_account` table + link/unlink endpoints + token refresh job | Medium |
| Backend: artist-ID-to-Spotify-ID mapping (curation tooling or admin step) + `spotify-top-tracks` endpoint + caching | Small–Medium |
| Client: `SpotifyAuthProviding` + Android/iOS `actual` implementations (Spotify Auth SDK integration, redirect URI registration on both platforms) | Medium |
| Client: link/unlink UI (likely a row in the profile screen) | Small |
| Client: top-tracks UI on `ArtistDetailScreen` (list + album art + deep-link-to-Spotify tap action) | Small |
| Client: App Remote SDK in-app playback (Option A, if pursued) | Medium–Large, plus ongoing maintenance (two more native SDKs, Premium/not-installed fallback states) |

### Suggested phasing

**Phase 1 — fetch + display + deep-link playback:**
1. Backend: `GET /artists/{id}/spotify-top-tracks` (app-credential only, no user linking required yet) with server-side caching.
2. Client: render top 5-10 tracks on `ArtistDetailScreen`, tapping a track opens it via deep link (`spotify:track:{id}` / `open.spotify.com` fallback) — Option B/C from Section 2b.
3. Account linking (Section 1) can ship in this phase too, but only if there's an actual reason to know *which* Spotify user someone is (e.g. showing "you've linked as {display name}" in profile) — it's not a prerequisite for tracks to appear, per the note in Section 3c. If there's no concrete use for the user-level link yet, defer Section 1 and 3a/3b/3c's `link`/`unlink` endpoints to Phase 2 and ship only the artist-top-tracks read path in Phase 1.

**Phase 2 — account linking, if not already done in Phase 1, plus evaluate in-app playback:**
1. `SpotifyAuthProviding` + link/unlink flow (Section 1, Section 3c's link/unlink endpoints).
2. Only if user feedback/data specifically motivates it: App Remote SDK in-app playback (Option A), gated behind Premium detection and "Spotify not installed" fallback UX, reusing the Phase 2 link flow's Spotify Auth SDK session for the App Remote connection.

This ordering front-loads the highest-value, lowest-risk piece (seeing top tracks with one tap out to Spotify) and treats the heavier, SDK-dependent in-app playback as an explicit, separately-justified follow-up rather than a Phase 1 assumption.
