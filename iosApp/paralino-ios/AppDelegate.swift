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
        
        Logr.i(tag:"delegate", message: "App start")
        
        coreAppManager.onAppStart()
        coreAppManager.onAppActive()
        
        return true
    }
    
    func appInForeground() {
        Logr.i(tag:"delegate", message: "App in foreground")
        coreAppManager.onAppActive()
    }
    
    func appInBackground() {
        Logr.i(tag:"delegate", message: "App in background")
        coreAppManager.onAppInactive()
    }
    
    func requestNotificationPermission() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            Logr.d(tag: "permission_request", message: "Notification result: \(granted)", t: KotlinThrowable(message: error?.localizedDescription))
            self.locationStatusProvider.refreshLocationStatus()
            if !granted {
                self.openAppSettingsPage()
            }
        }
    }
    
    func openAppSettingsPage() {
        DispatchQueue.main.async {
            if let appSettings = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(appSettings) {
                UIApplication.shared.open(appSettings)
            }
        }
    }
}
