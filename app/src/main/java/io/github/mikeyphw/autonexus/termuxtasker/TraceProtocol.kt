package io.github.mikeyphw.autonexus.termuxtasker

/** Pure source/identity/trace contract. No shell commands, intents or Tasker payloads enter logs. */
object TraceProtocol {
    const val SCHEMA_VERSION = 1
    const val PLUGIN = "com.termux.tasker"
    const val VERSION_NAME = "0.9.0"
    const val VERSION_CODE = 1002L
    // SHA-256 of the single F-Droid signing certificate in the supplied v0.9.0 APK.
    const val CERTIFICATE_SHA256 = "228fb2cfe90831c1499ec3ccaf61e96e8e1ce70766b9474672ce427334d41c42"
    const val MAX_EVENTS_PER_PROCESS = 160

    enum class Process { EDITOR, BACKGROUND }
    enum class Phase {
        ATTACHED, RECEIVER_ENTER, RECEIVER_RETURN, DISPATCH_ENTER, DISPATCH_RETURN,
        RESULT_SERVICE_ENTER, RESULT_SERVICE_RETURN,
        RESULT_HELPER_ENTER, RESULT_HELPER_RETURN, CALLBACK_ENTER, CALLBACK_RETURN,
        INCOMPATIBLE, HOOK_UNAVAILABLE
    }
    enum class Outcome { OBSERVED, RETURNED, THROW_SECURITY, THROW_ARGUMENT, THROW_RUNTIME,
        THROW_OTHER, RESULT_PENDING, RESULT_FAILED, RESULT_OK, RESULT_OTHER,
        CALLBACK_SENT_LOCAL, CALLBACK_FALSE, UNAVAILABLE }

    fun isTrustedIdentity(packageName: String?, versionName: String?, versionCode: Long,
                          certificateSha256: String?): Boolean =
        packageName == PLUGIN && versionName == VERSION_NAME && versionCode == VERSION_CODE &&
            certificateSha256 == CERTIFICATE_SHA256

    /** Android RESULT_OK=-1, RESULT_FIRST_USER=1; Termux:Tasker PENDING=3, FAILED=2. */
    fun classifyOrderedResult(ordered: Boolean, code: Int): Outcome = when {
        !ordered -> Outcome.UNAVAILABLE
        code == -1 -> Outcome.RESULT_OK
        code == 2 -> Outcome.RESULT_FAILED
        code == 3 -> Outcome.RESULT_PENDING
        else -> Outcome.RESULT_OTHER
    }

    fun exceptionKind(cause: Throwable?): Outcome = when (cause) {
        null -> Outcome.RETURNED
        is SecurityException -> Outcome.THROW_SECURITY
        is IllegalArgumentException -> Outcome.THROW_ARGUMENT
        is RuntimeException -> Outcome.THROW_RUNTIME
        else -> Outcome.THROW_OTHER
    }

    /** All strings are closed enums/constants; no untrusted messages or user input are formatted. */
    fun event(process: Process, phase: Phase, result: Outcome, sequence: Long,
              elapsedMs: Long): String =
        "AutoNexus TT_TRACE schema=$SCHEMA_VERSION process=${process.name} " +
            "phase=${phase.name} outcome=${result.name} seq=${sequence.coerceAtLeast(0)} " +
            "elapsedMs=${elapsedMs.coerceAtLeast(0)} correlation=PROCESS_LOCAL_ONLY"
}
