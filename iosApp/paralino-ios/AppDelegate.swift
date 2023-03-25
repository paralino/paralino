//
//  AppDelegate.swift
//  paralino-ios
//
//  Created by Zelimir on 24.03.23.
//

import Foundation
import UIKit
import shared

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        ParalinoCore.shared.doInit(
            preLaunch: { koinApp in koinApp },
            appModules: KoinIOS.shared.createIOSKoinModules(deviceProvider: DeviceApiProvider())
        )
        Logr.i(tag:"delegate", message: "App start")
        return true
    }

}
