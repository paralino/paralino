//
//  PushSetupProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import UIKit

public class PushSetupProvider: IPushProviderManager {
    
    lazy var userDataUpdate: UpdateUserDataUseCase = { KoinIOS.Instances.shared.userDataUpdate }()
    lazy var pushHandler: PushNotificationHandler = { KoinIOS.Instances.shared.pushNotificationsHandler }()
    
    
    public func logout() async throws {
        Logr.v(tag: "push_notifications", message: "Unregistering push notifications")
        await UIApplication.shared.unregisterForRemoteNotifications()
    }
    
    public func syncPushNotifications() {
        DispatchQueue.main.async { [weak self] in
            Task {
                do {
                    try await self?.pushHandler.checkForPushNotifications()
                } catch let error {
                    Logr.v(tag: "push_sync", message: "error syncing push notifications: \(error)")
                }
            }
        }
    }
    
    public func syncPushToken() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
    
    public func syncPushTokenImpl() {
        DispatchQueue.main.async { [weak self] in
            Task {
                do {
                    let response = try await self?.userDataUpdate.registerPushTokenIfNeeded()
                    let payload = PayloadSwift<KotlinUnit>(data: response)
                    switch payload {
                    case .loading: Logr.v(tag: "push_sync", message: "Push token not synced")
                    case .success: Logr.v(tag: "push_sync", message: "Push token synced")
                    case .error(error: let error):
                        Logr.v(tag: "push_sync", message: "error syncing push token: \(error)")
                    }
                } catch let error {
                    Logr.v(tag: "push_sync", message: "error syncing push token: \(error)")
                }
            }
        }
    }
}
