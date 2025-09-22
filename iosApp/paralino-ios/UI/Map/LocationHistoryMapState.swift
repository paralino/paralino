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
        vm = KoinIOS.ViewModels.shared.locationHistoryVM(groupId: groupId, memberUserId: memberUserId)
        
        super.init()
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let state = state as? LocationHistoryScreenState {
                let previousState = self?.uiState ?? LocationHistoryScreenState.companion.emptyState()
                self?.uiState = state
                
                if state.showPolyline {
                    self?.polylinePoints = state.selectedLocations
                        .compactMap {
                            if let lat = $0.lat, let lng = $0.lng {
                                CLLocationCoordinate2D(latitude: lat.doubleValue, longitude: lng.doubleValue)
                            } else {
                                nil
                            }
                        }
                } else {
                    self?.polylinePoints = []
                }
                
                if state.showPins {
                    self?.historyPinMarkers = state.selectedLocations
                        .compactMap {
                            if $0.lat != nil, $0.lng != nil {
                                HistoryMapMarkerData(id: $0.pointId, point: $0)
                            } else {
                                nil
                            }
                        }
                } else {
                    self?.historyPinMarkers = []
                }
                
                self?.clusterHistoryPins = state.clusterPins
                
                if state.focusType is LocationHistoryScreenFocusTypeOVERVIEW {
                    if let region = self?.getFocusRegion(state.selectedLocations) {
                        let areLocationsChanged: Bool = previousState.selectedLocations.map({ $0.pointId }) != state.selectedLocations.map({ $0.pointId })
                        self?.focusCamera(
                            rect: region,
                            animate: true,
                            canInteruptAnimation: areLocationsChanged
                        )
                    }
                } else if state.focusType.isMember {
                    if let pin = state.selectedMapPinData {
                        self?.pendingFocusedMarker = PendingFocusedMarkerData(
                            marker: pin.asFocusedMarker(),
                            animate: true,
                            forceZoom: state.focusType is LocationHistoryScreenFocusTypeMEMBER_FOCUSED
                        )
                    }
                }
                
                if state.dataViewType is LocationHistoryDataViewTypeStats {
                    self?.markers = []
                } else if state.dataViewType is LocationHistoryDataViewTypeLocationPoint {
                    if let pin = state.selectedMapPinData {
                        self?.markers = [MapAnnotatedItem(marker: pin)]
                    } else {
                        self?.markers = []
                    }
                }
                
                self?.isSatelliteMode = state.isSatelliteMode
            }
        }
    }
    
    private func getFocusRegion(_ points: [LocationHistoryPointState]) -> MKMapRect? {
        
        if points.isEmpty {
            return nil
        }
        
        let rect = points
            .compactMap {
                if let lat = $0.lat, let lng = $0.lng, let acc = $0.accuracy {
                    let region = MKCoordinateRegion.init(
                        center: CLLocationCoordinate2D(latitude: lat.doubleValue, longitude: lng.doubleValue),
                        latitudinalMeters: max(acc.doubleValue * 2, 250),
                        longitudinalMeters: max(acc.doubleValue * 2, 250)
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
        vm.onEvent(event: LocationHistoryUiEventOnMarkerClick(pin: markerData))
    }
    
    override func onHistoryMarkerTap(markerData: HistoryMapMarkerData) {
        vm.onEvent(event: LocationHistoryUiEventOnPointClicked(point: markerData.data))
    }
    
    deinit {
        vm.cleanup()
    }
}

