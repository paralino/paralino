//
//  CoreLocationManagerDelegate.swift
//  paralino-apple-core
//
//  Created by Zelimir on 2024-10-17.
//

import Foundation
import CoreLocation

public protocol CoreLocationManagerDelegate: NSObject, CLLocationManagerDelegate {
    var lastLocation: CLLocation? {get}
    var monitoredRegions: Set<CLRegion> {get}
    
    func startMonitoringRegion(region: CLCircularRegion)
    func stopMonitoringRegion(region: CLRegion)
}
