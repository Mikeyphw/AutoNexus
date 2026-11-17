package io.github.mikeyphw.autonexus.termuxtasker

/**
 * TT-F1 no-replay startup coordinator contract.
 * The plugin cannot stall an in-flight Tasker ordered broadcast while waiting
 * for Termux; waiting must be performed by the calling Tasker profile before
 * dispatch. Probe outcomes never authorize automatic command execution.
 */
object BootReadinessPolicy {
    enum class Phase { LOCKED, GRACE_PERIOD, DEPENDENCY_UNVERIFIED, MANUAL_CONFIRMATION }
    data class Evidence(val userUnlocked: Boolean, val secondsAfterUnlock: Long?,
                        val termuxServiceConfirmed: Boolean?)

    // This is an operator-configurable diagnostic *starting point*, not a
    // declaration that any device becomes ready after this fixed duration.
    const val DEFAULT_GRACE_SECONDS = 180L
    fun evaluate(e: Evidence): Phase = when {
        !e.userUnlocked -> Phase.LOCKED
        e.secondsAfterUnlock != null && e.secondsAfterUnlock < DEFAULT_GRACE_SECONDS -> Phase.GRACE_PERIOD
        e.termuxServiceConfirmed != true -> Phase.DEPENDENCY_UNVERIFIED
        else -> Phase.MANUAL_CONFIRMATION
    }
    fun isExecutionConfirmed(e: Evidence): Boolean = false

    const val explanation = "Tasker starts before Termux on this device. " +
        "A failed boot action works after startup. Do not run a command from a readiness probe " +
        "or replay a timed-out action: it may already have executed."

    const val taskerInstructions = "In your existing Tasker boot/unlock profile: " +
        "(1) Put a Wait action of about 3 minutes before the Termux:Tasker action. " +
        "(2) Prefer background execution when a terminal is not required. " +
        "(3) If you need stronger readiness, gate the profile on an independent " +
        "non-executing Termux availability check. Do not use the actual command as the probe. " +
        "(4) Test once after reboot and inspect TT-F0 evidence. " +
        "The delay is a workaround, not proof that the service is ready."
}
