# Login flows

Covers `presentation/feature/login` (`LoginScreen`, `SignInCard`,
`WelcomeBackground`).

## Flows

- `login_initial_render.yaml` — asserts the screen's baseline state: root
  screen, welcome hero, and an enabled sign-in button, with no loading
  spinner or error showing.
- `login_sign_in_tap_triggers_auth.yaml` — taps the sign-in button and
  confirms it isn't a no-op (loading state kicks in / hands off to the
  native auth surface), without trying to complete sign-in.

## Why there's no full end-to-end login flow here

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
screen" flow isn't practical to script here as-is. Two ways to make it
testable if that coverage becomes worth the investment:

1. **Pre-authorized test Google account on the device/emulator.** If the
   CI emulator image has a Google account already signed in (and Google's
   account-picker is bypassed because there's only one account /
   "always use this account" was previously chosen), Maestro could
   plausibly drive the one remaining consent tap. This is brittle across
   emulator images and Google Play Services versions, and out of this
   branch's scope to set up.
2. **The existing debug-only local-auth bypass.** `SzigetAuthService`
   already has `localSignIn()` (see
   `shared/src/commonMain/kotlin/com/ilyne/helloszigetkmp/core/auth/SzigetAuthService.kt`),
   which skips Google entirely and logs in with a fixed bearer token
   (`BEARER_TOKEN_LOCALHOST`) against the dev backend - but only when
   `appConfig.isDebug()` is true, so it can never activate in a
   release-shaped build regardless of the flag below. `LoginViewModel`
   already gates the call behind the `SKIP_GOOGLE_SIGN_IN` build-time
   constant (`core/config/AppConfig.kt`), which is off by default and is
   set via the `sziget.skipGoogleSignIn` Gradle property. Building the
   debug APK with
   `./gradlew :androidApp:assembleDebug -Psziget.skipGoogleSignIn=true`
   would make `login_sign_in_button` complete a real (local-account)
   sign-in and navigate to Main, which a Maestro flow could then assert on
   end-to-end. This wasn't wired up here because it changes what the debug
   build under test actually does (bypassing real auth), which should be a
   deliberate, separate decision rather than a side effect of adding these
   flows - but it's the most promising path to fuller coverage without
   scripting Google's UI.

Until one of those exists, these flows only cover what's reachable purely
within this app's own UI: initial render and confirming the sign-in tap
does something.
