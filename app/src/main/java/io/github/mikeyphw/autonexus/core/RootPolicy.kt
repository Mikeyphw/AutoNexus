package io.github.mikeyphw.autonexus.core

/** Plan-only privilege contract. There is no su invocation or root service in this bootstrap. */
enum class RootOperation { INSPECT_PACKAGE, INSPECT_PROCESS, INSPECT_SERVICE, REQUEST_RECOVERY }

object RootPolicy {
    fun authorize(operation: RootOperation, explicitlyApproved: Boolean): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val ignored = operation to explicitlyApproved
        return false // Fail closed until a reviewed, authenticated root helper exists.
    }
}
