//
//  GeofenceManager.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import CoreLocation
import UIKit

class GeofenceManager: IGeofenceManager {
    
    let appDelegate = AppDelegate.shared!
    
    func deleteAllGeofences() {
        Logr.d(tag: "geofences", message: "Removing all geofences")
        let locationManager = appDelegate.locationManager.manager
        for region in locationManager.monitoredRegions {
            locationManager.stopMonitoring(for: region)
        }
    }
    
    func deleteGeofences(placesIds: [String]) {
        let locationManager = appDelegate.locationManager.manager
        let regions = locationManager.monitoredRegions.filter { region in placesIds.contains { region.identifier == $0 } }
        for region in regions {
            locationManager.stopMonitoring(for: region)
        }
    }
    
    func distanceBetweenPoints(data: LocationDataModel, geofence: GroupPlaceGeofenceData) -> Float {
        return distanceBetweenPoints(lat1: data.lat, lng1: data.lng, lat2: geofence.lat, lng2: geofence.lng)
    }
    
    func distanceBetweenPoints(lat1: Double, lng1: Double, lat2: Double, lng2: Double) -> Float {
        return Float(
            CLLocation(latitude: lat1, longitude: lng1)
                .distance(from: CLLocation(latitude: lat2, longitude: lng2))
        )
    }
    
    func registerGeofences(places: [GroupPlaceGeofenceData]) {
        if CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) {
            Logr.d(tag: "geofences", message: "Region monitoring supported, re-init geofences")
            
            deleteAllGeofences()
            
            let locationManager = appDelegate.locationManager.manager
            let lastLocation = locationManager.location
            
            let regions = places.map {
                let region = CLCircularRegion(
                    center: CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng),
                    radius: Double($0.radiusMeters),
                    identifier: $0.placeId
                )
                region.notifyOnEntry = true
                region.notifyOnExit = true
                return region
            }
            
            let regionWithDistance = regions.map {
                if let lastLocation = lastLocation {
                    (
                        $0,
                        distanceBetweenPoints(
                            lat1: $0.center.latitude,
                            lng1: $0.center.longitude,
                            lat2: lastLocation.coordinate.latitude,
                            lng2: lastLocation.coordinate.longitude
                        )
                    )
                } else {
                    ($0, Float(0))
                }
            }
            
            let nearbyRegions = regionWithDistance
                .sorted{ a, b in return a.1 < b.1 }
                .prefix(20)
            
            Logr.d(tag: "geofences", message: "Registering \(nearbyRegions.count) nearby geofences")
            
            nearbyRegions.forEach {
                locationManager.startMonitoring(for: $0.0)
            }
        } else {
            Logr.w(tag: "geofences", message: "Region monitoring not supported")
        }
    }
}
