//
//  LocationStatusProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import shared
import CoreLocation

extension CLLocationManager {
    func locationCondtionStatus(isUnrestrictedBackgroundPermissionAllowed: Bool) -> LocationConditionStatus {
        let permissionStatus = self.authorizationStatus
        let locationAccuracy = self.accuracyAuthorization
        return LocationConditionStatus(
            isGpsOn: CLLocationManager.locationServicesEnabled(),
            isFinePermissionAllowed: (permissionStatus == .authorizedWhenInUse || permissionStatus == .authorizedAlways) && locationAccuracy == .fullAccuracy,
            isCoarsePermissionAllowed: permissionStatus == .authorizedWhenInUse || permissionStatus == .authorizedAlways,
            isBackgroundPermissionAllowed: permissionStatus == .authorizedAlways,
            isUnrestrictedBackgroundPermissionAllowed: isUnrestrictedBackgroundPermissionAllowed
        )
    }
}
