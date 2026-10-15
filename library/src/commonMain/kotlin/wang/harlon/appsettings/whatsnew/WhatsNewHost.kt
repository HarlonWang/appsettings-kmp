package wang.harlon.appsettings.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_whats_new_got_it
import wang.harlon.appsettings.resources.appsettings_whats_new_title

/**
 * 挂在根上：[state] 有待弹的就弹 [WhatsNewDialog]。
 *
 * @param language 展示语言（界面语言或系统语言），由宿主决定，见 [WhatsNewInfo.pick]
 */
@Composable
internal fun WhatsNewHost(state: WhatsNewState, language: String) {
    val info = state.pending ?: return
    val items = remember(info, language) { info.pick(language) }
    WhatsNewDialog(version = info.version, items = items, onDismiss = state::dismiss)
}

/** 更新说明弹窗本体：标题带版本号，条目逐行，一个「知道了」。想自己控制时机就直接用它。 */
@Composable
internal fun WhatsNewDialog(
    version: String,
    items: List<String>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.appsettings_whats_new_title, version)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item -> Text("• $item") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.appsettings_whats_new_got_it))
            }
        },
    )
}
