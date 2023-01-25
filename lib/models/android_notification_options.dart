import 'dart:ui';


/// Notification options for Android platform.
class AndroidNotificationOptions {
  /// Constructs an instance of [AndroidNotificationOptions].
  const AndroidNotificationOptions({
    this.backgroundColor,
  });

  /// Notification icon background color.
  final Color? backgroundColor;

  /// Returns the data fields of [AndroidNotificationOptions] in JSON format.
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
