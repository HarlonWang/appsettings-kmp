package wang.harlon.appsettings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.about.AboutContent
import wang.harlon.appsettings.about.AboutGroupScope
import wang.harlon.appsettings.appearance.AppearanceContent
import wang.harlon.appsettings.appearance.ColorLabContent
import wang.harlon.appsettings.appearance.rememberColorLabSession
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_about_title
import wang.harlon.appsettings.resources.appsettings_appearance_title
import wang.harlon.appsettings.resources.appsettings_back
import wang.harlon.appsettings.resources.appsettings_color_lab_discard
import wang.harlon.appsettings.resources.appsettings_color_lab_title

/** 外观页：深浅模式、配色、动态图标。[onOpenColorLab] 只在允许自定义色时会被调到。 */
@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    onOpenColorLab: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val appSettings = LocalAppSettings.current
    AppSettingsScaffold(title = stringResource(Res.string.appsettings_appearance_title), onBack = onBack, modifier = modifier) { padding ->
        AppearanceContent(
            state = appSettings.appearanceState,
            modifier = Modifier.padding(padding),
            onOpenColorLab = onOpenColorLab,
            icons = appSettings.appIconState,
        )
    }
}

/** 调色台：实时生效，顶栏撤销按钮整体回到进入时的状态，离开自动落定。 */
@Composable
fun ColorLabScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val session = rememberColorLabSession(LocalAppSettings.current.appearanceState)
    AppSettingsScaffold(
        title = stringResource(Res.string.appsettings_color_lab_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = session::undo, enabled = session.isDirty) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(Res.string.appsettings_color_lab_discard))
            }
        },
    ) { padding ->
        ColorLabContent(session, modifier = Modifier.padding(padding))
    }
}

/**
 * 关于页：应用名、版本号，配置里给了的链接，以及 [extraItems] 追加的行（赞助、调试开关这类 App 私有行）。
 * @param onVersionClick 点版本号；隐藏开发者入口的计数在宿主
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onVersionClick: (() -> Unit)? = null,
    extraItems: (AboutGroupScope.() -> Unit)? = null,
) {
    val appSettings = LocalAppSettings.current
    val about = appSettings.config.about
    val updateCheck = about.updateCheck
    val status = updateCheck?.status?.invoke()
    AppSettingsScaffold(title = stringResource(Res.string.appsettings_about_title), onBack = onBack, modifier = modifier) { padding ->
        AboutContent(
            name = appSettings.aboutName,
            version = appSettings.aboutVersion,
            modifier = Modifier.padding(padding),
            icon = about.icon,
            onVersionClick = onVersionClick,
            onLinkClick = about.onLinkClick,
        ) {
            group {
                if (updateCheck != null && status != null) updateCheck(status, updateCheck.onClick)
                about.resolvedLinks().forEach { link(it) }
                extraItems?.invoke(this)
            }
        }
    }
}

/** 库自带页面的壳：M3 Scaffold + TopAppBar + 返回键，顶栏颜色来自 `topBar { containerColor }`。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val containerColor = LocalAppSettings.current.config.topBar.containerColor?.invoke()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.appsettings_back))
                    }
                },
                actions = actions,
                colors = if (containerColor != null) {
                    TopAppBarDefaults.topAppBarColors(containerColor = containerColor, scrolledContainerColor = containerColor)
                } else {
                    TopAppBarDefaults.topAppBarColors()
                },
            )
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}
