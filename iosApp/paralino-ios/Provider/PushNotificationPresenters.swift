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
    func notify(notificationId: Int32, title: String, message: String?, timeMillis: Int64, timeInfo: String?) {
        let content = UNMutableNotificationContent()
        
        let formattedDate = timeInfo != nil ? "\n\(timeInfo ?? "")" : ""
        
        if let message = message {
            content.title = title
            content.body = message + formattedDate
        } else {
            content.body = title + formattedDate
        }
    
        let request = UNNotificationRequest(identifier: "\(notificationId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}

class GroupActivityNotificationPresenter: IGroupActivityNotificationPresenter {
    func notify(notificationId: Int32, title: String, message: String?, timeMillis: Int64, timeInfo: String?) {
        let content = UNMutableNotificationContent()
        
        let formattedDate = timeInfo != nil ? "\n\(timeInfo ?? "")" : ""
        
        if let message = message {
            content.title = title
            content.body = message + formattedDate
        } else {
            content.body = title + formattedDate
        }
    
        let request = UNNotificationRequest(identifier: "\(notificationId)", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}
