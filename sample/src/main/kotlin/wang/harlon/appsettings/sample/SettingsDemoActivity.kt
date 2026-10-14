package wang.harlon.appsettings.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Palette
import wang.harlon.appsettings.appearance.AppearanceContent
import wang.harlon.appsettings.appearance.AppearanceState
import wang.harlon.appsettings.appearance.AppearanceTheme
import wang.harlon.appsettings.appearance.ColorLabContent
import wang.harlon.appsettings.appearance.LocalAppearanceState
import wang.harlon.appsettings.appearance.label
import wang.harlon.appsettings.appearance.rememberAppearanceState
import wang.harlon.appsettings.appearance.rememberColorLabSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import wang.harlon.appsettings.about.AboutContent
import wang.harlon.appsettings.about.AboutLink
import wang.harlon.appsettings.about.UpdateStatus
import wang.harlon.appsettings.ui.InfoDialog
import wang.harlon.appsettings.ui.SettingsBottomSheet
import wang.harlon.appsettings.ui.SettingsDefaults
import wang.harlon.appsettings.ui.SettingsDropdownMenu
import wang.harlon.appsettings.ui.SettingsGroup
import wang.harlon.appsettings.whatsnew.WhatsNewHost
import wang.harlon.appsettings.whatsnew.rememberWhatsNewState
import androidx.compose.ui.platform.LocalContext
import com.russhwolf.settings.SharedPreferencesSettings
import java.util.Locale

private const val DEMO_VERSION = "1.0.0"
private const val DEMO_WHATS_NEW = """{"version":"1.0.0","zh":["演示：升级后首启弹一次"],"en":["Demo: shown once after an upgrade"]}"""

