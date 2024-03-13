package com.kagg886.sylu_eoa.ui.componment

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kagg886.utils.createLogger
import java.util.regex.Pattern
import kotlin.math.*

private val log = createLogger("RadarView")

@Composable
fun ComposeRadarView(
    modifier: Modifier = Modifier,
    data: List<RadarScore>,
    specialHandle: Boolean = false,
    itemsClickListener: (index: RadarScore) -> Unit = {},
) {
    val CIRCLE_TURN = 3
    val maxScore = roundUpToNearestTen(data.maxOf { it.value.coerceAtLeast(it.min) }.toInt())

    val colors = MaterialTheme.colorScheme
    var enable by remember {
        mutableStateOf(false)
    }

    var textToPos = remember(data) {
        mutableMapOf<RadarScore, Offset>()
    }

    val progress by animateFloatAsState(if (enable) 1f else 0f, animationSpec = tween(1000), label = "")
    Canvas(modifier = modifier
        .pointerInput(key1 = Unit) {
            detectTapGestures { point ->
                log.d("点击了:$point")
                textToPos.forEach { (score, offset) ->
                    log.d("与${score.text}的距离:${point distance offset}")
                    if (point distance offset < 100) {
                        log.d("点击命中:$score")
                        itemsClickListener(score)
                    }
                }
            }
        }
        .drawWithCache {
            val center = Offset(size.width / 2, size.height / 2)
            val textNeedRadius = 25.dp.toPx()
            val radarRadius = center.x - textNeedRadius
            val turnRadius = radarRadius / CIRCLE_TURN

            val itemAngle = 360 / data.size
            val startAngle = if (data.size % 2 == 0) {
                -90 - itemAngle / 2
            } else {
                -90
            }
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = colors.onSurface.toArgb()
            }
            val scorePaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = colors.primary.toArgb()
            }
            val minPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = colors.secondary.toArgb()
            }
            val scorePath = Path()
            val minPath = Path()
            onDrawWithContent {
                scorePath.reset()
                minPath.reset()
                // 绘制圆形
                for (turn in 0 until CIRCLE_TURN) {
                    drawCircle(colors.background, radius = turnRadius * (CIRCLE_TURN - turn))
                    drawCircle(
                        color = colors.onPrimaryContainer,
                        radius = turnRadius * (CIRCLE_TURN - turn),
                        style = Stroke(2f)
                    )
                }

                for (index in data.indices) {
                    val pointData = data[index]
                    // 绘制虚线
                    val currentAngle = startAngle + itemAngle * index
                    val xy = inCircleOffset(center, radarRadius, currentAngle)
                    drawLine(
                        colors.onSurface,
                        center,
                        xy,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // 绘制文字
                    val textPointRadius = radarRadius + 10f
                    val offset = inCircleOffset(center, textPointRadius, currentAngle)
                    val text = pointData.text
                    wrapText(
                        text,
                        textPaint,
                        size.width,
                        offset,
                        currentAngle,
                        if (specialHandle) textPaint.textSize * 2 else null
                    )
                    //记录文字坐标
                    textToPos[pointData] = offset
                    log.d("${pointData.text}坐标：${offset}")


                    //绘制分数
                    val scorePointRadius = progress * radarRadius * pointData.value / maxScore + 5f
                    val scoreOffset =
                        inCircleOffset(center, scorePointRadius.toFloat(), currentAngle)
                    val score = pointData.value
                    wrapText(
                        score
                            .toInt()
                            .toString(),
                        scorePaint,
                        size.width,
                        scoreOffset,
                        currentAngle,
                        if (specialHandle) scorePaint.textSize * 2 else null
                    )
                    if (pointData.min == 0.0) {
                        val minPointRadius = progress * radarRadius * pointData.min / maxScore + 5f
                        val minOffset = inCircleOffset(center, minPointRadius.toFloat(), currentAngle)
                        wrapText(
                            pointData.min
                                .toInt()
                                .toString(),
                            minPaint,
                            size.width,
                            minOffset,
                            currentAngle,
                            if (specialHandle) minPaint.textSize * 2 else null
                        )
                    }

                    // 绘制连接范围

                    val pointRadius = progress * radarRadius * pointData.value / maxScore
                    val fixPoint = inCircleOffset(center, pointRadius.toFloat(), currentAngle)
                    if (index == 0) {
                        scorePath.moveTo(fixPoint.x, fixPoint.y)
                    } else {
                        scorePath.lineTo(fixPoint.x, fixPoint.y)
                    }

                    val pointRadius2 = progress * radarRadius * pointData.min / maxScore
                    val fixPoint2 = inCircleOffset(center, pointRadius2.toFloat(), currentAngle)
                    if (index == 0) {
                        minPath.moveTo(fixPoint2.x, fixPoint2.y)
                    } else {
                        minPath.lineTo(fixPoint2.x, fixPoint2.y)
                    }
                }
                minPath.close()
                drawPath(minPath, colors.secondary.copy(alpha = 0.1f))
                drawPath(minPath, colors.secondary, style = Stroke(5f))
                scorePath.close()
                drawPath(scorePath, colors.primary.copy(alpha = 0.1f))
                drawPath(scorePath, colors.primary, style = Stroke(5f))
            }
        }
        .onGloballyPositioned {
            enable = it.boundsInRoot().top >= 0 && it.boundsInRoot().right > 0
        }) {}
}

