package io.github.mikeyphw.autonexus

import io.github.mikeyphw.autonexus.core.DomainRegistry
import io.github.mikeyphw.autonexus.core.Domain
import org.junit.Assert.*
import org.junit.Test

class DomainRegistryTest {
    @Test fun fourDomainsWithUniquePackages() {
        assertEquals(4, DomainRegistry.domains.size)
        assertEquals(4, DomainRegistry.domains.map { it.packageName }.distinct().size)
    }
    @Test fun exactScopeOnly() {
        for (domain in Domain.entries) assertEquals(domain, DomainRegistry.forPackage(domain.packageName))
        for (unknown in listOf("com.android.systemui", "android", "system_server", "net.dinglisch.android.taskerm", "com.termux", "", "com.termux.tasker.evil")) {
            assertNull(DomainRegistry.forPackage(unknown))
            assertFalse(DomainRegistry.isDefaultScope(unknown))
        }
    }
}
