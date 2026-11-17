package io.github.mikeyphw.autonexus.termuxtasker

/**
 * TT-E2: a bounded read-only view of the ORIGINAL Termux:Tasker v0.9.0
 * ArgumentTokenizer result. This code never attempts shell parsing itself.
 * The supplied [tokenize] callback MUST invoke the attested plugin class.
 */
object ArgumentStudio {
    const val MAX_PREVIEW_CHARS = 8192
    const val MAX_PREVIEW_TOKENS = 64
    const val MAX_VISIBLE_TOKEN_CHARS = 160

    data class Preview(
        val available: Boolean,
        val tokens: List<String>,
        val variableSubstitutionPending: Boolean,
        val message: String,
    ) {
        val count: Int get() = tokens.size
        fun display(): String {
            if (!available) return message
            val lines = tokens.mapIndexed { i, token ->
                val shown = if (token.length > MAX_VISIBLE_TOKEN_CHARS)
                    token.take(MAX_VISIBLE_TOKEN_CHARS) + "… [truncated for preview only]" else token
                "${i + 1}. $shown"
            }
            return buildString {
                append("${tokens.size} parsed argument(s) · local preview only\n")
                if (variableSubstitutionPending) {
                    append("Tasker %variables remain unresolved until execution.\n")
                }
                append("Preview never changes the literal arguments or runs the command.\n")
                append(lines.joinToString("\n"))
            }
        }
    }

    fun inspect(raw: String, tokenize: (String) -> List<String>?): Preview {
        if (raw.length > MAX_PREVIEW_CHARS) return Preview(false, emptyList(), false,
            "Preview unavailable: long argument strings remain untouched; use Original for details.")
        val hasVariables = '%' in raw
        val tokens = try { tokenize(raw) } catch (_: Throwable) { null }
            ?: return Preview(false, emptyList(), hasVariables,
                "Original plugin tokenizer unavailable or rejected the input. Literal text is preserved.")
        if (tokens.size > MAX_PREVIEW_TOKENS) return Preview(false, emptyList(), hasVariables,
            "Preview exceeds the 64-token display cap. Original arguments are preserved.")
        if (tokens.any { it.length > MAX_PREVIEW_CHARS }) return Preview(false, emptyList(), hasVariables,
            "Original tokenizer returned oversized data. No preview is displayed.")
        return Preview(true, tokens.toList(), hasVariables, "Ready")
    }
}
