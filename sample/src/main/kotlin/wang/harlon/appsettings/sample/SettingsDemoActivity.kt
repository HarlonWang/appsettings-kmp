package wang.harlon.appsettings.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import wang.harlon.appsettings.AboutScreen
import wang.harlon.appsettings.AppSettingsRoot
import wang.harlon.appsettings.AppearanceScreen
import wang.harlon.appsettings.ColorLabScreen
import wang.harlon.appsettings.GeneralSettingsGroup
import wang.harlon.appsettings.SettingsScreen
import wang.harlon.appsettings.about.UpdateStatus
import wang.harlon.appsettings.rememberAppSettings
import wang.harlon.appsettings.ui.SettingsGroup
import wang.harlon.appsettings.ui.SettingsPage

private const val DEMO_WHATS_NEW = """{"version":"1.0.0","zh":["演示：升级后首启弹一次"],"en":["Demo: shown once after an upgrade"]}"""

/**
 * 最小宿主：根上一个配置对象 + 一个 Root，设置页用库的框架摆几行，三个二级页直接用库的整页屏。
 * 导航用一个枚举状态代替，真实 App 换成自己的路由即可。
 */
class SettingsDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 演示用：每次启动都伪装成「从 0.9.0 升上来」，让更新说明每次都弹。
        // 库自建的 Settings 落在 <包名>_preferences；真实 App 不要碰这个 key。
        getSharedPreferences("${packageName}_preferences", MODE_PRIVATE).edit()
            .putString("appsettings.whatsnew.lastSeen", "0.9.0").apply()

        setContent {
            var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
            val appSettings = rememberAppSettings {
                appVersion = "1.0.0"
                about {
                    changelog = "https://example.com/changelog"
                    website = "https://example.com"
                    privacy = "https://example.com/privacy"
                    terms = "https://example.com/terms"
                    // 检查更新：逻辑在宿主，这里只是轮转状态演示各种 trailing
                    checkUpdates(status = { updateStatus }) { updateStatus = updateStatus.next() }
                }
                whatsNew { bundled = { DEMO_WHATS_NEW } }
                // 有自己顶栏风格的 App 改这一行即可：
                // topBar { containerColor = { MaterialTheme.colorScheme.surfaceContainer } }
            }
            AppSettingsRoot(appSettings) {
                DemoNavigation(onExit = ::finish)
            }
        }
    }
}

private enum class DemoPage { Settings, Appearance, ColorLab, About }

@Composable
private fun DemoNavigation(onExit: () -> Unit) {
    var page by remember { mutableStateOf(DemoPage.Settings) }
    var toggle by remember { mutableStateOf(true) }
    var versionTaps by remember { mutableIntStateOf(0) }
    BackHandler(enabled = page != DemoPage.Settings) {
        page = if (page == DemoPage.ColorLab) DemoPage.Appearance else DemoPage.Settings
    }

    when (page) {
        DemoPage.Settings -> SettingsScreen(
            onBack = onExit,
            onOpen = { target ->
                page = when (target) {
                    SettingsPage.Appearance -> DemoPage.Appearance
                    SettingsPage.About -> DemoPage.About
                }
            },
        ) {
            // 宿主自己的业务行，与库的行同一套卡片
            SettingsGroup(title = "Demo") {
                settingsItem(
                    icon = Icons.Default.Notifications,
                    title = { Text("Switch row") },
                    description = { Text("Row click and the switch both toggle") },
                    trailing = { Switch(checked = toggle, onCheckedChange = { toggle = it }) },
                    onClick = { toggle = !toggle },
                )
                settingsItem(icon = Icons.Default.Settings, title = { Text("Disabled row") }, enabled = false, onClick = {})
            }
            GeneralSettingsGroup()
        }
        DemoPage.Appearance -> AppearanceScreen(onBack = { page = DemoPage.Settings }, onOpenColorLab = { page = DemoPage.ColorLab })
        DemoPage.ColorLab -> ColorLabScreen(onBack = { page = DemoPage.Appearance })
        DemoPage.About -> AboutScreen(onBack = { page = DemoPage.Settings }, onVersionClick = { versionTaps++ }) {
            // 隐藏开发者入口：连点版本号 7 次
            if (versionTaps >= 7) item(icon = Icons.Default.Settings, title = { Text("Developer row") }, trailing = { Text("staging") }, onClick = {})
        }
    }
}

private fun UpdateStatus.next(): UpdateStatus = when (this) {
    UpdateStatus.Idle -> UpdateStatus.Checking
    UpdateStatus.Checking -> UpdateStatus.UpToDate
    UpdateStatus.UpToDate -> UpdateStatus.Failed
    UpdateStatus.Failed -> UpdateStatus.Available("2.0.0")
    is UpdateStatus.Available -> UpdateStatus.Idle
}
