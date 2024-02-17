//
//  PushSetupProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared

class PushSetupProvider: IPushProviderManager {
    
    lazy var userDataUpdate: UpdateUserDataUseCase = { KoinIOS.Instances.shared.userDataUpdate }()
    
    func logout() async throws {
        
    }
    
    func syncPushNotifications() {
        
    }
    
    func syncPushToken() {
        DispatchQueue.main.async { [weak self] in
            Task {
                do {
                    // TODO replace with correct token
                    shared.AppConfig.companion.instance.pushTokenUnsynced = "NOOP_IOS_TOKEN"
                    try await self?.userDataUpdate.registerPushTokenIfNeeded()
                } catch let error {
                    Logr.v(tag: "push_sync", message: "error syncing push token: \(error)")
                }
            }
        }
    }
}
