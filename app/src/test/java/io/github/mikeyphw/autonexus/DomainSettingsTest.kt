package io.github.mikeyphw.autonexus

import io.github.mikeyphw.autonexus.core.Domain
import io.github.mikeyphw.autonexus.core.DomainSettings
import io.github.mikeyphw.autonexus.core.DomainSettingsContract
import org.junit.Assert.*
import org.junit.Test

class DomainSettingsTest {
    @Test fun allDomainsDefaultOff() {
        for (domain in Domain.entries) {
            val settings = DomainSettings()
            assertFalse(settings.requestedEnabled)
            assertFalse(settings.localDiagnosticsEnabled)
            assertFalse(DomainSettingsContract.behaviorActive(domain, settings))
        }
    }

    @Test fun requestsCannotEnableUnsafeHooks() {
        for (domain in Domain.entries) {
            assertFalse(DomainSettingsContract.behaviorActive(domain, DomainSettings(requestedEnabled = true)))
        }
    }

    @Test fun exactIndependentPersistentKeyspace() {
        val keys = Domain.entries.flatMap { listOf(
            DomainSettingsContract.requestedKey(it),
            DomainSettingsContract.diagnosticsKey(it),
        ) }
        assertEquals(8, keys.size)
        assertEquals(8, keys.distinct().size)
        assertEquals("domain.termux_tasker.requested", DomainSettingsContract.requestedKey(Domain.TERMUX_TASKER))
        assertEquals("domain.auto_notification.local_diagnostics", DomainSettingsContract.diagnosticsKey(Domain.AUTO_NOTIFICATION))
        assertEquals(1, DomainSettingsContract.SCHEMA_VERSION)
    }
}
