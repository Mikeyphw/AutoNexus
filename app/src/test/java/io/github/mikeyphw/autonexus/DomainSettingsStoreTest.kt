package io.github.mikeyphw.autonexus

import android.content.SharedPreferences
import io.github.mikeyphw.autonexus.core.Domain
import io.github.mikeyphw.autonexus.core.DomainSettings
import io.github.mikeyphw.autonexus.core.DomainSettingsContract
import io.github.mikeyphw.autonexus.settings.DomainSettingsStore
import org.junit.Assert.*
import org.junit.Test

/** In-memory SharedPreferences test double: exercises the production store with no device. */
private class FakePreferences : SharedPreferences {
    val values = mutableMapOf<String, Any?>()
    var failCommit = false

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = getTyped(key, defValue)
    override fun getLong(key: String?, defValue: Long): Long = getTyped(key, defValue)
    override fun getFloat(key: String?, defValue: Float): Float = getTyped(key, defValue)
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = getTyped(key, defValue)
    private inline fun <reified T> getTyped(key: String?, default: T): T {
        if (!values.containsKey(key)) return default
        return values[key] as? T ?: throw ClassCastException("type mismatch")
    }
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val updates = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = putAny(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = putAny(key, values)
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = putAny(key, value)
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = putAny(key, value)
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = putAny(key, value)
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = putAny(key, value)
        private fun putAny(key: String?, value: Any?): SharedPreferences.Editor {
            requireNotNull(key)
            updates[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor { requireNotNull(key); removals.add(key); return this }
        override fun clear(): SharedPreferences.Editor { clearAll = true; return this }
        override fun commit(): Boolean {
            if (failCommit) return false
            if (clearAll) values.clear()
            removals.forEach { values.remove(it) }
            values.putAll(updates)
            return true
        }
        override fun apply() { commit() }
    }
}

class DomainSettingsStoreTest {
    @Test fun freshSettingsAreOff() {
        val storage = DomainSettingsStore(FakePreferences())
        for (domain in Domain.entries) {
            val read = storage.read(domain)
            assertTrue(read.readable)
            assertEquals(DomainSettings(), read.value)
        }
    }

    @Test fun independentFlagsSurviveRepositoryRecreation() {
        val prefs = FakePreferences()
        val store = DomainSettingsStore(prefs)
        assertTrue(store.save(Domain.AUTO_INPUT, DomainSettings(true, true)))
        assertTrue(store.save(Domain.AUTO_TOOLS, DomainSettings(false, true)))
        val reopened = DomainSettingsStore(prefs)
        assertEquals(DomainSettings(true, true), reopened.read(Domain.AUTO_INPUT).value)
        assertEquals(DomainSettings(false, true), reopened.read(Domain.AUTO_TOOLS).value)
        assertEquals(DomainSettings(), reopened.read(Domain.TERMUX_TASKER).value)
        assertEquals(DomainSettings(), reopened.read(Domain.AUTO_NOTIFICATION).value)
        assertEquals(DomainSettingsContract.SCHEMA_VERSION, prefs.values[DomainSettingsContract.SCHEMA_KEY])
    }

    @Test fun failedCommitDoesNotChangePriorState() {
        val prefs = FakePreferences()
        val store = DomainSettingsStore(prefs)
        assertTrue(store.save(Domain.AUTO_INPUT, DomainSettings(true, false)))
        prefs.failCommit = true
        assertFalse(store.save(Domain.AUTO_INPUT, DomainSettings(false, true)))
        assertEquals(DomainSettings(true, false), store.read(Domain.AUTO_INPUT).value)
    }

    @Test fun newerSchemaFailsClosedWithoutOverwriting() {
        val prefs = FakePreferences()
        prefs.values[DomainSettingsContract.SCHEMA_KEY] = 999
        val store = DomainSettingsStore(prefs)
        assertFalse(store.read(Domain.AUTO_INPUT).readable)
        assertEquals(DomainSettings(), store.read(Domain.AUTO_INPUT).value)
        assertFalse(store.save(Domain.AUTO_INPUT, DomainSettings(true, true)))
        assertEquals(999, prefs.values[DomainSettingsContract.SCHEMA_KEY])
    }

    @Test fun corruptBooleanAndOrphanedKeysFailClosed() {
        val prefs = FakePreferences()
        prefs.values[DomainSettingsContract.requestedKey(Domain.AUTO_INPUT)] = true
        var store = DomainSettingsStore(prefs)
        assertFalse(store.read(Domain.AUTO_INPUT).readable) // missing schema
        assertFalse(store.save(Domain.AUTO_INPUT, DomainSettings(true, true)))
        prefs.values[DomainSettingsContract.SCHEMA_KEY] = 1
        prefs.values[DomainSettingsContract.requestedKey(Domain.AUTO_INPUT)] = "not a bool"
        store = DomainSettingsStore(prefs)
        assertFalse(store.read(Domain.AUTO_INPUT).readable)
        assertEquals(DomainSettings(), store.read(Domain.AUTO_INPUT).value)
    }
}
