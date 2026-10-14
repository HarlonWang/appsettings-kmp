package wang.harlon.appsettings.ui

import androidx.compose.ui.unit.dp

/**
 * 设置 UI 基元的规格常量。
 *
 * 先不做成可覆盖的 token：现有宿主要的是同一套观感；以后要开放时加 CompositionLocal 不破坏兼容。
 */
object SettingsDefaults {
    /** 卡片组首尾、底部浮层、下拉菜单共用的大圆角。 */
    val LargeCorner = 24.dp

    /** 卡片组中间项的小圆角。 */
    val SmallCorner = 6.dp

    /** 卡片组内项与项之间的缝。 */
    val ItemGap = 4.dp

    /** 行首图标容器尺寸。 */
    val IconContainerSize = 40.dp

    /** 行首图标尺寸。 */
    val IconSize = 24.dp

    /** 整页内容两侧的留白，页面把它传给 [SettingsGroup] 的 modifier。 */
    val ContentHorizontalPadding = 16.dp

    /** 禁用态前景 alpha。 */
    const val DisabledAlpha = 0.38f
}
