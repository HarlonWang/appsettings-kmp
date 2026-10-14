package wang.harlon.appsettings.appearance

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * M3 Expressive 形状工具：补 material3 `RoundedPolygon.toShape()` 没有的那半边——
 * 两个多边形之间的形变（morph），把「选中」表达成形状变化而不是描边。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path: Path = morph.toPath(progress = progress)
        path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
        path.translate(size.center - path.getBounds().center)
        return Outline.Generic(path)
    }
}

@Composable
internal fun rememberMorph(start: RoundedPolygon, end: RoundedPolygon): Morph =
    remember(start, end) { Morph(start, end) }

/** 色卡用的一对形状：常态是 9 瓣饼干，选中时形变成太阳形。 */
internal object SwatchShapes {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val idle: RoundedPolygon get() = MaterialShapes.Cookie9Sided

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val selected: RoundedPolygon get() = MaterialShapes.Sunny
}
