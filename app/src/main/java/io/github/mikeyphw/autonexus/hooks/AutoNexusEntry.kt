package io.github.mikeyphw.autonexus.hooks

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.mikeyphw.autonexus.core.DomainRegistry
import io.github.mikeyphw.autonexus.core.RuntimeGate

/** Legacy entrypoint compatible with LSPosed implementations accepting Xposed API 82. */
class AutoNexusEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val domain = DomainRegistry.forPackage(lpparam.packageName) ?: return
        // Bootstrap health event only. Never log variables, notification text or script paths.
        XposedBridge.log("AutoNexus bootstrap scoped: ${domain.packageName} (inert)")
        if (!RuntimeGate.mayInstallBehaviorHook(lpparam.packageName)) return
        // Intentionally empty: no hooks can be registered until a reviewed milestone.
    }
}
