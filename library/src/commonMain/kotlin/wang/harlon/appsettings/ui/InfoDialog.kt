package wang.harlon.appsettings.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_ok

/**
 * 纯说明弹窗：标题 + 正文 + 一个「知道了」。
 *
 * 需要第二个动作的说明弹窗不适用——那属于「说明 + 多动作」，自行组装 `AlertDialog`。
 *
 * @param confirmText 按钮文字；null 用库内中英文兜底
 */
@Composable
fun InfoDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(confirmText ?: stringResource(Res.string.appsettings_ok))
            }
        },
    )
}
