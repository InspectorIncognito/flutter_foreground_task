import 'dart:convert';

enum NotificationType { ARRIVAL, TRAVEL, NORMAL }

/// Notification options for Android platform.
abstract class NotificationData {
  final Map<String, dynamic> _metadata;
  final NotificationType _type;
  final int _notificationID;
  final bool _vibrationEnabled;

  /// Constructs an instance of [NotificationData].
  const NotificationData(this._metadata, this._type, this._notificationID, this._vibrationEnabled);

  /// Returns the data fields of [NotificationData] in JSON format.
  Map<String, dynamic> toJson() {
    return {
      'notificationMetadata': jsonEncode(_metadata),
      'notificationType': _type.toString(),
      'notificationId': _notificationID,
      'notificationVibration': _vibrationEnabled,
    };
  }
}

class NormalNotificationData extends NotificationData {
  NormalNotificationData(int id, String title, String message, {bool vibrationEnabled=false})
      : super({"title": title, "message": message}, NotificationType.NORMAL, id, vibrationEnabled);
}

class ArrivalNotificationData extends NotificationData {
  ArrivalNotificationData.plate(
      int id, String stopCode, String topMessage, String bottomMessage, String plate, {bool arriving=false, bool vibrationEnabled=false})
      : super({
          "stopCode": stopCode,
          "topMessage": topMessage,
          "bottomMessage": bottomMessage,
          "arriving": arriving ? 1 : 0,
          "plate": plate,
        }, NotificationType.ARRIVAL, id, vibrationEnabled);

  ArrivalNotificationData(
      int id, String stopCode, String topMessage, String bottomMessage, {bool arriving=false, bool vibrationEnabled=false})
      : super({
    "stopCode": stopCode,
    "topMessage": topMessage,
    "bottomMessage": bottomMessage,
    "arriving": arriving ? 1 : 0,
  }, NotificationType.ARRIVAL, id, vibrationEnabled);
}

class TravelNotificationData extends NotificationData {

  TravelNotificationData(
      int id, String destinationCode, String destinationStops, String destinationName, String topMessage, {bool vibrationEnabled=false, String destinationStopsSuffix=""})
      : super({
    "destinationCode": destinationCode,
    "destinationStops": destinationStops,
    "destinationName": destinationName,
    "topMessage": topMessage,
    "destinationStopsSuffix": destinationStopsSuffix,
  }, NotificationType.TRAVEL, id, vibrationEnabled);
}
