package io.github.mikeyphw.autonexus.termuxtasker

/**
 * TT-F0: privacy-bounded, user-initiated analysis of an already exported LSPosed log.
 * It is not a Tasker host hook, an execution receipt, or proof of non-delivery.
 * No command, argv, stdin, stdout, stderr, secret or raw log line is retained.
 */
object TraceEvidence {
    const val MAX_IMPORT_BYTES = 512 * 1024
    const val MAX_INSPECTED_LINES = 12_000
    const val MAX_LINE_CHARS = 1024
    const val MAX_ACCEPTED_EVENTS = TraceProtocol.MAX_EVENTS_PER_PROCESS * 2
    private const val PREFIX = "AutoNexus TT_TRACE "
    private val record = Regex(
        """AutoNexus TT_TRACE schema=1 process=(EDITOR|BACKGROUND) phase=([A-Z_]+) outcome=([A-Z_]+) seq=([0-9]+) elapsedMs=([0-9]+) correlation=PROCESS_LOCAL_ONLY"""
    )

    data class Snapshot(
        val inspectedLines: Int,
        val acceptedEvents: Int,
        val rejectedTraceLines: Int,
        val truncated: Boolean,
        val phaseCounts: Map<TraceProtocol.Phase, Int>,
        val outcomeCounts: Map<TraceProtocol.Outcome, Int>,
        val backgroundEvents: Int,
        val editorEvents: Int,
    ) {
        private fun seen(phase: TraceProtocol.Phase) = (phaseCounts[phase] ?: 0) != 0

        /** Missing records can indicate missing or incomplete logs; never infer the script did not run. */
        fun interpretation(): String = when {
            truncated -> "Evidence incomplete (bounded import). No execution or host-delivery conclusion."
            acceptedEvents == 0 -> "No valid TT_TRACE events in this file. Verify LSPosed scope, identity and log export."
            !seen(TraceProtocol.Phase.RECEIVER_ENTER) ->
                "No receiver invocation observed in supplied trace. Missing events do not prove Tasker failed to dispatch."
            !seen(TraceProtocol.Phase.DISPATCH_ENTER) ->
                "Receiver observed, dispatch helper not observed. Command execution remains UNKNOWN."
            !seen(TraceProtocol.Phase.RESULT_SERVICE_ENTER) ->
                "Receiver/dispatch stage observed, result service not observed. Execution and callback remain UNKNOWN."
            !seen(TraceProtocol.Phase.CALLBACK_RETURN) ->
                "Result stage observed; local callback completion not observed. Tasker acceptance remains UNKNOWN."
            else -> "Local callback return observed. Tasker host acceptance remains UNKNOWN without host evidence."
        }

        /** Only closed enum counts and fixed text; never contains original file content. */
        fun sanitizedReport(): String = buildString {
            appendLine("AutoNexus Termux:Tasker TT-F0 redacted evidence report v1")
            appendLine("SOURCE=user-selected export; host acceptance=UNKNOWN; correlation=PROCESS_LOCAL_ONLY")
            appendLine("inspectedLines=$inspectedLines acceptedEvents=$acceptedEvents rejectedTraceLines=$rejectedTraceLines truncated=$truncated")
            appendLine("backgroundEvents=$backgroundEvents editorEvents=$editorEvents")
            for (phase in TraceProtocol.Phase.entries) {
                val count = phaseCounts[phase] ?: 0
                if (count > 0) appendLine("phase.${phase.name}=$count")
            }
            for (outcome in TraceProtocol.Outcome.entries) {
                val count = outcomeCounts[outcome] ?: 0
                if (count > 0) appendLine("outcome.${outcome.name}=$count")
            }
            appendLine("interpretation=${interpretation()}")
            appendLine("NOT PROOF OF: Tasker host acceptance, Termux execution, callback identity, reboot reliability")
        }
    }

    /** Runs only over a capped, in-memory, user-selected export, never device-wide logging. */
    fun analyze(lines: Sequence<String>, clippedByBytes: Boolean = false): Snapshot {
        val phases = mutableMapOf<TraceProtocol.Phase, Int>()
        val outcomes = mutableMapOf<TraceProtocol.Outcome, Int>()
        var inspected = 0
        var accepted = 0
        var rejected = 0
        var background = 0
        var editor = 0
        var truncated = clippedByBytes
        val iterator = lines.iterator()
        while (iterator.hasNext()) {
            if (inspected >= MAX_INSPECTED_LINES || accepted >= MAX_ACCEPTED_EVENTS) {
                truncated = true
                break
            }
            val line = iterator.next()
            inspected++
            if (line.length > MAX_LINE_CHARS) {
                if (line.contains(PREFIX)) rejected++
                continue
            }
            val start = line.indexOf(PREFIX)
            if (start < 0) continue
            val match = record.matchEntire(line.substring(start))
            if (match == null) { rejected++; continue }
            val (processText, phaseText, outcomeText, sequenceText, elapsedText) = match.destructured
            val phase = TraceProtocol.Phase.entries.firstOrNull { it.name == phaseText }
            val outcome = TraceProtocol.Outcome.entries.firstOrNull { it.name == outcomeText }
            if (phase == null || outcome == null || sequenceText.toLongOrNull() == null ||
                elapsedText.toLongOrNull() == null) { rejected++; continue }
            if (processText == "BACKGROUND") background++ else editor++
            phases[phase] = (phases[phase] ?: 0) + 1
            outcomes[outcome] = (outcomes[outcome] ?: 0) + 1
            accepted++
        }
        return Snapshot(inspected, accepted, rejected, truncated, phases.toMap(), outcomes.toMap(),
            background, editor)
    }
}
