//
//  AppwriteRemoteModule.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import Appwrite

class AppwriteRemoteModule: IRemoteModule {
    
    let sessionMonitor: UserSessionMonitor
    let backendConfig: BackendConfig
    
    init(sessionMonitor: UserSessionMonitor, backendConfig: BackendConfig) {
        self.sessionMonitor = sessionMonitor
        self.backendConfig = backendConfig
    }
    
    lazy var client: Client = {
        return Client()
            .setEndpoint(backendConfig.endpoint)
            .setProject(backendConfig.projectId)
            .setSelfSigned(false)
    }()
    
    lazy var account: Account = { return Account(client) }()
    lazy var database: Databases = { return Databases(client) }()
    lazy var functions: Functions = { return Functions(client) }()
    lazy var realtime: Realtime = { return Realtime(client) }()
    
    func execute<T>(action: (AppwriteRemoteModule) async throws -> T) async -> Payload<T> {
        do {
            return try await PayloadSuccess(data: action(self))
        } catch let error {
            let mappedError = mapError(error: error, userApi: false)
            sessionMonitor.processNetworkError(error: mappedError)
            return PayloadError(exception: mappedError)
        }
    }
    
    func executeUserApi<T>(action: (AppwriteRemoteModule) async throws -> T) async -> Payload<T> {
        do {
            return try await PayloadSuccess(data: action(self))
        } catch let error {
            let mappedError = mapError(error: error, userApi: true)
            sessionMonitor.processNetworkError(error: mappedError)
            return PayloadError(exception: mappedError)
        }
    }
    
    func mapError(error: Error, userApi: Bool = false) -> KotlinException {
        switch error {
        case let error as AppwriteError: return BaseRemoteException.companion.fromCode(
            code: KotlinInt(int: Int32(error.code!)),
            message: error.message,
            cause: nil,
            type: error.type ?? (userApi ? "general_unauthorized_scope" : nil)
        )
        default: return KotlinException(message: error.localizedDescription)
        }
    }
}
