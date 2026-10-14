package wang.harlon.appsettings.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsGroupShapeTest {

    private fun corners(index: Int, count: Int): List<Any> {
        val s = groupItemShape(index, count) as RoundedCornerShape
        return listOf(s.topStart, s.topEnd, s.bottomStart, s.bottomEnd)
    }

    private val large = androidx.compose.foundation.shape.CornerSize(SettingsDefaults.LargeCorner)
    private val small = androidx.compose.foundation.shape.CornerSize(SettingsDefaults.SmallCorner)

    @Test
    fun singleItemIsLargeOnAllCorners() {
        assertEquals(listOf(large, large, large, large), corners(0, 1))
    }

    @Test
    fun firstMiddleLastGetTopMiddleBottomCorners() {
        assertEquals(listOf(large, large, small, small), corners(0, 3))
        assertEquals(listOf(small, small, small, small), corners(1, 3))
        assertEquals(listOf(small, small, large, large), corners(2, 3))
    }
}
