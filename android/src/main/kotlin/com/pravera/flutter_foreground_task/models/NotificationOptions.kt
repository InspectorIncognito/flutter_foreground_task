package com.pravera.flutter_foreground_task.models

import android.content.Context
import org.json.JSONObject
import com.pravera.flutter_foreground_task.PreferencesKey as PrefsKey

data class NotificationOptions(
    val backgroundColorRgb: String?,
    val notificationData: NotificationData?
) {
    companion object {
        fun getData(context: Context): NotificationOptions {
            val prefs = context.getSharedPreferences(
                PrefsKey.NOTIFICATION_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)

            val backgroundColorRgb = prefs.getString(PrefsKey.NOTIFICATION_COLOR, null)

            val notificationDataJson = prefs.getString(PrefsKey.NOTIFICATION_DATA, null)
            var notificationData: NotificationData? = null
            if (notificationDataJson != null) {
                val notificationDataJsonObj = JSONObject(notificationDataJson)
                notificationData = NotificationData.factory(notificationDataJsonObj)
            }

            return NotificationOptions(
                backgroundColorRgb = backgroundColorRgb,
                notificationData = notificationData,
            )
        }

        fun putData(context: Context, map: Map<*, *>?) {
            val prefs = context.getSharedPreferences(
                PrefsKey.NOTIFICATION_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)

            val backgroundColorRgb = map?.get(PrefsKey.NOTIFICATION_COLOR) as? String ?: ""

            val notificationData = map?.get(PrefsKey.NOTIFICATION_DATA) as? Map<*, *>
            var notificationDataJson: String? = null
            if (notificationData != null) {
                notificationDataJson = JSONObject(notificationData).toString()
            }

            with(prefs.edit()) {
                putString(PrefsKey.NOTIFICATION_COLOR, backgroundColorRgb)
                putString(PrefsKey.NOTIFICATION_DATA, notificationDataJson)
                apply()
            }
        }

        fun updateContent(context: Context, map: Map<*, *>?) {
            val prefs = context.getSharedPreferences(
                PrefsKey.NOTIFICATION_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)

            val notificationData = map?.get(PrefsKey.NOTIFICATION_DATA) as? Map<*, *>
            var notificationDataJson: String? = null
            if (notificationData != null) {
                notificationDataJson = JSONObject(notificationData).toString()
            }

            with(prefs.edit()) {
                putString(PrefsKey.NOTIFICATION_DATA, notificationDataJson)
                apply()
            }
        }

        fun clearData(context: Context) {
            val prefs = context.getSharedPreferences(
                PrefsKey.NOTIFICATION_OPTIONS_PREFS_NAME, Context.MODE_PRIVATE)

            with(prefs.edit()) {
                clear()
                commit()
            }
        }
    }
}
