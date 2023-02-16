import 'dart:io';
import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_foreground_task/models/notification_data.dart';

import 'flutter_foreground_task_platform_interface.dart';
import 'models/android_notification_options.dart';
import 'models/foreground_task_options.dart';
import 'models/ios_notification_options.dart';

/// An implementation of [FlutterForegroundTaskPlatform] that uses method channels.
class MethodChannelFlutterForegroundTask extends FlutterForegroundTaskPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_foreground_task/methods');

  @override
  Future<void> initService({
    Function? callback,
  }) async {
    if (await isRunningService == false) {
      final options = {};
      if (callback != null) {
        options['callbackHandle'] =
            PluginUtilities.getCallbackHandle(callback)?.toRawHandle();
      }

      return await methodChannel.invokeMethod('initService', options);
    }
  }

  @override
  Future<bool> startService({
    required AndroidNotificationOptions androidNotificationOptions,
    required IOSNotificationOptions iosNotificationOptions,
    required ForegroundTaskOptions foregroundTaskOptions,
    required NotificationData notificationData,
    Function? callback,
  }) async {
    if (await isRunningService == false) {
      final options = Platform.isAndroid
          ? androidNotificationOptions.toJson()
          : iosNotificationOptions.toJson();
      options['notificationData'] = notificationData.toJson();
      options.addAll(foregroundTaskOptions.toJson());
      if (callback != null) {
        options['callbackHandle'] =
            PluginUtilities.getCallbackHandle(callback)?.toRawHandle();
      }
      return await methodChannel.invokeMethod('startService', options);
    }
    return false;
  }

  @override
  Future<bool> restartService() async {
    if (await isRunningService) {
      return await methodChannel.invokeMethod('restartService');
    }
    return false;
  }

  @override
  Future<bool> updateService({
    required NotificationData notificationData,
    Function? callback,
  }) async {
    if (await isRunningService) {
      final options = <String, dynamic>{
        'notificationData': notificationData.toJson(),
      };
      if (callback != null) {
        options['callbackHandle'] =
            PluginUtilities.getCallbackHandle(callback)?.toRawHandle();
      }
      return await methodChannel.invokeMethod('updateService', options);
    }
    return false;
  }

  @override
  Future<bool> notify({
    required NotificationData notificationData,
  }) async {
    final options = <String, dynamic>{
      'notificationData': notificationData.toJson(),
    };
    return await methodChannel.invokeMethod('notify', options);
  }

  @override
  Future<bool> cancelNotification({required int id}) async {
    final options = <String, dynamic>{
      'notificationId': id,
    };
    return await methodChannel.invokeMethod('cancelNotification', options);
  }

  @override
  Future<bool> stopService() async {
    if (await isRunningService) {
      return await methodChannel.invokeMethod('stopService');
    }
    return false;
  }

  @override
  Future<bool> get isRunningService async {
    return await methodChannel.invokeMethod('isRunningService');
  }

  @override
  void minimizeApp() => methodChannel.invokeMethod('minimizeApp');

  @override
  void launchApp([String? route]) {
    if (Platform.isAndroid) {
      methodChannel.invokeMethod('launchApp', route);
    }
  }

  @override
  void setOnLockScreenVisibility(bool isVisible) {
    if (Platform.isAndroid) {
      methodChannel
          .invokeMethod('setOnLockScreenVisibility', {'isVisible': isVisible});
    }
  }

  @override
  Future<bool> get isAppOnForeground async {
    return await methodChannel.invokeMethod('isAppOnForeground');
  }

  @override
  void wakeUpScreen() {
    if (Platform.isAndroid) {
      methodChannel.invokeMethod('wakeUpScreen');
    }
  }

  @override
  Future<bool> get isIgnoringBatteryOptimizations async {
    if (Platform.isAndroid) {
      return await methodChannel.invokeMethod('isIgnoringBatteryOptimizations');
    }
    return true;
  }

  @override
  Future<bool> openIgnoreBatteryOptimizationSettings() async {
    if (Platform.isAndroid) {
      return await methodChannel
          .invokeMethod('openIgnoreBatteryOptimizationSettings');
    }
    return true;
  }

  @override
  Future<bool> requestIgnoreBatteryOptimization() async {
    if (Platform.isAndroid) {
      return await methodChannel
          .invokeMethod('requestIgnoreBatteryOptimization');
    }
    return true;
  }

  @override
  Future<bool> get canDrawOverlays async {
    if (Platform.isAndroid) {
      return await methodChannel.invokeMethod('canDrawOverlays');
    }
    return true;
  }

  @override
  Future<bool> openSystemAlertWindowSettings({bool forceOpen = false}) async {
    if (Platform.isAndroid) {
      return await methodChannel.invokeMethod(
          'openSystemAlertWindowSettings', {'forceOpen': forceOpen});
    }
    return true;
  }
}
