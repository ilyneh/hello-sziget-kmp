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
#                          Connect API credentials (see below) and 'python3' + 'openssl' in PATH.
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
# --test-group hands off to scripts/assign_testflight_group.py, which talks to the App Store
# Connect API directly (no Fastlane dependency): it signs a short-lived ES256 JWT with the API
# key, waits for Apple to finish processing the uploaded build (this can take anywhere from a
# few minutes to a while — hence the poll/timeout), then adds the build to each named beta
# group. Create an API key under App Store Connect -> Users and Access -> Integrations ->
# App Store Connect API (App Manager role or higher).

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
    for required_cmd in python3 openssl; do
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

# --- TestFlight group assignment ---------------------------------------------------------------

if [[ ${#test_groups[@]} -gt 0 ]]; then
    echo
    echo "==> Assigning build $new_build_number to TestFlight group(s): ${test_groups[*]}"
    group_args=()
    for group_name in "${test_groups[@]}"; do
        group_args+=(--group "$group_name")
    done
    python3 "$script_dir/assign_testflight_group.py" \
        --bundle-id "$bundle_id" \
        --build-number "$new_build_number" \
        --asc-key-id "$asc_key_id" \
        --asc-issuer-id "$asc_issuer_id" \
        --asc-key-path "$asc_key_path" \
        --poll-timeout "$group_poll_timeout" \
        --poll-interval "$group_poll_interval" \
        "${group_args[@]}"
fi
