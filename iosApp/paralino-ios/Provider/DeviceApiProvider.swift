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
    
    override init(backendConfig: BackendConfig, locationStatusProvider: ILocationStatusProvider, connectionStatusProvider: IConnectionStatusProvider) {
        UIDevice.current.isBatteryMonitoringEnabled = true
        super.init(backendConfig: backendConfig, locationStatusProvider: locationStatusProvider, connectionStatusProvider: connectionStatusProvider)
    }
    
    override func logout() async throws {
        
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
    
    override var randomUUID: String {
        UUID().uuidString
    }
    
    override var defaultDeviceName: String {
        "iPhone"
    }
    
    override func createConfig() -> AppConfig {
        AppConfig(provider: AppConfigProvider())
    }
    
    override func createSystemLogr() -> SystemLogr {
        // TODO log to sentry
        if(backendConfig.logToConsole) {
            return DebbugLogr()
        } else {
            return DebbugLogr()
        }
    }
    
    override func doCopyToClipboard(data: String) {
        UIPasteboard.general.string = data
    }
}
