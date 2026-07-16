# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Kotlin Multiplatform (KMP) client for the Sziget festival app, targeting Android and iOS with Compose Multiplatform for shared UI. Package root: `com.ilyne.helloszigetkmp`.

- `/androidApp` — Android application entry point.
- `/iosApp` — iOS application entry point (SwiftUI shell hosting the Compose UI). Open this directory in Xcode to run/build iOS.
- `/shared` — all shared code, split into `commonMain`, `androidMain`, `iosMain` source sets (plus `commonTest`, `androidHostTest`, `iosTest`).

## Commands

Build/run:
- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open `/iosApp` in Xcode and run from there (no Gradle CLI path for iOS builds).

Tests:
- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`
- Run a single test class: append `--tests "com.ilyne.helloszigetkmp.SomeTestClass"` to the `testAndroidHostTest` command.

Lint/format:
- Format: `./gradlew spotlessApply` (ktlint 1.8.0 + the `io.nlopez.compose.rules` Compose ruleset, configured in `shared/build.gradle.kts`).
- Check formatting only: `./gradlew spotlessCheck`
- Static analysis: `./gradlew detekt` (config at `config/detekt/detekt.yml`, applied to every subproject; baselines are per-project `detekt-baseline.xml` files).

**Do not run `./gradlew spotlessApply` repo-wide as part of a routine fix.** `spotlessCheck` currently fails across 85+ pre-existing files (verified 2026-07) simply because they predate a config/version bump and haven't been reformatted since — a full `spotlessApply` would touch all of them (constructor param wrapping, block-body-to-expression-body conversions, supertype list wrapping, etc.), plus it fails outright on ~20 pre-existing non-autocorrectable `compose:*` lint violations unrelated to formatting. If a change you're making trips `spotlessCheck`, format only the files you touched (or fix by hand to match the surrounding style) rather than running `spotlessApply` across the whole repo — a mass reformat is a separate, deliberate cleanup task, not a side effect of an unrelated fix.

(Historical note: this section previously also warned that Spotless silently ignored the `ktlint_standard_multiline-expression-wrapping = disabled` override in `shared/build.gradle.kts`. Re-tested 2026-07 against the currently pinned Spotless 8.8.0 / ktlint 1.8.0 with several representative cases — `val x = listOf(...)`, constructor calls, `when`/`if-else` expressions, elvis chains, boolean chains — and could not reproduce it; the override appears to work correctly now. The claim may have been accurate against an older pin. If you hit re-wrapping behavior again, re-verify in an isolated `git worktree` before trusting either the old or new claim.)

## Architecture

### Layering (commonMain)

```
presentation/  (feature/<name>/ — Screen + ViewModel + UiState per feature; component/, theme/)
domain/        (model/, usecase/)
core/
  api/         (Ktor-based remote API services, incl. api/auth/)
  auth/        (SzigetAuthService, GoogleAuthProvider, TokenStorage, LogoutService)
  config/      (AppConfig — expect/actual per platform)
  db/          (Room KMP database + DAOs)
  network/     (Ktor HttpClient construction — baseHttpClient, createApiHttpClient)
  repository/  (UserRepository, FriendRepository, ArtistRepository, ScheduleRepository, SoftRefreshGate)
  sync/        (UsersSyncService and similar background sync services)
