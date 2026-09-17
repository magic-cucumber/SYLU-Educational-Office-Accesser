@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("DEPRECATION")

package top.kagg886.calendar.v2

import androidx.compose.runtime.*
import kotlinx.cinterop.cValue
import platform.EventKit.*
import platform.Foundation.NSBundle
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import top.kagg886.calendar.v2.state.CalendarState
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun rememberCalendarManagerState(): CalendarState {
    val api = remember { EKEventStore() }
    var state by remember { mutableStateOf<CalendarState>(CalendarState.Waiting) }

    LaunchedEffect(api) {
        state = CalendarState.Processing

        when (isIOS17OrLater()) {
            true -> {
                val usageDescriptionKey = "NSCalendarsFullAccessUsageDescription"
                logger.i("start ios calendar permission check: iOS17OrLater=true")

                val usageDescription = NSBundle.mainBundle
                    .objectForInfoDictionaryKey(usageDescriptionKey) as? String
                if (usageDescription.isNullOrBlank()) {
                    logger.e("Info.plist missing $usageDescriptionKey")
                    state = CalendarState.NotSupported
                    return@LaunchedEffect
                }

                val currentState = when (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)) {
                    EKAuthorizationStatusFullAccess -> CalendarState.Granted(EventKitCalendarManager(api))
                    EKAuthorizationStatusWriteOnly,
                    EKAuthorizationStatusDenied,
                    EKAuthorizationStatusRestricted -> CalendarState.Denied(permanent = true)
                    EKAuthorizationStatusNotDetermined -> null
                    else -> CalendarState.NotSupported
                }
                state = currentState ?: run {
                    val granted = suspendCancellableCoroutine<Boolean> { continuation ->
                        api.requestFullAccessToEventsWithCompletion { granted, error ->
                            if (error != null) {
                                logger.e("request full calendar access failed: ${error.localizedDescription}")
                            }
                            if (continuation.isActive) {
                                continuation.resume(granted)
                            }
                        }
                    }
                    if (granted) {
                        CalendarState.Granted(EventKitCalendarManager(api))
                    } else {
                        CalendarState.Denied(permanent = true)
                    }
                }
            }
            false -> {
                val usageDescriptionKey = "NSCalendarsUsageDescription"
                logger.i("start ios calendar permission check: iOS17OrLater=false")

                val usageDescription = NSBundle.mainBundle
                    .objectForInfoDictionaryKey(usageDescriptionKey) as? String
                if (usageDescription.isNullOrBlank()) {
                    logger.e("Info.plist missing $usageDescriptionKey")
                    state = CalendarState.NotSupported
                    return@LaunchedEffect
                }

                val currentState = when (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)) {
                    EKAuthorizationStatusAuthorized -> CalendarState.Granted(EventKitCalendarManager(api))
                    EKAuthorizationStatusDenied,
                    EKAuthorizationStatusRestricted -> CalendarState.Denied(permanent = true)
                    EKAuthorizationStatusNotDetermined -> null
                    else -> CalendarState.NotSupported
                }
                state = currentState ?: run {
                    val granted = suspendCancellableCoroutine<Boolean> { continuation ->
                        api.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, error ->
                            if (error != null) {
                                logger.e("request legacy calendar access failed: ${error.localizedDescription}")
                            }
                            if (continuation.isActive) {
                                continuation.resume(granted)
                            }
                        }
                    }
                    if (granted) {
                        CalendarState.Granted(EventKitCalendarManager(api))
                    } else {
                        CalendarState.Denied(permanent = true)
                    }
                }
            }
        }
    }

    return state
}

private fun isIOS17OrLater(): Boolean =
    NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
        cValue<NSOperatingSystemVersion> {
            majorVersion = 17
            minorVersion = 0
            patchVersion = 0
        }
    )
