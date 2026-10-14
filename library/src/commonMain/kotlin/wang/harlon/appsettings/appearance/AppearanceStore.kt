package wang.harlon.appsettings.appearance

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 外观状态的持久化。[appearance] 用 StateFlow 保证同步初值，首帧就是真实值。 */
interface AppearanceStore {
    val appearance: StateFlow<Appearance>

    fun update(transform: (Appearance) -> Appearance)
}

/** 默认存储 key。 */
const val DefaultAppearanceKey: String = "appsettings.appearance.state"

/**
 * 默认实现：一个 key 存一份带 schemaVersion 的 JSON。读不出来（新装、损坏、未来版本）、
 * 或读出的值在 [options] 里不合法（未知预设 id、不允许的来源、不在 modes 里的模式）都回落默认值。
 * 不读任何旧格式。
 */
fun AppearanceStore(
    settings: Settings,
    options: AppearanceOptions,
    key: String = DefaultAppearanceKey,
): AppearanceStore = SettingsAppearanceStore(settings, options, key)

internal const val APPEARANCE_SCHEMA_VERSION = 1

private class SettingsAppearanceStore(
    private val settings: Settings,
    private val options: AppearanceOptions,
    private val key: String,
) : AppearanceStore {
    private val flow = MutableStateFlow(read())

    override val appearance: StateFlow<Appearance> get() = flow

    override fun update(transform: (Appearance) -> Appearance) {
        val next = transform(flow.value)
        if (next == flow.value) return
        settings.putString(key, json.encodeToString(StoredAppearance.from(next)))
        flow.value = next
    }

    private fun read(): Appearance {
        val raw = settings.getStringOrNull(key)
        val stored = raw?.let { runCatching { json.decodeFromString<StoredAppearance>(it) }.getOrNull() }
        return stored?.toAppearance(options) ?: Appearance(mode = options.defaultMode, color = options.defaultColor)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

@Serializable
internal data class StoredCustom(val argb: Long, val style: String, val contrast: String) {
    fun toCustom() = CustomColor(argb, ThemeStyle.fromStorage(style), ThemeContrast.fromStorage(contrast))

    companion object {
        fun from(c: CustomColor) = StoredCustom(c.argb, c.style.storageValue, c.contrast.storageValue)
    }
}

@Serializable
internal data class StoredColor(val type: String, val id: String? = null, val custom: StoredCustom? = null) {
    companion object {
        const val PRESET = "preset"
        const val CUSTOM = "custom"
        const val SYSTEM = "system"

        fun from(choice: ColorChoice) = when (choice) {
            is ColorChoice.Preset -> StoredColor(PRESET, id = choice.id)
            is ColorChoice.Custom -> StoredColor(CUSTOM, custom = StoredCustom.from(choice.color))
            ColorChoice.SystemDynamic -> StoredColor(SYSTEM)
        }
    }
}

@Serializable
internal data class StoredAppearance(
    val schemaVersion: Int = APPEARANCE_SCHEMA_VERSION,
    val mode: String,
    val color: StoredColor,
    val customHistory: List<StoredCustom> = emptyList(),
) {
    /** 逐字段校验：不合法的字段单独回落，不连累其余。 */
    fun toAppearance(options: AppearanceOptions): Appearance {
        val mode = ThemeMode.fromStorage(mode)?.takeIf { it in options.modes } ?: options.defaultMode
        val choice: ColorChoice = when (color.type) {
            StoredColor.PRESET -> color.id?.takeIf { options.preset(it) != null }?.let { ColorChoice.Preset(it) }
            StoredColor.CUSTOM -> color.custom?.takeIf { options.allowCustom }?.let { ColorChoice.Custom(it.toCustom()) }
            StoredColor.SYSTEM -> ColorChoice.SystemDynamic.takeIf { options.allowSystemDynamic }
            else -> null
        } ?: options.defaultColor
        return Appearance(
            mode = mode,
            color = choice,
            customHistory = customHistory.map { it.toCustom() }.take(options.customHistoryLimit),
        )
    }

    companion object {
        fun from(a: Appearance) = StoredAppearance(
            mode = a.mode.name,
            color = StoredColor.from(a.color),
            customHistory = a.customHistory.map { StoredCustom.from(it) },
        )
    }
}
