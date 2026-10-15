package wang.harlon.appsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.appearance.label
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_about_title
import wang.harlon.appsettings.resources.appsettings_appearance_title
import wang.harlon.appsettings.resources.appsettings_general
import wang.harlon.appsettings.resources.appsettings_settings_title
import wang.harlon.appsettings.ui.LocalSettingsPageOpener
import wang.harlon.appsettings.ui.SettingsDefaults
import wang.harlon.appsettings.ui.SettingsGroup
import wang.harlon.appsettings.ui.SettingsGroupScope
import wang.harlon.appsettings.ui.SettingsPage

/**
 * 设置页框架：壳、滚动容器、页内间距由库给，组与行由宿主在 [content] 里摆。
 * 通用行用 [appearanceItem] / [aboutItem]（或整组 [GeneralSettingsGroup]），点击经 [onOpen] 交给宿主导航。
 *
 * ```
 * SettingsScreen(onBack = …, onOpen = { backStack.add(it.route()) }) {
 *     SettingsGroup(title = "账户") { settingsItem(…) }
 *     GeneralSettingsGroup()
 * }
 * ```
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSettingsScaffold(
        title = title ?: stringResource(Res.string.appsettings_settings_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHost = snackbarHost,
    ) { padding ->
        CompositionLocalProvider(LocalSettingsPageOpener provides onOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SettingsDefaults.ContentHorizontalPadding),
            ) {
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 「外观」入口行：图标、标题、当前模式的 trailing 都自带。[onClick] 不给就走 [SettingsScreen] 的 `onOpen`。 */
fun SettingsGroupScope.appearanceItem(onClick: (() -> Unit)? = null) {
    val open = onClick ?: openPage?.let { opener -> { opener(SettingsPage.Appearance) } }
    settingsItem(
        icon = Icons.Default.Palette,
        title = { Text(stringResource(Res.string.appsettings_appearance_title)) },
        trailing = {
            Text(
                text = LocalAppSettings.current.appearance.mode.label(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = open,
    )
}

/** 「关于」入口行。 */
fun SettingsGroupScope.aboutItem(onClick: (() -> Unit)? = null) {
    val open = onClick ?: openPage?.let { opener -> { opener(SettingsPage.About) } }
    settingsItem(
        icon = Icons.Default.Info,
        title = { Text(stringResource(Res.string.appsettings_about_title)) },
        onClick = open,
    )
}

/** 通用组：外观 + 关于，给不在乎位置的 App；要把通用行插进自己的组里就直接用 [appearanceItem] / [aboutItem]。 */
@Composable
fun GeneralSettingsGroup(modifier: Modifier = Modifier, title: String? = stringResource(Res.string.appsettings_general)) {
    SettingsGroup(modifier = modifier, title = title) {
        appearanceItem()
        aboutItem()
    }
}
