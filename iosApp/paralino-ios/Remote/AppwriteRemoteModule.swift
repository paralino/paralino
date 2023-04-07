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
    
    func execute<T>(action: (AppwriteRemoteModule) async throws -> T) async -> T {
        do {
            return try await action(self)
        } catch let error {
            let mappedError = mapError(error: error)
            sessionMonitor.processNetworkError(error: mappedError)
            return wrap(error: mappedError) as! T
        }
    }
    
    func mapError(error: Error) -> KotlinThrowable {
        switch error {
        case let error as AppwriteError: return BaseRemoteException.companion.fromCode(
            code: KotlinInt(int: Int32(error.code!)),
            message: error.message,
            cause: nil,
            type: error.type
        )
        default: return KotlinThrowable(message: error.localizedDescription)
        }
    }
    
}
