//
//  BackgroundServiceManager.swift
//  flutter_foreground_task
//
//  Created by WOO JIN HWANG on 2021/08/10.
//

import Flutter
import Foundation

class BackgroundServiceManager: NSObject {
  func start(call: FlutterMethodCall) -> Bool {
    if #available(iOS 10.0, *) {
      saveOptions(call: call)
      BackgroundService.sharedInstance.run(action: BackgroundServiceAction.START)
    } else {
      // Fallback on earlier versions
      return false
    }
    
    return true
  }
  
  func restart(call: FlutterMethodCall) -> Bool {
    return true
  }
  
  func update(call: FlutterMethodCall) -> Bool {
    if #available(iOS 10.0, *) {
      updateOptions(call: call)
      BackgroundService.sharedInstance.run(action: BackgroundServiceAction.UPDATE)
    } else {
      // Fallback on earlier versions
      return false
    }
    
    return true
  }
  
  func stop() -> Bool {
    if #available(iOS 10.0, *) {
      clearOptions()
      BackgroundService.sharedInstance.run(action: BackgroundServiceAction.STOP)
    } else {
      // Fallback on earlier versions
      return false
    }
    
    return true
  }
  
  func isRunningService() -> Bool {
    if #available(iOS 10.0, *) {
      return BackgroundService.sharedInstance.isRunningService
    } else {
      return false
    }
  }
    
  func notify(call: FlutterMethodCall) -> Bool {
    if #available(iOS 10.0, *) {
      guard let argsDict = call.arguments as? Dictionary<String, Any> else { return false }
      saveNotificationOptions(argsDict: argsDict)
      BackgroundService.sharedInstance.run(action: BackgroundServiceAction.NOTIFY)
    } else {
      // Fallback on earlier versions
      return false
    }
  
    return true
  }
    
  func cancelNotification(call: FlutterMethodCall) -> Bool {
    if #available(iOS 10.0, *) {
      guard let argsDict = call.arguments as? Dictionary<String, Any> else { return false }
      if let id = argsDict[NOTIFICATION_ID] as? Int {
        let center = UNUserNotificationCenter.current()
        center.removeDeliveredNotifications(withIdentifiers: ["\(id)"])
      }
    } else {
      // Fallback on earlier versions
      return false
    }
    
    return true
  }
    
    private func saveNotificationOptions(argsDict: Dictionary<String, Any>) {
        let prefs = UserDefaults.standard
        
        if let dictFromJSON = argsDict[NOTIFICATION_DATA] as? Dictionary<String, Any> {
          do {
            let notificationType = dictFromJSON[NOTIFICATION_TYPE] as? String ?? ""
            guard let id = dictFromJSON[NOTIFICATION_ID] as? Int else {
              print("WRONG ID")
              return
            }
            prefs.set("\(id)", forKey: NOTIFICATION_ID)
            guard let raw = dictFromJSON[NOTIFICATION_METADATA] as? String,
                  let metadata = try JSONSerialization.jsonObject(with: raw.data(using: .utf8)!, options: []) as? [String:Any] else {
              print("WRONG METADATA")
              return
            }
            var title = ""
            var message = ""
            if notificationType == "NotificationType.NORMAL" {
              title = metadata[NOTIFICATION_NORMAL_TITLE] as? String ?? ""
              message = metadata[NOTIFICATION_NORMAL_MESSAGE] as? String ?? ""
            } else if notificationType == "NotificationType.ARRIVAL" {
              let stopCode = metadata[NOTIFICATION_ARRIVAL_STOP_CODE] as? String ?? ""
              let start = metadata[NOTIFICATION_ARRIVAL_TOP] as? String ?? ""
              let end = metadata[NOTIFICATION_ARRIVAL_BOTTOM] as? String ?? ""
              title = "Esperando en paradero \(stopCode)"
              message = "\(start), \(end)"
            } else if notificationType == "NotificationType.TRAVEL" {
              let destination = metadata[NOTIFICATION_TRAVEL_CODE] as? String ?? ""
              if destination == "" {
                let topMessage = metadata[NOTIFICATION_TRAVEL_TOP] as? String ?? ""
                title = "En viaje, \(topMessage)"
                message = "Sin destino definido"
              } else {
                let stops = metadata[NOTIFICATION_TRAVEL_STOPS] as? String ?? ""
                let suffix = metadata[NOTIFICATION_TRAVEL_STOPS_SUFFIX] as? String ?? ""
                let name = metadata[NOTIFICATION_TRAVEL_NAME] as? String ?? ""
                title = "En viaje a paradero \(destination)"
                message = "\(stops) \(suffix) hasta \(name)"
              }
              title = metadata[NOTIFICATION_NORMAL_TITLE] as? String ?? ""
            }
            print("\(title), \(message)")
            
            if title != "" && message != "" {
              prefs.set(title, forKey: NOTIFICATION_CONTENT_TITLE)
              prefs.set(message, forKey: NOTIFICATION_CONTENT_TEXT)
            }
          } catch {}
        }
    }
  
  private func saveOptions(call: FlutterMethodCall) {
    guard let argsDict = call.arguments as? Dictionary<String, Any> else { return }
    let prefs = UserDefaults.standard
    
//    let notificationContentTitle = argsDict[NOTIFICATION_CONTENT_TITLE] as? String ?? ""
//    let notificationContentText = argsDict[NOTIFICATION_CONTENT_TEXT] as? String ?? ""
//    let showNotification = argsDict[SHOW_NOTIFICATION] as? Bool ?? true
//    let playSound = argsDict[PLAY_SOUND] as? Bool ?? false
    let taskInterval = argsDict[TASK_INTERVAL] as? Int ?? 5000
    let isOnceEvent = argsDict[IS_ONCE_EVENT] as? Bool ?? false
    let callbackHandle = argsDict[CALLBACK_HANDLE] as? Int64
      
    saveNotificationOptions(argsDict: argsDict)
    
    prefs.set(taskInterval, forKey: TASK_INTERVAL)
    prefs.set(isOnceEvent, forKey: IS_ONCE_EVENT)
    prefs.removeObject(forKey: CALLBACK_HANDLE)
    prefs.removeObject(forKey: CALLBACK_HANDLE_ON_RESTART)
    if callbackHandle != nil {
      prefs.set(callbackHandle, forKey: CALLBACK_HANDLE)
      prefs.set(callbackHandle, forKey: CALLBACK_HANDLE_ON_RESTART)
    }
  }
  
  private func updateOptions(call: FlutterMethodCall) {
    guard let argsDict = call.arguments as? Dictionary<String, Any> else { return }
    let prefs = UserDefaults.standard
    
    let callbackHandle = argsDict[CALLBACK_HANDLE] as? Int64
    saveNotificationOptions(argsDict: argsDict)
    
    prefs.removeObject(forKey: CALLBACK_HANDLE)
    if callbackHandle != nil {
      prefs.set(callbackHandle, forKey: CALLBACK_HANDLE)
      prefs.set(callbackHandle, forKey: CALLBACK_HANDLE_ON_RESTART)
    }
  }
  
  private func clearOptions() {
    let prefs = UserDefaults.standard
    prefs.removeObject(forKey: NOTIFICATION_CONTENT_TITLE)
    prefs.removeObject(forKey: NOTIFICATION_CONTENT_TEXT)
    prefs.removeObject(forKey: SHOW_NOTIFICATION)
    prefs.removeObject(forKey: PLAY_SOUND)
    prefs.removeObject(forKey: TASK_INTERVAL)
    prefs.removeObject(forKey: IS_ONCE_EVENT)
    prefs.removeObject(forKey: CALLBACK_HANDLE)
    prefs.removeObject(forKey: CALLBACK_HANDLE_ON_RESTART)
  }
  
  @available(iOS 10.0, *)
  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              didReceive response: UNNotificationResponse,
                              withCompletionHandler completionHandler: @escaping () -> Void) {
    BackgroundService.sharedInstance
      .userNotificationCenter(center, didReceive: response, withCompletionHandler: completionHandler)
  }
  
  @available(iOS 10.0, *)
  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              willPresent notification: UNNotification,
                              withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
    BackgroundService.sharedInstance
      .userNotificationCenter(center, willPresent: notification, withCompletionHandler: completionHandler)
  }
}