navigation/    (AppNavGraph — single shared nav graph used by both platforms)
di/            (Koin modules)
```

Platform-specific implementations of `expect` declarations (e.g. `AppConfig`, DB driver, HTTP engine) live in `androidMain`/`iosMain` mirroring the same package path, e.g. `core/config/AppConfig.android.kt` / `AppConfig.ios.kt`.

### Dependency injection (Koin)

Three modules, composed in `di/AppModule.kt`:
- `appModule` — the bulk of singletons: config, settings, auth services, Room database + DAOs, repositories, sync services, and pre-login ViewModels (e.g. `LoginViewModel`). Started via `initKoin(platformModules)`, which merges in platform-specific modules (see `androidMain`/`iosMain` for what those provide, e.g. Android-only credential manager bindings).
- `AuthenticatedApiModule.kt` — `createAuthenticatedApiModule(...)` builds a *session-scoped* module (authenticated Ktor client + `SzigetApiService`) once a user is logged in, since it needs the access/refresh tokens and a `baseUrl` at construction time. This is not part of the static `appModule` graph — it's created/loaded after successful auth.
- `PresentationModule.kt` — feature ViewModels that depend on the authenticated API layer.

### Auth flow

`SzigetAuthService` orchestrates `GoogleAuthProvider` (Google Sign-In) + `SzigetAuthApiService` (token exchange) + `TokenStorage` (persistence via `multiplatform-settings`'s `Settings`). On successful login, `createAuthenticatedApiModule` is loaded into Koin with the resulting tokens. `LogoutService` tears the session down.

### Data flow / sync pattern

Repositories (`core/repository/`) are the source of truth for ViewModels and wrap Room DAOs + remote API calls. `SoftRefreshGate` gates redundant refreshes by only persisting `lastFetchedAt` after a successful fetch. `UsersSyncService`-style sync services use `tryLock()` so a concurrent second fetch is dropped in favor of the in-flight one (callers awaiting freshness should use the service's `awaitSuccessfulSync()`-style API rather than assuming their own call triggers a fetch).

### Local persistence

Room KMP (`androidx.room`) via `core/db/SzigetDatabase.kt`, with schema JSON snapshots under `shared/schemas/`. DAOs live in `core/db/dao/`. Both Android and iOS targets generate Room code via KSP (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64` in `shared/build.gradle.kts`).

The database is still `version = 1` (only `1.json` exists) and `createDatabase()` uses `.fallbackToDestructiveMigration(dropAllTables = true)` — there are no real `Migration`s yet because there's been no version bump to migrate from. This is lower-stakes than it sounds: every Room table is a cache of backend data (friends, favorites, schedule, cached user), so a wipe just means the next screen load refetches instead of losing anything permanently — `createDatabase()` wires Room's `onDestructiveMigration` callback to `SoftRefreshGate.clearAll(settings)` so `SoftRefreshGate`'s `lastFetchedAt` timestamps (which live in `Settings`, not Room, so they'd otherwise survive the wipe) are cleared too, forcing an immediate refetch instead of screens looking "fresh" but empty for up to their staleness threshold. Still worth authoring a real `Migration` before bumping `version` past 1, since a full-screen reload/refetch is a worse experience than an in-place migration even if no data is truly lost.

### Networking

Ktor client, built in `core/network/HttpClients.kt`. `baseHttpClient` is the unauthenticated client (used for login/auth endpoints); `createApiHttpClient(...)` builds the authenticated per-session client used after login, wired with token refresh via `TokenStorage`.

### Feature module shape

Each screen under `presentation/feature/<name>/` follows: `<Name>Screen.kt` (Composable), `<Name>ViewModel.kt` (state holder, Koin-injected), and a `<Name>UiState`/`Status` sealed model. Current features: `discover`, `lineup` (MyLineup), `schedule`, `profile`, `addfriend`, `artistdetail`, `login`.

## Parallel fixes in worktrees

When asked to work on multiple fixes/issues in parallel, use one git worktree per fix rather than switching branches serially in the main checkout:

1. For each fix, create a branch named to describe the issue (e.g. `fix-schedule-timezone-bug`, not `fix-1` or `wip`), and a worktree for it: `git worktree add ../<branch-name> -b <branch-name>`.
2. Work each fix independently within its own worktree — no shared state or sequencing between them unless the fixes actually depend on each other.
3. When a fix is complete and verified, commit and push its branch to `origin` under the same branch name (`git push -u origin <branch-name>`).
4. Remove the worktree once its branch is pushed: `git worktree remove ../<branch-name>`.
5. Do not open PRs or merge automatically — the parent job's final output should just be the list of pushed branch names, so the user can review/PR each one themselves.
