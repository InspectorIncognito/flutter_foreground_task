package com.pravera.flutter_foreground_task

/**
 * Key values for data stored in SharedPreferences.
 *
 * @author Dev-hwang
 * @version 1.0
 */
object PreferencesKey {
    private const val prefix = "com.pravera.flutter_foreground_task.prefs."

    const val FOREGROUND_SERVICE_STATUS_PREFS_NAME = prefix + "FOREGROUND_SERVICE_STATUS"
    const val FOREGROUND_SERVICE_ACTION = "foregroundServiceAction"

    const val NOTIFICATION_OPTIONS_PREFS_NAME = prefix + "NOTIFICATION_OPTIONS"

    const val NOTIFICATION_COLOR = "backgroundColorRgb"
    const val NOTIFICATION_DATA = "notificationData"
    const val NOTIFICATION_METADATA = "notificationMetadata"
    const val NOTIFICATION_TYPE = "notificationType"
    const val NOTIFICATION_ID = "notificationId"
    const val NOTIFICATION_VIBRATION = "notificationVibration"

    const val NOTIFICATION_NORMAL_TITLE = "title"
    const val NOTIFICATION_NORMAL_MESSAGE = "message"
    const val NOTIFICATION_ARRIVAL_STOP_CODE = "stopCode"
    const val NOTIFICATION_ARRIVAL_TOP = "topMessage"
    const val NOTIFICATION_ARRIVAL_BOTTOM = "bottomMessage"
    const val NOTIFICATION_ARRIVAL_ARRIVING = "arriving"
    const val NOTIFICATION_ARRIVAL_PLATE = "plate"

    const val FOREGROUND_TASK_OPTIONS_PREFS_NAME = prefix + "FOREGROUND_TASK_OPTIONS"
    const val TASK_INTERVAL = "interval"
    const val CALLBACK_HANDLE = "callbackHandle"
    const val CALLBACK_HANDLE_ON_BOOT = "callbackHandleOnBoot"
}
