package top.kagg886.eoa.util.shared

import androidx.compose.ui.Modifier

inline fun Modifier.applyIf(condition: Boolean, modifier: Modifier.() -> Modifier) =
    if (condition) modifier() else this