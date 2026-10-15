package wang.harlon.appsettings.appearance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_color_lab_contrast
import wang.harlon.appsettings.resources.appsettings_color_lab_hex
import wang.harlon.appsettings.resources.appsettings_color_lab_hex_invalid
import wang.harlon.appsettings.resources.appsettings_color_lab_hue
import wang.harlon.appsettings.resources.appsettings_color_lab_preview_action
import wang.harlon.appsettings.resources.appsettings_color_lab_preview_chip
import wang.harlon.appsettings.resources.appsettings_color_lab_preview_desc
import wang.harlon.appsettings.resources.appsettings_color_lab_preview_item
import wang.harlon.appsettings.resources.appsettings_color_lab_preview_title
import wang.harlon.appsettings.resources.appsettings_color_lab_recent
import wang.harlon.appsettings.resources.appsettings_color_lab_saturation_value
import wang.harlon.appsettings.resources.appsettings_color_lab_style
import wang.harlon.appsettings.ui.SettingsDefaults

/** 拖动取色时的落盘防抖：拖一次会产生几十个中间值，全写盘既费 IO 也无意义。 */
private const val PERSIST_DEBOUNCE_MS = 120L

/**
 * 调色台会话：进入时以「当前生效的那一档」为起点，改动实时生效（防抖落盘），整个 App 立刻跟着变色；
 * [undo] 整体写回进入时的快照；离开时（[rememberColorLabSession] 的 dispose）自动 [commit]——
 * 记一条历史并发 [AppearanceEvent.CustomApplied]。只看看没改的，什么都不写、什么都不发。
 */
@Stable
internal class ColorLabSession internal constructor(
    private val state: AppearanceState,
    private val scope: CoroutineScope,
) {
    /** 进入这一刻的完整状态，「撤销修改」按它整体写回。 */
    private val entry: Appearance = state.appearance

    var draft: CustomColor by mutableStateOf(initialDraft(state))
        private set

    /** 单独记 HSV：饱和度或明度为 0 时色相会丢，从 draft 反推不回来。 */
    var hsv: Hsv by mutableStateOf(argbToHsv(draft.argb))
        private set

    var hexText: String by mutableStateOf(formatHexColor(draft.argb))
        private set

    var hexError: Boolean by mutableStateOf(false)
        private set

    // 落盘节拍器：每次用户交互 +1；0 表示「没有待写的改动」，也是撤销按钮的可用性判据
    private var pendingWrite by mutableIntStateOf(0)
    private var persistJob: Job? = null

    val isDirty: Boolean get() = pendingWrite > 0

    val history: List<CustomColor> get() = state.appearance.customHistory

    /** 取色面板拖动：整组 HSV 更新。 */
    fun updateHsv(value: Hsv) {
        hsv = value
        setArgb(hsvToArgb(value), syncHsv = false)
    }

    fun setHue(hue: Float) = updateHsv(hsv.copy(hue = hue))

    /** 色值输入框：只留十六进制字符并截断到 6 位；输到一半不算错，非空且解析不出来才提示。 */
    fun setHexInput(raw: String) {
        val cleaned = raw.trim().removePrefix("#")
            .filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            .take(6)
            .uppercase()
        hexText = cleaned
        val parsed = parseHexColor(cleaned)
        if (parsed != null) {
            hsv = argbToHsv(parsed)
            draft = draft.copy(argb = parsed)
            hexError = false
            touched()
        } else {
            hexError = cleaned.isNotEmpty()
        }
    }

    fun setStyle(style: ThemeStyle) {
        draft = draft.copy(style = style)
        touched()
    }

    fun setContrast(contrast: ThemeContrast) {
        draft = draft.copy(contrast = contrast)
        touched()
    }

    /** 从历史里整组取回（颜色 + 风格 + 对比度）。 */
    fun pick(color: CustomColor) {
        draft = color
        hsv = argbToHsv(color.argb)
        hexText = formatHexColor(color.argb)
        hexError = false
        touched()
    }

    /** 把主题整体写回进入这一刻的状态，面板同步回填，并取消挂起的防抖写。 */
    fun undo() {
        persistJob?.cancel()
        state.restore(entry)
        val restored = initialDraft(state)
        draft = restored
        hsv = argbToHsv(restored.argb)
        hexText = formatHexColor(restored.argb)
        hexError = false
        pendingWrite = 0
    }

    /**
     * 落定：补一次落盘（防抖写可能还没到点）、记一条历史、发一次事件。幂等，没改过不做任何事。
     * 历史在离开时记一条最终值，而不是每次防抖落盘都记——否则会被一堆中间色冲满。
     */
    fun commit() {
        if (!isDirty) return
        persistJob?.cancel()
        state.applyCustom(draft)
        state.pushHistory(draft)
        state.emit(AppearanceEvent.CustomApplied(draft))
        pendingWrite = 0
    }

    private fun setArgb(argb: Long, syncHsv: Boolean) {
        if (syncHsv) hsv = argbToHsv(argb)
        draft = draft.copy(argb = argb)
        hexText = formatHexColor(argb)
        hexError = false
        touched()
    }

    private fun touched() {
        pendingWrite++
        persistJob?.cancel()
        val snapshot = draft
        persistJob = scope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            state.applyCustom(snapshot)
        }
    }

    private companion object {
        /** 当前生效的那一档的等价自定义配置：预设用钦定风格 + 标准对比度，壁纸取色从默认预设起步。 */
        fun initialDraft(state: AppearanceState): CustomColor = when (val choice = state.appearance.color) {
            is ColorChoice.Custom -> choice.color
            is ColorChoice.Preset -> state.options.preset(choice.id)?.toCustom() ?: state.options.defaultCustom()
            ColorChoice.SystemDynamic -> state.options.defaultCustom()
        }
    }
}

