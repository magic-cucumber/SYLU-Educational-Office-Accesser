package top.kagg886.eoa.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    DisposableEffect(enabled, onBack) {
        val registry = KeyboardFocusManager.getCurrentKeyboardFocusManager()

        val handler = KeyEventDispatcher {
            val consume = it.keyCode == KeyEvent.VK_ESCAPE
            if (consume && enabled) {
                onBack()
            }
            consume
        }

        registry.addKeyEventDispatcher(handler)
        onDispose { registry.removeKeyEventDispatcher(handler) }
    }
}
