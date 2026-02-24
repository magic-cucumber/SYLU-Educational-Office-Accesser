package top.kagg886.eoa.util

import com.dokar.sonner.TextToastAction
import com.dokar.sonner.ToasterState
import kotlin.time.Clock
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val ran = Random(Clock.System.now().toEpochMilliseconds())

fun ToasterState.showSnackBar(
    type: SnackBarType,
    title: String,
    description: String? = null,
    actionTitle: String? = "关闭",
    action: (() -> Unit) = {},
    duration: Duration = 3.seconds,
) {
    val id = ran.nextInt()
    show(
        id = id,

        message = "$title${description?.let { "\n$it" } ?: ""}",
        type = when (type) {
            SnackBarType.Info -> com.dokar.sonner.ToastType.Info
            SnackBarType.Warning -> com.dokar.sonner.ToastType.Warning
            SnackBarType.Error -> com.dokar.sonner.ToastType.Error
            SnackBarType.Success -> com.dokar.sonner.ToastType.Success
        },
        duration = duration,


        action = actionTitle?.let {
            TextToastAction(
                text = actionTitle,
                onClick = {
                    action()
                    dismiss(id)
                }
            )
        },

        )
//    when (type) {
//        SnackBarType.Info -> showInfoSnackbar(
//            title,
//            description,
//            actionTitle,
//            action,
//            duration,
//        )
//        SnackBarType.Warning -> showWarningSnackbar(
//            title,
//            description,
//            actionTitle,
//            action,
//            duration,
//        )
//        SnackBarType.Error -> showErrorSnackbar(
//            title,
//            description,
//            actionTitle,
//            action,
//            duration,
//        )
//        SnackBarType.Success -> showSuccessSnackbar(
//            title,
//            description,
//            actionTitle,
//            action,
//            duration,
//        )
//    }
}

enum class SnackBarType {
    Info,
    Warning,
    Error,
    Success
}
