package com.pravera.flutter_foreground_task.models

import android.util.Log
import com.pravera.flutter_foreground_task.PreferencesKey
import org.json.JSONObject
import java.lang.IllegalStateException

abstract class NotificationData {
    companion object {
        fun factory(data: JSONObject): NotificationData {
            val type = data.getString(PreferencesKey.NOTIFICATION_TYPE) ?: ""

            if (!data.has(PreferencesKey.NOTIFICATION_METADATA)) {
                throw IllegalStateException("WRONG METADATA")
            }
            val metadata = data.getString(PreferencesKey.NOTIFICATION_METADATA)

            val metadataObj = JSONObject(metadata)

            Log.d("ForegroundService", type)
            if (type == "NotificationType.NORMAL") {
                return NormalNotificationData(
                    metadataObj.getString(PreferencesKey.NOTIFICATION_NORMAL_TITLE),
                    metadataObj.getString(PreferencesKey.NOTIFICATION_NORMAL_MESSAGE),
                )
            } else if (type == "NotificationType.ARRIVAL") {
                var plate: String? = null
                if (metadataObj.has(PreferencesKey.NOTIFICATION_ARRIVAL_PLATE)) {
                    plate = metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_PLATE)
                }
                return ArrivalNotificationData(
                    metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_STOP_CODE),
                    metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_TOP),
                    metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_BOTTOM),
                    metadataObj.getInt(PreferencesKey.NOTIFICATION_ARRIVAL_ARRIVING) == 1,
                    plate,
                )
            }
            throw IllegalStateException("WRONG TYPE")
        }
    }
}

class NormalNotificationData(val title: String, val message: String) :
    NotificationData()

class ArrivalNotificationData(val stopCode: String, val topMessage: String, val bottomMessage: String, val arriving: Boolean, val plate: String?) :
    NotificationData()