package io.github.mikeyphw.autonexus.termuxtasker

/**
 * This draft is transient in the original configuration activity. It is NEVER
 * inserted into the Locale/Tasker result bundle. The original app generates
 * that result using its own PluginBundleManager and finish() implementation.
 */
data class EditorDraft(
    val executable: String,
    val arguments: String,
    val workingDirectory: String,
    val stdin: String,
    val sessionAction: String,
    val logLevel: String,
    val inTerminal: Boolean,
    val waitForResult: Boolean,
) {
    fun validationProblems(sessionActionRange: IntRange?, logLevelRange: IntRange?): List<String> {
        val problems = ArrayList<String>()
        if (executable.isBlank()) problems += "Executable is required."
        // Prevent overflow of IPC/saved-state while retaining the original
        // editor as a fallback for an already-present larger legacy value.
        if (stdin.length > MAX_STDIN_CHARS) problems += "stdin is too large for this enhanced editor. Use Original."
        val approx = listOf(executable, arguments, workingDirectory, stdin, sessionAction, logLevel)
            .sumOf { it.length.toLong() * 4L }
        if (approx > MAX_ESTIMATED_BUNDLE_CHARS) problems += "Estimated Tasker data is too large. Use Original."
        numericProblem(sessionAction, sessionActionRange, "Session action")?.let { problems += it }
        numericProblem(logLevel, logLevelRange, "Background log level")?.let { problems += it }
        return problems
    }

    companion object {
        const val MAX_STDIN_CHARS = 45000
        const val MAX_ESTIMATED_BUNDLE_CHARS = 190000L
        // No semantic validation of Tasker variable placeholders without a host.
        fun numericProblem(value: String, range: IntRange?, field: String): String? {
            if (value.isBlank() || value.contains('%')) return null
            val parsed = value.trim().toIntOrNull() ?: return "$field must be an integer or a Tasker variable."
            if (range == null) return "$field range unavailable; open Original editor."
            if (parsed !in range) return "$field must be in ${range.first}..${range.last}."
            return null
        }

        const val RESTORE_SCHEMA = 1
        // Keys matched against the published v0.9.0 PluginBundleManager and
        // TaskerPlugin.Setting source. Unknown fields are NEVER dropped by the
        // enhanced editor; it refuses and leaves the original path available.
        val KNOWN_KEYS = setOf(
            "com.termux.tasker.extra.EXECUTABLE",
            "com.termux.execute.arguments",
            "com.termux.tasker.extra.WORKDIR",
            "com.termux.tasker.extra.STDIN",
            "com.termux.tasker.extra.SESSION_ACTION",
            "com.termux.tasker.extra.BACKGROUND_CUSTOM_LOG_LEVEL",
            "com.termux.tasker.extra.TERMINAL",
            "com.termux.tasker.extra.WAIT_FOR_RESULT",
            "com.termux.tasker.extra.VERSION_CODE",
            "net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS",
        )
        fun hasUnknownKeys(keys: Set<String>): Boolean = keys.any { it !in KNOWN_KEYS }
    }
}
