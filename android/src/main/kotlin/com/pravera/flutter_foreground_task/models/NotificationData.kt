package com.pravera.flutter_foreground_task.models

import com.pravera.flutter_foreground_task.PreferencesKey
import org.json.JSONObject
import java.lang.IllegalStateException

abstract class NotificationData(val id: Int, val vibration: Boolean) {
    companion object {
        fun factory(data: JSONObject): NotificationData {
            val type = data.getString(PreferencesKey.NOTIFICATION_TYPE) ?: ""
            val id = data.getInt(PreferencesKey.NOTIFICATION_ID)
            val vibration = data.getBoolean(PreferencesKey.NOTIFICATION_VIBRATION)

            if (!data.has(PreferencesKey.NOTIFICATION_METADATA)) {
                throw IllegalStateException("WRONG METADATA")
            }
            val metadata = data.getString(PreferencesKey.NOTIFICATION_METADATA)

            val metadataObj = JSONObject(metadata)

            when (type) {
                "NotificationType.NORMAL" -> {
                    return NormalNotificationData(
                        id,
                        vibration,
                        metadataObj.getString(PreferencesKey.NOTIFICATION_NORMAL_TITLE),
                        metadataObj.getString(PreferencesKey.NOTIFICATION_NORMAL_MESSAGE),
                    )
                }
                "NotificationType.ARRIVAL" -> {
                    var plate: String? = null
                    if (metadataObj.has(PreferencesKey.NOTIFICATION_ARRIVAL_PLATE)) {
                        plate = metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_PLATE)
                    }
                    return ArrivalNotificationData(
                        id,
                        vibration,
                        metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_STOP_CODE),
                        metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_TOP),
                        metadataObj.getString(PreferencesKey.NOTIFICATION_ARRIVAL_BOTTOM),
                        metadataObj.getInt(PreferencesKey.NOTIFICATION_ARRIVAL_ARRIVING) == 1,
                        plate,
                    )
                }
                "NotificationType.TRAVEL" -> {
                    return TravelNotificationData(
                        id,
                        vibration,
                        metadataObj.getString(PreferencesKey.NOTIFICATION_TRAVEL_CODE),
                        metadataObj.getString(PreferencesKey.NOTIFICATION_TRAVEL_STOPS),
                        metadataObj.getString(PreferencesKey.NOTIFICATION_TRAVEL_NAME),
                        metadataObj.getString(PreferencesKey.NOTIFICATION_TRAVEL_TOP),
                        metadataObj.getString(PreferencesKey.NOTIFICATION_TRAVEL_STOPS_SUFFIX),
                    )
                }
                else -> throw IllegalStateException("WRONG TYPE")
            }
        }
    }
}

class NormalNotificationData(id: Int, vibration: Boolean, val title: String, val message: String) :
    NotificationData(id, vibration)

class ArrivalNotificationData(id: Int, vibration: Boolean, val stopCode: String, val topMessage: String, val bottomMessage: String, val arriving: Boolean, val plate: String?) :
    NotificationData(id, vibration)

class TravelNotificationData(id: Int, vibration: Boolean, val destinationCode: String, val destinationStops: String, val destinationName: String, val topMessage: String, val destinationStopsSuffix: String) :
    NotificationData(id, vibration)