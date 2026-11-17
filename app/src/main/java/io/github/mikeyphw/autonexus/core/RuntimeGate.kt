package io.github.mikeyphw.autonexus.core

/**
 * Immutable bootstrap gate. A later milestone must replace this with a
 * reviewed, version-pinned, process-specific compatibility authority.
 * No feature side effects are available in this build.
 */
object RuntimeGate {
    const val BEHAVIOR_HOOKS_ENABLED = false
    const val SYSTEM_UI_ENABLED = false
    const val SYSTEM_SERVER_ENABLED = false
    const val ROOT_ENABLED = false

    fun mayInstallBehaviorHook(packageName: String?): Boolean =
        BEHAVIOR_HOOKS_ENABLED && DomainRegistry.isDefaultScope(packageName)
}
