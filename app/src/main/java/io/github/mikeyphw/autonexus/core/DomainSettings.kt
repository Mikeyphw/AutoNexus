package io.github.mikeyphw.autonexus.core

/** A request for a future, reviewed adapter; never permission to install a hook. */
data class DomainSettings(
    val requestedEnabled: Boolean = false,
    val localDiagnosticsEnabled: Boolean = false,
)

/** Stable, per-domain, private preference keys; no settings IPC into target apps. */
object DomainSettingsContract {
    const val SCHEMA_VERSION: Int = 1
    const val PREFS_NAME: String = "autonexus_domain_settings_v1"
    const val SCHEMA_KEY: String = "schema_version"

    private fun prefix(domain: Domain): String = "domain.${domain.name.lowercase()}."
    fun requestedKey(domain: Domain): String = prefix(domain) + "requested"
    fun diagnosticsKey(domain: Domain): String = prefix(domain) + "local_diagnostics"

    /** A staged request cannot override the immutable, source-level safety gate. */
    fun behaviorActive(domain: Domain, settings: DomainSettings): Boolean =
        settings.requestedEnabled && RuntimeGate.mayInstallBehaviorHook(domain.packageName)
}
