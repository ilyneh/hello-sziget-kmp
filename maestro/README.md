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
maestro test maestro/flows/login/login_initial_render.yaml   # a single flow
maestro test maestro                                          # everything
```

`maestro test` only looks for `config.yaml` in the exact root of the directory you pass it, not
in parent directories - so running the full suite must be `maestro test maestro` (this repo's
`config.yaml` lives at `maestro/config.yaml`, one level above `flows/`), not
`maestro test maestro/flows`. Running a single flow file directly is unaffected by this, since
each flow carries its own `appId` header. See `maestro/config.yaml`'s comments for details.

By default the installed debug build talks to the real dev backend and requires real Google
Sign-In, so flows either stop short of completing auth or (for flows that continue past login,
like `artistdetail/`) assume login was somehow already bypassed. For flows to actually complete
sign-in and run against stable data, build with the mocked/self-seeded backend below.

## Running against a mocked backend

This runs the app against a real backend (not a hand-rolled mock) — self-seeded with
deterministic dev users/friends and the real festival catalog, in a Docker container — so
Google Sign-In can be bypassed via the app's existing debug-only local-auth shortcut
(`sziget.skipGoogleSignIn`) and flows get stable data instead of whatever the live dev backend
currently has.

**One-time setup** (from a checkout of the
[`hello-sziget`](https://github.com/ilyneh/hello-sziget) backend repo — this repo never
references that checkout again after this step, only the resulting image tag):

```bash
docker build -f Dockerfile.e2e -t hello-sziget-backend:e2e .
```

**Every time you want to run Maestro against it:**

```bash
# 1. Start the self-seeded backend (Postgres + the e2e image), prints the values to use below.
./scripts/run_e2e_backend.sh

# 2. Add the printed values to local.properties (gitignored — see local.properties.example):
#      sziget.localBackendUrl=http://10.0.2.2:8081/api/v1
#      sziget.localBearerToken=<printed token>
#      sziget.skipGoogleSignIn=true

# 3. Rebuild and reinstall — these are compile-time-baked constants, not a runtime toggle.
./gradlew :androidApp:installDebug

# 4. Run flows as usual.
maestro test maestro

# 5. Tear down when done (next run starts from a fresh DB; safe to skip and leave running too,
#    since the seed scripts are idempotent).
./scripts/run_e2e_backend.sh down
```

With `sziget.skipGoogleSignIn=true`, tapping "Continue with Google" routes through
`SzigetAuthService.localSignIn()` instead of launching Google's native Sign-In UI — see
`maestro/flows/login/README.md` for why that native UI can't be driven by Maestro directly, and
why this bypass is the path taken instead.
