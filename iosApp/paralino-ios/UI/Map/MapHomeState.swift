//
//  MapHomeState.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import Foundation
import shared
import SwiftUI
import CoreLocation
import MapKit

class MapHomeState : BaseMapObservableObject {
    
    let vm: MapHomeViewModel
    
    @Published var groupState: Bool? = nil
    @Published var path: [NavigationGraph] = []
    @Published var uiState: MapHomeScreenState = MapHomeScreenState.Empty(currentUserImage: ImageModel.companion.placeholder(), isSatelliteMode: false)
    
    override init() {
        vm = KoinIOS.ViewModels.shared.mapHomeVM
        
        super.init()
        
        vm.observe(flow: vm.allGroupsState) { [weak self] state in
            if let state = state as? Bool {
                self?.groupState = state
                if state {
                    if self?.path.isEmpty != false {
                        self?.path = [.home(.map)]
                    } else {
                        self?.path[0] = .home(.map)
                    }
                } else {
                    self?.path = [.home(.groupInit)]
                }
            } else {
                self?.groupState = nil
            }
        }
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let state = state as? MapHomeScreenState {
                self?.uiState = state
                if let readyState = state as? MapHomeScreenState.Ready {
                    let markers = readyState.visibleMarkers.map { marker in
                        MapAnnotatedItem(
                            id: marker.markerId,
                            data: marker,
                            coordinate: CLLocationCoordinate2D(
                                latitude: marker.lat,
                                longitude: marker.lng
                            )
                        )
                    }
                    
                    self?.markers = markers
                    
                    if readyState.mapHomeScreenFocusType is shared.MapHomeScreenFocusTypeALL_PLACES {
                        self?.focusToMarkers(markers.filter { $0.data.pinType == MapPinType.place }, animate: true)
                    } else if readyState.mapHomeScreenFocusType is shared.MapHomeScreenFocusTypeALL_MEMBERS {
                        self?.focusToMarkers(markers.filter { $0.data.pinType == MapPinType.member }, animate: true)
                    } else if readyState.mapHomeScreenFocusType is shared.MapHomeScreenFocusTypeMEMBER {
                        self?.focusToMarker(data: readyState.trackingMarker)
                    } else if readyState.mapHomeScreenFocusType is shared.MapHomeScreenFocusTypePLACE {
                        self?.focusToMarker(data: readyState.trackingMarker)
                    } else if readyState.mapHomeScreenFocusType is shared.MapHomeScreenFocusTypeNONE {
//                        do nothing
                    }
                }
                
                self?.isSatelliteMode = state.isSatelliteMode
            }
        }
    }
    
    override func onMarkerTap(markerData: FocusedMarkerData) {
        vm.trackMarker(markerData: markerData)
    }
    
    deinit {
        vm.cleanup()
    }
}
