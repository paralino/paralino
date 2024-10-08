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
            content.subtitle = message
            content.body = formattedDate
        } else {
            content.title = title
            content.body = formattedDate
        }
        
        return content
    }
}
