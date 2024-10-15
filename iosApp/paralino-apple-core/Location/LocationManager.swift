//
//  LocationManager.swift
//  paralino-ios
//
//  Created by Zelimir on 2023-11-24.
//

import Foundation
import CoreLocation
import shared
import UserNotifications

public class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    
    public let manager = CLLocationManager()
    
    lazy var locationRepo: LocationRepositoryWrapper = { KoinIOS.Instances.shared.locationRepo }()
    lazy var placeRepo: PlacesRepository = { KoinIOS.Instances.shared.placesRepo }()
    lazy var locationSharingManager: LocationSharingManager = { KoinIOS.Instances.shared.locationSharingManager }()
    lazy var locationPerformanceManager: LocationPerformanceManager = { KoinIOS.Instances.shared.locationPerformanceManager }()
    
    public init(setupManager: Bool) {
        super.init()
        if (setupManager) {
            setup()
        }
    }
    
    func setup() {
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = false
        manager.pausesLocationUpdatesAutomatically = true
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }
    
    func startMonitoringAppState() {
        locationSharingManager.locationSharingStatus { [weak self] state in
            switch ResourceSwift<KotlinBoolean>.init(data: state) {
            case .notInitialized:
                break
            case .ready(data: let data):
                if data.boolValue {
                    self?.startLocationUpdates()
                } else {
                    self?.stopLocationUpdates()
                }
            }
        }
        
        locationPerformanceManager.locationPerformanceStatus { [weak self] model in
            Logr.i(tag: "location_Test", message: "Location performance changed: \(model)")
            switch model.current {
            case .batterySaver:
                self?.manager.pausesLocationUpdatesAutomatically = true
                self?.manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
                self?.manager.distanceFilter = 1500
            case .balanced:
                self?.manager.pausesLocationUpdatesAutomatically = true
                self?.manager.desiredAccuracy = kCLLocationAccuracyKilometer
                self?.manager.distanceFilter = 300
            case .mediumHigh:
                self?.manager.pausesLocationUpdatesAutomatically = true
                self?.manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
                self?.manager.distanceFilter = 50
            case .high:
                self?.manager.pausesLocationUpdatesAutomatically = false
                self?.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.manager.distanceFilter = 2
            case .veryHigh, .manualBoost:
                self?.manager.pausesLocationUpdatesAutomatically = false
                self?.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.manager.distanceFilter = kCLDistanceFilterNone
            default: break
            }
        }
    }

    func requestLocationPermission() {
        manager.requestWhenInUseAuthorization()
    }
    
    func requestAlwaysOnLocationPermission() {
        manager.requestAlwaysAuthorization()
    }
    
    func startLocationUpdates() {
        manager.startUpdatingLocation()
        manager.startMonitoringSignificantLocationChanges()
    }
    
    func stopLocationUpdates() {
        manager.stopUpdatingLocation()
        manager.stopMonitoringSignificantLocationChanges()
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        
        guard let location = locations.last?.toLocationModel() else { return }
        let systemLocation = manager.location?.toLocationModel()
        let locationModel = LocationDataModel.companion.getBetter(first: location, second: systemLocation)
        
        Logr.v(tag: "location_service", message: "Got location update from system \(locationModel != nil)")
        
        locationRepo.registerNewLocation(data: locationModel!)
    }
    
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: any Error) {
        Logr.w(tag: "location_service", message: "Location updates got error: \(error.localizedDescription)")
    }
    
    public func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        DispatchQueue.main.async { [weak self] in
            Task {
                let place = try? await self?.placeRepo.getPlaceLocallyOnce(placeId: region.identifier)
                Logr.i(tag: "place_activity_geofence", message: "User entered geofence: \(region.identifier) (\(place?.nameOrNull ?? "nil"))")
            }
        }
        Logr.i(tag: "place_activity_geofence", message: "User entered geofence: \(region.identifier)")
        placeRepo.placesEnter(placesIds: [region.identifier], trigger: PlaceActivityTriggerType.goefence)
    }
    
    public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        DispatchQueue.main.async { [weak self] in
            Task {
                let place = try? await self?.placeRepo.getPlaceLocallyOnce(placeId: region.identifier)
                Logr.i(tag: "place_activity_geofence", message: "User exited geofence: \(region.identifier) (\(place?.nameOrNull ?? "nil"))")
            }
        }
        placeRepo.placesExit(placesIds: [region.identifier], trigger: PlaceActivityTriggerType.goefence)
    }
    
    public func locationManagerDidPauseLocationUpdates(_ manager: CLLocationManager) {
        Logr.w(tag: "location_service", message: "Location updates are paused")
    }
    
    public func locationManagerDidResumeLocationUpdates(_ manager: CLLocationManager) {
        Logr.w(tag: "location_service", message: "Location updates are resumed")
    }
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        //        notifyUser(id: "LocationResume", message: "Permission changed: \(manager.authorizationStatus)")
    }
}

extension CLLocation {
    func toLocationModel() -> LocationDataModel {
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
            altitude: altitude
        )
    }
}
