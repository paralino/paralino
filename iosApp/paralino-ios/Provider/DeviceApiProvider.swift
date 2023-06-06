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
    
    override func logout() async throws {
        
    }
    
    override var appVersionCode: Int64 {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? Int64 ?? 0
    }
    
    override var batteryLevel: Int32 {
        UIDevice.current.isBatteryMonitoringEnabled = true
        return Int32(UIDevice.current.batteryLevel)
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
}
