package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test

class BootReadinessPolicyTest {
    @Test fun lockedNeverTreatedReady() {
        val e = BootReadinessPolicy.Evidence(false, null, true)
        assertEquals(BootReadinessPolicy.Phase.LOCKED, BootReadinessPolicy.evaluate(e))
        assertFalse(BootReadinessPolicy.isExecutionConfirmed(e))
    }
    @Test fun unlockGraceNotEquivalentToReady() {
        val e = BootReadinessPolicy.Evidence(true, 20L, true)
        assertEquals(BootReadinessPolicy.Phase.GRACE_PERIOD, BootReadinessPolicy.evaluate(e))
        assertFalse(BootReadinessPolicy.isExecutionConfirmed(e))
    }
    @Test fun serviceNeverGuessedFromElapsedTime() {
        val e = BootReadinessPolicy.Evidence(true, 10000L, null)
        assertEquals(BootReadinessPolicy.Phase.DEPENDENCY_UNVERIFIED, BootReadinessPolicy.evaluate(e))
        assertFalse(BootReadinessPolicy.isExecutionConfirmed(e))
    }
    @Test fun positiveSignalStillRequiresManualConfirmation() {
        val e = BootReadinessPolicy.Evidence(true, 200L, true)
        assertEquals(BootReadinessPolicy.Phase.MANUAL_CONFIRMATION, BootReadinessPolicy.evaluate(e))
        assertFalse(BootReadinessPolicy.isExecutionConfirmed(e))
    }
    @Test fun instructionDoesNotAdvocateAutomaticReplay() {
        assertTrue(BootReadinessPolicy.taskerInstructions.contains("Wait action"))
        assertTrue(BootReadinessPolicy.explanation.contains("replay"))
    }
}
