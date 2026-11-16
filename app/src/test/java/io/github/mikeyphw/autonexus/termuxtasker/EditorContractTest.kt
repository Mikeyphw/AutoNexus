package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test

class EditorContractTest {
    private val baseline = EditorDraft(
        executable="~/.termux/tasker/safe-script.sh", arguments="--name 'two words' %par1",
        workingDirectory="~/work", stdin="hello\nworld", sessionAction="", logLevel="",
        inTerminal=false, waitForResult=true,
    )

    @Test fun unchangedDraftRetainsExactArgumentsAndStdin() {
        val restored=baseline.copy()
        assertEquals(baseline,restored)
        assertEquals("--name 'two words' %par1",restored.arguments)
        assertEquals("hello\nworld",restored.stdin)
    }
    @Test fun validSettingsAreAccepted() {
        assertTrue(baseline.copy(sessionAction="2",logLevel="3")
            .validationProblems(0..3,0..6).isEmpty())
    }
    @Test fun missingExecutableIsRejected() {
        assertTrue(baseline.copy(executable=" ").validationProblems(0..3,0..6)
            .any { it.contains("Executable") })
    }
    @Test fun variableContainingNumericFieldsAreDeferred() {
        assertTrue(baseline.copy(sessionAction="%mode",logLevel="%custom")
            .validationProblems(0..3,0..6).isEmpty())
    }
    @Test fun outOfRangeAndMalformedNumbersAreRejected() {
        val result=baseline.copy(sessionAction="999",logLevel="nope")
            .validationProblems(0..3,0..6)
        assertEquals(2,result.size)
    }
    @Test fun unknownRangeRefusesConcreteNumericSave() {
        assertNotNull(EditorDraft.numericProblem("2",null,"Session action"))
        assertNull(EditorDraft.numericProblem("%mode",null,"Session action"))
    }
    @Test fun oversizeStdinIsRejectedWithoutTruncation() {
        val big="x".repeat(EditorDraft.MAX_STDIN_CHARS+1)
        assertEquals(big,baseline.copy(stdin=big).stdin)
        assertTrue(baseline.copy(stdin=big).validationProblems(0..3,0..6)
            .any { it.contains("too large") })
    }
    @Test fun unknownTaskerKeysAreRefused() {
        assertFalse(EditorDraft.hasUnknownKeys(EditorDraft.KNOWN_KEYS))
        assertTrue(EditorDraft.hasUnknownKeys(EditorDraft.KNOWN_KEYS+"inventedKey"))
    }
    @Test fun domainStateAndCommandExecutionAreNotPartOfDraft() {
        assertEquals(8,EditorDraft::class.java.declaredFields.count { !it.isSynthetic &&
            it.name in setOf("executable","arguments","workingDirectory","stdin",
                "sessionAction","logLevel","inTerminal","waitForResult") })
    }
}
