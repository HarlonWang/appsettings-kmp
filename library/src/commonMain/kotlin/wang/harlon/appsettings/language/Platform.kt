package wang.harlon.appsettings.language

import androidx.compose.runtime.Composable

/** 系统语言的完整标签（`zh-Hans-CN`），解析跟随系统时用；与根上只取主语言的 `platformSystemLanguage()` 不同。 */
internal expect fun platformSystemLanguageTag(): String

/** 平台能否在 App 内切换语言：Android 走 per-app locale；iOS 只能去系统设置改。 */
internal expect val platformSupportsInAppLanguage: Boolean

/**
 * 挂在 Root 上：给 [LanguageState.platformApply] 接上平台实现，并在启动时把状态与系统里的实际生效语言对齐
 * （Android：系统设置里改过就采纳，没生效过就应用一次）。iOS 无事可做。
 */
@Composable
internal expect fun PlatformLanguageEffect(state: LanguageState)

/** 打开本 App 的系统设置页（iOS 在那里改 App 语言）。 */
internal expect fun openPlatformAppSettings()

/** 语言标签在它自己语言里的显示名（`zh` → 「中文」，`en` → 「English」）。 */
internal expect fun platformLanguageDisplayName(tag: String): String
