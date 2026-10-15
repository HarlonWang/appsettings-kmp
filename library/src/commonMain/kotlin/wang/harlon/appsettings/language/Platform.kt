package wang.harlon.appsettings.language

import androidx.compose.runtime.Composable

/** 系统语言的完整标签（`zh-Hans-CN`），解析跟随系统时用；与根上只取主语言的 `platformSystemLanguage()` 不同。 */
internal expect fun platformSystemLanguageTag(): String

/** 平台能否在 App 内切换语言：Android 走 per-app locale；iOS 只能去系统设置改。 */
internal expect val platformSupportsInAppLanguage: Boolean

/** 把选择应用到平台。Android 调 `AppCompatDelegate.setApplicationLocales`（同值不重建）；iOS 无事可做。 */
@Composable
internal expect fun PlatformApplyLanguageEffect(language: AppLanguage)

/** 打开本 App 的系统设置页（iOS 在那里改 App 语言）。 */
internal expect fun openPlatformAppSettings()

/** 语言标签在它自己语言里的显示名（`zh` → 「中文」，`en` → 「English」）。 */
internal expect fun platformLanguageDisplayName(tag: String): String
