//
//  PushNotificationHandlerIOS.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared

class PlaceNotificationHandler: IPlaceNotificationsHandler {
    func process(payload: NotificationPayload) {
        guard payload is PlaceActivityNotificationPayload else { return }
    }
}

class GroupActivityNotificationHandler: IGroupMemberAddedNotificationsHandler {
    func process(payload: NotificationPayload) {
        guard payload is GroupMemberAddedNotificationPayload else { return }
    }
}
