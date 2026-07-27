#!/usr/bin/env bash
#
# Full refresh cycle for running Maestro against the mocked/test backend (see
# maestro/README.md): restarts the self-seeded backend from a clean DB (so the bearer token is
# always fresh - it expires 60 minutes after the backend starts), writes the resulting config
# into local.properties automatically, then rebuilds and reinstalls the debug APK so the app
# picks it up. Run this any time `maestro test` starts failing with the app stuck on the Login
# screen - that almost always means the previous token expired.
#
# Requires the hello-sziget-backend:e2e image to already exist locally (built once from a
# hello-sziget checkout: docker build -f Dockerfile.e2e -t hello-sziget-backend:e2e .).
#
# Usage:
#   scripts/reinstall_e2e_test_app.sh

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
compose_file="$repo_root/maestro/docker-compose.e2e.yml"
local_properties="$repo_root/local.properties"

if ! docker image inspect hello-sziget-backend:e2e >/dev/null 2>&1; then
    echo "error: image 'hello-sziget-backend:e2e' not found locally." >&2
    echo "Build it once from a hello-sziget checkout:" >&2
    echo "  docker build -f Dockerfile.e2e -t hello-sziget-backend:e2e ." >&2
    exit 1
fi

echo "==> Restarting the e2e backend from a clean DB (fresh bearer token, fresh seed data)..."
docker compose -f "$compose_file" down -v >/dev/null 2>&1 || true

# Retries a few times before giving up - transient registry timeouts pulling postgres:16 (seen
# on GH Actions shared runners, likely Docker Hub rate-limiting/slow-responding to anonymous
# pulls) are common enough to be worth a retry rather than failing the whole run outright.
compose_up_with_retry() {
    local attempt max_attempts=3
    for ((attempt = 1; attempt <= max_attempts; attempt++)); do
        if docker compose -f "$compose_file" up -d --wait; then
            return 0
        fi
        if ((attempt < max_attempts)); then
            echo "docker compose up failed (attempt $attempt/$max_attempts) - retrying in 10s..." >&2
            docker compose -f "$compose_file" down -v >/dev/null 2>&1 || true
            sleep 10
        fi
    done
    echo "error: docker compose up failed after $max_attempts attempts." >&2
    return 1
}

compose_up_with_retry

# The backend's healthcheck (which --wait above blocks on) can pass slightly before the seed
# step finishes and actually logs E2E_BEARER_TOKEN, so a single immediate grep can race and come
# up empty - poll for it instead of checking exactly once. The `|| true` matters: under
# `pipefail`, grep finding no match yet is a normal "still seeding" outcome, not a real error -
# without it, `set -e` would kill the whole script on the very first empty check instead of
# letting the loop retry.
token=""
for _ in $(seq 1 15); do
    token="$(docker compose -f "$compose_file" logs backend | grep '^backend.*E2E_BEARER_TOKEN=' | tail -n 1 | sed 's/.*E2E_BEARER_TOKEN=//' || true)"
    if [[ -n "$token" ]]; then
        break
    fi
    sleep 2
done
if [[ -z "$token" ]]; then
    echo "error: couldn't find E2E_BEARER_TOKEN in backend logs after 30s — it may still be seeding." >&2
    echo "Check with: docker compose -f \"$compose_file\" logs backend" >&2
    exit 1
fi

echo "==> Writing config into local.properties..."
touch "$local_properties"
set_property() {
    local key="$1" value="$2" tmp
    if grep -q "^${key}=" "$local_properties" 2>/dev/null; then
        # Redirect-to-temp-file-then-move instead of `sed -i` — `-i` takes its backup-suffix
        # argument differently between BSD sed (macOS) and GNU sed (Linux CI runners), and this
        # form works identically on both without needing to detect which one is running.
        tmp="$(mktemp)"
        sed "s#^${key}=.*#${key}=${value}#" "$local_properties" > "$tmp"
        mv "$tmp" "$local_properties"
    else
        printf '%s=%s\n' "$key" "$value" >> "$local_properties"
    fi
}
set_property "sziget.localBackendUrl" "http://10.0.2.2:8081/api/v1"
set_property "sziget.localBearerToken" "$token"
set_property "sziget.skipGoogleSignIn" "true"

echo "==> Rebuilding and reinstalling the debug APK..."
cd "$repo_root"
./gradlew :androidApp:installDebug -q

echo
echo "Done. Backend up at http://localhost:8081/api/v1, debug APK reinstalled with a fresh token."
echo "Run Maestro as usual: maestro test maestro"