/** 建会话并在离开时自动落定；进入时发 [AppearanceEvent.ColorLabOpened]。宿主把 [ColorLabSession.undo] 接到顶栏按钮。 */
@Composable
internal fun rememberColorLabSession(state: AppearanceState): ColorLabSession {
    val scope = rememberCoroutineScope()
    val session = remember(state) { ColorLabSession(state, scope) }
    LaunchedEffect(session) { state.emit(AppearanceEvent.ColorLabOpened) }
    DisposableEffect(session) { onDispose { session.commit() } }
    return session
}

/** 调色台整页内容，自带滚动：预览卡、饱和度 × 明度面板、色相条、色值、最近使用、风格、对比度。 */
@Composable
internal fun ColorLabContent(session: ColorLabSession, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SettingsDefaults.ContentHorizontalPadding, vertical = 12.dp),
    ) {
        PreviewCard()

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(Res.string.appsettings_color_lab_saturation_value))
        SaturationValuePanel(hsv = session.hsv, onChange = session::updateHsv)

        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(Res.string.appsettings_color_lab_hue))
        HueSlider(hue = session.hsv.hue, onChange = session::setHue)

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = session.hexText,
            onValueChange = session::setHexInput,
            label = { Text(stringResource(Res.string.appsettings_color_lab_hex)) },
            prefix = { Text("#") },
            singleLine = true,
            isError = session.hexError,
            supportingText = if (session.hexError) {
                { Text(stringResource(Res.string.appsettings_color_lab_hex_invalid)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        if (session.history.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(Res.string.appsettings_color_lab_recent))
            RecentRow(history = session.history, current = session.draft, onPick = session::pick)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(Res.string.appsettings_color_lab_style))
        StyleChips(selected = session.draft.style, onSelect = session::setStyle)

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(Res.string.appsettings_color_lab_contrast))
        ContrastRow(selected = session.draft.contrast, onSelect = session::setContrast)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** 预览卡：把 primary / secondaryContainer / surface 三层同时摆出来——设置页本身几乎全是中性色，只看那一页会误判「换了颜色没反应」。 */
@Composable
private fun PreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.appsettings_color_lab_preview_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.appsettings_color_lab_preview_item),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.appsettings_color_lab_preview_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {}) { Text(stringResource(Res.string.appsettings_color_lab_preview_action)) }
                AssistChip(onClick = {}, label = { Text(stringResource(Res.string.appsettings_color_lab_preview_chip)) })
            }
        }
    }
}

