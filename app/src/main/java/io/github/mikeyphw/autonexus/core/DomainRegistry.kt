package io.github.mikeyphw.autonexus.core

/** Boundaries, not capability claims. Each domain defaults to no mutating hooks. */
enum class Domain(val packageName: String, val displayName: String, val versionName: String, val versionCode: Long) {
    AUTO_NOTIFICATION("com.joaomgcd.autonotification", "AutoNotification", "4.3.14", 182),
    AUTO_INPUT("com.joaomgcd.autoinput", "AutoInput", "3.0.12", 104),
    AUTO_TOOLS("com.joaomgcd.autotools", "AutoTools", "2.3.19", 197),
    TERMUX_TASKER("com.termux.tasker", "Termux:Tasker", "0.9.0", 1002);
}

object DomainRegistry {
    val domains: List<Domain> = Domain.entries.toList()
    private val byPackage = domains.associateBy { it.packageName }
    fun forPackage(packageName: String?): Domain? = byPackage[packageName]
    fun isDefaultScope(packageName: String?): Boolean = forPackage(packageName) != null
}
