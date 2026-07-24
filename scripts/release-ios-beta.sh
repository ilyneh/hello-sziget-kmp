#!/usr/bin/env bash
#
# Builds and uploads an iOS beta (TestFlight) release, wrapping the manual steps documented in
# docs/RUNBOOK.md under "TestFlight / App Store release". Must run on macOS with Xcode installed.
#
# Usage:
#   scripts/release-ios-beta.sh [options]
#
# Options:
#   --build-number N      Set CURRENT_PROJECT_VERSION to N instead of auto-incrementing it.
#   --no-bump             Skip bumping CURRENT_PROJECT_VERSION (use the current value as-is).
#   --test-group NAME      Add the uploaded build to this TestFlight beta group once processing
#                          finishes. Repeatable to add to multiple groups. Requires App Store
#                          Connect API credentials (see below) and 'jq' + 'python3' in PATH.
#   --asc-key-id ID        App Store Connect API key ID (or env ASC_API_KEY_ID).
#   --asc-issuer-id ID     App Store Connect API issuer ID (or env ASC_API_ISSUER_ID).
#   --asc-key-path PATH    Path to the API private key .p8 file (or env ASC_API_KEY_PATH).
#   --group-poll-timeout N Max seconds to wait for the build to finish processing before adding
#                          it to a test group (default 1800).
#   --group-poll-interval N Seconds between processing-status checks (default 30).
#   --yes, -y              Skip the confirmation prompt before archiving/uploading.
#   -h, --help              Show this help.
#
# By default, CURRENT_PROJECT_VERSION in iosApp/Configuration/Release.xcconfig is incremented by
# 1 before building — App Store Connect rejects a duplicate build number, so re-running the
# script without any flags "just works" for a fresh beta upload. The bump is left as an
# uncommitted change in Release.xcconfig; commit it once the upload succeeds so the checked-in
# build number stays in sync with what's live on TestFlight.
#
# xcodebuild -exportArchive uploads straight to App Store Connect as its last step (see
# ExportOptions.plist's destination=upload), so there is no separate "upload" step here.
#
# --test-group talks to the App Store Connect API directly (no Fastlane dependency): it signs a
# short-lived ES256 JWT with the API key, waits for Apple to finish processing the uploaded
# build (this can take anywhere from a few minutes to a while — hence the poll/timeout), then
# adds the build to each named beta group. Create an API key under App Store Connect ->
# Users and Access -> Integrations -> App Store Connect API (App Manager role or higher).

set -euo pipefail

if [[ "$(uname)" != "Darwin" ]]; then
    echo "error: this script builds an iOS app with xcodebuild and only runs on macOS." >&2
    exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
ios_app_dir="$repo_root/iosApp"
xcconfig="$ios_app_dir/Configuration/Release.xcconfig"
export_options="$ios_app_dir/Configuration/ExportOptions.plist"
xcodeproj="$ios_app_dir/iosApp.xcodeproj"

build_number_override=""
skip_bump=false
skip_confirm=false
test_groups=()
asc_key_id="${ASC_API_KEY_ID:-}"
asc_issuer_id="${ASC_API_ISSUER_ID:-}"
asc_key_path="${ASC_API_KEY_PATH:-}"
group_poll_timeout=1800
group_poll_interval=30

while [[ $# -gt 0 ]]; do
    case "$1" in
        --build-number)
            build_number_override="${2:?--build-number requires a value}"
            shift 2
            ;;
        --no-bump)
            skip_bump=true
            shift
            ;;
        --test-group)
            test_groups+=("${2:?--test-group requires a value}")
            shift 2
            ;;
        --asc-key-id)
            asc_key_id="${2:?--asc-key-id requires a value}"
            shift 2
            ;;
        --asc-issuer-id)
            asc_issuer_id="${2:?--asc-issuer-id requires a value}"
            shift 2
            ;;
        --asc-key-path)
            asc_key_path="${2:?--asc-key-path requires a value}"
            shift 2
            ;;
        --group-poll-timeout)
            group_poll_timeout="${2:?--group-poll-timeout requires a value}"
            shift 2
            ;;
        --group-poll-interval)
            group_poll_interval="${2:?--group-poll-interval requires a value}"
            shift 2
            ;;
        --yes|-y)
            skip_confirm=true
            shift
            ;;
        -h|--help)
            sed -n '2,37p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "error: unknown option '$1' (see --help)" >&2
            exit 1
            ;;
    esac
done

if [[ ! -f "$xcconfig" ]]; then
    echo "error: $xcconfig not found" >&2
    exit 1
fi

current_build_number="$(sed -n 's/^CURRENT_PROJECT_VERSION=\(.*\)$/\1/p' "$xcconfig")"
if [[ -z "$current_build_number" ]]; then
    echo "error: could not read CURRENT_PROJECT_VERSION from $xcconfig" >&2
    exit 1
fi

marketing_version="$(sed -n 's/^MARKETING_VERSION=\(.*\)$/\1/p' "$xcconfig")"
bundle_id="$(sed -n 's/^PRODUCT_BUNDLE_IDENTIFIER=\(.*\)$/\1/p' "$xcconfig")"

