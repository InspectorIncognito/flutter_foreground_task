package com.pravera.flutter_foreground_task.models

import android.content.Context
import android.util.Log
import com.pravera.flutter_foreground_task.PreferencesKey as PrefsKey

data class ForegroundTaskOptions(
    val interval: Long,
    val isOnceEvent: Boolean,
    val autoRunOnBoot: Boolean,
    val allowWakeLock: Boolean,
    val allowWifiLock: Boolean,
    val callbackHandle: Long?,
    val callbackHandleOnBoot: Long?
) {
    companion object {
        fun getData(context: Context): ForegroundTaskOptions {
            val prefs = context.getSharedPreferences(
                PrefsKey.FOREGROUND_TASK_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)

            val interval = prefs.getLong(PrefsKey.TASK_INTERVAL, 5000L)
            val isOnceEvent = false
            val autoRunOnBoot = false
            val allowWakeLock = true
            val allowWifiLock = false
            val callbackHandle = if (prefs.contains(PrefsKey.CALLBACK_HANDLE)) {
                prefs.getLong(PrefsKey.CALLBACK_HANDLE, 0L)
            } else {
                null
            }
            val callbackHandleOnBoot = if (prefs.contains(PrefsKey.CALLBACK_HANDLE_ON_BOOT)) {
                prefs.getLong(PrefsKey.CALLBACK_HANDLE_ON_BOOT, 0L)
            } else {
                null
            }
            Log.d("ForegroundTaskOptions", "get handler: $callbackHandle")

            return ForegroundTaskOptions(
                interval = interval,
                isOnceEvent = isOnceEvent,
                autoRunOnBoot = autoRunOnBoot,
                allowWakeLock = allowWakeLock,
                allowWifiLock = allowWifiLock,
                callbackHandle = callbackHandle,
                callbackHandleOnBoot = callbackHandleOnBoot
            )
        }

        fun putData(context: Context, map: Map<*, *>?) {
            val prefs = context.getSharedPreferences(
                PrefsKey.FOREGROUND_TASK_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)

            val interval = "${map?.get(PrefsKey.TASK_INTERVAL)}".toLongOrNull() ?: 5000L
            val callbackHandle = "${map?.get(PrefsKey.CALLBACK_HANDLE)}".toLongOrNull()
            Log.d("ForegroundTaskOptions", "put handler: $callbackHandle")
            with(prefs.edit()) {
                putLong(PrefsKey.TASK_INTERVAL, interval)
                remove(PrefsKey.CALLBACK_HANDLE)
                remove(PrefsKey.CALLBACK_HANDLE_ON_BOOT)
                if (callbackHandle != null) {
                    putLong(PrefsKey.CALLBACK_HANDLE, callbackHandle)
                    putLong(PrefsKey.CALLBACK_HANDLE_ON_BOOT, callbackHandle)
                }
                apply()
            }
        }

        fun updateCallbackHandle(context: Context, map: Map<*, *>?) {
            val prefs = context.getSharedPreferences(
                PrefsKey.FOREGROUND_TASK_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)

            val callbackHandle = "${map?.get(PrefsKey.CALLBACK_HANDLE)}".toLongOrNull()
            Log.d("ForegroundTaskOptions", "updateCallbackHandle handler: $callbackHandle")
            with(prefs.edit()) {
                remove(PrefsKey.CALLBACK_HANDLE)
                if (callbackHandle != null) {
                    putLong(PrefsKey.CALLBACK_HANDLE, callbackHandle)
                    putLong(PrefsKey.CALLBACK_HANDLE_ON_BOOT, callbackHandle)
                }
                apply()
            }
        }

        fun clearData(context: Context) {
            val prefs = context.getSharedPreferences(
                PrefsKey.FOREGROUND_TASK_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)
            Log.d("ForegroundTaskOptions", "clear handler")
            with(prefs.edit()) {
                clear()
                apply()
            }
        }
    }
}
