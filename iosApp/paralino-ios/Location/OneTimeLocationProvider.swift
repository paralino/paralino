//
//  OneTimeLocationProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-04-25.
//

import Foundation
import shared

class OneTimeLocationProvider: IOneTimeLocationProvider {
    
    var locationManager: LocationManager
    
    init(locationManager: LocationManager, locationStatusProvider: ILocationStatusProvider, locationCacheRepo: LocationCacheRepository) {
        self.locationManager = locationManager
        super.init(locationStatusProvider: locationStatusProvider, locationCacheRepo: locationCacheRepo)
    }
    
    override func getLastLocationImpl() async throws -> LocationDataModel? {
        return locationManager.manager.location?.toLocationModel()
    }
}