if [[ ${#test_groups[@]} -gt 0 ]]; then
    if [[ -z "$asc_key_id" || -z "$asc_issuer_id" || -z "$asc_key_path" ]]; then
        echo "error: --test-group requires App Store Connect API credentials: --asc-key-id," \
            "--asc-issuer-id and --asc-key-path (or the ASC_API_KEY_ID / ASC_API_ISSUER_ID /" \
            "ASC_API_KEY_PATH env vars). See docs/RUNBOOK.md." >&2
        exit 1
    fi
    if [[ ! -r "$asc_key_path" ]]; then
        echo "error: App Store Connect API key not readable at $asc_key_path" >&2
        exit 1
    fi
    for required_cmd in jq python3 openssl; do
        if ! command -v "$required_cmd" >/dev/null 2>&1; then
            echo "error: '$required_cmd' is required for --test-group but was not found in PATH." >&2
            exit 1
        fi
    done
fi

if [[ -n "$build_number_override" ]]; then
    new_build_number="$build_number_override"
elif [[ "$skip_bump" == true ]]; then
    new_build_number="$current_build_number"
else
    new_build_number=$((current_build_number + 1))
fi

if [[ "$new_build_number" != "$current_build_number" ]]; then
    sed -i '' "s/^CURRENT_PROJECT_VERSION=.*/CURRENT_PROJECT_VERSION=$new_build_number/" "$xcconfig"
    echo "Bumped CURRENT_PROJECT_VERSION: $current_build_number -> $new_build_number ($xcconfig)"
fi

work_dir="$(mktemp -d /tmp/ios-beta-release.XXXXXX)"
archive_path="$work_dir/iosApp.xcarchive"
export_path="$work_dir/export"
trap 'echo "Build artifacts left in $work_dir"' EXIT

echo
echo "About to archive and upload to App Store Connect / TestFlight:"
echo "  Marketing version: $marketing_version"
echo "  Build number:      $new_build_number"
echo "  Bundle ID:          $bundle_id"
echo "  Team (export):       J6TQZMUWUM"
if [[ ${#test_groups[@]} -gt 0 ]]; then
    echo "  TestFlight group(s): ${test_groups[*]}"
fi
echo

if [[ "$skip_confirm" != true ]]; then
    read -r -p "Continue? [y/N] " reply
    if [[ ! "$reply" =~ ^[Yy]$ ]]; then
        echo "Aborted. Reverting CURRENT_PROJECT_VERSION bump."
        if [[ "$new_build_number" != "$current_build_number" ]]; then
            sed -i '' "s/^CURRENT_PROJECT_VERSION=.*/CURRENT_PROJECT_VERSION=$current_build_number/" "$xcconfig"
        fi
        exit 1
    fi
fi

echo "==> Archiving (xcodebuild archive)..."
xcodebuild archive \
    -project "$xcodeproj" \
    -scheme iosApp \
    -configuration Release \
    -archivePath "$archive_path" \
    -allowProvisioningUpdates

echo "==> Exporting and uploading to App Store Connect (xcodebuild -exportArchive)..."
xcodebuild -exportArchive \
    -archivePath "$archive_path" \
    -exportPath "$export_path" \
    -exportOptionsPlist "$export_options" \
    -allowProvisioningUpdates

echo
echo "Done. Build $new_build_number ($marketing_version) uploaded to App Store Connect."
if [[ "$new_build_number" != "$current_build_number" ]]; then
    echo "Don't forget to commit the CURRENT_PROJECT_VERSION bump in Release.xcconfig."
fi

# --- TestFlight group assignment via the App Store Connect API --------------------------------

# Signs a fresh (19-minute) ES256 JWT for each API call — cheap, and avoids the 20-minute-max
# token lifetime becoming a problem across a long processing-status poll. Shells out to openssl
# for the actual EC signing (no Python crypto library required) and does the DER-to-JOSE
# raw-signature conversion inline, since a P-256 ECDSA-Sig-Value is a simple two-INTEGER DER
# SEQUENCE.
generate_jwt() {
    python3 - "$asc_key_id" "$asc_issuer_id" "$asc_key_path" <<'PY'
import base64
import json
import subprocess
import sys
import time

key_id, issuer_id, key_path = sys.argv[1:4]


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


header = {"alg": "ES256", "kid": key_id, "typ": "JWT"}
now = int(time.time())
payload = {"iss": issuer_id, "iat": now, "exp": now + 19 * 60, "aud": "appstoreconnect-v1"}

signing_input = (
    b64url(json.dumps(header, separators=(",", ":")).encode())
    + "."
    + b64url(json.dumps(payload, separators=(",", ":")).encode())
)

der_sig = subprocess.run(
    ["openssl", "dgst", "-sha256", "-sign", key_path],
    input=signing_input.encode(),
    stdout=subprocess.PIPE,
    check=True,
).stdout


def parse_der_uint(buf, offset):
    if buf[offset] != 0x02:
        raise ValueError("expected INTEGER tag in ECDSA-Sig-Value")
    length = buf[offset + 1]
    start = offset + 2
    return buf[start : start + length], start + length


def to_fixed(b, size=32):
    b = b.lstrip(b"\x00")
    if len(b) > size:
        raise ValueError("integer too large for P-256 signature component")
    return b.rjust(size, b"\x00")


if der_sig[0] != 0x30:
    raise ValueError("expected SEQUENCE tag in ECDSA-Sig-Value")
offset = 2  # skip SEQUENCE tag + short-form length byte (always short-form for a P-256 sig)
r, offset = parse_der_uint(der_sig, offset)
s, offset = parse_der_uint(der_sig, offset)

raw_sig = to_fixed(r) + to_fixed(s)
print(signing_input + "." + b64url(raw_sig))
PY
}

# Issues one App Store Connect API request, printing the response body on success. Bundles HTTP
# status checking (curl doesn't fail on 4xx/5xx by default) and prints Apple's error payload
# before exiting on failure.
asc_api() {
    local method="$1" url="$2" data="${3:-}" jwt response http_code body
    jwt="$(generate_jwt)"
    if [[ -n "$data" ]]; then
        response="$(curl -sS -X "$method" "$url" \
            -H "Authorization: Bearer $jwt" \
            -H "Content-Type: application/json" \
            -d "$data" \
            -w $'\n%{http_code}')"
    else
        response="$(curl -sS -X "$method" "$url" \
            -H "Authorization: Bearer $jwt" \
            -w $'\n%{http_code}')"
    fi
    http_code="${response##*$'\n'}"
    body="${response%$'\n'*}"
    if [[ "$http_code" -ge 400 ]]; then
        echo "error: App Store Connect API request failed (HTTP $http_code): $method $url" >&2
        if [[ -n "$body" ]]; then
            echo "$body" | jq -r '.errors[]? | "  \(.title): \(.detail // "")"' >&2 2>/dev/null || echo "$body" >&2
        fi
        exit 1
    fi
    echo "$body"
}

get_app_id() {
    local resp id
    resp="$(asc_api GET "https://api.appstoreconnect.apple.com/v1/apps?filter[bundleId]=$bundle_id&limit=1")"
    id="$(jq -r '.data[0].id // empty' <<<"$resp")"
    if [[ -z "$id" ]]; then
        echo "error: no App Store Connect app found with bundle id $bundle_id" >&2
        exit 1
    fi
    echo "$id"
}

wait_for_build_id() {
    local app_id="$1" build_number="$2" deadline resp build_id state
    deadline=$((SECONDS + group_poll_timeout))
    echo "Waiting for build $build_number to finish processing in App Store Connect (this can take a while)..." >&2
    while true; do
        resp="$(asc_api GET "https://api.appstoreconnect.apple.com/v1/builds?filter[app]=$app_id&filter[version]=$build_number&limit=1")"
        build_id="$(jq -r '.data[0].id // empty' <<<"$resp")"
        if [[ -n "$build_id" ]]; then
            state="$(jq -r '.data[0].attributes.processingState' <<<"$resp")"
            case "$state" in
                VALID)
                    echo "$build_id"
                    return 0
                    ;;
                FAILED|INVALID)
                    echo "error: build $build_number processing finished with state $state" >&2
                    exit 1
                    ;;
            esac
        fi
        if (( SECONDS >= deadline )); then
            echo "error: timed out after ${group_poll_timeout}s waiting for build $build_number to finish processing" >&2
            exit 1
        fi
        sleep "$group_poll_interval"
    done
}

get_group_id() {
    local app_id="$1" name="$2" encoded_name resp id
    encoded_name="$(jq -rn --arg v "$name" '$v|@uri')"
    resp="$(asc_api GET "https://api.appstoreconnect.apple.com/v1/betaGroups?filter[app]=$app_id&filter[name]=$encoded_name&limit=1")"
    id="$(jq -r '.data[0].id // empty' <<<"$resp")"
    if [[ -z "$id" ]]; then
        echo "error: no TestFlight beta group named '$name' found for this app" >&2
        exit 1
    fi
    echo "$id"
}

add_build_to_group() {
    local build_id="$1" group_id="$2" payload
    payload="$(jq -n --arg id "$group_id" '{data: [{type: "betaGroups", id: $id}]}')"
    asc_api POST "https://api.appstoreconnect.apple.com/v1/builds/$build_id/relationships/betaGroups" "$payload" >/dev/null
}

if [[ ${#test_groups[@]} -gt 0 ]]; then
    echo
    echo "==> Assigning build $new_build_number to TestFlight group(s): ${test_groups[*]}"
    app_id="$(get_app_id)"
    build_id="$(wait_for_build_id "$app_id" "$new_build_number")"
    for group_name in "${test_groups[@]}"; do
        group_id="$(get_group_id "$app_id" "$group_name")"
        add_build_to_group "$build_id" "$group_id"
        echo "Added build $new_build_number to group '$group_name'"
    done
fi
