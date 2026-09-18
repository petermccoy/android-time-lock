package com.petermccoy.applock.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.petermccoy.applock.R
import com.petermccoy.applock.accessibility.LockAccessibilityService
import com.petermccoy.applock.data.InstalledAppInfo
import com.petermccoy.applock.data.LockedAppsRepository
import com.petermccoy.applock.session.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    lockedAppsRepository: LockedAppsRepository,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var lockedPackages by remember { mutableStateOf(lockedAppsRepository.getAllLocked()) }
    var showSystemApps by remember { mutableStateOf(false) }

    val accessibilityEnabled by rememberAccessibilityServiceEnabled(context)
    val activeUnlocks by rememberActiveUnlocks()

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
    }

    val visibleApps = remember(apps, showSystemApps) {
        if (showSystemApps) apps else apps.filter { it.isLaunchable }
    }
    val labelsByPackage = remember(apps) { apps.associateBy({ it.packageName }, { it.label }) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_list_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (!accessibilityEnabled) {
                AccessibilityPromptBanner(onOpenAccessibilitySettings = onOpenAccessibilitySettings)
            }

            if (activeUnlocks.isNotEmpty()) {
                ActiveUnlocksSection(activeUnlocks = activeUnlocks, labelsByPackage = labelsByPackage)
            }

            ScreenPinningTipCard(onOpenSecuritySettings = onOpenSecuritySettings)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_list_show_system_apps),
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = showSystemApps, onCheckedChange = { showSystemApps = it })
            }

            LazyColumn {
                items(visibleApps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        isLocked = lockedPackages.contains(app.packageName),
                        onToggle = { locked ->
                            lockedAppsRepository.setLocked(app.packageName, locked)
                            lockedPackages = lockedAppsRepository.getAllLocked()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessibilityPromptBanner(onOpenAccessibilitySettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.accessibility_disabled_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.accessibility_disabled_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.accessibility_disabled_button))
            }
        }
    }
}

@Composable
private fun ScreenPinningTipCard(onOpenSecuritySettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.screen_pinning_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.screen_pinning_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onOpenSecuritySettings,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.screen_pinning_button))
            }
        }
    }
}

@Composable
private fun ActiveUnlocksSection(
    activeUnlocks: Map<String, Long?>,
    labelsByPackage: Map<String, String>,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.active_unlocks_title),
            style = MaterialTheme.typography.titleSmall,
        )
        activeUnlocks.forEach { (packageName, remainingMs) ->
            val label = labelsByPackage[packageName] ?: packageName
            val statusText = if (remainingMs == null) {
                stringResource(R.string.active_unlocks_until_screen_off, label)
            } else {
                val remainingMinutes = ((remainingMs + 59_999L) / 60_000L).toInt()
                stringResource(R.string.active_unlocks_remaining, label, remainingMinutes)
            }
            Text(text = statusText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledAppInfo,
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isLocked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Checkbox(checked = isLocked, onCheckedChange = onToggle)
    }
}

@Composable
private fun rememberAccessibilityServiceEnabled(context: Context): State<Boolean> {
    val state = remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

@Composable
private fun rememberActiveUnlocks(): State<Map<String, Long?>> {
    return produceState(initialValue = SessionState.activeUnlocks()) {
        while (true) {
            value = SessionState.activeUnlocks()
            delay(15_000L)
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, LockAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        val component = ComponentName.unflattenFromString(splitter.next())
        if (component == expected) return true
    }
    return false
}

private fun loadInstalledApps(context: Context): List<InstalledAppInfo> {
    val pm = context.packageManager
    val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getInstalledApplications(0)
    }

    return installedApps
        .filter { it.packageName != context.packageName }
        .map { appInfo ->
            InstalledAppInfo(
                packageName = appInfo.packageName,
                label = pm.getApplicationLabel(appInfo).toString(),
                icon = pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap(),
                isLaunchable = pm.getLaunchIntentForPackage(appInfo.packageName) != null,
            )
        }
        .sortedBy { it.label.lowercase() }
}
