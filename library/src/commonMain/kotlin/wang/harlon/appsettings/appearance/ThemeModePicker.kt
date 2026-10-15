package wang.harlon.appsettings.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import wang.harlon.appsettings.icons.AppSettingsIcons
import wang.harlon.appsettings.icons.BrightnessAuto
import wang.harlon.appsettings.icons.Contrast
import wang.harlon.appsettings.icons.DarkMode
import wang.harlon.appsettings.icons.LightMode

/**
 * 深浅模式选择：2×2 卡片而非扁扁的分段按钮。
 * 图标 + 文字的卡片把上半屏撑起来，也让「纯黑」这种需要解释的档位有地方放图标。只列 [AppearanceOptions.modes]。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeModePicker(state: AppearanceState, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2,
    ) {
        state.options.modes.forEach { mode ->
            ThemeModeCard(
                modifier = Modifier.weight(1f),
                mode = mode,
                selected = mode == state.appearance.mode,
                onClick = { state.setMode(mode) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeCard(
    mode: ThemeMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.semantics { role = Role.RadioButton },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(mode.icon(), contentDescription = null, tint = contentColor)
            Spacer(Modifier.height(8.dp))
            Text(text = mode.label(), style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

internal fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.FollowSystem -> AppSettingsIcons.BrightnessAuto
    ThemeMode.Light -> AppSettingsIcons.LightMode
    ThemeMode.Dark -> AppSettingsIcons.DarkMode
    ThemeMode.Amoled -> AppSettingsIcons.Contrast
}