/**
 * 饱和度（横轴）× 明度（纵轴）取色面板：底色是当前色相的纯色，叠「白→透明」横向渐变与「透明→黑」纵向渐变，
 * 任意一点的颜色恰好等于 Hsv(hue, x/w, 1 - y/h)。
 */
@Composable
private fun SaturationValuePanel(hsv: Hsv, onChange: (Hsv) -> Unit) {
    val pureHue = Color(hsvToArgb(Hsv(hsv.hue, 1f, 1f)))
    val description = stringResource(Res.string.appsettings_color_lab_saturation_value)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = description }
            .pointerInput(hsv.hue) {
                detectTapGestures { offset ->
                    onChange(offsetToSv(hsv.hue, offset, size.width.toFloat(), size.height.toFloat()))
                }
            }
            .pointerInput(hsv.hue) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange(offsetToSv(hsv.hue, change.position, size.width.toFloat(), size.height.toFloat()))
                }
            },
    ) {
        drawRect(color = pureHue)
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        drawThumb(
            center = Offset(
                x = hsv.saturation.coerceIn(0f, 1f) * size.width,
                y = (1f - hsv.value.coerceIn(0f, 1f)) * size.height,
            ),
        )
    }
}

/** 色相条：0..360 的完整色轮铺成一条横向渐变。 */
@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val hueColors = remember { (0..360 step 60).map { Color(hsvToArgb(Hsv(it.toFloat(), 1f, 1f))) } }
    val description = stringResource(Res.string.appsettings_color_lab_hue)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { offset -> onChange((offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange((change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f)
                }
            },
    ) {
        drawRect(brush = Brush.horizontalGradient(hueColors))
        drawThumb(center = Offset(x = (hue / 360f).coerceIn(0f, 1f) * size.width, y = size.height / 2f))
    }
}

/** 取色光标：白圈 + 黑描边，任何底色上都看得见。 */
private fun DrawScope.drawThumb(center: Offset) {
    drawCircle(color = Color.White, radius = 10f, center = center)
    drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = 12f, center = center, style = Stroke(width = 2f))
}

/** 面板坐标 → HSV；手指滑出面板钳制到边缘而非丢弃。 */
private fun offsetToSv(hue: Float, offset: Offset, width: Float, height: Float): Hsv = Hsv(
    hue = hue,
    saturation = (offset.x / width).coerceIn(0f, 1f),
    value = (1f - offset.y / height).coerceIn(0f, 1f),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleChips(selected: ThemeStyle, onSelect: (ThemeStyle) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeStyle.entries.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option.label()) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContrastRow(selected: ThemeContrast, onSelect: (ThemeContrast) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeContrast.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeContrast.entries.size),
                label = { Text(text = option.label(), style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

/**
 * 最近使用过的自定义配置，点一下整组取回。选中态不能靠 primary 描边——选中的那颗恰好就是当前主题色的来源，
 * 描边与填充同源几乎融为一体；改成 onSurface 高对比外圈 + 一圈间隙 + 勾。间隙常驻，布局不跳。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentRow(history: List<CustomColor>, current: CustomColor, onPick: (CustomColor) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        history.forEach { entry ->
            val color = Color(entry.argb)
            val selected = entry == current
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(entry) }),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (color.luminance() < 0.5f) Color.White else Color.Black,
                    )
                }
            }
        }
    }
}
