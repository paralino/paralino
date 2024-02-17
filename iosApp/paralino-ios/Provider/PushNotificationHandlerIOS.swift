//
//  PushNotificationHandlerIOS.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import UserNotifications

class PlaceNotificationHandler: IPlaceNotificationsHandler {
    func process(push: PushNotificationRaw) {
        let content = UNMutableNotificationContent()
        content.title = "Paralino"
        content.body = MR.shared.strings.place_notification_default.localizedString()
    
        let request = UNNotificationRequest(identifier: push.id, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}

class GroupActivityNotificationHandler: IGroupMemberAddedNotificationsHandler {
    func process(push: PushNotificationRaw) {
        let content = UNMutableNotificationContent()
        content.title = "Paralino"
        content.body = MR.shared.strings.group_member_add_notification_default.localizedString()
    
        let request = UNNotificationRequest(identifier: push.id, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}
