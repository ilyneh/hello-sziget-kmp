# Maestro E2E flows

End-to-end UI flows for the Android debug build, driven by [Maestro](https://maestro.mobile.dev/).

## Setup

```bash
curl -Ls "https://get.maestro.mobile.dev" | bash
./gradlew :androidApp:installDebug
```

A device or emulator must already be running (`adb devices`).

## Layout

```
maestro/
  config.yaml          shared appId used by every flow
  flows/
    <feature>/          one directory per presentation/feature/<name> screen
      *.yaml             individual flows for that screen
```

`appId` is `com.ilyne.helloszigetkmp.debug` (the `debug` build type's `applicationIdSuffix`).

## Targeting elements

Compose nodes are exposed to Maestro as Android resource-ids via
`Modifier.testTag(...)`, because `App.kt` sets
`Modifier.semantics { testTagsAsResourceId = true }` at the root of the
composition. Flows should prefer `id: "<testTag>"` selectors over text
matching where a screen already has (or has been given) a stable `testTag`;
fall back to `text:` selectors only for content that's inherently
copy-driven (validation errors, labels) and unlikely to need a stable id.

## Running

```bash
maestro test maestro/flows/login
maestro test maestro/flows          # everything
```
