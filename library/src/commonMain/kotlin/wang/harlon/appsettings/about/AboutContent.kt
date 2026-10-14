package wang.harlon.appsettings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.icons.AppSettingsIcons
import wang.harlon.appsettings.icons.Article
import wang.harlon.appsettings.icons.Description
import wang.harlon.appsettings.icons.Feedback
import wang.harlon.appsettings.icons.History
import wang.harlon.appsettings.icons.PrivacyTip
import wang.harlon.appsettings.icons.Public
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_about_changelog
import wang.harlon.appsettings.resources.appsettings_about_check_failed
import wang.harlon.appsettings.resources.appsettings_about_check_updates
import wang.harlon.appsettings.resources.appsettings_about_feedback
import wang.harlon.appsettings.resources.appsettings_about_licenses
import wang.harlon.appsettings.resources.appsettings_about_privacy
import wang.harlon.appsettings.resources.appsettings_about_terms
import wang.harlon.appsettings.resources.appsettings_about_up_to_date
import wang.harlon.appsettings.resources.appsettings_about_update_available
import wang.harlon.appsettings.resources.appsettings_about_website
import wang.harlon.appsettings.ui.SettingsDefaults
import wang.harlon.appsettings.ui.SettingsGroup
import wang.harlon.appsettings.ui.SettingsGroupScope

/**
 * 关于页整页内容，自带滚动：头部（应用名、版本号、可选图标）+ 若干卡片组。顶栏、返回键由宿主套。
 *
 * ```
 * AboutContent(name = "My App", version = appVersion) {
 *     group {
 *         updateCheck(status = updateStatus) { checker.manualCheck() }
 *         link(AboutLink.Changelog(CHANGELOG_URL))
 *         link(AboutLink.Privacy(PRIVACY_URL))
 *         item(icon = Icons.Default.VolunteerActivism, title = { Text("捐助") }) { showDonate = true }
 *     }
 * }
 * ```
 *
 * @param onVersionClick 点版本号；隐藏开发者入口的计数与阈值在宿主
 * @param onLinkClick 所有 [AboutLink] 的统一出口，宿主在这里决定打开方式并埋点；null 时用 `LocalUriHandler`（系统浏览器）
 */
@Composable
fun AboutContent(
    name: String,
    version: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onVersionClick: (() -> Unit)? = null,
    onLinkClick: ((AboutLink) -> Unit)? = null,
    content: AboutScope.() -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val openLink: (AboutLink) -> Unit = onLinkClick ?: { link -> uriHandler.openUri(link.url) }
    val groups = AboutScope().apply(content).groups

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AboutHeader(name = name, version = version, icon = icon, onVersionClick = onVersionClick)
        groups.forEachIndexed { index, group ->
            // 有标题的组自带上边距；无标题的组之间补一段，别让两块卡片贴在一起
            if (index > 0 && group.title == null) Spacer(Modifier.height(GroupGap))
            SettingsGroup(
                modifier = Modifier.padding(horizontal = SettingsDefaults.ContentHorizontalPadding),
                title = group.title,
            ) {
                group.content(AboutGroupScope(this, openLink))
            }
        }
        Spacer(Modifier.height(BottomPadding))
    }
}

/** 头部：居中的应用名 + 版本号，可选图标在上方。单独可用，宿主想自己排版时拿它拼。 */
@Composable
fun AboutHeader(
    name: String,
    version: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onVersionClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = HeaderVerticalPadding),
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.height(16.dp))
        }
        Text(text = name, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            text = version,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // 隐藏入口不要水波纹，否则连点时像个按钮
            modifier = if (onVersionClick != null) {
                Modifier.clickable(interactionSource = null, indication = null, onClick = onVersionClick)
            } else {
                Modifier
            },
        )
    }
}

/** 收集关于页的卡片组。 */
class AboutScope internal constructor() {
    internal class Group(val title: String?, val content: AboutGroupScope.() -> Unit)

    internal val groups = mutableListOf<Group>()

    fun group(title: String? = null, content: AboutGroupScope.() -> Unit) {
        groups += Group(title, content)
    }
}

/** 一个卡片组里的行：内置链接、检查更新、任意自定义行。 */
class AboutGroupScope internal constructor(
    private val inner: SettingsGroupScope,
    private val openLink: (AboutLink) -> Unit,
) {
    /** 一条链接。内置类型的标题与图标可覆盖。 */
    fun link(
        link: AboutLink,
        title: (@Composable () -> String)? = null,
        icon: ImageVector? = null,
    ) {
        inner.settingsItem(
            icon = icon ?: link.defaultIcon(),
            title = { Text(title?.invoke() ?: link.defaultLabel()) },
            onClick = { openLink(link) },
        )
    }

    /** 检查更新行。[UpdateStatus.Checking] 时自动不可点。 */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun updateCheck(status: UpdateStatus, onClick: () -> Unit) {
        inner.settingsItem(
            icon = Icons.Filled.Refresh,
            title = { Text(stringResource(Res.string.appsettings_about_check_updates)) },
            enabled = status !is UpdateStatus.Checking,
            trailing = {
                when (status) {
                    UpdateStatus.Idle -> Unit
                    UpdateStatus.Checking -> LoadingIndicator(modifier = Modifier.size(24.dp))
                    UpdateStatus.UpToDate -> Text(
                        stringResource(Res.string.appsettings_about_up_to_date),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    UpdateStatus.Failed -> Text(
                        stringResource(Res.string.appsettings_about_check_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    is UpdateStatus.Available -> Text(
                        stringResource(Res.string.appsettings_about_update_available, status.version),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            onClick = onClick,
        )
    }

    /** 任意自定义行，签名同 [SettingsGroupScope.settingsItem]。赞助、调试开关这类 App 私有行走这里。 */
    fun item(
        icon: ImageVector? = null,
        leading: (@Composable () -> Unit)? = null,
        title: @Composable () -> Unit,
        description: (@Composable () -> Unit)? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
    ) {
        inner.settingsItem(icon, leading, title, description, enabled, onClick, trailing)
    }
}

@Composable
private fun AboutLink.defaultLabel(): String = when (this) {
    is AboutLink.Privacy -> stringResource(Res.string.appsettings_about_privacy)
    is AboutLink.Terms -> stringResource(Res.string.appsettings_about_terms)
    is AboutLink.Website -> stringResource(Res.string.appsettings_about_website)
    is AboutLink.Changelog -> stringResource(Res.string.appsettings_about_changelog)
    is AboutLink.Feedback -> stringResource(Res.string.appsettings_about_feedback)
    is AboutLink.Licenses -> stringResource(Res.string.appsettings_about_licenses)
    is AboutLink.Custom -> label()
}

private fun AboutLink.defaultIcon(): ImageVector? = when (this) {
    is AboutLink.Privacy -> AppSettingsIcons.PrivacyTip
    is AboutLink.Terms -> AppSettingsIcons.Description
    is AboutLink.Website -> AppSettingsIcons.Public
    is AboutLink.Changelog -> AppSettingsIcons.History
    is AboutLink.Feedback -> AppSettingsIcons.Feedback
    is AboutLink.Licenses -> AppSettingsIcons.Article
    is AboutLink.Custom -> icon
}

private val HeaderVerticalPadding = 32.dp
private val GroupGap = 16.dp
private val BottomPadding = 24.dp
