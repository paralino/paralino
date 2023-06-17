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

class LocationStatusProvider: ILocationStatusProvider {
    // this is set in app delegate
    var hasNotificationPermission: Bool = false
    
    override init() {
        super.init()
        refreshLocationStatus()
    }
    
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
    
}
