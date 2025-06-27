//
//  LocationHistoryMapState.swift
//  paralino-ios
//
//  Created by Zelimir on 2025-06-26.
//

import Foundation
import MapKit
import SwiftUI
import shared
import paralino_apple_core

class LocationHistoryMapState : BaseMapObservableObject {
    
    let vm: LocationHistoryViewModel
    
    @Published var uiState: LocationHistoryScreenState = LocationHistoryScreenState.companion.emptyState()
    
    init(groupId: String, memberUserId: String) {
        vm = KoinIOS.ViewModels.shared.locationHistoryVM
        
        vm.onEvent(event: LocationHistoryUiEventLoadHistory(groupId: groupId, memberUserId: memberUserId))
        
        super.init()
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let state = state as? LocationHistoryScreenState {
                self?.uiState = state
                
                self?.polylinePoints = state.selectedLocations
                    .compactMap { $0.point.loc }
                    .map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) }
                
                if state.mapFocusType is LocationHistoryMapFocusTypeStats {
                    if let region = self?.getFocusRegion(state.selectedLocations) {
                        self?.focusCamera(rect: region, animate: true, canInteruptAnimation: true)
                    }
                    self?.markers = []
                } else if state.mapFocusType is LocationHistoryMapFocusTypeLocationPoint {
                    if let pin = state.selectedMapPinData {
                        self?.markers = [MapAnnotatedItem(marker: pin)]
                        self?.focusToMarker(data: pin.asFocusedMarker(), forceZoom: false)
                    } else {
                        self?.markers = []
                    }
                } else if state.mapFocusType is LocationHistoryMapFocusTypeTimeline {
                    // nothing for now
                }
                
                self?.isSatelliteMode = state.isSatelliteMode
            }
        }
    }
    
    func getFocusRegion(_ points: [LocationHistoryPointState]) -> MKMapRect? {
        
        if markers.isEmpty {
            return nil
        }
        
        let rect = points
            .compactMap {
                if let loc = $0.point.loc {
                    let region = MKCoordinateRegion.init(
                        center: CLLocationCoordinate2D(latitude: loc.lat, longitude: loc.lng),
                        latitudinalMeters: max(Double(loc.acc) * 2, 250),
                        longitudinalMeters: max(Double(loc.acc) * 2, 250)
                    )
                    return region.rect
                } else {
                    return nil
                }
            }
            .reduce(MKMapRect.null) { $0.union($1) }
        
        let mappedRect = MKMapRect(
            x: rect.origin.x,
            y: rect.origin.y,
            width: rect.size.width * 1.4,
            height: rect.size.height * 1.4
        )
            .offsetBy(dx: -rect.width * 0.2, dy: -rect.height * 0.2)
    
        return mappedRect
    }
    
    override func onMarkerTap(markerData: MapPinData) {
        
    }
    
    deinit {
        vm.cleanup()
    }
}

