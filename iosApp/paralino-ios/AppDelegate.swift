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
import CoreLocation

class AppDelegate: NSObject, UIApplicationDelegate, ObservableObject, UNUserNotificationCenterDelegate {
    
    static internal var shared: AppDelegate!
    
    let locationStatusProvider = LocationStatusProvider()
    lazy var coreAppManager: CoreAppManager = { KoinIOS.Instances.shared.coreAppManager }()
    
    lazy var locationSharingManager: LocationSharingManager = { KoinIOS.Instances.shared.locationSharingManager }()
    lazy var locationPerformanceManager: LocationPerformanceManager = { KoinIOS.Instances.shared.locationPerformanceManager }()
    lazy var locationManager = LocationManager()
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        initKoin(locationStatusProvider: locationStatusProvider)
        
        Logr.i(tag:"delegate", message: "App start")
        
        locationManager.manager.delegate = locationManager
        UNUserNotificationCenter.current().delegate = self
        
        locationSharingManager.locationSharingStatus { [weak self] state in
            switch ResourceSwift<KotlinBoolean>.init(data: state) {
            case .notInitialized:
                break
            case .ready(data: let data):
                if data.boolValue {
                    self?.locationManager.startLocationUpdates()
                } else {
                    self?.locationManager.stopLocationUpdates()
                }
            }
        }
        
        locationPerformanceManager.locationPerformanceStatus { [weak self] model in
            Logr.v(tag: "location_Test", message: "Location performance changed: \(model)")
            switch model.current {
            case .batterySaver: 
                self?.locationManager.manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
                self?.locationManager.manager.distanceFilter = 1500
            case .balanced:
                self?.locationManager.manager.desiredAccuracy = kCLLocationAccuracyKilometer
                self?.locationManager.manager.distanceFilter = 300
            case .mediumHigh:
                self?.locationManager.manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
                self?.locationManager.manager.distanceFilter = 50
            case .high:
                self?.locationManager.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.locationManager.manager.distanceFilter = 5
            case .veryHigh, .manualBoost:
                self?.locationManager.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.locationManager.manager.distanceFilter = kCLDistanceFilterNone
            default: break
            }
        }
        
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
    
    func appInactive() {
        Logr.i(tag:"delegate", message: "App inactive")
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.list, .banner, .badge, .sound])
    }
}
