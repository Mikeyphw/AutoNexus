package io.github.mikeyphw.autonexus.settings

import android.content.Context
import android.content.SharedPreferences
import io.github.mikeyphw.autonexus.core.Domain
import io.github.mikeyphw.autonexus.core.DomainSettings
import io.github.mikeyphw.autonexus.core.DomainSettingsContract

/**
 * Module-UID-only, schema-versioned preferences. This data is deliberately
 * NOT made world-readable and is NOT observed inside injected target processes.
 * Future behavior adapters require separately reviewed authenticated IPC.
 */
class DomainSettingsStore(private val preferences: SharedPreferences) {
    data class ReadResult(val value: DomainSettings, val readable: Boolean)

    companion object {
        fun from(context: Context): DomainSettingsStore = DomainSettingsStore(
            context.getSharedPreferences(DomainSettingsContract.PREFS_NAME, Context.MODE_PRIVATE)
        )
    }

    fun read(domain: Domain): ReadResult = try {
        val schemaKey = DomainSettingsContract.SCHEMA_KEY
        val hasSchema = preferences.contains(schemaKey)
        val schema = if (hasSchema) preferences.getInt(schemaKey, -1) else 1
        val orphaned = !hasSchema && preferences.all.keys.any { it.startsWith("domain.") }
        if (schema != DomainSettingsContract.SCHEMA_VERSION || orphaned) {
            ReadResult(DomainSettings(), false)
        } else {
            ReadResult(
                DomainSettings(
                    requestedEnabled = preferences.getBoolean(DomainSettingsContract.requestedKey(domain), false),
                    localDiagnosticsEnabled = preferences.getBoolean(DomainSettingsContract.diagnosticsKey(domain), false),
                ),
                true,
            )
        }
    } catch (_: RuntimeException) {
        // Corrupt or inaccessible preferences must never enable a domain.
        ReadResult(DomainSettings(), false)
    }

    /** Synchronous atomic commit: update the visible state only when successful. */
    fun save(domain: Domain, desired: DomainSettings): Boolean {
        if (!read(domain).readable) return false
        return try {
            preferences.edit()
                .putInt(DomainSettingsContract.SCHEMA_KEY, DomainSettingsContract.SCHEMA_VERSION)
                .putBoolean(DomainSettingsContract.requestedKey(domain), desired.requestedEnabled)
                .putBoolean(DomainSettingsContract.diagnosticsKey(domain), desired.localDiagnosticsEnabled)
                .commit()
        } catch (_: RuntimeException) {
            false
        }
    }
}
