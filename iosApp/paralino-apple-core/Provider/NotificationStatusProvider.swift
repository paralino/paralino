//
//  NotificationStatusProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-24.
//

import shared
import UserNotifications

class NotificationStatusProvider: INotificationStatusProvider, @unchecked Sendable {
    
    private var hasNotificationPermission: Bool = false
    
    override func createCurrentStatus() -> NotificationConditionStatus {
        return NotificationConditionStatus(isNotificationPermissionAllowed: hasNotificationPermission)
    }
    
    override func refreshNotificationsStatus() {
        updateNotificationPermissionStatus()
    }
    
    private func refreshParentStatus() {
        super.refreshNotificationsStatus()
    }
    
    private func updateNotificationPermissionStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            switch settings.authorizationStatus {
            case .denied:
                self?.hasNotificationPermission = false
            case .authorized, .provisional:
                self?.hasNotificationPermission = true
            default:
                self?.hasNotificationPermission = false
            }
            self?.refreshParentStatus()
        }
    }
}