/** 把 ui 包的四个基元各摆一遍：卡片组、开关行、下拉行、浮层行、说明弹窗；根上挂一次更新说明。 */
class SettingsDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 演示用：每次启动都伪装成「从 0.9.0 升上来」，让更新说明每次都弹。真实 App 不要碰这个 key。
        getSharedPreferences("demo", MODE_PRIVATE).edit().putString("appsettings.whatsnew.lastSeen", "0.9.0").apply()
        setContent {
            val settings = remember { SharedPreferencesSettings(getSharedPreferences("demo", MODE_PRIVATE)) }
            // 外观状态挂在根上：深浅、配色、动画与系统栏都由它管
            val appearance = rememberAppearanceState(settings)
            AppearanceTheme(appearance) {
                DemoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreen() {
    var toggle by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var choice by remember { mutableStateOf("A") }
    var showSheet by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showColorLab by remember { mutableStateOf(false) }
    val appearance = LocalAppearanceState.current
    when {
        showColorLab -> { ColorLabDemo(appearance, onBack = { showColorLab = false }); return }
        showAppearance -> { AppearanceDemo(appearance, onBack = { showAppearance = false }, onOpenColorLab = { showColorLab = true }); return }
        showAbout -> { AboutDemo(onBack = { showAbout = false }); return }
    }

    val context = LocalContext.current
    val settings = remember { SharedPreferencesSettings(context.getSharedPreferences("demo", 0)) }
    val whatsNew = rememberWhatsNewState(settings, currentVersion = DEMO_VERSION, load = { DEMO_WHATS_NEW })
    WhatsNewHost(whatsNew, language = Locale.getDefault().language)

    Scaffold(topBar = { TopAppBar(title = { Text("AppSettings Demo") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingsGroup(
                    title = "Group with controls",
                    modifier = Modifier.padding(horizontal = SettingsDefaults.ContentHorizontalPadding),
                ) {
                    settingsItem(
                        icon = Icons.Default.Palette,
                        title = { Text("Appearance") },
                        trailing = { Text(appearance.appearance.mode.label(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { showAppearance = true },
                    )
                    settingsItem(
                        icon = Icons.Default.Notifications,
                        title = { Text("Switch row") },
                        description = { Text("Row click and the switch both toggle") },
                        trailing = { Switch(checked = toggle, onCheckedChange = { toggle = it }) },
                        onClick = { toggle = !toggle },
                    )
                    settingsItem(
                        icon = Icons.Default.Menu,
                        title = { Text("Dropdown row") },
                        trailing = {
                            Box {
                                Text(choice, color = MaterialTheme.colorScheme.primary)
                                SettingsDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    listOf("A", "B", "C").forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = { choice = option; menuExpanded = false })
                                    }
                                }
                            }
                        },
                        onClick = { menuExpanded = true },
                    )
                    settingsItem(
                        icon = Icons.Default.Settings,
                        title = { Text("Bottom sheet row") },
                        onClick = { showSheet = true },
                    )
                }
            }
            item {
                SettingsGroup(
                    title = "Group with states",
                    modifier = Modifier.padding(horizontal = SettingsDefaults.ContentHorizontalPadding),
                ) {
                    settingsItem(icon = Icons.Default.Info, title = { Text("Info dialog") }, onClick = { showInfo = true })
                    settingsItem(icon = Icons.Default.Info, title = { Text("About page") }, onClick = { showAbout = true })
                    settingsItem(icon = Icons.Default.Info, title = { Text("Disabled row") }, enabled = false, onClick = {})
                    settingsItem(title = { Text("No icon, not clickable") }, description = { Text("Carries no button semantics") })
                }
            }
        }
    }

    if (showSheet) {
        SettingsBottomSheet(onDismissRequest = { showSheet = false }, title = "Pick one") {
            SettingsGroup {
                listOf("A", "B", "C").forEach { option ->
                    settingsItem(title = { Text(option) }, onClick = { choice = option; showSheet = false })
                }
            }
        }
    }
    if (showInfo) {
        InfoDialog(title = "About this row", content = "A plain explanation with a single dismiss action.", onDismiss = { showInfo = false })
    }
}

/** 关于页：内置链接 + 检查更新行（状态轮转）+ 自定义行。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutDemo(onBack: () -> Unit) {
    var status by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var taps by remember { mutableStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        AboutContent(
            name = "AppSettings Demo",
            version = if (taps >= 7) "$DEMO_VERSION (developer)" else DEMO_VERSION,
            modifier = Modifier.padding(padding),
            onVersionClick = { taps++ },
        ) {
            group {
                updateCheck(status) {
                    status = when (status) {
                        UpdateStatus.Idle -> UpdateStatus.Checking
                        UpdateStatus.Checking -> UpdateStatus.UpToDate
                        UpdateStatus.UpToDate -> UpdateStatus.Failed
                        UpdateStatus.Failed -> UpdateStatus.Available("2.0.0")
                        is UpdateStatus.Available -> UpdateStatus.Idle
                    }
                }
                link(AboutLink.Changelog("https://example.com/changelog"))
                link(AboutLink.Website("https://example.com"))
                link(AboutLink.Privacy("https://example.com/privacy"))
                link(AboutLink.Terms("https://example.com/terms"))
                link(AboutLink.Feedback("https://example.com/feedback"))
                link(AboutLink.Licenses("https://example.com/licenses"))
            }
            group(title = "Custom") {
                item(icon = Icons.Default.Settings, title = { Text("Host-specific row") }, trailing = { Text("staging") }, onClick = {})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceDemo(state: AppearanceState, onBack: () -> Unit, onOpenColorLab: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        AppearanceContent(state, modifier = Modifier.padding(padding), onOpenColorLab = onOpenColorLab)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorLabDemo(state: AppearanceState, onBack: () -> Unit) {
    val session = rememberColorLabSession(state)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Color") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = session::undo, enabled = session.isDirty) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Discard changes")
                    }
                },
            )
        },
    ) { padding ->
        ColorLabContent(session, modifier = Modifier.padding(padding))
    }
}
