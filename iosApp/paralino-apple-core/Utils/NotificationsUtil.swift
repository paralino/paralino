//
//  NotificationsUtil.swift
//  paralino_apple_core
//
//  Created by Zelimir on 2024-10-08.
//

import Foundation
import UserNotifications
import shared

extension IPushNotificationPresenter.Payload {
    public func notificationContent() -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        
        let formattedDate = timeInfo ?? ""
        
        if let message = message {
            content.title = title
            content.subtitle = formattedDate
            content.body = message
        } else {
            content.title = "Paralino"
            content.subtitle = formattedDate
            content.body = title
        }
        
        return content
    }
}
