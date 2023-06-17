//
//  AppDelegate.swift
//  paralino-ios
//
//  Created by Zelimir on 24.03.23.
//

import Foundation
import UIKit
import shared
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate {
    
    let locationStatusProvider = LocationStatusProvider()

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        initKoin(locationStatusProvider: locationStatusProvider)
        Logr.i(tag:"delegate", message: "App start")
        return true
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            switch settings.authorizationStatus {
            case .denied:
                self?.locationStatusProvider.hasNotificationPermission = false
            case .authorized:
                self?.locationStatusProvider.hasNotificationPermission = true
            default:
                self?.locationStatusProvider.hasNotificationPermission = false
            }
            self?.locationStatusProvider.refreshLocationStatus()
        }
    }

}
