@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa.component.snack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.*

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/8 12:47
 * ================================================
 */

@Composable
fun EOAToaster(
    state: ToasterState = rememberToasterState(),
    dark: Boolean = false,
    modifier: Modifier = Modifier
) {
    Toaster(
        state = state,
        modifier = modifier,
        richColors = true,
        darkTheme = dark,
        actionSlot = { toast ->
            when (val action = toast.action) {
                null -> {}

                is TextToastAction -> {
                    val contentColor = ToasterDefaults.contentColor(toast, richColors = true, darkTheme = dark)
                    TextButton(
                        onClick = { action.onClick(toast) },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = action.text,
                            color = contentColor
                        )
                    }
                }

                else -> throw IllegalStateException(
                    "Please provide a custom action slot to " +
                            "display this type: ${action::class.simpleName}"
                )
            }
        }
    )
}
