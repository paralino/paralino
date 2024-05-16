//
//  DeviceApiProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 25.03.23.
//

import Foundation
import shared
import UIKit

class DeviceApiProvider: IDeviceApiProvider {
    
    private let backendConfig: BackendConfig
    
    init(backendConfig: BackendConfig, locationStatusProvider: ILocationStatusProvider, connectionStatusProvider: IConnectionStatusProvider) {
        self.backendConfig = backendConfig
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
    
    override func createConfig() -> AppConfig {
        AppConfig(provider: AppConfigProvider())
    }
    
    override func createSystemLogr() -> SystemLogr {
        return DebbugLogr(canLogToConsole: backendConfig.logToConsole)
    }
    
    override func createCrashReportingLogr() -> SystemLogr {
        return CrashReportingLogr(canLogToRemote: backendConfig.logErrorsToRemote)
    }
    
    override func doCopyToClipboard(data: String) {
        UIPasteboard.general.string = data
    }
}
