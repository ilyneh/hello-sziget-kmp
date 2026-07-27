# Login flows

Covers `presentation/feature/login` (`LoginScreen`, `SignInCard`,
`WelcomeBackground`).

## Flows

- `login_initial_render.yaml` — asserts the screen's baseline state: root
  screen, welcome hero, and an enabled sign-in button, with no loading
  spinner or error showing.
- `login_sign_in_tap_triggers_auth.yaml` — taps the sign-in button and
  asserts landing on the authenticated Main screen. This only completes
  end-to-end when the debug app under test was built with
  `-Psziget.skipGoogleSignIn=true` (see `maestro/README.md`'s "Running
  against a mocked backend" section) — see below for why.
- `login_debug_config_sheet.yaml` — asserts the debug-only "DEBUG" button
  opens the debug config bottom sheet (base URL/token inputs, skip-Google-
  sign-in switch) and that saving dismisses it. Works against any debug
  build, not just one built with `-Psziget.skipGoogleSignIn=true` — see
  "Runtime debug config" below.

## Runtime debug config

`LoginScreen` shows a "DEBUG" button (`login_debug_button`) whenever
`AppConfiguring.isDebug()` is true. It opens a bottom sheet
(`DebugConfigSheet.kt`) backed by `core/config/DebugConfigStore.kt` that lets
a base URL, auth token, and "skip Google sign-in" be set **at runtime**,
persisted across app restarts (until changed again or the app is
uninstalled) — the runtime counterpart to the compile-time-baked
`sziget.localBackendUrl`/`sziget.localBearerToken`/`sziget.skipGoogleSignIn`
Gradle properties described above, useful when the backend/token needs to
change between Maestro runs without a rebuild+reinstall.

Saving with "Skip Google Sign-In" on makes `login_sign_in_button` route
through `SzigetAuthService.localSignIn()` exactly like the compile-time flag
does (see `LoginViewModel.signInWithGoogle()`), using the sheet's token
override in place of `BEARER_TOKEN_LOCALHOST` and the sheet's base URL
override in place of `sziget.localBackendUrl` — same debug-only guarantees
apply (`appConfig.isDebug()` gate in `SzigetAuthService.localSignIn()`), so
this can never activate outside a debug build.

Use `maestro/flows/common/set_debug_config.yaml` (a `runFlow`-only shared
subflow, see `maestro/README.md`'s "Sharing actions between flows") to drive
this from another flow — it reads `BASE_URL`/`TOKEN` env vars via
`maestro test --env`.

## Why this only works with the debug-only local-auth bypass

`LoginViewModel.signInWithGoogle()` (see
`shared/src/commonMain/kotlin/com/ilyne/helloszigetkmp/presentation/feature/login/LoginViewModel.kt`)
drives `SzigetAuthService.signIn()` by default, which calls
`GoogleAuthProvider.signIn()`. That launches Google's native Sign-In /
Credential Manager account-picker UI - a system surface outside this app's
own window and outside its Compose tree, so:

- it isn't reachable via `testTagsAsResourceId` (there's nothing for this
  app's `testTag`s to attach to),
- its content and flow depend on which Google account(s), if any, are
  signed into the device/emulator, which isn't something a checked-in
  Maestro flow can assume or control,
- driving it would mean asserting on Google's own UI, which is out of scope
  for this app's e2e suite and liable to break on any change upstream.

So a real, fully-automated "tap sign in -> land on the authenticated Main
screen" flow isn't practical against a plain debug build. This repo takes the
following path instead:

**The debug-only local-auth bypass.** `SzigetAuthService` has `localSignIn()`
(see
`shared/src/commonMain/kotlin/com/ilyne/helloszigetkmp/core/auth/SzigetAuthService.kt`),
which skips Google entirely and logs in with a fixed bearer token
(`BEARER_TOKEN_LOCALHOST`) against whatever backend `sziget.localBackendUrl`
points at - but only when `appConfig.isDebug()` is true, so it can never
activate in a release-shaped build regardless of the flag below.
`LoginViewModel` gates the call behind the `SKIP_GOOGLE_SIGN_IN` build-time
constant (`core/config/AppConfig.kt`), set via the `sziget.skipGoogleSignIn`
Gradle property (off by default). Building the debug APK with
`-Psziget.skipGoogleSignIn=true` makes `login_sign_in_button` complete a real
(local-account) sign-in and navigate to Main, which `login_sign_in_tap_triggers_auth.yaml`
and the `artistdetail_*` flows assert on end-to-end.

For that sign-in to land on a screen with real data (not empty lists), the
bearer token needs to be backed by an actual account and catalog on the
backend it points at — see `maestro/README.md`'s "Running against a mocked
backend" section for the self-seeded Docker backend that provides both.

An alternative not used here: a pre-authorized test Google account on the
device/emulator (CI emulator image with an account already signed in and
"always use this account" previously chosen, so only one consent tap
remains). This is brittle across emulator images and Google Play Services
versions, and the local-auth bypass above is simpler and fully
deterministic, so this path wasn't pursued.
