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
import paralino_apple_core

class AppDelegate: NSObject, UIApplicationDelegate, ObservableObject, UNUserNotificationCenterDelegate {
    
    static internal var sharedInstance: AppDelegate!
    
    lazy var locationManager = LocationManager(setupManager: true)
    lazy var appRatingPresenter = AppRatingPresenter()
    
    lazy var coreAppManager: CoreAppManager = { KoinIOS.Instances.shared.coreAppManager }()
    lazy var pushProvider: PushSetupProvider = { KoinIOS.Instances.shared.pushProvider as! PushSetupProvider }()
    lazy var deviceApiProvider: IDeviceApiProvider = { KoinIOS.Instances.shared.deviceApiProvider }()
    
    lazy var notificationPermissionHandler = NotificationPermissionHandler(notificationStatusProvider: deviceApiProvider.notificationStatusProvider)
    lazy var locationPermissionHandler = LocationPermissionHandler(locationManager: locationManager, locationStatusProvider: deviceApiProvider.locationStatusProvider)
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        locationManager.manager.delegate = locationManager
        
        KoinSetup.initKoin(locationManager: locationManager, appRatingPresenter: appRatingPresenter)
        
        Logr.i(tag:"delegate", message: "App start")
        
        UNUserNotificationCenter.current().delegate = self
        
        locationManager.startMonitoringAppState()
        coreAppManager.onAppStart()
        
        BackgroundRefresh.registerAppRefresh()
        BackgroundRefresh.registerNotificationsRefresh()
        BackgroundRefresh.scheduleAppRefresh()
        BackgroundRefresh.scheduleNotificationsRefresh()
        
        return true
    }
    
    func appInForeground() {
        coreAppManager.onAppActive()
        let user = User()
        user.userId = AppPrefs.Companion.shared.instance.errorReportingID
        SentrySDK.setUser(user)
        Logr.w(tag: "delegate", message: "Usage (\(DeviceUtil.getMemoryPrintString()))")
    }
    
    func appInBackground() {
        coreAppManager.onAppBackground()
    }
    
    func appInactive() {
        coreAppManager.onAppInactive()
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.list, .banner, .badge, .sound])
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%.2hhx", $0) }.joined()
        
        Logr.v(tag: "push_notifications", message: "Got push token from system")
        
        shared.AppPrefs.companion.instance.pushTokenUnsynced = token
        pushProvider.syncPushTokenImpl()
    }
}
