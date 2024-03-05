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
    func notify(notificationId: Int32, title: String, message: String?, timeMillis: Int64) {
        let content = UNMutableNotificationContent()
        content.title = title
        if let message = message { content.body = message }
    
        let request = UNNotificationRequest(identifier: "\(notificationId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}

class GroupActivityNotificationPresenter: IGroupMemberAddedNotificationPresenter {
    func notify(notificationId: Int32, title: String, message: String?, timeMillis: Int64) {
        let content = UNMutableNotificationContent()
        content.title = title
        if let message = message { content.body = message }
    
        let request = UNNotificationRequest(identifier: "\(notificationId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}
