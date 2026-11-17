package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test

class TraceEvidenceTest {
    private fun line(process: TraceProtocol.Process, phase: TraceProtocol.Phase,
                     outcome: TraceProtocol.Outcome = TraceProtocol.Outcome.OBSERVED): String =
        "2026-11-17T01:00:00Z " + TraceProtocol.event(process, phase, outcome, 42, 8)

    @Test fun noEvidenceNeverBecomesNonDeliveryProof() {
        val report = TraceEvidence.analyze(sequenceOf("unrelated secret=123"))
        assertEquals(0, report.acceptedEvents)
        assertTrue(report.interpretation().contains("No valid TT_TRACE"))
        assertTrue(report.sanitizedReport().contains("host acceptance=UNKNOWN"))
        assertFalse(report.sanitizedReport().contains("secret"))
    }

    @Test fun realReceiverWithoutDispatchIsUnknownExecution() {
        val report = TraceEvidence.analyze(sequenceOf(
            line(TraceProtocol.Process.BACKGROUND, TraceProtocol.Phase.RECEIVER_ENTER),
            line(TraceProtocol.Process.BACKGROUND, TraceProtocol.Phase.RECEIVER_RETURN,
                TraceProtocol.Outcome.RESULT_PENDING)))
        assertTrue(report.interpretation().contains("execution remains UNKNOWN", ignoreCase = true))
        assertEquals(2, report.backgroundEvents)
        assertEquals(0, report.editorEvents)
    }

    @Test fun resultAndCallbackDoNotProveHostAcceptance() {
        val report = TraceEvidence.analyze(sequenceOf(
            line(TraceProtocol.Process.BACKGROUND, TraceProtocol.Phase.RECEIVER_ENTER),
            line(TraceProtocol.Process.BACKGROUND, TraceProtocol.Phase.DISPATCH_ENTER),
            line(TraceProtocol.Process.EDITOR, TraceProtocol.Phase.RESULT_SERVICE_ENTER),
            line(TraceProtocol.Process.EDITOR, TraceProtocol.Phase.CALLBACK_RETURN,
                TraceProtocol.Outcome.CALLBACK_SENT_LOCAL)))
        assertEquals(4, report.acceptedEvents)
        assertTrue(report.interpretation().contains("acceptance remains UNKNOWN"))
        assertFalse(report.sanitizedReport().contains("Tasker completed"))
    }

    @Test fun invalidAndSecretLogsAreNeverReExported() {
        val malicious = "AutoNexus TT_TRACE schema=1 process=BACKGROUND phase=RECEIVER_ENTER " +
            "outcome=OBSERVED seq=1 elapsedMs=0 correlation=PROCESS_LOCAL_ONLY secret=password"
        val log = TraceEvidence.analyze(sequenceOf("my command --token=VERYSECRET", malicious,
            line(TraceProtocol.Process.EDITOR, TraceProtocol.Phase.ATTACHED)))
        assertEquals(1, log.acceptedEvents)
        assertEquals(1, log.rejectedTraceLines)
        val out = log.sanitizedReport()
        assertFalse(out.contains("VERYSECRET"))
        assertFalse(out.contains("password"))
        assertFalse(out.contains("command --token"))
    }

    @Test fun hugeLinesAndUntrustedSchemaAreRejected() {
        val report = TraceEvidence.analyze(sequenceOf(
            line(TraceProtocol.Process.EDITOR, TraceProtocol.Phase.ATTACHED) + "!".repeat(2048),
            "AutoNexus TT_TRACE schema=2 process=EDITOR phase=ATTACHED outcome=OBSERVED " +
                "seq=1 elapsedMs=0 correlation=PROCESS_LOCAL_ONLY"))
        assertEquals(0, report.acceptedEvents)
        assertEquals(2, report.rejectedTraceLines)
    }

    @Test fun byteClippingAlwaysDisablesConclusiveInterpretation() {
        val report = TraceEvidence.analyze(sequenceOf(
            line(TraceProtocol.Process.EDITOR, TraceProtocol.Phase.ATTACHED)), true)
        assertTrue(report.truncated)
        assertTrue(report.interpretation().contains("Evidence incomplete"))
    }

    @Test fun inspectedLinesBoundIncludesIrrelevantLines() {
        val report = TraceEvidence.analyze(generateSequence { "uninformative" }.take(TraceEvidence.MAX_INSPECTED_LINES+8))
        assertTrue(report.truncated)
        assertEquals(TraceEvidence.MAX_INSPECTED_LINES, report.inspectedLines)
    }

    @Test fun eventCountBoundAndProcessLocalOnly() {
        val events = generateSequence { line(TraceProtocol.Process.BACKGROUND, TraceProtocol.Phase.RECEIVER_ENTER) }
        val report = TraceEvidence.analyze(events.take(TraceEvidence.MAX_ACCEPTED_EVENTS+1))
        assertEquals(TraceEvidence.MAX_ACCEPTED_EVENTS, report.acceptedEvents)
        assertTrue(report.truncated)
        assertTrue(report.sanitizedReport().contains("PROCESS_LOCAL_ONLY"))
    }
}
