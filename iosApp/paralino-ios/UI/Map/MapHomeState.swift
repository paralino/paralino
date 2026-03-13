//
//  MapHomeState.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import Foundation
import shared
import MapKit
import SwiftUI

class MapHomeState : BaseMapObservableObject {
    
    let vm: MapHomeViewModel
    
    @Published var uiState: MapHomeScreenState = MapHomeScreenState.Empty(
        groupsEmptyState: nil,
        selectedGroupId: nil,
        selectedGroupImage: nil,
        currentUserImage: ImageModel.companion.placeholder(),
        isPersonalSpace: true,
        androidMapSDKProvider: nil,
        isSatelliteMode: false,
        alertDialog: nil
    )
    
    private var historyUiState: LocationHistoryScreenUiState? = nil
    
    override init() {
        vm = KoinIOS.ViewModels.shared.mapHomeVM
        
        super.init()
        
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let state = state as? MapHomeScreenState {
                self?.uiState = state
                if let readyState = state as? MapHomeScreenState.Ready, readyState.locationHistoryMode == nil {
                    let markers = readyState.visibleMarkers.map { marker in
                        MapAnnotatedItem(marker: marker)
                    }
                    
                    self?.markers = markers
                    self?.polylinePoints = []
                    self?.historyPinMarkers = []
                    self?.clusterHistoryPins = true
                    self?.pendingFocusedMarker = nil
                    self?.historyUiState = nil
                    
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
        
        vm.observe(flow: vm.historyUiState) { [weak self] state in
            let uiState = state as? LocationHistoryScreenUiState
            let previousState = self?.historyUiState
            self?.historyUiState = uiState
            if let state = uiState {
                
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
                
                self?.clusterHistoryPins = state.clusteringPinsMode.isClusteringOn
                
                if state.focusType is LocationHistoryScreenFocusTypeOVERVIEW {
                    if let region = self?.getFocusRegion(state.selectedLocations) {
                        let areLocationsChanged: Bool = previousState?.selectedLocations.map({ $0.pointId }) != state.selectedLocations.map({ $0.pointId })
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            self?.focusCamera(
                                rect: region,
                                animate: true,
                                canInteruptAnimation: areLocationsChanged
                            )
                        }
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
        if (uiState.asReady()?.locationHistoryMode == nil) {
            onMainEvent(event: MapHomeScreenUiEventOnMarkerClick(pin: markerData))
        } else {
            onHistoryEvent(event: LocationHistoryUiEventOnMarkerClick(pin: markerData))
        }
    }
    
    override func onHistoryMarkerTap(markerData: HistoryMapMarkerData) {
        onHistoryEvent(event: LocationHistoryUiEventOnPointClicked(point: markerData.data))
    }
    
    func onMapTouch() {
        if (uiState.asReady()?.locationHistoryMode == nil) {
            onMainEvent(event: MapHomeScreenUiEventOnMapTouch())
        } else {
            onHistoryEvent(event: LocationHistoryUiEventOnMapTouch())
        }
    }
    
    func onMainEvent(event: MapHomeScreenUiEvent) {
        vm.onEvent(event_: event)
    }
    
    func onHistoryEvent(event: LocationHistoryUiEvent) {
        vm.onEvent(event: event)
    }
    
    deinit {
        vm.cleanup()
    }
}
