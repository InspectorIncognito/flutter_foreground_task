//
//  BackgroundService.swift
//  flutter_foreground_task
//
//  Created by WOO JIN HWANG on 2021/08/11.
//

import Flutter
import Foundation
import UserNotifications
import CoreLocation

// let NOTIFICATION_ID: String = "flutter_foreground_task/backgroundNotification"
let BG_ISOLATE_NAME: String = "flutter_foreground_task/backgroundIsolate"
let BG_CHANNEL_NAME: String = "flutter_foreground_task/background"

let ACTION_TASK_START: String = "onStart"
let ACTION_TASK_EVENT: String = "onEvent"
let ACTION_TASK_DESTROY: String = "onDestroy"

@available(iOS 10.0, *)
class BackgroundService: BusableController {
  static let sharedInstance = BackgroundService()
  
  var isRunningService: Bool = false
  
  private let userNotificationCenter: UNUserNotificationCenter
  private var isGrantedNotificationAuthorization: Bool = false
  
  private var notificationContentTitle: String = ""
  private var notificationContentText: String = ""
  private var showNotification: Bool = true
  private var playSound: Bool = false
  private var taskInterval: Int = 5000
  private var isOnceEvent: Bool = false
  
  private var flutterEngine: FlutterEngine? = nil
  private var backgroundChannel: FlutterMethodChannel? = nil
  private var backgroundTaskTimer: Timer? = nil
  private var lastLocation: CLLocation? = nil
    
    /// are setup in the ViewController.setupBus method
  var subs: BusableController.Subs = [:]
  override var SubscriptionEvents: BusableController.Subs {
      get { return self.subs }
  }
  
  override init() {
    userNotificationCenter = UNUserNotificationCenter.current()
    super.init()
    setupBus()
    register()
    LocationManager.shared.requestAccess()
    // userNotificationCenter.delegate = self
  }
    
  deinit {
    deregister()
  }
  
  func run(action: BackgroundServiceAction) {
    let prefs = UserDefaults.standard
    showNotification = true
//    showNotification = prefs.bool(forKey: SHOW_NOTIFICATION)
//    playSound = prefs.bool(forKey: PLAY_SOUND)

    taskInterval = prefs.integer(forKey: TASK_INTERVAL)
//    isOnceEvent = prefs.bool(forKey: IS_ONCE_EVENT)
    
    switch action {
      case .START:
        requestNotificationAuthorization()
        isRunningService = true
        if let callbackHandle = prefs.object(forKey: CALLBACK_HANDLE) as? Int64 {
          executeDartCallback(callbackHandle: callbackHandle)
          self.startJob()
          self.sendNotification()
        }
        break
      case .RESTART:
        break
      case .UPDATE:
        self.sendNotification()
        isRunningService = true
        if let callbackHandle = prefs.object(forKey: CALLBACK_HANDLE) as? Int64 {
          executeDartCallback(callbackHandle: callbackHandle)
        }
        break
      case .STOP:
        destroyBackgroundChannel() { _ in
          self.isRunningService = false
          self.isGrantedNotificationAuthorization = false
          self.stopJob()
          self.removeAllNotification()
        }
        break
    case .NOTIFY:
        self.sendNotification()
    }
  }
  
  private func requestNotificationAuthorization() {
    if showNotification {
      let options = UNAuthorizationOptions(arrayLiteral: .alert, .sound)
      userNotificationCenter.requestAuthorization(options: options) { success, error in
        if let error = error {
          print("Authorization error: \(error)")
        } else {
          if (success) {
            self.isGrantedNotificationAuthorization = true
            self.sendNotification()
          } else {
            print("Notification authorization denied.")
          }
        }
      }
    }
  }
  
  private func sendNotification() {
    if isGrantedNotificationAuthorization && showNotification {
      let prefs = UserDefaults.standard
        
      guard let notificationContentTitle = prefs.string(forKey: NOTIFICATION_CONTENT_TITLE),
            let notificationContentText = prefs.string(forKey: NOTIFICATION_CONTENT_TEXT),
            let notificationId = prefs.string(forKey: NOTIFICATION_ID) else {
        return
      }
        
      let notificationContent = UNMutableNotificationContent()
      notificationContent.title = notificationContentTitle
      notificationContent.body = notificationContentText
      if playSound {
        notificationContent.sound = UNNotificationSound.default
      }
      
      let request = UNNotificationRequest(identifier: notificationId, content: notificationContent, trigger: nil)
      userNotificationCenter.add(request, withCompletionHandler: nil)
    }
  }
  
