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
    let appConfig: AppConfigRepository
    let networkQualityMonitor: NetworkQualityMonitor
    
    init(
        sessionMonitor: UserSessionMonitor,
        appConfig: AppConfigRepository,
        networkQualityMonitor: NetworkQualityMonitor
    ) {
        self.sessionMonitor = sessionMonitor
        self.appConfig = appConfig
        self.networkQualityMonitor = networkQualityMonitor
    }
    
    lazy var client: Client = {
        let systemBundleId = Bundle.main.bundleIdentifier ?? "app.paralino.ios"
        let bundleId = systemBundleId.starts(with: "app.paralino.ios.test") ? "app.paralino.ios.test" : "app.paralino.ios"
        return Client(userDefaultsGroup: "group.\(bundleId)")
            .setEndpoint(appConfig.endpoint)
            .setProject(appConfig.projectId)
            .setSelfSigned(false)
    }()
    
    lazy var account: Account = { return Account(client) }()
    lazy var tablesDB: TablesDB = { return TablesDB(client) }()
    lazy var functions: Functions = { return Functions(client) }()
    lazy var realtime: Realtime = { return Realtime(client) }()
    
    func execute<T>(
        userApi: Bool = false,
        action: @escaping (AppwriteRemoteModule) async throws -> T
    ) async -> Payload<T> {
        let key = UUID().uuidString
        networkQualityMonitor.addEntry(key: key)

        do {
            let result: T
            do {
                result = try await action(self)
            } catch {
                if error.isConnectionError {
                    // Retry logic for connection errors
                    try await Task.sleep(nanoseconds: 200_000_000) // 200ms
                    result = try await action(self)
                } else {
                    throw error
                }
            }

            // Mark successful completion in network monitor
            networkQualityMonitor.stopEntry(key: key, errorType: nil)
            return PayloadSuccess(data: result)
        } catch let error {
            // Identify and process specific error types
            if error.isSocketTimeout {
                networkQualityMonitor.stopEntry(key: key, errorType: .timeout)
            } else if error.isConnectionError {
                networkQualityMonitor.stopEntry(key: key, errorType: .unknownHost)
            } else {
                networkQualityMonitor.stopEntry(key: key, errorType: .other)
            }

            // Map and process the exception
            let mappedError = mapError(error: error, userApi: false)
            sessionMonitor.processNetworkError(error: mappedError)

            return PayloadError(exception: mappedError)
        }
    }
    
    func executeUserApi<T>(action: @escaping (AppwriteRemoteModule) async throws -> T) async -> Payload<T> {
        return await execute(userApi: true, action: action)
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

extension Error {
    var isConnectionError: Bool {
        let nsError = self as NSError
        return nsError.domain == NSURLErrorDomain &&
               (nsError.code == NSURLErrorNotConnectedToInternet || nsError.code == NSURLErrorCannotFindHost)
    }
    
    var isSocketTimeout: Bool {
        let nsError = self as NSError
        return nsError.domain == NSURLErrorDomain && nsError.code == NSURLErrorTimedOut
    }
}
