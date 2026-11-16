package io.github.mikeyphw.autonexus

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.mikeyphw.autonexus.core.Domain
import io.github.mikeyphw.autonexus.core.DomainRegistry

/** Read-only launcher and genuine package visibility check; no fake feature toggles. */
class MainActivity : Activity() {
    private fun label(message: String, size: Float, color: Int, pad: Int = 14): TextView = TextView(this).apply {
        text = message
        textSize = size
        setTextColor(color)
        setPadding(pad, pad, pad, pad)
    }

    private fun installedVersion(domain: Domain): String = try {
        @Suppress("DEPRECATION")
        val info = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(domain.packageName, PackageManager.PackageInfoFlags.of(0))
        } else packageManager.getPackageInfo(domain.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION") info.versionCode.toLong()
        }
        val matches = code == domain.versionCode && info.versionName == domain.versionName
        "Installed: ${info.versionName} ($code) · Reference match: ${if (matches) "yes" else "no"}"
    } catch (_: PackageManager.NameNotFoundException) {
        "Not installed or not visible"
    } catch (_: SecurityException) {
        "Package visibility unavailable"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val margin = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(margin, margin, margin, margin) }
        root.addView(label("AutoNexus", 29f, Color.rgb(28, 50, 78)), LinearLayout.LayoutParams(-1, -2))
        root.addView(label("One LSPosed module • Four independent domains", 16f, Color.DKGRAY), LinearLayout.LayoutParams(-1, -2))
        root.addView(label("BOOTSTRAP / SAFE-OFF. No behavior-changing hooks, privileged operations, or SystemUI integration are active.", 14f, Color.rgb(138, 68, 26)), LinearLayout.LayoutParams(-1, -2))
        for (domain in DomainRegistry.domains) {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.rgb(241, 245, 250))
                val m = LinearLayout.LayoutParams(-1, -2).apply { topMargin = margin / 2 }
                layoutParams = m
            }
            container.addView(label(domain.displayName, 19f, Color.rgb(25, 49, 78), 12))
            container.addView(label(installedVersion(domain), 13f, Color.DKGRAY, 12))
            container.addView(label("Hook status: off • Root: off • Original plugin unchanged", 12f, Color.rgb(58, 91, 70), 12))
            root.addView(container)
        }
        root.addView(label("Manage LSPosed scopes in the framework manager. Bootstrap merely logs scoped process loads; it does not enable any automation fixes.", 13f, Color.DKGRAY))
        val scroll = ScrollView(this).apply { addView(root, ViewGroup.LayoutParams(-1, -2)); isFillViewport = true }
        setContentView(scroll)
    }
}
