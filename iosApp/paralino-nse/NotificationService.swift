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
    
    override init() {
        self.locationManager = LocationManager(setupManager: false)
    }

    override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)
        
        locationManager.manager.delegate = locationManager
        KoinSetup.initKoin(locationManager: self.locationManager)
        
        Logr.w(tag: "notification", message: "Received alert notification")
        
        if let bestAttemptContent = bestAttemptContent {
            // Modify the notification content here...
            
            DispatchQueue.main.async { [weak self] in
                Task {
                    let notificationId = bestAttemptContent.userInfo["notificationId"] as? String
                    if let notificationId, let pushProvider = self?.pushProvider {
                        let payload = await pushProvider.getNotificationPayload(notificationId: notificationId)
                        let content = payload?.notificationContent()
                        
                        if let content {
                            contentHandler(content)
                        } else {
                            self?.suppressNotification(contentHandler)
                        }
                    } else {
                        self?.suppressNotification(contentHandler)
                    }
                }
            }
        } else {
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
