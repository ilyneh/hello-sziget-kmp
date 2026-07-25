#!/usr/bin/env python3
"""Adds an already-uploaded TestFlight build to one or more beta groups.

Waits for App Store Connect to finish processing the build, then adds it to each named
group. Talks to the App Store Connect API directly (no Fastlane, no third-party pip
packages) - JWT signing shells out to `openssl` (already required by
scripts/release-ios-beta.sh, and present by default on macOS) rather than depending on a
Python crypto library.

Usage:
    assign_testflight_group.py --bundle-id ID --build-number N --group NAME
        --asc-key-id ID --asc-issuer-id ID --asc-key-path PATH
        [--group NAME ...] [--poll-timeout SECONDS] [--poll-interval SECONDS]

Requires an App Store Connect API key (App Store Connect -> Users and Access ->
Integrations -> App Store Connect API; App Manager role or higher).
"""

from __future__ import annotations

import argparse
import base64
import json
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

API_BASE = "https://api.appstoreconnect.apple.com/v1"
JWT_LIFETIME_SECONDS = 19 * 60  # Apple caps JWTs at 20 minutes; stay comfortably under that.


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def generate_jwt(key_id: str, issuer_id: str, key_path: str) -> str:
    """Signs a fresh ES256 JWT for one API call.

    Regenerated per-request (rather than reused across a long processing-status poll) so a
    single 19-minute token lifetime is never a concern. The DER-to-JOSE conversion below is
    straightforward because a P-256 ECDSA-Sig-Value is just a two-INTEGER DER SEQUENCE.
    """
    header = {"alg": "ES256", "kid": key_id, "typ": "JWT"}
    now = int(time.time())
    payload = {"iss": issuer_id, "iat": now, "exp": now + JWT_LIFETIME_SECONDS, "aud": "appstoreconnect-v1"}

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

    r, offset = _parse_der_uint(der_sig, 2)  # skip SEQUENCE tag + short-form length byte
    s, _ = _parse_der_uint(der_sig, offset)
    raw_sig = _to_fixed(r) + _to_fixed(s)
    return signing_input + "." + b64url(raw_sig)


def _parse_der_uint(buf: bytes, offset: int) -> tuple[bytes, int]:
    if buf[offset] != 0x02:
        raise ValueError("expected INTEGER tag in ECDSA-Sig-Value")
    length = buf[offset + 1]
    start = offset + 2
    return buf[start : start + length], start + length


def _to_fixed(value: bytes, size: int = 32) -> bytes:
    value = value.lstrip(b"\x00")
    if len(value) > size:
        raise ValueError("integer too large for P-256 signature component")
    return value.rjust(size, b"\x00")


class AscApiError(RuntimeError):
    pass


def asc_api(method: str, url: str, credentials: "AscCredentials", data: dict | None = None) -> dict:
    """Issues one App Store Connect API request and returns the parsed JSON body."""
    jwt = generate_jwt(credentials.key_id, credentials.issuer_id, credentials.key_path)
    headers = {"Authorization": f"Bearer {jwt}"}
    body = None
    if data is not None:
        body = json.dumps(data).encode()
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            raw = response.read()
    except urllib.error.HTTPError as error:
        raw = error.read()
        _raise_for_error_body(method, url, error.code, raw)

    return json.loads(raw) if raw else {}


def _raise_for_error_body(method: str, url: str, http_code: int, raw: bytes) -> None:
    message = f"App Store Connect API request failed (HTTP {http_code}): {method} {url}"
    try:
        errors = json.loads(raw).get("errors", [])
        details = "\n".join(f"  {e.get('title', '')}: {e.get('detail', '')}" for e in errors)
        if details:
            message += "\n" + details
    except (json.JSONDecodeError, AttributeError):
        if raw:
            message += "\n" + raw.decode(errors="replace")
    raise AscApiError(message)


class AscCredentials:
    def __init__(self, key_id: str, issuer_id: str, key_path: str):
        self.key_id = key_id
        self.issuer_id = issuer_id
        self.key_path = key_path


