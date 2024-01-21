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

class MapHomeState : ObservableObject {
    
    let vm: MapHomeViewModel
    
    @Published var groupState: Bool? = nil
    @Published var path: [NavigationGraph] = []
    @Published var uiState: MapHomeScreenState = MapHomeScreenState.Empty(currentUserImage: ImageModel.companion.placeholder(), isSatelliteMode: false)
    
    var readyState: MapHomeScreenState.Ready? {
        uiState as? MapHomeScreenState.Ready
    }
    
    @Published var markers: [MapAnnotatedItem] = []
    
    @Published var region: MapCameraPosition = MapCameraPosition.region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: 0,
                longitude: 0
            ),
            span: MKCoordinateSpan(latitudeDelta: 160, longitudeDelta: 160)
        )
    )
    
    @Published var trackingMarker: FocusedMarkerData? = nil
    var isUpdatingRegion: Bool = false
    
    init() {
        vm = KoinIOS.ViewModels.shared.mapHomeVM
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
                let oldState = self?.uiState as? MapHomeScreenState.Ready
                self?.uiState = state
                if let readyState = state as? MapHomeScreenState.Ready {
                    self?.markers = readyState.visibleMarkers.map { marker in
                        MapAnnotatedItem(
                            id: marker.markerId,
                            data: marker,
                            coordinate: CLLocationCoordinate2D(
                                latitude: marker.lat,
                                longitude: marker.lng
                            )
                        )
                    }
                }
                self?.trackingMarker = state.asReady()?.trackingMarker
                self?.updateRegion()
            }
        }
    }
    
    func updateRegion() {
        guard let selection = trackingMarker else { return }
        let marker = markers.first { $0.id == selection.markerId }
        guard let marker = marker else { return }
        guard !isUpdatingRegion else {
            Logr.v(tag: "marker_update", message: "Marker animation in progress, abort update")
            return
        }
        let center = CLLocationCoordinate2D(
            latitude: marker.coordinate.latitude,
            longitude: marker.coordinate.longitude
        )
        
        withAnimation {
            isUpdatingRegion = true
            Logr.v(tag: "marker_update", message: "Start animation")
            region = MapCameraPosition.camera(MapCamera(centerCoordinate: center, distance: 3000))
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
