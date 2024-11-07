//
//  LseLocationManager.swift
//  paralino-lse
//
//  Created by Zelimir on 2024-10-17.
//

import Foundation
import CoreLocation
import paralino_apple_core

class NseLocationManager: NSObject, CoreLocationManagerDelegate {
    
    public var lastLocation: CLLocation? {
        nil
    }
    
    public var monitoredRegions: Set<CLRegion> {
        []
    }
    
    public func startMonitoringRegion(region: CLCircularRegion) {}
    public func stopMonitoringRegion(region: CLRegion) {}
}
