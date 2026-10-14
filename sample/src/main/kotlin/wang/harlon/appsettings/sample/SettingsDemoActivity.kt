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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import wang.harlon.appsettings.ui.InfoDialog
import wang.harlon.appsettings.ui.SettingsBottomSheet
import wang.harlon.appsettings.ui.SettingsDefaults
import wang.harlon.appsettings.ui.SettingsDropdownMenu
import wang.harlon.appsettings.ui.SettingsGroup

/** 把 ui 包的四个基元各摆一遍：卡片组、开关行、下拉行、浮层行、说明弹窗。 */
class SettingsDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
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

    Scaffold(topBar = { TopAppBar(title = { Text("AppSettings Demo") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingsGroup(
                    title = "Group with controls",
                    modifier = Modifier.padding(horizontal = SettingsDefaults.ContentHorizontalPadding),
                ) {
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
