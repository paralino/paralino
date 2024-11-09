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

public class LocationManager: NSObject, ObservableObject, CoreLocationManagerDelegate {
    
    public let manager = CLLocationManager()
    
    lazy var locationRepo: LocationRepositoryWrapper = { KoinIOS.Instances.shared.locationRepo }()
    lazy var placeRepo: PlacesRepository = { KoinIOS.Instances.shared.placesRepo }()
    lazy var locationSharingManager: LocationSharingManager = { KoinIOS.Instances.shared.locationSharingManager }()
    lazy var locationPerformanceManager: LocationPerformanceManager = { KoinIOS.Instances.shared.locationPerformanceManager }()
    lazy var pushProvider: IPushProviderManager = { KoinIOS.Instances.shared.pushProvider }()
    
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
    
    public var lastLocation: CLLocation? {
        manager.location
    }
    
    public var monitoredRegions: Set<CLRegion> {
        manager.monitoredRegions
    }
    
    public func startMonitoringRegion(region: CLCircularRegion) {
        manager.startMonitoring(for: region)
    }
    
    public func stopMonitoringRegion(region: CLRegion) {
        manager.stopMonitoring(for: region)
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
    
    func startLocationUpdates() {
        manager.startUpdatingLocation()
        manager.startMonitoringSignificantLocationChanges()
        
        manager.startMonitoringLocationPushes { [weak self] data, error in
            if let data = data {
                let token = data.map { String(format: "%.2hhx", $0) }.joined()
                shared.AppPrefs.companion.instance.locationPushTokenUnsynced = token
                self?.pushProvider.syncPushToken()
            }
            
            if let error = error {
                Logr.d(tag:"location_service", message: "Error getting location push token: \(error.localizedDescription)")
            }
        }
    }
    
    func stopLocationUpdates() {
        manager.stopUpdatingLocation()
        manager.stopMonitoringSignificantLocationChanges()
        manager.stopMonitoringLocationPushes()
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
        Logr.i(tag: "place_activity_geofence", message: "User entered geofence: \(region.identifier)")
        placeRepo.placesEnter(placesIds: [region.identifier], trigger: PlaceActivityTriggerType.goefence)
    }
    
    public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        placeRepo.placesExit(placesIds: [region.identifier], trigger: PlaceActivityTriggerType.goefence)
    }
    
    public func locationManagerDidPauseLocationUpdates(_ manager: CLLocationManager) {
        Logr.w(tag: "location_service", message: "Location updates are paused")
    }
    
    public func locationManagerDidResumeLocationUpdates(_ manager: CLLocationManager) {
        Logr.w(tag: "location_service", message: "Location updates are resumed")
    }
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Logr.w(tag: "location_service", message: "Location permission changed: \(manager.authorizationStatus)")
    }
    
    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        Logr.w(tag: "location_service", message: "Location permission changed2: \(manager.authorizationStatus)")
    }
}