private infix fun Offset.distance(offset: Offset): Float {
    //sqrt((x1-x2)^2 + (y1-y2)^2)
    return sqrt((offset.x - x).pow(2) + (offset.y - y).pow(2))
}

data class RadarScore(
    val text: String,
    val value: Double,
    val min: Double,
)

fun inCircleOffset(center: Offset, radius: Float, angle: Int): Offset {
    return Offset(
        (center.x + radius * cos(angle * PI / 180)).toFloat(),
        (center.y + radius * sin(angle * PI / 180)).toFloat()
    )
}

fun DrawScope.wrapText(
    text: String,
    textPaint: TextPaint,
    width: Float,
    offset: Offset,
    currentAngle: Int,
    chineseWrapWidth: Float? = null, // 用来处理UI需求中文每两个字符换行
) {
    val quadrant = quadrant(currentAngle)
//    Log.i("TTTT", "$text $quadrant")
    var textMaxWidth = width
    when (quadrant) {
        0 -> {
            textMaxWidth = width / 2
        }

        -1, 1, 2 -> {

            textMaxWidth = offset.x
        }

        -2, 3, 4 -> {
            textMaxWidth = size.width - offset.x
        }
    }
    //需要特殊处理换行&&包含中文字符&&文本绘制一行的宽度>文本最大宽度
    if (chineseWrapWidth != null && isContainChinese(text) && textPaint.measureText(text) > textMaxWidth) {
        textMaxWidth = chineseWrapWidth
    }
    val staticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, textPaint, textMaxWidth.toInt()).apply {
            this.setAlignment(Layout.Alignment.ALIGN_NORMAL)
        }.build()
    val textHeight = staticLayout.height
    val lines = staticLayout.lineCount
    val isWrap = lines > 1
    val textTrueWidth = if (isWrap) staticLayout.getLineWidth(0) else textPaint.measureText(text)
    drawContext.canvas.nativeCanvas.save()
    when (quadrant) {
        0 -> {
            drawContext.canvas.nativeCanvas.translate(
                offset.x - textTrueWidth / 2,
                offset.y - textHeight
            )
        }

        -1 -> {
            drawContext.canvas.nativeCanvas.translate(offset.x, offset.y - textHeight / 2)
        }

        -2 -> {
            drawContext.canvas.nativeCanvas.translate(
                offset.x - textTrueWidth,
                offset.y - textHeight / 2
            )
        }

        1 -> {
            drawContext.canvas.nativeCanvas.translate(
                offset.x,
                if (!isWrap) offset.y - textHeight / 2 else offset.y - (textHeight - textHeight / lines / 2)
            )
        }

        2 -> {
            drawContext.canvas.nativeCanvas.translate(
                offset.x,
                if (!isWrap) offset.y - textHeight / 2 else offset.y - textHeight / lines / 2
            )
        }

        3 -> {
            drawContext.canvas.nativeCanvas.translate(
                offset.x - textTrueWidth,
                if (!isWrap) offset.y - textHeight / 2 else offset.y - textHeight / lines / 2
            )
        }

        4 -> {
            drawContext.canvas.nativeCanvas.translate(
                offset.x - textTrueWidth,
                if (!isWrap) offset.y - textHeight / 2 else offset.y - (textHeight - textHeight / lines / 2)
            )
        }
    }
    staticLayout.draw(drawContext.canvas.nativeCanvas)
    drawContext.canvas.nativeCanvas.restore()
}

private fun isContainChinese(str: String): Boolean {
    val p = Pattern.compile("[\u4e00-\u9fa5]")
    val m = p.matcher(str)
    return m.find()
}

private fun quadrant(angle: Int): Int {
    return if (angle == -90 || angle == 90) {
        0 // 垂直
    } else if (angle == 0) {
        -1 // 水平右边
    } else if (angle == 180) {
        -2 // 水平左边
    } else if (angle > -90 && angle < 0) {
        1
    } else if (angle in 1..89) {
        2
    } else if (angle in 91..179) {
        3
    } else {
        4
    }
}

fun roundUpToNearestTen(number: Int): Int {
    val remainder = number % 10
    val roundingValue = if (remainder >= 0) 10 - remainder else 0
    return number + roundingValue
}

@Preview
@Composable
private fun Preview() {
    ComposeRadarView(
        modifier = Modifier.size(300.dp), data = listOf(
            RadarScore("1", 45.0, 0.0),
            RadarScore("2", 25.0, 10.0),
            RadarScore("3", 80.0, 40.0),
            RadarScore("4", 10.0, 30.0),
            RadarScore("5", 100.0, 0.0),
        )
    )
}