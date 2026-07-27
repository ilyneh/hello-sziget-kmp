#!/usr/bin/env bash
#
# Brings up the self-seeded backend for running Maestro against a mocked/test backend (see
# maestro/README.md). Requires the hello-sziget-backend:e2e image to already exist locally
# (built once from a hello-sziget checkout: docker build -f Dockerfile.e2e -t
# hello-sziget-backend:e2e .) — this script never references that checkout directly.
#
# Usage:
#   scripts/run_e2e_backend.sh          Start (or reuse) the backend, print the bearer token.
#   scripts/run_e2e_backend.sh down     Tear everything down (fresh DB next time).
#
# The bearer token is always written to $E2E_BEARER_TOKEN_FILE (default:
# /tmp/hello-sziget-e2e-bearer-token) — read it from there instead of scraping this script's
# stdout. Outside CI it's also echoed to stdout for convenience (to hand-paste into
# local.properties); under CI (the $CI env var GitHub Actions sets automatically) it's not,
# since CI job logs are a worse place for even a short-lived, dev-only token to sit than a file
# on the ephemeral runner's own disk.

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
compose_file="$repo_root/maestro/docker-compose.e2e.yml"
token_file="${E2E_BEARER_TOKEN_FILE:-/tmp/hello-sziget-e2e-bearer-token}"

if [[ "${1:-}" == "down" ]]; then
    docker compose -f "$compose_file" down -v
    rm -f "$token_file"
    exit 0
fi

if ! docker image inspect hello-sziget-backend:e2e >/dev/null 2>&1; then
    echo "error: image 'hello-sziget-backend:e2e' not found locally." >&2
    echo "Build it once from a hello-sziget checkout:" >&2
    echo "  docker build -f Dockerfile.e2e -t hello-sziget-backend:e2e ." >&2
    exit 1
fi

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

echo
echo "Backend is up at http://localhost:8081/api/v1"
echo
echo "Set these in local.properties:"
echo "  sziget.localBackendUrl=http://10.0.2.2:8081/api/v1"

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

(umask 077 && echo "$token" > "$token_file")
if [[ -n "${CI:-}" ]]; then
    echo "  sziget.localBearerToken=(written to $token_file, not printed under CI)"
else
    echo "  sziget.localBearerToken=$token"
fi
echo "  sziget.skipGoogleSignIn=true"
