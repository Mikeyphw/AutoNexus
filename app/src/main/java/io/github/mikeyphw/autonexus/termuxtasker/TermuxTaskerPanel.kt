package io.github.mikeyphw.autonexus.termuxtasker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Diagnostics only: does not start services, execute scripts or grant permissions. */
@Composable
fun TermuxTaskerPanel() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val facts = remember(refresh) { Readiness.inspect(context) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Termux:Tasker · communication", style = MaterialTheme.typography.titleLarge)
            Text("TT-E1 · enhanced editor + TT-F0 read-only trace; exact v0.9.0/F-Droid identity",
                 style = MaterialTheme.typography.labelMedium)
            Text("The original Tasker action serializer remains unchanged. " +
                 "The enhanced editor has Save, Discard and Original fallback. No automatic script replay or callback retry.")
            HorizontalDivider()
            Text("Observed device state", fontWeight = FontWeight.SemiBold)
            Text("Plugin: ${facts.plugin}")
            Text("Tasker host: ${facts.tasker}")
            Text("Termux: ${facts.termux}")
            Text("Tasker RUN_COMMAND grant: ${facts.runCommand}")
            Text("User unlocked: ${facts.unlocked}")
            Text("Termux script-directory access: UNKNOWN from the companion UID")
            OutlinedButton(onClick = { refresh++ }) { Text("Refresh package and permission checks") }
            HorizontalDivider()
            Text("Trace pipeline", fontWeight = FontWeight.SemiBold)
            Text("1. Receiver entry/return (separate :background process)")
            Text("2. Execution service dispatch attempted / helper returned")
            Text("3. Plugin result service received and helper returned")
            Text("4. Tasker signalFinish() attempted and returned true/false/exception")
            Text("5. Tasker host accepted the callback: UNKNOWN without host-side evidence")
            Text("Events appear only in LSPosed's log as AutoNexus TT_TRACE when its scope " +
                 "is enabled and the installed APK identity is supported. This app cannot " +
                 "read LSPosed logs or establish cross-process request correlation.",
                 style = MaterialTheme.typography.bodySmall)
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Safely reproduce the non-response", style = MaterialTheme.typography.titleMedium)
            Text("1. In LSPosed, scope AutoNexus to com.termux.tasker only for this diagnostic. " +
                 "Restart both plugin processes after updating the module.")
            Text("2. Run ONE known non-destructive existing Tasker action; do not repeat " +
                 "actions that could have executed already.")
            Text("3. Compare host error time with TT_TRACE stages in LSPosed logs. " +
                 "Missing receiver events alone cannot prove Tasker failed to dispatch.")
            Text("4. Record whether failure is before first unlock, just after unlock, " +
                 "or normal operation. Check the Tasker permission without granting it automatically.")
            Text("The editor changes a Tasker action only after explicit Save. No IPC repair, " +
                 "new Tasker bundle keys, SystemUI scope, root execution or permission bypass.",
                 style = MaterialTheme.typography.bodySmall)
        }
    }
}

private object Readiness {
    data class Facts(val plugin: String, val tasker: String, val termux: String,
                     val runCommand: String, val unlocked: String)

    @Suppress("DEPRECATION")
    private fun packageLabel(context: Context, name: String): String = try {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(name, PackageManager.PackageInfoFlags.of(0))
        } else context.packageManager.getPackageInfo(name, 0)
        "Installed (${info.versionName ?: "version unknown"})"
    } catch (_: PackageManager.NameNotFoundException) {
        "Not installed or not visible"
    } catch (_: Exception) {
        "UNKNOWN (package lookup failed)"
    }

    fun inspect(context: Context): Facts {
        val taskerPackage = "net.dinglisch.android.taskerm"
        val tasker = packageLabel(context, taskerPackage)
        val grant = if (!tasker.startsWith("Installed")) "UNKNOWN (Tasker identity unavailable)"
        else try {
            if (context.packageManager.checkPermission("com.termux.permission.RUN_COMMAND", taskerPackage)
                == PackageManager.PERMISSION_GRANTED) "Granted to Tasker" else "Not granted to Tasker"
        } catch (_: Exception) { "UNKNOWN (permission query failed)" }
        val unlocked = try {
            val manager = context.getSystemService(UserManager::class.java)
            if (manager?.isUserUnlocked == true) "Yes" else if (manager == null) "UNKNOWN" else "No"
        } catch (_: Exception) { "UNKNOWN" }
        return Facts(TrustedTarget.inspect(context).label,
                     tasker, packageLabel(context, "com.termux"), grant, unlocked)
    }
}
