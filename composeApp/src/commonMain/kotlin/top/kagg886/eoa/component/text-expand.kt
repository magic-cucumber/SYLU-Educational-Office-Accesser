package top.kagg886.eoa.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableText(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,

    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit = {},

    controller: @Composable () -> Unit = {
        TextButton(
            onClick = { onExpandChange(!isExpanded) },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isExpanded) "收起" else "展开",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
) {
    var hasOverflow by remember { mutableStateOf(false) }

    Text(
        text = text,
        style = style,
        color = color,
        maxLines = if (isExpanded) Int.MAX_VALUE else maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult: TextLayoutResult ->
            val currentHasOverflow = textLayoutResult.hasVisualOverflow
            // 只有在收起状态下才更新hasOverflow，这样展开后仍然记住原本有溢出
            if (!isExpanded) {
                hasOverflow = currentHasOverflow
            }
        },
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    )

    // 展开/收起按钮
    if (text.isNotEmpty() && hasOverflow) {
        controller()
    }
}
