//
//  MapHomeState.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import Foundation
import shared

class MapHomeState : BaseMapObservableObject {
    
    let vm: MapHomeViewModel
    
    @Published var groupState: Bool? = nil
    
    @Published var uiState: MapHomeScreenState = MapHomeScreenState.Empty(
        currentUserImage: ImageModel.companion.placeholder(),
        isSatelliteMode: false,
        alertDialog: nil
    )
    
    override init() {
        vm = KoinIOS.ViewModels.shared.mapHomeVM
        
        super.init()
        
        vm.observe(flow: vm.allGroupsState) { [weak self] state in
            self?.groupState = state as? Bool
        }
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let state = state as? MapHomeScreenState {
                self?.uiState = state
                if let readyState = state as? MapHomeScreenState.Ready {
                    let markers = readyState.visibleMarkers.map { marker in
                        MapAnnotatedItem(marker: marker)
                    }
                    
                    self?.markers = markers
                    
                    if readyState.mapHomeScreenFocusType is shared.MapHomeScreenFocusTypeALL_PLACES {
                        self?.focusToMarkers(markers.filter { $0.data.pinType == MapPinType.place }, animate: true)
                    } else if readyState.mapHomeScreenFocusType is shared.MapHomeScreenFocusTypeALL_MEMBERS {
                        self?.focusToMarkers(markers.filter { $0.data.pinType == MapPinType.member }, animate: true)
                    } else if let focus = readyState.mapHomeScreenFocusType as? shared.MapHomeScreenFocusTypeMEMBER {
                        self?.focusToMarker(data: readyState.trackingMarker, forceZoom: focus.forceZoom)
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
