//
//  OneTimeLocationProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-04-25.
//

import Foundation
import shared
import CoreLocation
import SwiftLocation

class OneTimeLocationProvider: IOneTimeLocationProvider, CLLocationManagerDelegate {
    
    var locationManager: LocationManager

    
    init(
        locationManager: LocationManager,
        locationStatusProvider: ILocationStatusProvider,
        locationCacheRepo: LocationCacheRepository
    ) {
        self.locationManager = locationManager
        super.init(locationStatusProvider: locationStatusProvider, locationCacheRepo: locationCacheRepo)
    }
    
    override func fetchLastLocation(requestType: IOneTimeLocationProviderRequestTypeFetch) async throws -> LocationDataModel? {
        let manager = CLLocationManager()
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = true
        manager.pausesLocationUpdatesAutomatically = true
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.distanceFilter = kCLDistanceFilterNone
        
        Logr.v(tag: "location_service", message: "iOS fetch device location once")
        let swiftLocation = Location(locationManager: manager)
        var accuracyFilter = AccuracyFilter.horizontal(50)
        
        switch requestType.type {
        case .batterySaver: accuracyFilter = .horizontal(200)
        case .balanced: accuracyFilter = .horizontal(100)
        case .mediumHigh: accuracyFilter = .horizontal(50)
        case .high: accuracyFilter = .horizontal(30)
        case .veryHigh, .manualBoost: accuracyFilter = .horizontal(5)
        default: accuracyFilter = .horizontal(50)
        }
        
        let location = try? await swiftLocation.requestLocation(
            accuracy: [accuracyFilter],
            timeout: TimeInterval(requestType.timeoutMillis / 1000)
        ).location
        let lastLocation = locationManager.manager.location
        
        Logr.v(tag: "location_service", message: "iOS got new location from one time fetch: \(location != nil) - has lastLocation: \(lastLocation != nil)")
        return LocationDataModel.companion.getBetter(
            first: location?.toLocationModel(),
            second: lastLocation?.toLocationModel()
        )
    }
}
