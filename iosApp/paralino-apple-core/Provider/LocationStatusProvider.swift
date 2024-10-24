//
//  LocationStatusProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import shared
import CoreLocation

class LocationStatusProvider: ILocationStatusProvider {
    override func createCurrentStatus() -> LocationConditionStatus {
        let locationManager = CLLocationManager()
        return locationManager.locationCondtionStatus()
    }
}

extension CLLocationManager {
    func locationCondtionStatus() -> LocationConditionStatus {
        let permissionStatus = self.authorizationStatus
        let locationAccuracy = self.accuracyAuthorization
        return LocationConditionStatus(
            isGpsOn: CLLocationManager.locationServicesEnabled(),
            isPermissionAllowed: (permissionStatus == .authorizedWhenInUse || permissionStatus == .authorizedAlways) && locationAccuracy == .fullAccuracy,
            isBackgroundPermissionAllowed: permissionStatus == .authorizedAlways
        )
    }
}
