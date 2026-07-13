# TODO

## CI scaffold (`.github/workflows/ci-android.yml`, `ci-ios.yml`)

- [ ] Add `GOOGLE_SERVICES_JSON_B64` repo secret (see `docs/notes/google-services-json-ci-plan.md`) —
      required for every Android job (`quality`, `test`, `build-debug`, `build-release`).
- [ ] Add Android keystore secrets — `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_STORE_PASSWORD`,
      `ANDROID_KEYSTORE_KEY_ALIAS`, `ANDROID_KEYSTORE_KEY_PASSWORD` — required for `build-release`
      on `main`.
- [ ] Register the self-hosted macOS runner with labels `self-hosted, macOS, ios` — the iOS
      workflow's jobs will queue/pend until it exists (see `docs/notes/ci-cd-plan.md`).
- [ ] Push `ci/scaffold-android-ios-workflows` and open a PR.
