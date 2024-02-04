//
//  LocationManager.swift
//  paralino-ios
//
//  Created by Zelimir on 2023-11-24.
//

import Foundation
import CoreLocation
import shared

class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    
    let manager = CLLocationManager()
    
    lazy var locationRepo: LocationRepositoryWrapper = { KoinIOS.Instances.shared.locationRepo }()
    lazy var placeRepo: PlacesRepository = { KoinIOS.Instances.shared.placesRepo }()

    @Published var location: CLLocationCoordinate2D?

    override init() {
        super.init()
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func requestLocationPermission() {
        manager.requestWhenInUseAuthorization()
    }
    
    func requestAlwaysOnLocationPermission() {
        manager.requestAlwaysAuthorization()
    }
    
    func startLocationUpdates() {
        manager.startUpdatingLocation()
//        manager.startMonitoringSignificantLocationChanges()
    }
    
    func stopLocationUpdates() {
        manager.stopUpdatingLocation()
//        manager.stopMonitoringSignificantLocationChanges()
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
                try await self?.placeRepo.placesEnter(placesIds: [region.identifier])
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        DispatchQueue.main.async { [weak self] in
            Task {
                try await self?.placeRepo.placesExit(placesIds: [region.identifier])
            }
        }
    }
}
