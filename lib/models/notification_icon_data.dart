import 'package:flutter/material.dart';

/// Data for setting the notification icon.
class NotificationIconData {
  /// Constructs an instance of [NotificationIconData].
  const NotificationIconData({
    this.backgroundColor,
  });

  /// Notification icon background color.
  final Color? backgroundColor;

  /// Returns the data fields of [NotificationIconData] in JSON format.
  Map<String, dynamic> toJson() {
    String? backgroundColorRgb;
    if (backgroundColor != null) {
      backgroundColorRgb =
          '${backgroundColor!.red},${backgroundColor!.green},${backgroundColor!.blue}';
    }

    return {
      'backgroundColorRgb': backgroundColorRgb,
    };
  }
}
