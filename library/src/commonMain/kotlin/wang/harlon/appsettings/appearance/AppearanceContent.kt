package wang.harlon.appsettings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.icons.AppSettingsIcons
import wang.harlon.appsettings.icons.Apps
import wang.harlon.appsettings.icons.DarkMode
import wang.harlon.appsettings.icons.Palette
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_app_icon_section
import wang.harlon.appsettings.resources.appsettings_theme_color_section
import wang.harlon.appsettings.resources.appsettings_theme_mode_section
import wang.harlon.appsettings.ui.SettingsDefaults

/**
 * 外观页整页内容，自带滚动：深浅模式 → 配色色板（[ColorSource.Seed] 才有）→ 动态图标（[icons] 支持才有）→ [footer]。
 * 顶栏、返回键由宿主套；调色台是独立页面，进入由宿主导航。
 */
@Composable
internal fun AppearanceContent(
    state: AppearanceState,
    modifier: Modifier = Modifier,
    onOpenColorLab: () -> Unit = {},
    icons: AppIconState? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SettingsDefaults.ContentHorizontalPadding, vertical = 12.dp),
    ) {
        SectionHeader(AppSettingsIcons.DarkMode, stringResource(Res.string.appsettings_theme_mode_section))
        ThemeModePicker(state)

        if (state.options.colorSource is ColorSource.Seed) {
            Spacer(Modifier.height(SectionGap))
            SectionHeader(AppSettingsIcons.Palette, stringResource(Res.string.appsettings_theme_color_section))
            PalettePicker(state, onOpenColorLab = onOpenColorLab)
        }

        if (icons != null && icons.isSupported) {
            Spacer(Modifier.height(SectionGap))
            SectionHeader(AppSettingsIcons.Apps, stringResource(Res.string.appsettings_app_icon_section))
            AppIconPicker(icons)
        }

        footer?.let {
            Spacer(Modifier.height(SectionGap))
            it()
        }
    }
}

/** 节标题：图标 + 文字一行，宿主往 footer 里加自己的节时也可以用它对齐。 */
@Composable
internal fun SectionHeader(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(bottom = 12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

private val SectionGap = 20.dp
