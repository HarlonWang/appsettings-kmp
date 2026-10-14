package wang.harlon.appsettings.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * 统一的下拉菜单：固定大圆角（[SettingsDefaults.LargeCorner]），与卡片组同一套圆角语言。
 * 需要特殊容器色 / elevation 时传参，别绕开它自己写 `DropdownMenu`。
 */
@Composable
fun SettingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(SettingsDefaults.LargeCorner),
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        content = content,
    )
}
