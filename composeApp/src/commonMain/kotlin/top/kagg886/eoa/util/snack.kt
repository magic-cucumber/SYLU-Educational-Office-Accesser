package top.kagg886.eoa.util

import StackedSnackbarDuration
import StackedSnakbarHostState

fun StackedSnakbarHostState.showSnackBar(
    type: SnackBarType,
    title: String,
    description: String? = null,
    actionTitle: String = "关闭",
    action: (() -> Unit) = {},
    duration: StackedSnackbarDuration = StackedSnackbarDuration.Short,
) {
    when (type) {
        SnackBarType.Info -> showInfoSnackbar(
            title,
            description,
            actionTitle,
            action,
            duration,
        )
        SnackBarType.Warning -> showWarningSnackbar(
            title,
            description,
            actionTitle,
            action,
            duration,
        )
        SnackBarType.Error -> showErrorSnackbar(
            title,
            description,
            actionTitle,
            action,
            duration,
        )
        SnackBarType.Success -> showSuccessSnackbar(
            title,
            description,
            actionTitle,
            action,
            duration,
        )
    }
}

enum class SnackBarType {
    Info,
    Warning,
    Error,
    Success
}
