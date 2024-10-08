//
//  LocationStatusProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import CoreLocation
import UIKit
import paralino_apple_core

class LocationStatusProvider: ILocationStatusProvider {
    // this is set in app delegate
    private var hasNotificationPermission: Bool = false
    
    override func createCurrentStatus() -> LocationConditionStatus {
        let locationManager = CLLocationManager()
        let permissionStatus = locationManager.authorizationStatus
        let locationAccuracy = locationManager.accuracyAuthorization
        return LocationConditionStatus(
            isGpsOn: CLLocationManager.locationServicesEnabled(),
            isPermissionAllowed: (permissionStatus == .authorizedWhenInUse || permissionStatus == .authorizedAlways) && locationAccuracy == .fullAccuracy,
            isBackgroundPermissionAllowed: permissionStatus == .authorizedAlways,
            isNotificationPermissionAllowed: hasNotificationPermission
        )
    }
    
    override func refreshLocationStatus() {
        updateNotificationPermissionStatus()
        super.refreshLocationStatus()
    }
    
    private func refreshParentStatus() {
        super.refreshLocationStatus()
    }
    
    private func updateNotificationPermissionStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            Logr.i(tag:"delegate", message: "Notification status: \(settings.authorizationStatus)")
            switch settings.authorizationStatus {
            case .denied:
                self?.hasNotificationPermission = false
            case .authorized, .provisional:
                self?.hasNotificationPermission = true
            default:
                self?.hasNotificationPermission = false
            }
            self?.refreshParentStatus()
        }
    }
    
}
