//
//  PushNotificationHandlerIOS.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import UserNotifications

class PlaceNotificationPresenter: IPlaceActivityNotificationPresenter {
    override func showPushNotification(payload: IPushNotificationPresenter.Payload) {
        let content = payload.notificationContent()
    
        let request = UNNotificationRequest(identifier: "\(payload.notificationId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}

class GroupActivityNotificationPresenter: IGroupActivityNotificationPresenter {
    override func showPushNotification(payload: IPushNotificationPresenter.Payload) {
        let content = payload.notificationContent()
    
        let request = UNNotificationRequest(identifier: "\(payload.notificationId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}

class InfoNotificationPresenter: IInfoMessagesNotificationPresenter {
    override func showPushNotification(payload: IPushNotificationPresenter.Payload) {
        let content = payload.notificationContent()
    
        let request = UNNotificationRequest(identifier: "\(payload.notificationId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}

extension IPushNotificationPresenter.Payload {
    func notificationContent() -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        
        let formattedDate = timeInfo != nil ? "\n\(timeInfo ?? "")" : ""
        
        if let message = message {
            content.title = title
            content.body = message + formattedDate
        } else {
            content.body = title + formattedDate
        }
        
        return content
    }
}
