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
    lazy var placesActivityProcessor: PlacesActivityProcessor = { KoinIOS.Instances.shared.placesActivityProcessor }()
    lazy var locationSharingManager: LocationSharingManager = { KoinIOS.Instances.shared.locationSharingManager }()
    lazy var locationPerformanceManager: LocationPerformanceManager = { KoinIOS.Instances.shared.locationPerformanceManager }()
    lazy var pushProvider: PushSetupProvider = { KoinIOS.Instances.shared.pushProvider as! PushSetupProvider }()
    
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
        manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
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
            self?.manager.showsBackgroundLocationIndicator = model.showLocationIndicator
            switch model.current {
            case .highBatterySaver:
                let settings = LocationPerformanceSettingsTypeBatterySaverLevel1.shared
                self?.manager.pausesLocationUpdatesAutomatically = true
                self?.manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
                self?.manager.distanceFilter = CLLocationDistance(settings.minUpdateDistanceMeters)
            case .batterySaver:
                let settings = model.batterySaverPerformanceSettings
                self?.manager.pausesLocationUpdatesAutomatically = true
                self?.manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
                self?.manager.distanceFilter = CLLocationDistance(settings.minUpdateDistanceMeters)
            case .balanced:
                let settings = model.balancedPerformanceSettings
                self?.manager.pausesLocationUpdatesAutomatically = model.pausesLocationAutomatically
                if settings.useHighPerformanceLocation {
                    self?.manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
                } else {
                    self?.manager.desiredAccuracy = kCLLocationAccuracyKilometer
                }
                
                // In best performance balanced mode, even in minUpdateDistance is set to 1 meter, iOS puts the app to sleep after few updates
                // Only when set to 0 app can have high resolution updates when traveling
                if settings.minUpdateDistanceMeters == 0 && !settings.useHighPerformanceLocation {
                    self?.manager.distanceFilter = 10
                } else {
                    self?.manager.distanceFilter = CLLocationDistance(settings.minUpdateDistanceMeters)
                }
            case .mediumHigh:
                self?.manager.pausesLocationUpdatesAutomatically = false
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
                self?.pushProvider.syncPushTokenImpl()
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
        placesActivityProcessor.placesEnter(placesIds: [region.identifier], trigger: PlaceActivityTriggerType.goefence)
    }
    
    public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        placesActivityProcessor.placesExit(placesIds: [region.identifier], trigger: PlaceActivityTriggerType.goefence)
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
