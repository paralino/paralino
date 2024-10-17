//
//  NotificationService.swift
//  paralino-nse
//
//  Created by Zelimir on 2024-10-08.
//

import UserNotifications
import paralino_apple_core
import shared
import Sentry

class NotificationService: UNNotificationServiceExtension {
    
    let locationManager: LocationManager
    
    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?
    lazy var pushProvider: PushSetupProvider = { KoinIOS.Instances.shared.pushProvider as! PushSetupProvider }()
    lazy var inAppLogger: InAppLogger = { KoinIOS.Instances.shared.inAppLogger }()
    
    override init() {
        self.locationManager = LocationManager(setupManager: false)
        SentrySDK.start { options in
            options.dsn = BuildKonfig.shared.glitchTipDSN
            options.debug = false
        }
        locationManager.manager.delegate = locationManager
        KoinSetup.initKoin(locationManager: self.locationManager)
    }
    
    override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)
        
        Logr.w(tag: "notification", message: "Received alert notification")
        
        if let bestAttemptContent = bestAttemptContent {
            DispatchQueue.main.async { [weak self] in
                Task {
                    Logr.v(tag: "notification", message: "Try to parse notification ID")
                    let notificationId = bestAttemptContent.userInfo["notificationId"] as? String
                    if let notificationId, let pushProvider = self?.pushProvider {
                        Logr.v(tag: "notification", message: "Try to fetch new alert notification")
                        let payload = try? await pushProvider.getNotificationPayload(notificationId: notificationId)
                        
                        if let payload = payload {
                            self?.inAppLogger.logNotificationPayload(payload: payload)
                        }
                        
                        if let content = payload?.notificationContent() {
                            try? await pushProvider.deleteNotification(notificationId: notificationId)
                            Logr.v(tag: "notification", message: "Display new alert notification")
                            
                            if AppBuildConfig.companion.instance.isDevMode {
                                content.title = "NSE: \(content.title)"
                            }
                            
                            contentHandler(content)
                        } else {
                            Logr.w(tag: "notification", message: "Notification not fetched")
                            if AppBuildConfig.companion.instance.isDevMode {
                                bestAttemptContent.title = "NSE: \(bestAttemptContent.title)"
                                bestAttemptContent.body = "Not fetched: \(bestAttemptContent.body)"
                            }
                            self?.suppressNotification(contentHandler, content: bestAttemptContent)
                        }
                    } else {
                        Logr.w(tag: "notification", message: "No notification id")
                        if AppBuildConfig.companion.instance.isDevMode {
                            bestAttemptContent.title = "NSE: \(bestAttemptContent.title)"
                            bestAttemptContent.body = "Missing ID: \(bestAttemptContent.body)"
                        }
                        self?.suppressNotification(contentHandler, content: bestAttemptContent)
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
        contentHandler(content ?? UNNotificationContent())
    }
    
    override func serviceExtensionTimeWillExpire() {
        // Called just before the extension will be terminated by the system.
        // Use this as an opportunity to deliver your "best attempt" at modified content, otherwise the original push payload will be used.
        if let contentHandler = contentHandler, let bestAttemptContent =  bestAttemptContent {
            if AppBuildConfig.companion.instance.isDevMode {
                bestAttemptContent.title = "NSE: \(bestAttemptContent.title)"
                bestAttemptContent.body = "Expired: \(bestAttemptContent.body)"
            }
            suppressNotification(contentHandler, content: bestAttemptContent)
        }
    }
}
