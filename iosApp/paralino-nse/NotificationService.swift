//
//  NotificationService.swift
//  paralino-nse
//
//  Created by Zelimir on 2024-10-08.
//

import UserNotifications
import paralino_apple_core
import shared

class NotificationService: UNNotificationServiceExtension {
    
    let locationManager: LocationManager
    
    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?
    lazy var pushProvider: PushSetupProvider = { KoinIOS.Instances.shared.pushProvider as! PushSetupProvider }()
    lazy var inAppLogger: InAppLogger = { KoinIOS.Instances.shared.inAppLogger }()
    
    override init() {
        self.locationManager = LocationManager(setupManager: false)
        locationManager.manager.delegate = locationManager
        KoinSetup.initKoin(locationManager: self.locationManager)
    }
    
    override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)
        
        Logr.w(tag: "notification", message: "Received alert notification")
        
        if let bestAttemptContent = bestAttemptContent {
            Logr.w(tag: "notification", message: "Launch main async to fetch notification")
            DispatchQueue.main.async { [weak self] in
                Logr.w(tag: "notification", message: "Launch new Task to fetch notification")
                Task {
                    Logr.w(tag: "notification", message: "Try to parse notification ID")
                    let notificationId = bestAttemptContent.userInfo["notificationId"] as? String
                    if let notificationId, let pushProvider = self?.pushProvider {
                        Logr.v(tag: "notification", message: "Try to fetch new alert notification")
                        let payload = try? await pushProvider.getNotificationPayload(notificationId: notificationId)
                        
                        if let payload = payload {
                            self?.inAppLogger.logNotificationPayload(payload: payload)
                        }
                        
                        if let content = payload?.notificationContent() {
                            Logr.v(tag: "notification", message: "Display new alert notification")
                            //                                try? await pushProvider.deleteNotification(notificationId: notificationId)
                            contentHandler(content)
                        } else {
                            Logr.w(tag: "notification", message: "Notification not fetched")
                            self?.suppressNotification(contentHandler)
                        }
                    } else {
                        Logr.w(tag: "notification", message: "No notification id")
                        self?.suppressNotification(contentHandler)
                    }
                }
            }
        } else {
            Logr.w(tag: "notification", message: "Suppress new alert notification")
            suppressNotification(contentHandler)
        }
    }
    
    func suppressNotification(_ contentHandler: @escaping (UNNotificationContent) -> Void, content: UNNotificationContent? = nil) {
        // Notification will be suppressed only if com.apple.developer.usernotifications.filtering is granted
        contentHandler(UNNotificationContent())
    }
    
    override func serviceExtensionTimeWillExpire() {
        // Called just before the extension will be terminated by the system.
        // Use this as an opportunity to deliver your "best attempt" at modified content, otherwise the original push payload will be used.
        if let contentHandler = contentHandler, let bestAttemptContent =  bestAttemptContent {
            suppressNotification(contentHandler, content: bestAttemptContent)
        }
    }
}
