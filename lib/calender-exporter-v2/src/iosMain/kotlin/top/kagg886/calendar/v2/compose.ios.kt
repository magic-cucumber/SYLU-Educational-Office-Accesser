package top.kagg886.calendar.v2

import androidx.compose.runtime.*
import platform.EventKit.*
import platform.Foundation.NSBundle
import top.kagg886.calendar.v2.state.CalendarState

@Composable
actual fun rememberCalendarManagerState(): CalendarState {
    var state by remember {
        mutableStateOf<CalendarState>(CalendarState.Waiting)
    }



    LaunchedEffect(Unit) {
        val api = EKEventStore()
        logger.i("start ios calendar permission granted")
        state = CalendarState.Processing

        //check Info.plist
        val value = NSBundle.mainBundle.objectForInfoDictionaryKey("NSCalendarsUsageDescription")
        if ((value as? String)?.isNotBlank() != true) {
            logger.e("Info.plist missing NSCalendarsUsageDescription")
            state = CalendarState.NotSupported
            return@LaunchedEffect
        }

        //runtime check
        state = when (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)) {
            EKAuthorizationStatusAuthorized -> CalendarState.Granted(EventKitCalendarManager(api))
            EKAuthorizationStatusDenied, EKAuthorizationStatusRestricted -> CalendarState.Denied(permanent = true)
            EKAuthorizationStatusNotDetermined -> {
                api.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, error ->
                    if (!granted) {
                        logger.e("check permission failed: ${error?.localizedDescription}")
                        return@requestAccessToEntityType
                    }
                    state = CalendarState.Granted(EventKitCalendarManager(api))
                }
                CalendarState.Processing
            }

            else -> CalendarState.NotSupported
        }
    }

    return state
}
