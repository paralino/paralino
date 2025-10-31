//
//  DeviceApiProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 25.03.23.
//

import Foundation
import UIKit
import shared

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
        Int64(Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "")
            ?? 0
    }

    override var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
            ?? ""
    }

    override var packageName: String {
        return Bundle.main.bundleIdentifier ?? "app.paralino.ios.undefined"
    }

    override var deviceOSVersion: String {
        return UIDevice.current.systemVersion
    }

    override var _batteryLevel: Int32 {
        UIDevice.current.isBatteryMonitoringEnabled = true
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

    override var isLowPowerMode: Bool {
        return ProcessInfo.processInfo.isLowPowerModeEnabled
    }

    override var defaultDeviceName: String {
        "iPhone"
    }

    override func createSystemLogr() -> SystemLogr {
        return DebbugLogr(canLogToConsole: appBuildConfig.logToConsole)
    }

    override func doCopyToClipboard(data: String) {
        UIPasteboard.general.string = data
    }

    override func deviceCornerRadius() -> Float {

        var deviceModel: String? = ""
        #if targetEnvironment(simulator)
            if let dir = ProcessInfo().environment["SIMULATOR_MODEL_IDENTIFIER"]
            {
                deviceModel = dir
            }
        #else
            var systemInfo = utsname()
            uname(&systemInfo)
            let _ = Mirror(reflecting: systemInfo.machine)
            deviceModel = withUnsafePointer(to: &systemInfo.machine) {
                $0.withMemoryRebound(to: CChar.self, capacity: 1) {
                    ptr in String.init(validatingUTF8: ptr)
                }
            }
        #endif

        switch deviceModel {
        case "iPhone10,3", "iPhone10,6", "iPhone11,2", "iPhone11,4",
            "iPhone11,6", "iPhone12,3", "iPhone12,5":  // iPhone X, Xs, Xs Max, 11 Pro, 11 Pro Max
            return 39.0

        case "iPhone11,8", "iPhone12,1":  // iPhone Xr, 11
            return 41.5

        case "iPhone13,1", "iPhone14,4":  // iPhone 12 mini, 13 mini
            return 44.0

        case "iPhone13,2", "iPhone13,3", "iPhone14,2", "iPhone14,7",
            "iPhone17,5":  // iPhone 12, 12 Pro, 13 Pro, 14, 16e
            return 47.33

        case "iPhone13,4", "iPhone14,3", "iPhone14,8":  // iPhone 12 Pro Max, 13 Pro Max, 14 Plus
            return 53.33

        case "iPhone15,2", "iPhone15,3", "iPhone15,4", "iPhone15,5",
            "iPhone16,1", "iPhone16,2", "iPhone17,3", "iPhone17,4":  // iPhone 14 Pro, 14 Pro Max, 15, 15 Plus, 15 Pro, 15 Pro Max, 16, 16 Plus
            return 55.0

        case "iPhone17,1", "iPhone17,2", "iPhone18,3", "iPhone18,1",
            "iPhone18,2", "iPhone18,4", "iPad12,1":  // iPhone 16 Pro, 16 Pro Max, 17, 17 Pro, 17 Pro Max, Air
            return 62.0

        case "iPad11,3", "iPad11,4", "iPad13,1", "iPad13,2":  // iPad Air / iPad Pro 11-inch / 12.9-inch
            return 18.0

        default:
            return 62.0  // Unknown device
        }
    }
}
