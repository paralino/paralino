//
//  DeviceApiProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 25.03.23.
//

import Foundation
import shared
import UIKit
import paralino_apple_core
import Sentry

class DeviceApiProvider: IDeviceApiProvider {
    
    private let appBuildConfig: AppBuildConfig
    
    init(
        appBuildConfig: AppBuildConfig,
        locationStatusProvider: ILocationStatusProvider,
        connectionStatusProvider: IConnectionStatusProvider
    ) {
        self.appBuildConfig = appBuildConfig
        super.init(locationStatusProvider: locationStatusProvider, connectionStatusProvider: connectionStatusProvider)
    }
    
    override func logout() async throws {
        BackgroundRefresh.logout()
    }
    
    override var appVersionCode: Int64 {
        Int64(Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "") ?? 0
    }
    
    override var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
    }
    
    override var deviceOSVersion: Int32 {
        // TODO use correct value
        return 0
    }
    
    override var _batteryLevel: Int32 {
        let level = UIDevice.current.batteryLevel
        if level >= 0 && level <= 1 {
            return Int32(level * 100)
        } else {
            return -1
        }
    }
    
    override var isCharging: Bool {
        let state = UIDevice.current.batteryState
        return state == .charging || state == .full
    }
    
    override var deviceType: DeviceType {
        DeviceTypeIOS.shared
    }
    
    override var defaultDeviceName: String {
        "iPhone"
    }
    
    override func createConfig() -> AppPrefs {
        AppPrefs(provider: AppPrefsProvider(appBuildConfig: appBuildConfig))
    }
    
    override func createSystemLogr() -> SystemLogr {
        return DebbugLogr(canLogToConsole: appBuildConfig.logToConsole)
    }
    
    override func createCrashReportingLogr() -> SystemLogr {
        return CrashReportingLogr(canLogToRemote: appBuildConfig.logErrorsToRemote)
    }
    
    override func doCopyToClipboard(data: String) {
        UIPasteboard.general.string = data
    }
}

class CrashReportingLogr: SystemLogr {
    
    let canLogToRemote: Bool
    
    init(canLogToRemote: Bool) {
        self.canLogToRemote = canLogToRemote
    }
    
    override func systemLog(priority: LogPriority, tag: String?, message: String, t: KotlinThrowable?) {
        guard canLogToRemote else { return }
        guard priority == LogPriority.error else { return }
        guard let error = t?.description() else { return }
        if t is InternalConnectionException { return }
        
        SentrySDK.capture(error: KotlinError(message: message, stackTrace: error))
    }
}
