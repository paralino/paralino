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
import Sentry

class AppDelegate: NSObject, UIApplicationDelegate, ObservableObject, UNUserNotificationCenterDelegate {
    
    static internal var shared: AppDelegate!
    
    let locationStatusProvider = LocationStatusProvider()
    lazy var coreAppManager: CoreAppManager = { KoinIOS.Instances.shared.coreAppManager }()
    lazy var locationManager = LocationManager()
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        SentrySDK.start { options in
            options.dsn = BuildKonfig.shared.glitchTipDSN
            options.debug = false
        }
        
        initKoin(locationStatusProvider: locationStatusProvider)
        
        Logr.i(tag:"delegate", message: "App start")
        
        UNUserNotificationCenter.current().delegate = self
        locationManager.manager.delegate = locationManager
        locationManager.startMonitoringAppState()
        
        coreAppManager.onAppStart()
        
        BackgroundRefresh.registerAppRefresh()
        BackgroundRefresh.registerNotificationsRefresh()
        BackgroundRefresh.scheduleAppRefresh()
        BackgroundRefresh.scheduleNotificationsRefresh()
        
        return true
    }
    
    func appInForeground() {
        Logr.d(tag:"delegate", message: "App in foreground")
        coreAppManager.onAppActive()
    }
    
    func appInBackground() {
        Logr.d(tag:"delegate", message: "App in background")
        coreAppManager.onAppBackground()
    }
    
    func appInactive() {
        Logr.d(tag:"delegate", message: "App inactive")
        coreAppManager.onAppInactive()
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.list, .banner, .badge, .sound])
    }
}
