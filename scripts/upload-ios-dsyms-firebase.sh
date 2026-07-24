#!/usr/bin/env bash
#
# Manually uploads iOS dSYMs to Firebase Crashlytics. Must run on macOS with Xcode installed.
#
# Xcode already does this automatically on every build via the "Crashlytics: Run" build phase
# (iosApp.xcodeproj), so this script is for the cases that phase can't cover: re-uploading after
# a build-time upload failed (e.g. no network at build time), or uploading dSYMs downloaded later
# from App Store Connect / Xcode Organizer (Xcode -> Window -> Organizer -> Archives -> Download
# dSYMs), which can differ from what was on disk at build time.
#
# Usage:
#   scripts/upload-ios-dsyms-firebase.sh [options] <path>
#
# <path> is one of:
#   - a .xcarchive produced by `xcodebuild archive` (e.g. by scripts/release-ios-beta.sh — dSYMs
#     are read from its dSYMs/ subfolder)
#   - a .zip of dSYMs, e.g. as downloaded from App Store Connect / Xcode Organizer
#   - a directory containing one or more .dSYM bundles
#   - a single .dSYM bundle
#
# Options:
#   --config Debug|Release        Which GoogleService-Info-*.plist to use (default: Release).
#   --google-service-plist PATH   Explicit GoogleService-Info.plist path, overrides --config.
#   --upload-symbols-path PATH    Path to Firebase's upload-symbols binary (or env
#                                 UPLOAD_SYMBOLS_PATH). Auto-discovered under
#                                 ~/Library/Developer/Xcode/DerivedData by default — requires
#                                 having built/archived this project at least once so Swift
#                                 Package Manager has checked out firebase-ios-sdk.
#   --verbose                     Pass -v through to upload-symbols.
#   -h, --help                    Show this help.

set -euo pipefail

if [[ "$(uname)" != "Darwin" ]]; then
    echo "error: this script uploads dSYMs via a macOS-only Firebase tool and only runs on macOS." >&2
    exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
ios_app_dir="$repo_root/iosApp"

config="Release"
google_service_plist=""
upload_symbols_path="${UPLOAD_SYMBOLS_PATH:-}"
verbose=false
input_path=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --config)
            config="${2:?--config requires a value}"
            shift 2
            ;;
        --google-service-plist)
            google_service_plist="${2:?--google-service-plist requires a value}"
            shift 2
            ;;
        --upload-symbols-path)
            upload_symbols_path="${2:?--upload-symbols-path requires a value}"
            shift 2
            ;;
        --verbose)
            verbose=true
            shift
            ;;
        -h|--help)
            sed -n '2,30p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        -*)
            echo "error: unknown option '$1' (see --help)" >&2
            exit 1
            ;;
        *)
            if [[ -n "$input_path" ]]; then
                echo "error: unexpected extra argument '$1' (see --help)" >&2
                exit 1
            fi
            input_path="$1"
            shift
            ;;
    esac
done

if [[ -z "$input_path" ]]; then
    echo "error: missing <path> argument (see --help)" >&2
    exit 1
fi
if [[ ! -e "$input_path" ]]; then
    echo "error: '$input_path' does not exist" >&2
    exit 1
fi

if [[ -z "$google_service_plist" ]]; then
    case "$config" in
        Release)
            google_service_plist="$ios_app_dir/GoogleService-Info-Release.plist"
            ;;
        Debug)
            google_service_plist="$ios_app_dir/GoogleService-Info-Debug.plist"
            ;;
        *)
            echo "error: --config must be 'Debug' or 'Release', got '$config'" >&2
            exit 1
            ;;
    esac
fi
if [[ ! -f "$google_service_plist" ]]; then
    echo "error: GoogleService-Info.plist not found at $google_service_plist (see docs/RUNBOOK.md's" \
        "'Google Sign-In setup' section for where to get it)" >&2
    exit 1
fi

if [[ -z "$upload_symbols_path" ]]; then
    # Newest match wins if multiple DerivedData checkouts exist (e.g. across Xcode versions).
    # `find -exec ... {} +` only invokes the command when there's at least one match (unlike
    # piping through xargs, which by default still runs its command once with zero arguments on
    # empty input — that would list the current directory instead of doing nothing here).
    upload_symbols_path="$(find "$HOME/Library/Developer/Xcode/DerivedData" \
        -path "*/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/upload-symbols" \
        -exec ls -td {} + 2>/dev/null | head -n1 || true)"
fi
if [[ -z "$upload_symbols_path" || ! -x "$upload_symbols_path" ]]; then
    echo "error: could not find Firebase's upload-symbols tool." >&2
    echo "  Build or archive this project at least once (e.g. open it in Xcode, or run" >&2
    echo "  scripts/release-ios-beta.sh) so Swift Package Manager checks out firebase-ios-sdk," >&2
    echo "  or pass its path explicitly with --upload-symbols-path." >&2
    exit 1
fi

cleanup_dir=""
cleanup() {
    if [[ -n "$cleanup_dir" ]]; then
        rm -rf "$cleanup_dir"
    fi
}
trap cleanup EXIT

dsym_source="$input_path"
case "$input_path" in
    *.xcarchive)
        dsym_source="$input_path/dSYMs"
        if [[ ! -d "$dsym_source" ]]; then
            echo "error: no dSYMs/ folder found inside archive $input_path" >&2
            exit 1
        fi
        ;;
    *.zip)
        cleanup_dir="$(mktemp -d /tmp/ios-dsym-upload.XXXXXX)"
        echo "==> Unzipping $input_path..."
        unzip -q "$input_path" -d "$cleanup_dir"
        dsym_source="$cleanup_dir"
        ;;
esac

echo "==> Uploading dSYMs from $dsym_source to Firebase Crashlytics ($config: $(basename "$google_service_plist"))..."
upload_symbols_args=(-gsp "$google_service_plist" -p ios "$dsym_source")
if [[ "$verbose" == true ]]; then
    upload_symbols_args=(-v "${upload_symbols_args[@]}")
fi
"$upload_symbols_path" "${upload_symbols_args[@]}"

echo "Done."
