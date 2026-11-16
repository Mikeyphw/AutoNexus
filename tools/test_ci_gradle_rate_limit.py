"""CI retry behavior and negative gates; no Gradle, network, or sleeping."""
from __future__ import annotations

import io
import sys
import unittest
from contextlib import redirect_stderr, redirect_stdout
from unittest.mock import patch

import ci_gradle_rate_limit as gate

CMD = ("gradle", ":app:assembleDebug", ":app:testDebugUnitTest", ":app:lintDebug", "--no-daemon", "--stacktrace")
MAVEN_429_LOG = """FAILURE: Build failed with an exception.
A problem occurred configuring root project 'AutoNexus'.
> Could not resolve all artifacts for configuration 'classpath'.
> Could not resolve org.jetbrains.kotlin:kotlin-stdlib:2.0.21.
> Could not get resource 'https://repo.maven.apache.org/maven2/kotlin-stdlib.pom'.
> Could not GET 'https://repo.maven.apache.org/maven2/kotlin-stdlib.pom'. Received status code 429 from server: Too Many Requests
"""


class RetryClassificationTests(unittest.TestCase):
    def test_confirmed_maven_rate_limit(self):
        self.assertTrue(gate.is_retryable_rate_limit(MAVEN_429_LOG))

    def test_maven_429_without_gradle_resolution_not_retryable(self):
        self.assertFalse(gate.is_retryable_rate_limit("Could not GET https://repo.maven.apache.org/ Received status code 429 from server: Too Many Requests"))

    def test_http_403_not_retryable(self):
        self.assertFalse(gate.is_retryable_rate_limit(MAVEN_429_LOG.replace("429", "403").replace("Too Many Requests", "Forbidden")))

    def test_missing_artifact_404_not_retryable(self):
        self.assertFalse(gate.is_retryable_rate_limit(MAVEN_429_LOG.replace("429", "404").replace("Too Many Requests", "Not Found")))

    def test_kotlin_compiler_error_not_retryable(self):
        self.assertFalse(gate.is_retryable_rate_limit("Execution failed for task ':app:compileDebugKotlin'\n" + MAVEN_429_LOG))

    def test_jvm_test_failure_not_retryable(self):
        self.assertFalse(gate.is_retryable_rate_limit("There were failing tests. See the report\n" + MAVEN_429_LOG))

    def test_only_classpath_failure_without_429_not_retryable(self):
        self.assertFalse(gate.is_retryable_rate_limit("Could not resolve all artifacts for configuration 'classpath'."))


class RetryExecutionTests(unittest.TestCase):
    def _run(self, outcomes):
        calls = []
        waits = []
        def fake_runner(argv):
            self.assertEqual(CMD, tuple(argv))
            calls.append(1)
            return outcomes[len(calls)-1]
        with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
            rc = gate.run_with_bounded_retry(CMD, run_one=fake_runner, sleep=waits.append, delays=(0, 0))
        return rc, len(calls), waits

    def test_first_success_does_not_retry(self):
        self.assertEqual((0, 1, []), self._run([(0, "BUILD SUCCESSFUL")]))

    def test_transient_http_429_then_success(self):
        self.assertEqual((0, 2, [0]), self._run([(1, MAVEN_429_LOG), (0, "BUILD SUCCESSFUL")]))

    def test_persistent_http_429_exhausts_and_fails(self):
        self.assertEqual((7, 3, [0, 0]), self._run([(1, MAVEN_429_LOG), (1, MAVEN_429_LOG), (7, MAVEN_429_LOG)]))

    def test_non_network_build_failure_is_not_retried(self):
        self.assertEqual((3, 1, []), self._run([(3, "Execution failed for task ':app:compileDebugKotlin'")]))

    def test_mixed_compile_failure_and_429_is_not_retried(self):
        self.assertEqual((2, 1, []), self._run([(2, MAVEN_429_LOG + "\nExecution failed for task ':app:testDebugUnitTest'")]))

    def test_empty_gradle_command_refuses(self):
        with self.assertRaises(ValueError):
            gate.run_with_bounded_retry((), run_one=lambda argv: (0, ""), delays=(0, 0))

    def test_excessive_or_unbounded_retry_schedule_refuses(self):
        with self.assertRaises(ValueError):
            gate.run_with_bounded_retry(CMD, run_one=lambda argv: (0, ""), delays=(0, 999))

    def test_cli_rejects_omitted_security_check(self):
        with patch.object(sys, "stderr", io.StringIO()):
            with self.assertRaises(SystemExit) as ctx:
                gate.main(["--", "gradle", ":app:assembleDebug"])
        self.assertEqual(ctx.exception.code, 2)

    def test_cli_accepts_exact_required_tasks(self):
        with patch.object(gate, "run_with_bounded_retry", return_value=0) as fn:
            self.assertEqual(0, gate.main(["--", *CMD]))
            fn.assert_called_once_with(list(CMD))


if __name__ == "__main__":
    unittest.main()