def get_app_id(bundle_id: str, credentials: AscCredentials) -> str:
    url = f"{API_BASE}/apps?filter[bundleId]={urllib.parse.quote(bundle_id)}&limit=1"
    resp = asc_api("GET", url, credentials)
    data = resp.get("data") or []
    if not data:
        raise AscApiError(f"no App Store Connect app found with bundle id {bundle_id}")
    return data[0]["id"]


def wait_for_build_id(
    app_id: str,
    build_number: str,
    credentials: AscCredentials,
    timeout: int,
    interval: int,
) -> str:
    url = f"{API_BASE}/builds?filter[app]={app_id}&filter[version]={build_number}&limit=1"
    deadline = time.monotonic() + timeout
    print(
        f"Waiting for build {build_number} to finish processing in App Store Connect "
        "(this can take a while)...",
        file=sys.stderr,
    )
    while True:
        resp = asc_api("GET", url, credentials)
        data = resp.get("data") or []
        if data:
            state = data[0]["attributes"]["processingState"]
            if state == "VALID":
                return data[0]["id"]
            if state in ("FAILED", "INVALID"):
                raise AscApiError(f"build {build_number} processing finished with state {state}")
        if time.monotonic() >= deadline:
            raise AscApiError(f"timed out after {timeout}s waiting for build {build_number} to finish processing")
        time.sleep(interval)


def get_group(app_id: str, name: str, credentials: AscCredentials) -> tuple[str, bool]:
    """Returns (group_id, is_internal_group).

    App Store Connect distinguishes Internal Testing groups (App Store Connect team members,
    who get access to every processed build automatically) from External Testing groups (an
    explicit tester list, subject to Apple review, that builds must be explicitly added to via
    the betaGroups relationship). Callers need to know which kind they got back, since builds
    can only be explicitly assigned to the latter.
    """
    url = f"{API_BASE}/betaGroups?filter[app]={app_id}&filter[name]={urllib.parse.quote(name)}&limit=1"
    resp = asc_api("GET", url, credentials)
    data = resp.get("data") or []
    if not data:
        raise AscApiError(f"no TestFlight beta group named '{name}' found for this app")
    group = data[0]
    return group["id"], bool(group["attributes"]["isInternalGroup"])


def add_build_to_group(build_id: str, group_id: str, credentials: AscCredentials) -> None:
    url = f"{API_BASE}/builds/{build_id}/relationships/betaGroups"
    payload = {"data": [{"type": "betaGroups", "id": group_id}]}
    asc_api("POST", url, credentials, data=payload)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--bundle-id", required=True, help="App's bundle identifier, e.g. com.ilyne.hellosziget")
    parser.add_argument("--build-number", required=True, help="CURRENT_PROJECT_VERSION of the uploaded build")
    parser.add_argument(
        "--group",
        action="append",
        dest="groups",
        required=True,
        metavar="NAME",
        help="TestFlight beta group name to add the build to (repeatable)",
    )
    parser.add_argument("--asc-key-id", required=True, help="App Store Connect API key ID")
    parser.add_argument("--asc-issuer-id", required=True, help="App Store Connect API issuer ID")
    parser.add_argument("--asc-key-path", required=True, help="Path to the API private key .p8 file")
    parser.add_argument("--poll-timeout", type=int, default=1800, help="Max seconds to wait for processing (default 1800)")
    parser.add_argument("--poll-interval", type=int, default=30, help="Seconds between processing-status checks (default 30)")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    credentials = AscCredentials(args.asc_key_id, args.asc_issuer_id, args.asc_key_path)

    try:
        app_id = get_app_id(args.bundle_id, credentials)
        build_id = wait_for_build_id(app_id, args.build_number, credentials, args.poll_timeout, args.poll_interval)
        for group_name in args.groups:
            group_id, is_internal = get_group(app_id, group_name, credentials)
            if is_internal:
                print(
                    f"'{group_name}' is an Internal Testing group - its members already have "
                    "access to every processed build automatically, so no explicit assignment "
                    "is needed (and App Store Connect's API rejects trying). Skipping.",
                )
                continue
            add_build_to_group(build_id, group_id, credentials)
            print(f"Added build {args.build_number} to group '{group_name}'")
    except (AscApiError, subprocess.CalledProcessError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
