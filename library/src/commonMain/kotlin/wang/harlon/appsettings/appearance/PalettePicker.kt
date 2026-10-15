package wang.harlon.appsettings.appearance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.icons.AppSettingsIcons
import wang.harlon.appsettings.icons.Wallpaper
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_theme_color_custom
import wang.harlon.appsettings.resources.appsettings_theme_color_system

/**
 * 配色色板：预设 + 壁纸取色（平台支持且允许时）+ 自定义档（允许时，同时是调色台入口）。
 * 每行 5 列等宽：内置 13 个预设 + 壁纸取色 + 自定义正好 5×3 满排。只在 [ColorSource.Seed] 下有意义。
 *
 * @param onOpenColorLab 点自定义档：已调过色就先把它应用上，再进调色台——点击只有一种结果，
 * 不做「选中态下再点才是编辑」那种要靠猜的二段交互
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PalettePicker(
    state: AppearanceState,
    onOpenColorLab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = state.appearance
    val isDark = state.isDark()
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
            maxItemsInEachRow = 5,
        ) {
            state.options.palette.forEach { preset ->
                val color = Color(preset.argb)
                MorphSwatch(
                    modifier = Modifier.weight(1f),
                    name = preset.label(),
                    brush = SolidColor(color),
                    contentTint = if (color.luminance() < 0.5f) Color.White else Color.Black,
                    idleIcon = null,
                    selected = (appearance.color as? ColorChoice.Preset)?.id == preset.id,
                    onClick = { state.select(ColorChoice.Preset(preset.id)) },
                )
            }
            if (state.showsSystemDynamic) {
                val system = systemDynamicColorScheme(isDark)
                val brush = system?.let { Brush.linearGradient(listOf(it.primary, it.tertiary)) }
                    ?: SolidColor(MaterialTheme.colorScheme.primary)
                MorphSwatch(
                    modifier = Modifier.weight(1f),
                    name = stringResource(Res.string.appsettings_theme_color_system),
                    brush = brush,
                    contentTint = if ((system?.primary ?: MaterialTheme.colorScheme.primary).luminance() < 0.5f) Color.White else Color.Black,
                    idleIcon = AppSettingsIcons.Wallpaper,
                    selected = appearance.color is ColorChoice.SystemDynamic,
                    onClick = { state.select(ColorChoice.SystemDynamic) },
                )
            }
            if (state.options.allowCustom) {
                CustomSwatch(
                    modifier = Modifier.weight(1f),
                    customColor = state.lastCustomColor,
                    selected = appearance.color is ColorChoice.Custom,
                    onClick = {
                        state.lastCustomColor?.let { state.select(ColorChoice.Custom(it)) }
                        onOpenColorLab()
                    },
                )
            }
        }
    }
}

/**
 * 色板末尾的自定义档，同时也是调色台入口：入口和结果在同一处，
 * 用户从这里进去、调完回来看到它变成自己的颜色并选中，认知是连贯的。
 * 没调过色时是一圈色轮渐变 + 加号，一眼看出「这颗是随便挑颜色」。
 */
@Composable
private fun CustomSwatch(
    customColor: CustomColor?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val wheel = remember {
        Brush.sweepGradient((0..360 step 30).map { Color(hsvToArgb(Hsv(it.toFloat(), 0.85f, 0.95f))) })
    }
    val color = customColor?.let { Color(it.argb) }
    MorphSwatch(
        modifier = modifier,
        name = stringResource(Res.string.appsettings_theme_color_custom),
        brush = color?.let { SolidColor(it) } ?: wheel,
        // 色轮档底色浅深都有，白色勾 / 加号在任何一段色相上都看得清
        contentTint = if (color == null || color.luminance() < 0.5f) Color.White else Color.Black,
        idleIcon = if (customColor == null) Icons.Filled.Add else null,
        selected = selected,
        onClick = onClick,
    )
}

/** 色卡直径：48dp 是 M3 建议的最小触控目标，五列等宽排布下也不至于把列挤破。 */
private val SwatchSize = 48.dp

/**
 * 主题色卡：M3 Expressive 的多边形色卡，选中时形状从 9 瓣饼干形变成太阳形并微微放大，
 * 勾随形变弹入——用形状变化而不是描边来表达选中。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MorphSwatch(
    name: String,
    brush: Brush,
    contentTint: Color,
    idleIcon: ImageVector?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val morph = rememberMorph(SwatchShapes.idle, SwatchShapes.selected)
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    )
    val swatchScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(SwatchSize)
                .scale(swatchScale)
                // shape 依赖 progress，形变过程中每帧都是新实例，这是 morph 的固有代价
                .clip(MorphPolygonShape(morph, progress))
                .background(brush)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = name },
            contentAlignment = Alignment.Center,
        ) {
            when {
                selected -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.scale(progress),
                    tint = contentTint,
                )
                idleIcon != null -> Icon(imageVector = idleIcon, contentDescription = null, tint = contentTint)
            }
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
