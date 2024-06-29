package com.kagg886.sylu_eoa.ui.componment

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun AnimationPopupOverlay(
    content: @Composable (toggle: ((Offset, IntSize) -> Unit)) -> Unit,
    dialogView: @Composable (modifier: Modifier, anim: Transition<Boolean>, closeFunc: () -> Unit) -> Unit
) {
    var show by remember { mutableStateOf(false) }

    var initialOffset by remember { mutableStateOf(Offset.Zero) }
    var initialSize by remember { mutableStateOf(IntSize.Zero) }

    val anim = updateTransition(targetState = show, label = "")

    Box {
        content { widgetOffset: Offset, widgetSize: IntSize ->
            initialOffset = widgetOffset
            initialSize = widgetSize
            show = true
        }

        val progress by anim.animateFloat { if (it) 1f else 0f }

        if (show || progress > 0f) {
            val windowSize = LocalConfiguration.current
            val density = LocalDensity.current

            val width by anim.animateDp(label = "w") { if (it) windowSize.screenWidthDp.dp else with(density) { initialSize.width.toDp() } }
            val height by anim.animateDp(label = "h") { if (it) windowSize.screenHeightDp.dp else with(density) { initialSize.height.toDp() } }
            val offsetX by anim.animateInt(label = "x") { if (it) 0 else initialOffset.x.toInt() }
            val offsetY by anim.animateInt(label = "y") { if (it) 0 else initialOffset.y.toInt() }


            dialogView(
                Modifier
                    .absoluteOffset { IntOffset(offsetX, offsetY) }
                    .size(width, height), anim
            ) {
                show = false
            }
        }
    }
}

@Composable
@Preview
fun AnimationPopupOverlayPreview() {
    MaterialTheme {
        AnimationPopupOverlay(content = { showView ->
            LazyVerticalGrid(columns = GridCells.Fixed(4)) {
                items(25) {
                    var size by remember { mutableStateOf(IntSize.Zero) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    Card(modifier = Modifier
                        .clickable {
                            showView(offset, size)
                        }
                        .size(300.dp, 200.dp)
                        .padding(5.dp)
                        .onGloballyPositioned {
                            size = it.size
                            offset = it.positionInRoot()
                        }) {
                        Text("$it")
                    }
                }
            }
        }, dialogView = { modifier, anim, closeFunc ->
            val alpha by anim.animateFloat(label = "alpha") { if (it) 1f else 0.2f }

            val corner by anim.animateFloat(label = "corner") { if (it) 0f else 5f}
            Surface(modifier = modifier, shape = RoundedCornerShape(corner)) {
                Box(modifier = Modifier.alpha(alpha)) {
                    Button(onClick = { closeFunc() }) {
                        Text("close")
                    }
                }
            }
        })
    }
}