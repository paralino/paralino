//
//  LocationUtil.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-17.
//

import CoreLocation
import shared

extension CLLocation {
    public func toLocationModel() -> LocationDataModel {
        let location = self
        let accuracy: Float = if !location.horizontalAccuracy.isNaN && location.horizontalAccuracy > 0 { Float(location.horizontalAccuracy) } else { 0 }
        let speed: KotlinFloat? = if !location.speed.isNaN && location.speed > 0 { KotlinFloat(floatLiteral: location.speed) } else { nil }
        let altitude: KotlinFloat? = if !location.altitude.isNaN { KotlinFloat(floatLiteral: location.altitude) } else { nil }
        let timeUTC = location.timestamp.ISO8601Format(.iso8601(timeZone: .gmt))
        
        return LocationDataModel(
            lat: location.coordinate.latitude,
            lng: location.coordinate.longitude,
            accuracy: accuracy,
            timeUTC: timeUTC,
            endTimeUTC: nil,
            speed: speed,
            altitude: altitude,
            isMasked: false,
            isApproximate: false,
            isInPrivacyZone: false,
            privacyZoneMessage: nil
        )
    }
}
