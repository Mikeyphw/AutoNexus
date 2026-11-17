package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test

class TraceProtocolTest {
    @Test fun requiresExactIdentity() {
        assertTrue(TraceProtocol.isTrustedIdentity(TraceProtocol.PLUGIN, "0.9.0", 1002,
            TraceProtocol.CERTIFICATE_SHA256))
        assertFalse(TraceProtocol.isTrustedIdentity("com.termux", "0.9.0", 1002,
            TraceProtocol.CERTIFICATE_SHA256))
        assertFalse(TraceProtocol.isTrustedIdentity(TraceProtocol.PLUGIN, "0.9.1", 1002,
            TraceProtocol.CERTIFICATE_SHA256))
        assertFalse(TraceProtocol.isTrustedIdentity(TraceProtocol.PLUGIN, "0.9.0", 1003,
            TraceProtocol.CERTIFICATE_SHA256))
        assertFalse(TraceProtocol.isTrustedIdentity(TraceProtocol.PLUGIN, "0.9.0", 1002, "wrong"))
        assertFalse(TraceProtocol.isTrustedIdentity(TraceProtocol.PLUGIN, "0.9.0", 1002, null))
    }

    @Test fun distinguishOrderedResults() {
        assertEquals(TraceProtocol.Outcome.RESULT_PENDING, TraceProtocol.classifyOrderedResult(true, 3))
        assertEquals(TraceProtocol.Outcome.RESULT_FAILED, TraceProtocol.classifyOrderedResult(true, 2))
        assertEquals(TraceProtocol.Outcome.RESULT_OK, TraceProtocol.classifyOrderedResult(true, -1))
        assertEquals(TraceProtocol.Outcome.RESULT_OTHER, TraceProtocol.classifyOrderedResult(true, 0))
        assertEquals(TraceProtocol.Outcome.UNAVAILABLE, TraceProtocol.classifyOrderedResult(false, 3))
    }

    @Test fun exceptionFamiliesCannotRevealMessages() {
        assertEquals(TraceProtocol.Outcome.RETURNED, TraceProtocol.exceptionKind(null))
        assertEquals(TraceProtocol.Outcome.THROW_SECURITY,
            TraceProtocol.exceptionKind(SecurityException("PASSWORD_123")))
        assertEquals(TraceProtocol.Outcome.THROW_ARGUMENT,
            TraceProtocol.exceptionKind(IllegalArgumentException("command --secret")))
        assertEquals(TraceProtocol.Outcome.THROW_RUNTIME,
            TraceProtocol.exceptionKind(IllegalStateException("hidden stdout")))
    }

    @Test fun traceIsBoundedRedactedAndProcessLocal() {
        val line = TraceProtocol.event(TraceProtocol.Process.BACKGROUND,
            TraceProtocol.Phase.RECEIVER_RETURN, TraceProtocol.Outcome.RESULT_PENDING, 99, 12)
        assertTrue(line.contains("schema=1"))
        assertTrue(line.contains("seq=99"))
        assertTrue(line.contains("correlation=PROCESS_LOCAL_ONLY"))
        assertFalse(line.contains("stdout"))
        assertFalse(line.contains("executable"))
        assertFalse(line.contains("Tasker completion accepted"))
        assertEquals(160, TraceProtocol.MAX_EVENTS_PER_PROCESS)
    }

    @Test fun negativeNumbersAreBounded() {
        val line = TraceProtocol.event(TraceProtocol.Process.EDITOR,
            TraceProtocol.Phase.INCOMPATIBLE, TraceProtocol.Outcome.UNAVAILABLE, -100, -70)
        assertTrue(line.contains("seq=0 elapsedMs=0"))
    }
}
