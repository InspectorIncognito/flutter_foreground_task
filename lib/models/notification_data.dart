import 'dart:convert';

enum NotificationType { ARRIVAL, TRAVEL, NORMAL }

/// Notification options for Android platform.
abstract class NotificationData {
  final Map<String, dynamic> _metadata;
  final NotificationType _type;

  /// Constructs an instance of [NotificationData].
  const NotificationData(this._metadata, this._type);

  /// Returns the data fields of [NotificationData] in JSON format.
  Map<String, dynamic> toJson() {
    return {
      'notificationMetadata': jsonEncode(_metadata),
      'notificationType': _type.toString()
    };
  }
}

class NormalNotificationData extends NotificationData {
  NormalNotificationData(String title, String message)
      : super({"title": title, "message": message}, NotificationType.NORMAL);
}

class ArrivalNotificationData extends NotificationData {
  ArrivalNotificationData.plate(
      String stopCode, String topMessage, String bottomMessage, String plate, {bool arriving=false})
      : super({
          "stopCode": stopCode,
          "topMessage": topMessage,
          "bottomMessage": bottomMessage,
          "arriving": arriving ? 1 : 0,
          "plate": plate,
        }, NotificationType.ARRIVAL);

  ArrivalNotificationData(
      String stopCode, String topMessage, String bottomMessage, {bool arriving=false})
      : super({
    "stopCode": stopCode,
    "topMessage": topMessage,
    "bottomMessage": bottomMessage,
    "arriving": arriving ? 1 : 0,
  }, NotificationType.ARRIVAL);
}
