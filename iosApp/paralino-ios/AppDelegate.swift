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

class AppDelegate: NSObject, UIApplicationDelegate, ObservableObject {
    
    let locationStatusProvider = LocationStatusProvider()
    lazy var coreAppManager: CoreAppManager = { KoinIOS.Instances.shared.coreAppManager }()
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        initKoin(locationStatusProvider: locationStatusProvider)
        coreAppManager.onAppStart()
        Logr.i(tag:"delegate", message: "App start")
        return true
    }
    
    func appInForeground() {
        Logr.i(tag:"delegate", message: "App in foreground")
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            Logr.i(tag:"delegate", message: "Notification status: \(settings.authorizationStatus)")
            switch settings.authorizationStatus {
            case .denied:
                self?.locationStatusProvider.hasNotificationPermission = false
            case .authorized, .provisional:
                self?.locationStatusProvider.hasNotificationPermission = true
            default:
                self?.locationStatusProvider.hasNotificationPermission = false
            }
        }
        coreAppManager.onAppActive()
    }
    
    func appInBackground() {
        Logr.i(tag:"delegate", message: "App in background")
        coreAppManager.onAppInactive()
    }
}
