//
//  MapComponent.swift
//  paralino-ios
//
//  Created by Zelimir on 2023-12-19.
//

import SwiftUI
import shared
import MapKit

struct MapComponent: View {
    
    @StateObject var mapState: MapHomeState
    @State var selectedMarkerId: String?
    
    let trackMarker: (FocusedMarkerData) -> Void
    let onMapTouch: () -> Void
    
    var body: some View {
//        MapReader { reader in
        Map(position: $mapState.region, selection: $selectedMarkerId) {
                ForEach(mapState.markers) { marker in
                    
                    MapCircle(center: marker.coordinate, radius: CLLocationDistance(Int32(marker.data.accuracy)))
                        .foregroundStyle(marker.data.pinType == MapPinType.member ? Color.themePrimary.opacity(0.6) : Color.blue.opacity(0.4))
                        .mapOverlayLevel(level: marker.data.pinType == MapPinType.member ? .aboveLabels : .aboveRoads)
                    
                    Annotation(
                        coordinate: marker.coordinate,
                        anchor: UnitPoint(x: 0.5, y: marker.data.markerStyle == MapMarkerStyle.pin ? 1.0 : 0.5),
                        content: {
                            Button(
                                action: {
                                    trackMarker(
                                        FocusedMarkerData(markerId: marker.id, type: marker.data.pinType)
                                    )
                                },
                                label: { MapMarker(pinData: marker.data) }
                            )
                        },
                        label: {
                            Text(marker.data.title ?? "--")
                        }
                    )
                }
            }
            .mapControls {
//                        MapUserLocationButton()
                MapCompass()
            }
            .mapStyle(.standard(showsTraffic: true))
            .safeAreaPadding(.top, 80)
            .safeAreaPadding(.horizontal)
            .onChange(of: selectedMarkerId, initial: true) { old, new in
                guard let selection = new else { return }
                let marker = mapState.markers.first { $0.id == selection }
                guard let marker = marker else { return }
                let center = CLLocationCoordinate2D(
                    latitude: marker.coordinate.latitude,
                    longitude: marker.coordinate.longitude
                )
                
//                mapState.region = MapCameraPosition.camera(MapCamera(centerCoordinate: center, distance: 3000))
            }
//        }
//        .onTapGesture {
//            // TODO fix: not working
//            onMapTouch()
//        }
    }
}

