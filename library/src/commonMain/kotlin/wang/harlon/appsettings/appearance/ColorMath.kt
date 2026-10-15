package wang.harlon.appsettings.appearance

import kotlin.math.abs
import kotlin.math.roundToInt

/** HSV 三元组：hue 0..360，saturation / value 0..1。 */
internal data class Hsv(val hue: Float, val saturation: Float, val value: Float)

/**
 * `#RRGGBB` / `RRGGBB` → 不透明 ARGB。大小写、首尾空白、`#` 前缀都容忍；
 * 位数不对或含非十六进制字符返回 null。只接受 6 位：seed 必须不透明，8 位带 alpha 没有意义。
 */
internal fun parseHexColor(input: String): Long? {
    val hex = input.trim().removePrefix("#")
    if (hex.length != 6) return null
    if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    val rgb = hex.toLongOrNull(16) ?: return null
    return 0xFF000000L or rgb
}

/** ARGB → 6 位大写 hex（不含 `#`），alpha 位丢弃。 */
internal fun formatHexColor(argb: Long): String {
    val rgb = argb and 0xFFFFFFL
    return rgb.toString(16).uppercase().padStart(6, '0')
}

/** ARGB → HSV，用于把已有 seed 反填到取色面板的光标位置。 */
internal fun argbToHsv(argb: Long): Hsv {
    val r = ((argb shr 16) and 0xFF).toFloat() / 255f
    val g = ((argb shr 8) and 0xFF).toFloat() / 255f
    val b = (argb and 0xFF).toFloat() / 255f

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val saturation = if (max == 0f) 0f else delta / max
    return Hsv(hue, saturation, max)
}

/** HSV → 不透明 ARGB；越界输入钳制而不是产生异常颜色。 */
internal fun hsvToArgb(hsv: Hsv): Long {
    val h = ((hsv.hue % 360f) + 360f) % 360f
    val s = hsv.saturation.coerceIn(0f, 1f)
    val v = hsv.value.coerceIn(0f, 1f)

    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((r1 + m) * 255f).roundToInt().toLong()
    val g = ((g1 + m) * 255f).roundToInt().toLong()
    val b = ((b1 + m) * 255f).roundToInt().toLong()
    return 0xFF000000L or (r shl 16) or (g shl 8) or b
}
