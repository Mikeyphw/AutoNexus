package io.github.mikeyphw.autonexus.termuxtasker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var imported by remember { mutableStateOf<TraceEvidence.Snapshot?>(null) }
    var importStatus by remember { mutableStateOf("No user-selected trace imported. Host acceptance UNKNOWN.") }
    val scope = rememberCoroutineScope()
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importStatus = "Reading explicitly selected evidence, without storing raw lines..."
            val result = withContext(Dispatchers.IO) { readSelectedEvidence(context, uri) }
            if (result == null) {
                imported = null
                importStatus = "Cannot read selected file with current permissions. No evidence retained."
            } else {
                imported = result
                importStatus = "Only bounded enum counts retained in memory; raw log discarded."
            }
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val report = imported
        if (uri != null && report != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(report.sanitizedReport().toByteArray(Charsets.UTF_8))
                        stream.flush()
                    } != null
                } catch (_: Exception) { false }
            }
            importStatus = if (ok) "Redacted enum-only report saved to selected document."
                           else "Export failed; no new Tasker or Termux state was modified."
        }
    }
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
            Text("TT-F0 · opt-in trace evidence", style = MaterialTheme.typography.titleMedium)
            Text("Select an existing LSPosed log export yourself. Only matching AutoNexus TT_TRACE " +
                 "enum events are summarized. Raw lines, scripts, arguments and output are never retained or exported.")
            OutlinedButton(onClick = { importer.launch(arrayOf("text/*", "application/octet-stream")) }) {
                Text("Import exported TT_TRACE log (read-only)")
            }
            Text(importStatus, style = MaterialTheme.typography.bodySmall)
            imported?.let { report ->
                Text("Accepted ${report.acceptedEvents} / inspected ${report.inspectedLines}; " +
                     "invalid ${report.rejectedTraceLines}; incomplete ${report.truncated}.")
                Text("Plugin background records: ${report.backgroundEvents}; " +
                     "plugin main-process records: ${report.editorEvents}.")
                Text(report.interpretation(), fontWeight = FontWeight.SemiBold)
                Text("Process-local events cannot be joined into a Tasker host execution ID. " +
                     "Root and Tasker-side tracing remain unverified; no command may be retried automatically.",
                     style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { exporter.launch("autonexus-tt-f0-evidence.txt") }) {
                    Text("Save redacted evidence report (explicit)")
                }
            }
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
            HorizontalDivider()
            Text("Boot-readiness · user-confirmed", fontWeight = FontWeight.SemiBold)
            Text(BootReadinessPolicy.explanation)
            Text(BootReadinessPolicy.taskerInstructions)
            Text("The observed boot race is still uncorrelated to a specific failed IPC request. " +
                 "No automatic replay, Tasker host hook or shell execution is enabled.",
                 style = MaterialTheme.typography.bodySmall)
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

/**
 * OpenDocument provides an explicit OS-granted one-file read. Avoid unbounded
 * memory/CPU, no root or private LSPosed directory access, no persistence of raw
 * bytes. A clipped file cannot support a conclusive negative observation.
 */
private fun readSelectedEvidence(context: Context, uri: Uri): TraceEvidence.Snapshot? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(8192)
        val captured = ByteArrayOutputStream()
        var clipped = false
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            val remaining = TraceEvidence.MAX_IMPORT_BYTES - captured.size()
            if (n > remaining) {
                if (remaining > 0) captured.write(buffer, 0, remaining)
                clipped = true
                break
            }
            captured.write(buffer, 0, n)
            if (captured.size() == TraceEvidence.MAX_IMPORT_BYTES) {
                clipped = input.read() != -1
                break
            }
        }
        TraceEvidence.analyze(captured.toString(Charsets.UTF_8.name()).lineSequence(), clipped)
    }
} catch (_: Exception) { null }
