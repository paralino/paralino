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

class AppDelegate: NSObject, UIApplicationDelegate, ObservableObject, CLLocationManagerDelegate {
    
    let locationStatusProvider = LocationStatusProvider()
    lazy var coreAppManager: CoreAppManager = { KoinIOS.Instances.shared.coreAppManager }()
    
    lazy var locationSharingManager: LocationSharingManager = { KoinIOS.Instances.shared.locationSharingManager }()
    lazy var locationManager = LocationManager(delegate: self)
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        initKoin(locationStatusProvider: locationStatusProvider)
        
        Logr.i(tag:"delegate", message: "App start")
        
        coreAppManager.onAppStart()
        coreAppManager.onAppActive()
        
        if locationStatusProvider.currentStatus.isBackgroundPermissionAllowed
            && locationSharingManager.isSharingActive() {
            locationManager.startLocationUpdates()
        } else {
            locationManager.stopLocationUpdates()
        }
        
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
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locationManager.locationManager(manager, didUpdateLocations: locations)
    }
}
