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
    
    private let appBuildConfig: AppBuildConfig
    
    init(
        appBuildConfig: AppBuildConfig,
        locationStatusProvider: ILocationStatusProvider,
        connectionStatusProvider: IConnectionStatusProvider,
        notificationStatusProvider: INotificationStatusProvider
    ) {
        self.appBuildConfig = appBuildConfig
        super.init(
            locationStatusProvider: locationStatusProvider,
            connectionStatusProvider: connectionStatusProvider,
            notificationStatusProvider: notificationStatusProvider
        )
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
    
    override var packageName: String {
        return Bundle.main.bundleIdentifier ?? "app.paralino.ios.undefined"
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
    
    override var defaultDeviceName: String {
        "iPhone"
    }
    
    override func createConfig() -> AppPrefs {
        AppPrefs(provider: AppPrefsProvider(appBuildConfig: appBuildConfig))
    }
    
    override func createSystemLogr() -> SystemLogr {
        return DebbugLogr(canLogToConsole: appBuildConfig.logToConsole)
    }
    
    override func doCopyToClipboard(data: String) {
        UIPasteboard.general.string = data
    }
}
