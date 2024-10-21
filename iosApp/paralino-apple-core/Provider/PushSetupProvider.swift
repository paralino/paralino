//
//  PushSetupProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared

public class PushSetupProvider: IPushProviderManager {
    
    lazy var userDataUpdate: UpdateUserDataUseCase = { KoinIOS.Instances.shared.userDataUpdate }()
    lazy var pushHandler: PushNotificationHandler = { KoinIOS.Instances.shared.pushNotificationsHandler }()
    
    
    public func logout() async throws {
        
    }
    
    public func getNotificationPayload(notificationId: String) async throws -> IPushNotificationPresenter.Payload? {
        return try await pushHandler.retrievePushPresenterPayload(notificationId: notificationId, force: true)
    }
    
    public func deleteNotification(notificationId: String) async throws {
        try await pushHandler.deletePushNotification(notificationId: notificationId)
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
        DispatchQueue.main.async { [weak self] in
            Task {
                do {
                    let response = try await self?.userDataUpdate.registerPushTokenIfNeeded()
                    let payload = PayloadSwift<KotlinUnit>(data: response)
                    switch payload {
                    case .loading: break
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
