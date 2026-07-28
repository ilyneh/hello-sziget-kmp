# Runbook: Config, Builds & Releases

Operational reference for setting up local config and building/releasing this KMP app.
For architecture and general dev commands, see [`CLAUDE.md`](../CLAUDE.md).

## Table of contents

- [Local config setup](#local-config-setup)
- [Android builds](#android-builds)
- [iOS builds](#ios-builds)
- [Google Sign-In setup](#google-sign-in-setup)
- [Versioning](#versioning)
- [Lint / format / static analysis](#lint--format--static-analysis)
- [CI/CD status](#cicd-status)
- [Secrets checklist](#secrets-checklist)

---

## Local config setup

Three files are gitignored and must be created locally before the app will build/run fully.
Templates for all of them are checked in.

| File | Copy from | Purpose |
|---|---|---|
| `local.properties` | [`local.properties.example`](../local.properties.example) | Backend URL override, dev bearer token, skip-Google-Sign-In flag; also holds `sdk.dir` (managed by Android Studio) |
| `androidApp/keystore.properties` | [`androidApp/keystore.properties.example`](../androidApp/keystore.properties.example) | Android release/beta signing credentials |
| `androidApp/google-services.json` | — (get from Firebase console) | Android Google Sign-In / Crashlytics |
| `iosApp/iosApp/GoogleService-Info.plist` | — (get from Firebase console) | iOS Google Sign-In |

### `local.properties` keys

```properties
# Point debug builds at a local backend instead of the dev Cloud Run URL.
# Android emulator -> http://10.0.2.2:8000/api/v1
# iOS simulator/device -> http://<your-machine-LAN-IP>:8000/api/v1
sziget.localBackendUrl=

# Dev-only bearer token for the "skip Google sign-in" shortcut.
sziget.localBearerToken=

# When true, bypasses Google Sign-In on the login screen (debug builds only, ignored in release).
sziget.skipGoogleSignIn=false
```

Any of these keys can be passed as a Gradle project property instead, and a `-P` value always
wins over `local.properties`:

```sh
./gradlew :androidApp:assembleDebug -Psziget.localBackendUrl=http://10.0.2.2:8000/api/v1
```

**How it's wired:** `shared/build.gradle.kts` resolves each key (Gradle property, falling back to
`local.properties`) and feeds it to a generated `SzigetBuildConfig` object
(`buildSrc/src/main/kotlin/GenerateSzigetBuildConfigTask.kt`), which `AppConfig`
(`shared/src/commonMain/.../core/config/AppConfig.kt` + platform actuals) reads at runtime. This
is the only build-time-configurable path — everything else in `AppConfig` (e.g. `BASE_URL_DEV`,
`BASE_URL_PROD`) is a hardcoded constant.

---

## Android builds

Build types (`androidApp/build.gradle.kts`) — no product flavors, three build types:

| Build type | Application ID suffix | Signing | Minify/shrink |
|---|---|---|---|
| `debug` | `.debug` | AGP auto debug keystore | off |
| `beta` | `.beta` | release keystore (`initWith(release)`) | on (inherits release) |
| `release` | (none) | release keystore | on, Crashlytics mapping upload enabled |

```sh
# Assemble (APK)
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:assembleBeta
./gradlew :androidApp:assembleRelease

# Bundle (AAB, for Play Store)
./gradlew :androidApp:bundleDebug
./gradlew :androidApp:bundleBeta
./gradlew :androidApp:bundleRelease
```

`beta`/`release` builds require `androidApp/keystore.properties` to exist — the build fails
loudly (rather than producing an unsigned artifact) if it's missing.

Publishing the beta AAB to Google Play Console (via the
[Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) plugin) additionally
requires `androidApp/play-service-account.json` — see [CI/CD status](#cicd-status) below:

```sh
./gradlew :androidApp:publishBetaBundle
```

---

## iOS builds

No CocoaPods, no Fastlane — the shared KMP framework is embedded via the standard KMM pattern:
Xcode runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` as a build phase, so a normal
Xcode build/run/archive handles it automatically.

- Open `iosApp/iosApp.xcodeproj` in Xcode (no `.xcworkspace`).
- Debug config → `iosApp/Configuration/Config.xcconfig` (bundle id `com.ilyne.hellosziget.debug`).
- Release config → `iosApp/Configuration/Release.xcconfig` (bundle id `com.ilyne.hellosziget`).
- Both set `CURRENT_PROJECT_VERSION` (build number) and `MARKETING_VERSION` (marketing version);
  `Info.plist` has no explicit version keys — they're generated from the active xcconfig.

### TestFlight / App Store release

No CI automation yet, but `scripts/release-ios-beta.sh` wraps the manual local steps (must run
on macOS with Xcode installed):

```sh
scripts/release-ios-beta.sh
```

By default it increments `CURRENT_PROJECT_VERSION` in `iosApp/Configuration/Release.xcconfig` by
1 (App Store Connect rejects a duplicate build number), prompts for confirmation, then runs
`xcodebuild archive` followed by `xcodebuild -exportArchive` — which uploads straight to App
Store Connect / TestFlight as its last step, since `ExportOptions.plist` sets
`destination=upload`. The build-number bump is left as an uncommitted change in
`Release.xcconfig` for you to commit once the upload succeeds. Flags: `--build-number N` to set
an explicit build number, `--no-bump` to reuse the current one, `--yes`/`-y` to skip the
confirmation prompt, `--help` for details.

To automatically add the uploaded build to a TestFlight beta group (instead of assigning it by
hand in App Store Connect afterward), pass `--test-group NAME` (repeatable for multiple groups).
This hands off to `scripts/assign_testflight_group.py`, which talks to the App Store Connect API
directly — no Fastlane, no third-party pip packages — and needs:
- An App Store Connect API key (App Store Connect → Users and Access → Integrations → App Store
  Connect API; App Manager role or higher), giving you a key ID, an issuer ID, and a downloaded
  `.p8` private key file.
- `python3` and `openssl` in `PATH` (only checked/required when `--test-group` is used).

Pass the key via `--asc-key-id`/`--asc-issuer-id`/`--asc-key-path`, or the
`ASC_API_KEY_ID`/`ASC_API_ISSUER_ID`/`ASC_API_KEY_PATH` env vars:

```sh
scripts/release-ios-beta.sh --test-group "Internal Testers" --test-group "Sziget Team" \
  --asc-key-id ABC123DEF4 \
  --asc-issuer-id 69a6de70-03db-47e3-e053-5b8c7c11a4d1 \
  --asc-key-path ~/.appstoreconnect/AuthKey_ABC123DEF4.p8
```

After uploading, the script polls the build's processing status (`--group-poll-interval`,
default 30s; `--group-poll-timeout`, default 1800s) until Apple finishes processing it, then
adds it to each named group. Group names must already exist in App Store Connect — the script
doesn't create groups, only looks them up by exact name per app.

Equivalent manual steps, if you'd rather not use the script:

```sh
xcodebuild archive \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -archivePath /tmp/iosApp-archive/iosApp.xcarchive \
  -allowProvisioningUpdates

xcodebuild -exportArchive \
  -archivePath /tmp/iosApp-archive/iosApp.xcarchive \
  -exportPath /tmp/iosApp-export \
  -exportOptionsPlist iosApp/Configuration/ExportOptions.plist \
  -allowProvisioningUpdates
```

Prerequisites:
- Signed into Xcode with access to team `J6TQZMUWUM` (the distribution team used in
  `ExportOptions.plist`; note this differs from the `U3TDTWW6W5` dev team in the xcconfig files).
- A valid `iPhone Distribution` signing identity in your Keychain.
- App Store Connect provisioning profile **"Hello Sziget Production"** for
  `com.ilyne.hellosziget`.

Troubleshooting: missing provisioning profile, invalid/duplicate signing cert, or stale
DerivedData are the usual causes of archive/export failures — clean DerivedData and re-check
profile/cert validity in Xcode's Signing & Capabilities tab first.

### Manually uploading dSYMs to Firebase Crashlytics

Xcode already uploads dSYMs to Crashlytics automatically on every build, via the "Crashlytics:
Run" build phase in `iosApp.xcodeproj` (runs Firebase's `Crashlytics/run` script from the
SPM-checked-out `firebase-ios-sdk` package). `scripts/upload-ios-dsyms-firebase.sh` is for the
cases that build-time step can't cover — re-uploading after it failed (e.g. no network at build
time), or uploading dSYMs downloaded later from App Store Connect / Xcode Organizer:

```sh
# From an .xcarchive (e.g. the one scripts/release-ios-beta.sh leaves under /tmp):
scripts/upload-ios-dsyms-firebase.sh /tmp/ios-beta-release.XXXXXX/iosApp.xcarchive

# From a dSYMs .zip downloaded via Xcode -> Window -> Organizer -> Archives -> Download dSYMs:
scripts/upload-ios-dsyms-firebase.sh ~/Downloads/appDsyms.zip
```

It defaults to `iosApp/GoogleService-Info-Release.plist`; pass `--config Debug` or
`--google-service-plist PATH` for a different Firebase app. It auto-discovers Firebase's
`upload-symbols` binary under `~/Library/Developer/Xcode/DerivedData` (requires having
built/archived the project at least once so Swift Package Manager has checked out
`firebase-ios-sdk`); override with `--upload-symbols-path` if needed. `--help` for full details.

---

## Google Sign-In setup

- **Android**: place `androidApp/google-services.json` (from Firebase console) — required by the
  `google-services` Gradle plugin; any Gradle task touching `:androidApp` fails without it.
- **iOS**: place two per-configuration files at `iosApp/GoogleService-Info-Debug.plist`
  (Firebase app `com.ilyne.hellosziget.debug`) and `iosApp/GoogleService-Info-Release.plist`
  (Firebase app `com.ilyne.hellosziget`). A "Select GoogleService-Info.plist" Run Script build
  phase copies the one matching the active `$(CONFIGURATION)` to
  `iosApp/iosApp/GoogleService-Info.plist` (gitignored, build-generated) before every build —
  the build fails fast with a clear error if the source file for the active configuration is
  missing. `Info.plist` declares a matching `GIDClientID` and URL scheme.
- **Shared Web Client ID**: hardcoded in
  `shared/src/commonMain/kotlin/.../core/auth/GoogleAuthConfig.kt` — update this if the OAuth
  Web Client ID ever changes (it is not build-time configurable).

### iOS Keychain query attributes (`SecureSettingsFactory.ios.kt`)

`TokenStorage`'s iOS backing (`shared/src/iosMain/kotlin/.../core/settings/SecureSettingsFactory.ios.kt`)
uses `com.russhwolf.settings.KeychainSettings`, constructed with a set of "default properties"
(currently `kSecAttrService` + `kSecAttrAccessible`). **Every one of those properties is folded
into the query the library issues for every read/update/delete, not just for item creation.** If
you ever add, remove, or change one of these properties, any item already stored under the old
property set becomes invisible to the new query: reads silently return null (looks like "no
session", easy to miss), but `SecItemUpdate` returns `errSecItemNotFound`, which the library turns
into a thrown `"Keychain error: the specified item could not be found in the keychain"` — this
surfaces as a sign-in failure specifically on any device/simulator that had already signed in with
a build from *before* your change, since it hits the very next `tokenStorage.save()` after a
successful Google auth (see git history around commit `4ca5fb0` for the real incident this
happened from).

If you change these properties again, either bump `KEYCHAIN_SERVICE_NAME` (points at a brand-new,
empty keychain entry - simplest, but forces every existing session to sign in again) or add/update
a raw `SecItemDelete` (or read-and-re-add) migration step in `createSecureSettings()` that queries
by a strict subset of attributes guaranteed not to have changed, mirroring
`purgeLegacyAccessibilityKeychainItem()`.

---

## Versioning

Android and iOS version independently — there is no single source of truth across platforms.

- **Android**: fully git-derived (`androidApp/build.gradle.kts`).
  - `versionCode` = `git rev-list --count HEAD`
  - `versionName` = `1.0-<versionCode>-<shortSha>` (plus `-debug`/`-beta` suffix)
  - The `1.0` base is the one manually maintained value — bump `versioningBaseName` for a
    marketing-version change.
  - Requires a full git history (`fetch-depth: 0` in CI) — a shallow clone falls back to
    `1`/`1.0-nogit` and logs a warning.
- **iOS**: fully manual, set in `iosApp/Configuration/*.xcconfig`.
  - `CURRENT_PROJECT_VERSION` → build number (`CFBundleVersion`) — **must be bumped before every
    TestFlight/App Store upload**.
  - `MARKETING_VERSION` → `CFBundleShortVersionString`.

---

## Lint / format / static analysis

```sh
./gradlew spotlessApply     # format (ktlint 1.8.0 + Compose ruleset)
./gradlew spotlessCheck     # check only, no changes
./gradlew detekt            # static analysis, config/detekt/detekt.yml, per-project baselines
```

---

## CI/CD status

`.github/workflows/unit-tests.yml` — runs on every push to `main` and every PR (plus manual
`workflow_dispatch`). Two jobs: `android-unit-tests` runs `./gradlew :shared:testAndroidHostTest`
on `ubuntu-latest` (no `google-services.json` needed — `:shared` doesn't apply the
`google-services` plugin, only `:androidApp` does); `ios-unit-tests` runs
`./gradlew :shared:iosSimulatorArm64Test` on `macos-latest`. Both upload their test reports as
build artifacts.

`.github/workflows/maestro-tests.yml` — runs on every PR (plus manual `workflow_dispatch`) on the
standard `ubuntu-latest` runner (2-core/7GB — no "Larger runner" access on this repo). The
emulator, Postgres/backend containers, and Gradle daemon competing for that RAM caused an ANR in
one run, so the workflow minimizes concurrent memory pressure instead of throwing more hardware
at it: the seed backend is stopped (not `down` — that would rotate the bearer token already baked
into the install command) right after its bearer token is captured and only restarted right
before `maestro test` runs, and `./gradlew --stop` kills the Gradle daemon immediately after
`installDebug` finishes, since nothing needs it again. Frees disk space first (removes preinstalled toolchains this build doesn't use — `dotnet`,
Android NDK, GHC, CodeQL, Boost — since a full run otherwise gets close enough to
`ubuntu-latest`'s disk limit that the emulator's AVD userdata partition can fail to allocate).
Writes `androidApp/google-services.json` from the same `GOOGLE_SERVICES_JSON` secret used by the
beta release workflow, pulls the self-seeded backend image (`ghcr.io/ilyneh/hello-sziget-backend:e2e`
— built by the separate `hello-sziget` backend repo's own CI, see
[`maestro/README.md`](../maestro/README.md)) and tags it locally as `hello-sziget-backend:e2e`,
then starts it via `scripts/run_e2e_backend.sh` (a plain step — this only needs `docker`, not an
emulator, so it runs before/independently of the emulator boot rather than being gated behind
it). That script writes the bearer token to a file (`/tmp/hello-sziget-e2e-bearer-token` by
default) rather than printing the raw value under CI; the workflow reads it from there and
registers it with `::add-mask::` so it's redacted in the log from that point on, including where
it's interpolated into the emulator step's script below. Then it boots a KVM-accelerated
`google_apis`/API 34 emulator via
[`reactivecircus/android-emulator-runner`](https://github.com/ReactiveCircus/android-emulator-runner),
builds and installs the debug APK against the already-running backend via `-P` Gradle properties
(`sziget.localBackendUrl`, `sziget.localBearerToken`, `sziget.skipGoogleSignIn=true` — bypassing
`local.properties` entirely), and runs `maestro test maestro` (all the flows under
[`maestro/flows/`](../maestro/flows)) with the [Maestro CLI](https://maestro.mobile.dev). The
seed backend container is always torn down afterward regardless of outcome. See
`maestro/README.md` for how to run the same backend locally (`scripts/reinstall_e2e_test_app.sh`
is the equivalent one-shot local convenience wrapper, not used directly by this workflow).

`.github/workflows/play-beta-release.yml` — manual (`workflow_dispatch`-only) Android beta
release to Google Play Console, using the
[Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) plugin
(`com.github.triplet.play`, configured in `androidApp/build.gradle.kts`) rather than a separate
upload action or Fastlane, since it runs in-process as part of the existing Gradle build and
build-type setup. It writes `google-services.json`, the release keystore +
`keystore.properties`, and a Play Console service account key from repo secrets (see the
checklist below), then runs `./gradlew :androidApp:publishBetaBundle`, which builds and uploads
the `beta` build type's AAB to the Play Console **internal** track ("Internal testing" — no
Google review, immediate availability, capped at 100 testers managed via a tester list in Play
Console) and deletes all the secret-derived files afterward regardless of outcome. This is
distinct from Play Console's own track literally named "beta" ("Open testing", which requires
review and is far more widely joinable) — the build type is named "beta" independently of that,
and the two aren't related.

Because the `beta` build type uses `applicationIdSuffix = ".beta"`, it publishes under its own
package (`com.ilyne.helloszigetkmp.beta`) with its own separate Play Console app listing —
distinct from the `release` app (`com.ilyne.helloszigetkmp`). That app listing must already
exist in Play Console (Play Publisher can publish to an existing app, not create a new one), and
its internal testing track needs at least one manual release already promoted through it once
before automated publishing works.

Planning notes exist locally under the gitignored `docs/notes/` directory
(`google-services-json-ci-plan.md`, `create-gcloud-serviceaccounts.md`) covering GCP service
accounts already provisioned for backend deploys. The manual iOS release steps above are the
main remaining candidate for future automation.

---

## Secrets checklist

Never commit these (all gitignored already — verify with `git status` before committing config
changes):

- `local.properties`
- `androidApp/keystore.properties`, `androidApp/keystore/`, any `*.jks` / `*.keystore`
- `androidApp/google-services.json`
- `iosApp/iosApp/GoogleService-Info.plist`
- `androidApp/release/`
- `androidApp/play-service-account.json`

Committed templates to copy from instead: `local.properties.example`,
`androidApp/keystore.properties.example`.

### GitHub Actions secrets

`GOOGLE_SERVICES_JSON` is also consumed by `maestro-tests.yml` (needed to build the debug APK) —
same value as below. `unit-tests.yml` needs no secrets.

Repo secrets — set these under **Settings → Secrets and variables → Actions**:

| Secret | Used by | Contents |
|---|---|---|
| `GOOGLE_SERVICES_JSON` | `play-beta-release.yml`, `maestro-tests.yml` | Full contents of `androidApp/google-services.json` |
| `ANDROID_KEYSTORE_BASE64` | `play-beta-release.yml` | Release keystore file, base64-encoded (`base64 -i release.keystore \| pbcopy` or equivalent) |
| `ANDROID_KEYSTORE_STORE_PASSWORD` | `play-beta-release.yml` | `storePassword` from `keystore.properties` |
| `ANDROID_KEYSTORE_KEY_ALIAS` | `play-beta-release.yml` | `keyAlias` from `keystore.properties` |
| `ANDROID_KEYSTORE_KEY_PASSWORD` | `play-beta-release.yml` | `keyPassword` from `keystore.properties` |
| `PLAY_SERVICE_ACCOUNT_JSON` | `play-beta-release.yml` | Full contents of a Play Console API service account JSON key with Release Manager access to the `com.ilyne.helloszigetkmp.beta` app listing (Play Console → Setup → API access) |
| `GHCR_PULL_TOKEN` | `maestro-tests.yml` | A GitHub PAT (classic or fine-grained) with `read:packages` scope and access to `ilyneh/hello-sziget`, used to `docker login ghcr.io` and pull the private `ghcr.io/ilyneh/hello-sziget-backend:e2e` seed backend image. See `maestro/README.md` for the companion workflow that publishes that image from the backend repo. |
