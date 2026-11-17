package io.github.mikeyphw.autonexus

import io.github.mikeyphw.autonexus.core.*
import org.junit.Assert.*
import org.junit.Test

class RuntimeGateTest {
    @Test fun bootstrapIsFullyInert() {
        assertFalse(RuntimeGate.BEHAVIOR_HOOKS_ENABLED)
        assertFalse(RuntimeGate.SYSTEM_UI_ENABLED)
        assertFalse(RuntimeGate.SYSTEM_SERVER_ENABLED)
        assertFalse(RuntimeGate.ROOT_ENABLED)
        for (domain in DomainRegistry.domains) assertFalse(RuntimeGate.mayInstallBehaviorHook(domain.packageName))
    }
    @Test fun rootRejectsEveryOperationEvenIfApproved() {
        for (operation in RootOperation.entries) {
            assertFalse(RootPolicy.authorize(operation, true))
            assertFalse(RootPolicy.authorize(operation, false))
        }
    }
}