  private func removeAllNotification() {
    userNotificationCenter.removeAllDeliveredNotifications()
    userNotificationCenter.removeAllPendingNotificationRequests()
  }
  
  private func executeDartCallback(callbackHandle: Int64) {
    destroyBackgroundChannel() { _ in
      // The backgroundChannel cannot be registered unless the registerPlugins function is called.
      if (SwiftFlutterForegroundTaskPlugin.registerPlugins == nil) { return }
      
      self.flutterEngine = FlutterEngine(name: BG_ISOLATE_NAME, project: nil, allowHeadlessExecution: true)
      
      let callbackInfo = FlutterCallbackCache.lookupCallbackInformation(callbackHandle)
      let entrypoint = callbackInfo?.callbackName
      let uri = callbackInfo?.callbackLibraryPath
      self.flutterEngine?.run(withEntrypoint: entrypoint, libraryURI: uri)
      
      SwiftFlutterForegroundTaskPlugin.registerPlugins!(self.flutterEngine!)
      
      let backgroundMessenger = self.flutterEngine!.binaryMessenger
      self.backgroundChannel = FlutterMethodChannel(name: BG_CHANNEL_NAME, binaryMessenger: backgroundMessenger)
      self.backgroundChannel?.setMethodCallHandler(self.onMethodCall)
    }
  }
  
  private func destroyBackgroundChannel(onComplete: @escaping (Bool) -> Void) {
    //stopJob()
    
    // The background task destruction is complete and a new background task can be started.
    if backgroundChannel == nil {
      onComplete(true)
    } else {
      backgroundChannel?.invokeMethod(ACTION_TASK_DESTROY, arguments: nil) { _ in
        self.flutterEngine?.destroyContext()
        self.flutterEngine = nil
        self.backgroundChannel = nil
        onComplete(true)
      }
    }
  }
  
  private func onMethodCall(call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
      case "initialize":
        //startBackgroundTask()
        break
      default:
        result(FlutterMethodNotImplemented)
    }
  }
  
  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              didReceive response: UNNotificationResponse,
                              withCompletionHandler completionHandler: @escaping () -> Void) {
    // If it is not a notification requested by this plugin, the processing below is ignored.
    if response.notification.request.identifier != NOTIFICATION_ID { return }
    
    completionHandler()
  }
  
  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              willPresent notification: UNNotification,
                              withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
    // If it is not a notification requested by this plugin, the processing below is ignored.
    if notification.request.identifier != NOTIFICATION_ID { return }
    
    if playSound {
      completionHandler([.alert, .sound])
    } else {
      completionHandler([.alert])
    }
  }
}

extension BackgroundService {
    
    func startJob() {
        print("VC: startJob")
        let gps = LocationManager.shared
        if gps.isHasAccess() {
            gps.startMonitoring()
            print("VC: LongProcessJob start")
            self.backgroundChannel?.invokeMethod(ACTION_TASK_START, arguments: nil)
            let queue = DispatchQueue(label: "LongProcessJob start")
            // start async
            queue.async {
                while true {
                    var data = ""
                    if let last = self.lastLocation {
                        data = "\(last.coordinate.latitude)|\(last.coordinate.longitude)"
                    }
                    self.backgroundChannel?.invokeMethod(ACTION_TASK_EVENT, arguments: data)
                    if !self.isRunningService {
                        break
                    }
                    let secs = Double(self.taskInterval / 1000)
                    // sleep for secs
                    Thread.sleep(until: Date(timeIntervalSinceNow: secs))
                }
                print("VC: LongProcessJob stop")
            }
        } else {
            print("VC: no access")
        }
    }
    
    func stopJob() {
        print("VC: stopJob")
        self.backgroundChannel?.invokeMethod(ACTION_TASK_DESTROY, arguments: nil)
        let gps = LocationManager.shared
        if gps.state == .Monitoring {
            gps.stopMonitoring()
        }
    }
}

extension BackgroundService {
    func setupBus() {
        self.subs = [
            .AppEnteredBackground: self.enteredBackground(_:),
            .AppEnteredForeground: self.enteredForeground(_:),
            .LocationUpdate: self.locationAccessChanged(notification:),
        ]
    }
    
    private func enteredBackground(_: Notification) {
        print("VC: App entered background")
    }
    
    private func enteredForeground(_: Notification) {
        print("VC: App entered foreground")
    }
    
    private func locationAccessChanged(notification: Notification) {
        print("VC: new location")
        let info = notification.userInfo
        if let locations = info?["locations"] as? [CLLocation] {
            if locations.isEmpty {
                return
            }
            lastLocation = locations.first
        }
    }
}
