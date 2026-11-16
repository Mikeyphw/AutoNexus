#!/usr/bin/env python3
"""Run the exact CI Gradle command with bounded retries ONLY for Maven HTTP 429.

This does not alter Gradle task selection, bypass tests, or interpret a skipped
build as successful. Complete native Gradle output is forwarded on each attempt.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
from collections.abc import Callable, Sequence

MAX_ATTEMPTS = 3
RETRY_DELAYS_SECONDS = (45, 120)
MAX_CAPTURE = 1024 * 1024
MAVEN_429 = re.compile(r"Received status code\s+429\s+from server:\s+Too Many Requests", re.I)
REMOTE_REQUEST = re.compile(r"Could not (?:GET|get resource).*https?://", re.I)
RESOLUTION_FAILURE = re.compile(r"Could not resolve (?:all (?:artifacts|files)|org\.|com\.|net\.|io\.)", re.I)
SOURCE_FAILURE = re.compile(
    r"(?:Execution failed for task|\bCompilation error\b|\berror: (?!Could not GET)|There were failing tests)", re.I
)


def is_retryable_rate_limit(output: str) -> bool:
    """Require a Gradle dependency-resolution failure plus observed HTTP 429.

    Other failures (Kotlin compilation, unit tests, lint, missing artifacts,
    status 403/404, or generic network errors) are not retried.
    """
    return bool(
        MAVEN_429.search(output)
        and REMOTE_REQUEST.search(output)
        and RESOLUTION_FAILURE.search(output)
        and not SOURCE_FAILURE.search(output)
    )


def stream_native_gradle(argv: Sequence[str]) -> tuple[int, str]:
    """Forward actual stdout and stderr in order, keeping only a bounded tail."""
    try:
        process = subprocess.Popen(
            list(argv), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, errors="replace", bufsize=1,
        )
    except OSError as exc:
        print(f"AUTONEXUS_CI_GRADLE_LAUNCH_FAILED {type(exc).__name__}: {exc}", file=sys.stderr)
        return 127, ""
    assert process.stdout is not None
    tail = ""
    with process.stdout:
        for line in process.stdout:
            sys.stdout.write(line)
            sys.stdout.flush()
            tail = (tail + line)[-MAX_CAPTURE:]
    return process.wait(), tail


def run_with_bounded_retry(
    argv: Sequence[str],
    *,
    run_one: Callable[[Sequence[str]], tuple[int, str]] = stream_native_gradle,
    sleep: Callable[[float], None] = time.sleep,
    delays: Sequence[int] = RETRY_DELAYS_SECONDS,
) -> int:
    if len(delays) != MAX_ATTEMPTS - 1 or any(s < 0 or s > 180 for s in delays):
        raise ValueError("Invalid bounded Gradle retry delay schedule")
    if not argv or argv[0] != "gradle":
        raise ValueError("Expected explicit Gradle invocation")
    for i in range(MAX_ATTEMPTS):
        print(f"AUTONEXUS_CI_GRADLE_ATTEMPT {i + 1}/{MAX_ATTEMPTS}", flush=True)
        exit_code, output = run_one(argv)
        print(f"AUTONEXUS_CI_GRADLE_EXIT attempt={i + 1} code={exit_code}", flush=True)
        if exit_code == 0:
            return 0
        if not is_retryable_rate_limit(output):
            print("AUTONEXUS_CI_GRADLE_NOT_RETRYABLE; native failure retained", file=sys.stderr, flush=True)
            return exit_code if exit_code > 0 else 1
        if i == MAX_ATTEMPTS - 1:
            print("AUTONEXUS_CI_GRADLE_HTTP_429_RETRIES_EXHAUSTED; failing CI", file=sys.stderr, flush=True)
            return exit_code if exit_code > 0 else 1
        delay = delays[i]
        print(f"AUTONEXUS_CI_GRADLE_HTTP_429_RETRY delay_seconds={delay} next_attempt={i + 2}", flush=True)
        sleep(delay)
    raise AssertionError("Unreachable retry state")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args(argv)
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    expected = ["gradle", ":app:assembleDebug", ":app:testDebugUnitTest", ":app:lintDebug", "--no-daemon", "--stacktrace"]
    if command != expected:
        parser.error("Only the exact required Android build/unit-test/lint task set is permitted")
    return run_with_bounded_retry(command)


if __name__ == "__main__":
    raise SystemExit(main())
