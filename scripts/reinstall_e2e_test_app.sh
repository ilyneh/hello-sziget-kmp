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
docker compose -f "$compose_file" up -d --wait

token="$(docker compose -f "$compose_file" logs backend | grep '^backend.*E2E_BEARER_TOKEN=' | tail -n 1 | sed 's/.*E2E_BEARER_TOKEN=//')"
if [[ -z "$token" ]]; then
    echo "error: couldn't find E2E_BEARER_TOKEN in backend logs — it may still be seeding." >&2
    echo "Check with: docker compose -f \"$compose_file\" logs backend" >&2
    exit 1
fi

echo "==> Writing config into local.properties..."
touch "$local_properties"
set_property() {
    local key="$1" value="$2"
    if grep -q "^${key}=" "$local_properties" 2>/dev/null; then
        sed -i '' "s#^${key}=.*#${key}=${value}#" "$local_properties"
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
