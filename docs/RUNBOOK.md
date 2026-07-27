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

Manual process (no CI automation yet). **Bump `CURRENT_PROJECT_VERSION` in
`iosApp/Configuration/Release.xcconfig` before every upload** — App Store Connect rejects
duplicate build numbers.

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

`.github/workflows/maestro-tests.yml` — runs on every PR (plus manual `workflow_dispatch`).
Writes `androidApp/google-services.json` from the same `GOOGLE_SERVICES_JSON` secret used by the
beta release workflow (required because `assembleDebug` also touches `:androidApp`), builds the
debug APK, then boots a KVM-accelerated `google_apis`/API 34 emulator via
[`reactivecircus/android-emulator-runner`](https://github.com/ReactiveCircus/android-emulator-runner)
and runs the flows under [`.maestro/`](../.maestro) against it with the
[Maestro CLI](https://maestro.mobile.dev). Currently just `.maestro/smoke_test.yaml`, a
launch-and-assert-login-screen check — flows past sign-in need a CI-friendly way to bypass Google
Sign-In first (see the `sziget.skipGoogleSignIn` flag above).

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

Repo secrets consumed by the Play Console beta release workflow — set these under
**Settings → Secrets and variables → Actions**:

| Secret | Contents |
|---|---|
| `GOOGLE_SERVICES_JSON` | Full contents of `androidApp/google-services.json` |
| `ANDROID_KEYSTORE_BASE64` | Release keystore file, base64-encoded (`base64 -i release.keystore \| pbcopy` or equivalent) |
| `ANDROID_KEYSTORE_STORE_PASSWORD` | `storePassword` from `keystore.properties` |
| `ANDROID_KEYSTORE_KEY_ALIAS` | `keyAlias` from `keystore.properties` |
| `ANDROID_KEYSTORE_KEY_PASSWORD` | `keyPassword` from `keystore.properties` |
| `PLAY_SERVICE_ACCOUNT_JSON` | Full contents of a Play Console API service account JSON key with Release Manager access to the `com.ilyne.helloszigetkmp.beta` app listing (Play Console → Setup → API access) |
