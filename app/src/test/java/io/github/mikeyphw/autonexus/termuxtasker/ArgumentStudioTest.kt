package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test

class ArgumentStudioTest {
    @Test fun originalParserInputIsUnchanged() {
        val raw = " --name 'two words'  %par1 "
        var provided: String? = null
        val preview = ArgumentStudio.inspect(raw) { provided = it; listOf("--name", "two words", "%par1") }
        assertEquals(raw, provided)
        assertEquals(3, preview.count)
        assertTrue(preview.variableSubstitutionPending)
        assertTrue(preview.display().contains("Tasker %variables remain unresolved"))
    }
    @Test fun noStandaloneTokenizerImplemented() {
        val p = ArgumentStudio.inspect("hello") { null }
        assertFalse(p.available)
        assertTrue(p.tokens.isEmpty())
    }
    @Test fun tokenizerThrowDoesNotChangeArguments() {
        val p = ArgumentStudio.inspect("\"unfinished") { throw IndexOutOfBoundsException("bad quote") }
        assertFalse(p.available)
        assertFalse(p.display().contains("bad quote"))
    }
    @Test fun oversizedPreviewRefusesBeforeTokenizer() {
        var ran = false
        val p = ArgumentStudio.inspect("x".repeat(ArgumentStudio.MAX_PREVIEW_CHARS + 1)) {
            ran = true; emptyList()
        }
        assertFalse(ran)
        assertFalse(p.available)
    }
    @Test fun excessiveTokensAreNotDisplayed() {
        val p = ArgumentStudio.inspect("dummy") { (0..ArgumentStudio.MAX_PREVIEW_TOKENS).map { "arg$it" } }
        assertFalse(p.available)
        assertTrue(p.tokens.isEmpty())
    }
    @Test fun longTokenIsOnlyTruncatedInVisualPreview() {
        val raw = "x".repeat(200)
        val p = ArgumentStudio.inspect(raw) { listOf(it) }
        assertEquals(raw, p.tokens.single())
        assertTrue(p.display().contains("truncated for preview only"))
    }
    @Test fun zeroArgumentsIsValid() {
        val p = ArgumentStudio.inspect(" ") { emptyList() }
        assertTrue(p.available)
        assertEquals(0, p.count)
    }
}
