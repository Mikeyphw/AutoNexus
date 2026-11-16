package io.github.mikeyphw.autonexus.termuxtasker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Read-only installation attestation. An unknown or non-F-Droid build is never hooked. */
object TrustedTarget {
    data class Observation(val supported: Boolean, val label: String)

    @Suppress("DEPRECATION")
    fun inspect(context: Context): Observation = try {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
                    else PackageManager.GET_SIGNATURES
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                TraceProtocol.PLUGIN, PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            context.packageManager.getPackageInfo(TraceProtocol.PLUGIN, flags)
        }
        val signers = if (Build.VERSION.SDK_INT >= 28) {
            if (info.signingInfo?.hasMultipleSigners() == true) emptyArray() else
                info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        }
        val digest = if (signers?.size == 1)
            MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        else null
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        val supported = TraceProtocol.isTrustedIdentity(info.packageName, info.versionName, code, digest)
        Observation(supported, if (supported) "Exact v0.9.0/F-Droid certificate verified"
            else "Installed but version/certificate not in pinned compatibility set")
    } catch (_: PackageManager.NameNotFoundException) {
        Observation(false, "Not installed or hidden by package visibility")
    } catch (_: SecurityException) {
        Observation(false, "Package inspection blocked by Android")
    } catch (_: Exception) {
        Observation(false, "Installation identity unavailable")
    }
}
