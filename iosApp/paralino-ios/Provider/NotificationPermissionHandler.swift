//
//  NotificationPermissionHandler.swift
//  paralino-ios
//
//  Created by Zelimir on 2023-10-06.
//

import Foundation
import UIKit
import shared
import UserNotifications
import paralino_apple_core

class NotificationPermissionHandler {
    
    private var requested: Bool = false
    let notificationStatusProvider: INotificationStatusProvider
    
    init(notificationStatusProvider: INotificationStatusProvider) {
        self.notificationStatusProvider = notificationStatusProvider
    }

    func requestNotificationPermission(
        messageDispather: ICoreMessageDispatcher
    ) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            Logr.d(tag: "permission_request", message: "Notification result: \(granted)", t: KotlinThrowable(message: error?.localizedDescription))
            self.notificationStatusProvider.refreshNotificationsStatus()
            if !granted {
                if self.requested {
                    self.openAppSettingsPage()
                } else {
                    let data = AppSnackbarData(
                        text: MR.shared.strings.error_notification_permission_missing_sheet_message.localizedString(),
                        actionText: MR.shared.strings.button_open_settings.localizedString(),
                        duration: SnackbarDuration.long_,
                        id: "notification_permission"
                    )
                    DispatchQueue.main.async {
                        messageDispather.showSnackbarForResult(data: data) { result, error in
                            if result?.result == MaterialSnackbarResult.actionperformed {
                                self.requested = true
                                self.openAppSettingsPage()
                            }
                        }
                    }
                }
            }
        }
    }
    
    func openAppSettingsPage() {
        DispatchQueue.main.async {
            if let appSettings = URL(string: UIApplication.openNotificationSettingsURLString), UIApplication.shared.canOpenURL(appSettings) {
                UIApplication.shared.open(appSettings)
            }
        }
    }
}
