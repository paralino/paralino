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

class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    
    let manager = CLLocationManager()
    
    lazy var locationRepo: LocationRepositoryWrapper = { KoinIOS.Instances.shared.locationRepo }()
    lazy var placeRepo: PlacesRepository = { KoinIOS.Instances.shared.placesRepo }()
    lazy var locationSharingManager: LocationSharingManager = { KoinIOS.Instances.shared.locationSharingManager }()
    lazy var locationPerformanceManager: LocationPerformanceManager = { KoinIOS.Instances.shared.locationPerformanceManager }()

    @Published var location: CLLocationCoordinate2D?

    override init() {
        super.init()
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = true
        manager.pausesLocationUpdatesAutomatically = false
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
            Logr.v(tag: "location_Test", message: "Location performance changed: \(model)")
            self?.notifyPerformance(performance: "\(model.current)")
            switch model.current {
            case .batterySaver:
                self?.manager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
                self?.manager.distanceFilter = 1500
            case .balanced:
                self?.manager.desiredAccuracy = kCLLocationAccuracyKilometer
                self?.manager.distanceFilter = 300
            case .mediumHigh:
                self?.manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
                self?.manager.distanceFilter = 50
            case .high, .veryHigh, .manualBoost:
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
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        
        guard let location = locations.last else { return }
        self.location = location.coordinate
        
        let accuracy: Float = if !location.horizontalAccuracy.isNaN && location.horizontalAccuracy > 0 { Float(location.horizontalAccuracy) } else { 0 }
        let speed: KotlinFloat? = if !location.speed.isNaN && location.speed > 0 { KotlinFloat(floatLiteral: location.speed) } else { nil }
        let altitude: KotlinFloat? = if !location.altitude.isNaN { KotlinFloat(floatLiteral: location.altitude) } else { nil }
        
        let locationModel = LocationDataModel(
            lat: location.coordinate.latitude,
            lng: location.coordinate.longitude,
            accuracy: accuracy,
            timeUTC: location.timestamp.ISO8601Format(.iso8601(timeZone: .gmt)),
            endTimeUTC: nil,
            speed: speed,
            altitude: altitude
        )
        
        Logr.v(tag: "location_Test", message: "Got location update")
        
        DispatchQueue.main.async { [weak self] in
            Task {
                do {
                    let result = try await self?.locationRepo.pushLocation(data: locationModel)
                    if let result = result {
                        let payload = PayloadSwift(result)
                        switch payload {
                        case .loading:
                            Logr.v(tag: "location_Test", message: "error pushing location: loading")
                        case .success(data: _):
                            Logr.v(tag: "location_Test", message: "Location pushed successfully")
                        case .error(error: let error):
                            Logr.v(tag: "location_Test", message: "error pushing location: \(error)")
                        }
                    }
                } catch let error {
                    Logr.v(tag: "location_Test", message: "error pushing location: \(error)")
                }
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        DispatchQueue.main.async { [weak self] in
            Task {
                let place = try? await self?.placeRepo.getPlaceLocallyOnce(placeId: region.identifier)
                self?.notifyUser(id: "placeEnter_\(region.identifier)", message: "User entered place \(place?.nameOrNull ?? region.identifier)")
                try await self?.placeRepo.placesEnter(placesIds: [region.identifier])
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        DispatchQueue.main.async { [weak self] in
            Task {
                let place = try? await self?.placeRepo.getPlaceLocallyOnce(placeId: region.identifier)
                self?.notifyUser(id: "placeExit_\(region.identifier)", message: "User exited place \(place?.nameOrNull ?? region.identifier)")
                try await self?.placeRepo.placesExit(placesIds: [region.identifier])
            }
        }
    }
    
    func locationManagerDidPauseLocationUpdates(_ manager: CLLocationManager) {
        notifyUser(id: "LocationResume", message: "Location updates are paused")
    }
    
    func locationManagerDidResumeLocationUpdates(_ manager: CLLocationManager) {
        notifyUser(id: "LocationResume", message: "Location updates are resumed")
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
//        notifyUser(id: "LocationResume", message: "Permission changed: \(manager.authorizationStatus)")
    }
    
    private func notifyUser(id: String, message: String) {
        let content = UNMutableNotificationContent()
        content.title = "Paralino location updates"
        content.body = message
    
        let request = UNNotificationRequest(identifier: id, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
    
    func notifyPerformance(performance: String) {
        let content = UNMutableNotificationContent()
        content.title = "Paralino tracking"
        content.body = performance
        content.interruptionLevel = .passive
        
        let request = UNNotificationRequest(identifier: "app_track", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}
