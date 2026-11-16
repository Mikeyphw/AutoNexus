package io.github.mikeyphw.autonexus

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mikeyphw.autonexus.core.Domain
import io.github.mikeyphw.autonexus.core.DomainRegistry
import io.github.mikeyphw.autonexus.core.DomainSettings
import io.github.mikeyphw.autonexus.core.DomainSettingsContract
import io.github.mikeyphw.autonexus.settings.DomainSettingsStore
import kotlinx.coroutines.launch

/** A dashboard for explicit future intent and read-only checks, not a hook switch. */
class MainActivity : ComponentActivity() {
    private data class PackageObservation(val summary: String, val details: String)

    @Suppress("DEPRECATION")
    private fun inspectPackage(domain: Domain): PackageObservation = try {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(domain.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageInfo(domain.packageName, 0)
        }
        val version = info.versionName ?: "unknown"
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        val matches = version == domain.versionName && code == domain.versionCode
        PackageObservation(
            if (matches) "Installed • reference matches" else "Installed • different version",
            "Observed $version ($code) · Reference ${domain.versionName} (${domain.versionCode})",
        )
    } catch (_: PackageManager.NameNotFoundException) {
        PackageObservation("Not installed or not visible", "Android package visibility cannot distinguish these cases.")
    } catch (_: SecurityException) {
        PackageObservation("Package inspection unavailable", "The operating system denied package visibility.")
    } catch (_: RuntimeException) {
        PackageObservation("Package inspection unavailable", "Package manager did not return a reliable result.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = DomainSettingsStore.from(this)
        setContent {
            val context = LocalContext.current
            val dark = isSystemInDarkTheme()
            val colors = when {
                Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
                Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = colors) {
                AutoNexusDashboard(store, ::inspectPackage)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AutoNexusDashboard(
        store: DomainSettingsStore,
        packageProbe: (Domain) -> PackageObservation,
    ) {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var refreshNonce by remember { mutableIntStateOf(0) }
        val host = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val failure: (String) -> Unit = { reason -> scope.launch { host.showSnackbar(reason) } }

        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("AutoNexus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("LSPosed companion • Safe-off", style = MaterialTheme.typography.labelMedium)
                    }
                })
            },
            snackbarHost = { SnackbarHost(host) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("◉") },
                        label = { Text("Domains") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("ⓘ") },
                        label = { Text("Diagnostics") },
                    )
                }
            },
        ) { insets ->
            Column(
                modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (selectedTab == 0) {
                    SafetyCard()
                    Text("Independent domains", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Preferences are staged only. They do not activate LSPosed behavior hooks.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    for (domain in DomainRegistry.domains) {
                        val observed = remember(domain, refreshNonce) { packageProbe(domain) }
                        DomainCard(domain, observed, store, failure)
                    }
                    OutlinedButton(onClick = { refreshNonce++ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Recheck installed packages")
                    }
                } else {
                    DiagnosticsCard(refreshNonce, onRefresh = { refreshNonce++ })
                    for (domain in DomainRegistry.domains) {
                        val observed = remember(domain, refreshNonce) { packageProbe(domain) }
                        val result = remember(domain, refreshNonce) { store.read(domain) }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(domain.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(observed.summary)
                                Text(observed.details, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (!result.readable) "Settings unavailable (safe default)"
                                    else "Requested: ${result.value.requestedEnabled} • Local details: ${result.value.localDiagnosticsEnabled}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    ValidationSteps()
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    @Composable
    private fun SafetyCard() {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SAFE-OFF · No behavior-changing hooks", style = MaterialTheme.typography.titleMedium)
                Text("No root execution, SystemUI injection, Tasker IPC rewriting, or permission bypass.")
                Text("All four original plugins remain unchanged. LSPosed scope activation must be verified in its manager.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun DomainCard(
        domain: Domain,
        observed: PackageObservation,
        store: DomainSettingsStore,
        onFailure: (String) -> Unit,
    ) {
        var result by remember(domain) { mutableStateOf(store.read(domain)) }
        val settings = result.value
        val enabled = result.readable
        fun save(next: DomainSettings) {
            if (store.save(domain, next)) result = DomainSettingsStore.ReadResult(next, true)
            else onFailure("Could not save ${domain.displayName}. Existing settings were preserved.")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(domain.displayName, style = MaterialTheme.typography.titleLarge)
                Text(domain.packageName, style = MaterialTheme.typography.bodySmall)
                Text(observed.summary, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Request future enhancements", style = MaterialTheme.typography.bodyMedium)
                        Text("Saved intent only • currently inactive", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.requestedEnabled,
                        onCheckedChange = { save(settings.copy(requestedEnabled = it)) },
                        enabled = enabled,
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show local diagnostics", style = MaterialTheme.typography.bodyMedium)
                        Text("Package metadata only • no logging", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.localDiagnosticsEnabled,
                        onCheckedChange = { save(settings.copy(localDiagnosticsEnabled = it)) },
                        enabled = enabled,
                    )
                }
                if (settings.localDiagnosticsEnabled && enabled) {
                    Text(observed.details, style = MaterialTheme.typography.bodySmall)
                }
                if (!enabled) {
                    Text("Preferences are unreadable or from a newer schema. Controls are disabled; no settings were overwritten.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    if (DomainSettingsContract.behaviorActive(domain, settings))
                        "Active" else "Runtime hook status: OFF (inert)",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    @Composable
    private fun DiagnosticsCard(refreshNonce: Int, onRefresh: () -> Unit) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Read-only device facts", style = MaterialTheme.typography.titleLarge)
                Text("Android API ${Build.VERSION.SDK_INT} • Android ${Build.VERSION.RELEASE}")
                Text("Behavior hooks: OFF • SystemUI: OFF • system_server: OFF • root: OFF")
                Text("LSPosed enabled/scope status: UNKNOWN from this app. Verify in the LSPosed manager.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Refresh number: $refreshNonce", style = MaterialTheme.typography.labelSmall)
                Button(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Refresh observed versions")
                }
            }
        }
    }

    @Composable
    private fun ValidationSteps() {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Physical LSPosed acceptance", style = MaterialTheme.typography.titleLarge)
                Text("1. Install the debug APK and confirm the AutoNexus dashboard opens.")
                Text("2. In LSPosed, enable AutoNexus only for the four named plugin packages. Do not add SystemUI or Android system.")
                Text("3. Relaunch each installed plugin; inspect LSPosed logs for its scoped inert load marker (no notification or Tasker contents).")
                Text("4. Restart AutoNexus. Verify each staged preference persisted independently, and that all runtime hooks remain off.")
                Text("5. Test normal plugin behavior. Record any crash or missing marker without retrying commands with side effects.")
                HorizontalDivider()
                Text("This screen is a checklist, not a device-test result. A successful hosted build does not prove module injection or plugin compatibility.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
