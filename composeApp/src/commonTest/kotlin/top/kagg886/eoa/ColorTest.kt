package top.kagg886.eoa

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test

class ColorTest {
    @Test
    fun testColor() {
        val colorA = Color(20,46,15,11)

        val data = colorA.toArgb()
        println(data)

        val colorB = Color(data)
        println(colorB)
    }
}