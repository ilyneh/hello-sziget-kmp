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

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
compose_file="$repo_root/maestro/docker-compose.e2e.yml"

if [[ "${1:-}" == "down" ]]; then
    docker compose -f "$compose_file" down -v
    exit 0
fi

if ! docker image inspect hello-sziget-backend:e2e >/dev/null 2>&1; then
    echo "error: image 'hello-sziget-backend:e2e' not found locally." >&2
    echo "Build it once from a hello-sziget checkout:" >&2
    echo "  docker build -f Dockerfile.e2e -t hello-sziget-backend:e2e ." >&2
    exit 1
fi

docker compose -f "$compose_file" up -d --wait

echo
echo "Backend is up at http://localhost:8081/api/v1"
echo
echo "Set these in local.properties:"
echo "  sziget.localBackendUrl=http://10.0.2.2:8081/api/v1"
token="$(docker compose -f "$compose_file" logs backend | grep '^backend.*E2E_BEARER_TOKEN=' | tail -n 1 | sed 's/.*E2E_BEARER_TOKEN=//')"
if [[ -z "$token" ]]; then
    echo "error: couldn't find E2E_BEARER_TOKEN in backend logs yet — it may still be seeding." >&2
    echo "Check with: docker compose -f \"$compose_file\" logs backend" >&2
    exit 1
fi
echo "  sziget.localBearerToken=$token"
echo "  sziget.skipGoogleSignIn=true"
