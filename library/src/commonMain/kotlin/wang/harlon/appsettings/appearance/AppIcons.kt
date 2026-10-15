package wang.harlon.appsettings.appearance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 一个动态图标档位。alias 声明与图标资源在宿主，机制在库。
 *
 * @param androidAlias 完整 activity-alias 类名；null 表示该档在 Android 不可用
 * @param iosIconName iOS 备用图标名；null 表示主图标
 * @param preview 预览怎么画由宿主决定（合成的、位图都行），库只负责选中环、缩放动画、标签与无障碍语义；
 * 预览会被裁成圆
 */
@Immutable
data class AppIconOption(
    val id: String,
    val androidAlias: String?,
    val iosIconName: String?,
    val label: @Composable () -> String,
    val preview: @Composable () -> Unit,
)

/** 动态图标状态。不自己持久化：真实状态只有系统里那一份，读 [current] 就是读系统。 */
@Stable
internal class AppIconState internal constructor(
    val options: List<AppIconOption>,
    private val platform: AppIconPlatform,
) {
    internal var onEvent: (AppearanceEvent) -> Unit = {}

    /** 平台是否支持切换；不支持时 [AppIconPicker] 不显示。 */
    val isSupported: Boolean get() = platform.isSupported && options.isNotEmpty()

    /** 当前生效的 id，读自系统真实状态；读不出来时取第一个。 */
    var current: String by mutableStateOf(platform.current(options) ?: options.firstOrNull()?.id.orEmpty())
        private set

    /** 保证同时只启用一个 alias，单个失败不连累其余；失败回退并发 [AppearanceEvent.AppIconChangeFailed]。 */
    fun select(id: String) {
        val target = options.firstOrNull { it.id == id } ?: return
        if (id == current) return
        val previous = current
        current = id
        platform.apply(target, options) { failure ->
            if (failure == null) {
                onEvent(AppearanceEvent.AppIconChanged(id))
            } else {
                current = platform.current(options) ?: previous
                onEvent(AppearanceEvent.AppIconChangeFailed(id, failure))
            }
        }
    }

    /** 重新读一次系统状态（例如从系统设置回来后）。 */
    fun refresh() {
        platform.current(options)?.let { current = it }
    }
}

/**
 * Android 上 debug 包首次调用时校验每个 androidAlias 在 manifest 里存在、且有且只有一个默认启用，
 * 不满足抛 IllegalStateException 并给出 manifest 写法；release 只打日志。
 */
@Composable
internal fun rememberAppIconState(
    options: List<AppIconOption>,
    onEvent: (AppearanceEvent) -> Unit = {},
): AppIconState {
    val platform = rememberAppIconPlatform(options)
    val state = remember(options, platform) { AppIconState(options, platform) }
    SideEffect { state.onEvent = onEvent }
    return state
}

/** 动态图标选择：3 列网格，每格是宿主画的预览 + 标签。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppIconPicker(icons: AppIconState, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            maxItemsInEachRow = 3,
        ) {
            icons.options.forEach { option ->
                AppIconSwatch(
                    modifier = Modifier.weight(1f),
                    option = option,
                    selected = option.id == icons.current,
                    onClick = { icons.select(option.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppIconSwatch(
    option: AppIconOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val name = option.label()
    val swatchScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val ringColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        // selectable 放在整列上，图标 + 标签构成一个完整触摸目标
        modifier = modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(swatchScale)
                .border(3.dp, ringColor, CircleShape)
                .padding(6.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            option.preview()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
