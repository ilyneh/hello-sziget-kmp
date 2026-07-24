#!/usr/bin/env bash
#
# Builds and uploads an iOS beta (TestFlight) release, wrapping the manual steps documented in
# docs/RUNBOOK.md under "TestFlight / App Store release". Must run on macOS with Xcode installed.
#
# Usage:
#   scripts/release-ios-beta.sh [options]
#
# Options:
#   --build-number N   Set CURRENT_PROJECT_VERSION to N instead of auto-incrementing it.
#   --no-bump          Skip bumping CURRENT_PROJECT_VERSION entirely (use the current value as-is).
#   --yes, -y          Skip the confirmation prompt before archiving/uploading.
#   -h, --help         Show this help.
#
# By default, CURRENT_PROJECT_VERSION in iosApp/Configuration/Release.xcconfig is incremented by
# 1 before building — App Store Connect rejects a duplicate build number, so re-running the
# script without any flags "just works" for a fresh beta upload. The bump is left as an
# uncommitted change in Release.xcconfig; commit it once the upload succeeds so the checked-in
# build number stays in sync with what's live on TestFlight.
#
# xcodebuild -exportArchive uploads straight to App Store Connect as its last step (see
# ExportOptions.plist's destination=upload), so there is no separate "upload" step here.

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
        --yes|-y)
            skip_confirm=true
            shift
            ;;
        -h|--help)
            sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
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
echo "  Bundle ID:          com.ilyne.hellosziget"
echo "  Team (export):       J6TQZMUWUM"
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
