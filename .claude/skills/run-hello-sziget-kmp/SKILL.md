---
name: run-hello-sziget-kmp
description: Build, install, launch, and drive the Sziget KMP app (Android debug build) against a self-seeded local backend, using the repo's existing Maestro E2E flows as the interaction harness. Use when asked to run, launch, test, screenshot, or verify a change in the Sziget app — "run the app", "does this screen work", "take a screenshot of My Lineup/Discover/Schedule/Profile", "drive the login flow".
---

# Running hello-sziget-kmp

Paths below are relative to the repo root (`hello-sziget-kmp/`), not this
skill directory.

This is a Kotlin Multiplatform app (Android + iOS, Compose Multiplatform
shared UI). The repo already has a full interaction harness for the Android
build: [Maestro](https://maestro.mobile.dev/) E2E flows under `maestro/flows/`,
driven against a real (self-seeded, dockerized) backend so login and data
screens work deterministically without real Google Sign-In. **Use that
harness — don't build a new one.** It was verified working end-to-end in this
container: emulator boot → backend up → APK install → `maestro test` →
real screenshot of the logged-in "My Lineup" screen.

There is no programmatic driver script beyond what's already in the repo
(`maestro/README.md`, `scripts/run_e2e_backend.sh`,
`scripts/reinstall_e2e_test_app.sh`) — Maestro's CLI + YAML flows *are* the
driver.

## Prerequisites (verified in this container)

- Android SDK with an AVD already provisioned (`~/Library/Android/sdk`,
  emulator name `Pixel_10_Pro` — check with
  `~/Library/Android/sdk/emulator/emulator -list-avds`).
- `adb` at `~/Library/Android/sdk/platform-tools/adb` (not on `PATH` by
  default in this container — add it).
- Docker running (`docker info`).
- The `hello-sziget-backend:e2e` image already built locally (check with
  `docker image inspect hello-sziget-backend:e2e`). If missing, it must be
  built once from a checkout of the separate `hello-sziget` backend repo —
  this repo never needs that checkout again after the image exists. If you
  don't have that repo available, you cannot get real/stable data, but
  `login_initial_render.yaml`-style flows (pre-auth) still work.
- Maestro CLI — install with:

```bash
curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH:$HOME/.maestro/bin"
```

## Run (agent path — this is the one to use)

```bash
export PATH="$HOME/Library/Android/sdk/platform-tools:$HOME/Library/Android/sdk/emulator:$HOME/.maestro/bin:$PATH"

# 1. Boot the emulator (skip if adb devices already shows one attached).
nohup emulator -avd Pixel_10_Pro -no-snapshot-save -no-boot-anim > /tmp/emulator.log 2>&1 &
disown
# Wait for it: `adb devices` should show a line ending in "device" (not "offline").

# 2. Start the self-seeded backend + write local.properties + rebuild + install,
#    all in one step:
./scripts/reinstall_e2e_test_app.sh
# (Or do it manually: ./scripts/run_e2e_backend.sh prints the values to hand-paste
#  into local.properties, then `./gradlew :androidApp:installDebug`.)

# 3. Drive the app with Maestro flows:
maestro test maestro/flows/login/login_initial_render.yaml     # pre-auth smoke check
maestro test maestro/flows/lineup/reach_lineup_and_verify_state.yaml  # full sign-in + nav
maestro test maestro                                            # whole suite (needs config.yaml at maestro/, so pass "maestro", not "maestro/flows")

# 4. Screenshot whatever's on screen right now:
adb exec-out screencap -p > /tmp/app_screenshot.png
```

Any flow past `login/` completes a real sign-in and lands on real seeded
data because `local.properties` was written with `sziget.skipGoogleSignIn=true`
by `reinstall_e2e_test_app.sh` — see `maestro/flows/login/README.md` for why
this bypass exists (native Google Sign-In UI can't be driven by Maestro).

Tear down the backend when done (optional — idempotent, safe to leave running):

```bash
./scripts/run_e2e_backend.sh down
```

## Direct invocation (unit/JVM tests, no emulator needed)

Most PRs in this repo touch shared Kotlin logic, not just UI — for those,
skip the emulator entirely:

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :shared:testAndroidHostTest --tests "com.ilyne.helloszigetkmp.SomeTestClass"
```

## Run (human path)

- Android: `./gradlew :androidApp:assembleDebug`, then install/launch via
  Android Studio or `adb install`. Without the `skipGoogleSignIn` bypass,
  login requires a real Google account on the device.
- iOS: open `/iosApp` in Xcode and run from there — no Gradle CLI path for
  iOS builds. (Not exercised in this container; macOS/Xcode-only, not
  re-verified here beyond confirming `xcrun simctl list devices` shows
  simulators available.)

## Gotchas

- **`maestro test maestro/flows` silently ignores `config.yaml`.** Maestro
  only looks for `config.yaml` in the *exact* directory you pass — the repo's
  lives at `maestro/config.yaml`, one level above `flows/`. Run the whole
  suite as `maestro test maestro`, not `maestro test maestro/flows`. Single
  flow files are unaffected (each carries its own `appId` header).
- **The bearer token expires 60 minutes after the backend container starts.**
  If `maestro test` starts failing with the app stuck on the Login screen,
  that's almost always this — rerun `./scripts/reinstall_e2e_test_app.sh` to
  get a fresh token, fresh seed data, and a rebuilt APK in one shot.
- **`sziget.skipGoogleSignIn` is a compile-time constant, not a runtime
  toggle.** Changing `local.properties` requires a rebuild+reinstall
  (`./gradlew :androidApp:installDebug`), not just relaunching the existing
  APK.
- **`adb`/`emulator` aren't on `PATH` by default** in this container — they're
  under `~/Library/Android/sdk/platform-tools` and
  `~/Library/Android/sdk/emulator`.
- **The `hello-sziget-backend:e2e` image is not built from this repo.** It
  comes from a separate `hello-sziget` backend repo's `Dockerfile.e2e`. If
  it's missing and you don't have that checkout, you're limited to flows
  that don't need auth/data (e.g. `login_initial_render.yaml`).
- Compose nodes are exposed to Maestro via `Modifier.testTag(...)` → Android
  resource-id (root-level `testTagsAsResourceId` in `App.kt`). Flows use
  `id: "<testTag>"` selectors, not text matching, wherever a screen already
  has one.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `command not found: adb` / `emulator` | Add `~/Library/Android/sdk/platform-tools` and `~/Library/Android/sdk/emulator` to `PATH`. |
| `maestro: command not found` right after install | New shells pick up `~/.bash_profile`/`~/.zshrc` (installer appends there); in the current shell, `export PATH="$PATH:$HOME/.maestro/bin"`. |
| `error: image 'hello-sziget-backend:e2e' not found locally` | Build it once from a `hello-sziget` backend checkout: `docker build -f Dockerfile.e2e -t hello-sziget-backend:e2e .` — this repo doesn't contain that Dockerfile. |
| Flow stuck on Login screen / assertion on `login_screen` never proceeds | Bearer token expired — run `./scripts/reinstall_e2e_test_app.sh`. |
| `adb devices` shows the emulator as `offline` right after boot | Not booted yet — poll `adb devices` every few seconds until the line reads `device`, not `offline`. |
