package top.kagg886.eoa.util

import androidx.compose.runtime.Composable

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/4 15:50
 * ================================================
 */
@Composable
expect fun BackHandler(enabled: Boolean = true,onBack:()-> Unit)
