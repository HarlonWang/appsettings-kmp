package wang.harlon.appsettings.whatsnew

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 最近一次看过更新说明的版本号；null 表示首次安装（从未记录）。 */
internal const val LAST_SEEN_KEY = "appsettings.whatsnew.lastSeen"

/**
 * 随包内置的新版本更新说明。
 *
 * JSON 格式：`{"version":"1.2.0","zh":[...],"en":[...]}`——`version` 之外的每个 key 都是一种语言，
 * 值是条目列表；不是字符串数组的 key 会被忽略。
 */
data class WhatsNewInfo(
    val version: String,
    /** 语言码 → 条目，保持 JSON 里的顺序。 */
    val notes: Map<String, List<String>>,
) {
    val isEmpty: Boolean get() = notes.values.all { it.isEmpty() }

    /**
     * 按语言挑一份：精确命中 → 语言前缀命中（`zh-Hans` → `zh`）→ `en` → 第一个非空。
     * 全空返回空表。
     */
    fun pick(language: String): List<String> {
        val tag = language.lowercase()
        nonEmpty(tag)?.let { return it }
        val base = baseOf(tag)
        notes.entries.firstOrNull { (key, items) -> items.isNotEmpty() && baseOf(key.lowercase()) == base }
            ?.let { return it.value }
        nonEmpty("en")?.let { return it }
        return notes.values.firstOrNull { it.isNotEmpty() } ?: emptyList()
    }

    private fun nonEmpty(key: String): List<String>? =
        notes.entries.firstOrNull { it.key.lowercase() == key }?.value?.takeIf { it.isNotEmpty() }

    private fun baseOf(tag: String): String = tag.substringBefore('-').substringBefore('_')
}

/** 解析随包 JSON；不是合法 JSON 对象返回 null。缺 `version` 当空串。 */
fun parseWhatsNew(raw: String): WhatsNewInfo? {
    val obj = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return null
    val version = (obj["version"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: ""
    val notes = obj.entries
        .filter { it.key != "version" }
        .mapNotNull { (key, value) ->
            val array = value as? JsonArray ?: return@mapNotNull null
            key to array.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
        }
        .toMap()
    return WhatsNewInfo(version, notes)
}

/**
 * 升级后首启弹一次。首次安装（lastSeen 为空，调用方静默写入当前版本）与
 * 内置文件版本不匹配（dev 构建 / 占位文件）都不弹。版本号按字符串精确比较，预发布后缀也要一致。
 */
fun shouldShowWhatsNew(
    currentVersion: String,
    bundledVersion: String,
    lastSeenVersion: String?,
): Boolean =
    lastSeenVersion != null &&
        lastSeenVersion != currentVersion &&
        bundledVersion == currentVersion &&
        // 占位文件永远不弹，哪怕 dev 包的版本号也是 0.0.0
        bundledVersion != PLACEHOLDER_VERSION

private const val PLACEHOLDER_VERSION = "0.0.0"

/**
 * 本次启动是否待弹更新说明，同步、不读内置 JSON，用 lastSeen != current 近似。
 * 供宿主在同一次启动里避开双弹窗（例如跳过自动更新检查）：即便最终因 JSON 不匹配没弹，
 * [WhatsNewState] 也会在该次启动把 lastSeen 写成当前版本，下次启动恢复。
 */
fun isWhatsNewPending(settings: Settings, currentVersion: String): Boolean {
    val lastSeen = settings.getStringOrNull(LAST_SEEN_KEY)
    return lastSeen != null && lastSeen != currentVersion
}
