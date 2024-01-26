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
    
    @StateObject var mapState: BaseMapObservableObject
    
    let onMapTouch: () -> Void
    
    var body: some View {
        //        MapReader { reader in
        Map(position: $mapState.region) {
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
                                mapState.onMarkerTap(markerData: marker.data.asFocusedMarker())
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
        .mapStyle(
            mapState.isSatelliteMode ? .hybrid(showsTraffic: true) : .standard(showsTraffic: true)
        )
        .onMapCameraChange(frequency: .onEnd) { action in
            mapState.isUpdatingRegion = false
        }
        //        }
        //        .onTapGesture {
        //            // TODO fix: not working
        //            onMapTouch()
        //        }
    }
}

