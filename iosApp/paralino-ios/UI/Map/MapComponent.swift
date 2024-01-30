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
    @State private var clickedCoordinate: CLLocationCoordinate2D?
    
    @State var task: Task<Void, Never>?
    
    let onMapTap: (_ coordinate: CLLocationCoordinate2D) -> Void
    let onMapLongPress: (_ coordinate: CLLocationCoordinate2D) -> Void
    let onMapTouch: () -> Void
    
    var body: some View {
        ZStack {
            GeometryReader { proxy in
                MapReader { reader in
                    Map(position: $mapState.cameraPosition) {
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
                        MapCompass()
                    }
                    .mapStyle(
                        mapState.isSatelliteMode ? .hybrid(showsTraffic: true) : .standard(showsTraffic: true)
                    )
                    .onMapCameraChange(frequency: .onEnd) { action in
                        mapState.isUpdatingRegion = false
                        let oldCameraRect = mapState.cameraPosition.rect
                        
                        task?.cancel()
                        task = Task {
                            try? await Task.sleep(for: .seconds(1))
                            // ensure camera didn't change in the meantime
                            if !Task.isCancelled, oldCameraRect == mapState.cameraPosition.rect {
                                mapState.cameraPosition = MapCameraPosition.rect(action.rect)
                            }
                        }
                    }
                    .onMapCameraChange(frequency: .continuous) {
                        if !mapState.isUpdatingRegion {
                            task?.cancel()
                            onMapTouch()
                        }
                    }
                    .gesture(
                        LongPressGesture(minimumDuration: 0.20)
                            .sequenced(before: DragGesture(minimumDistance: 0, coordinateSpace: .local))
                            .onEnded { value in
                                switch value {
                                case .second(true, let drag):
                                    let longPressLocation = drag?.location ?? .zero
                                    let customLocation = convertTap(
                                        at: longPressLocation,
                                        for: proxy.size
                                    )
                                    if let customLocation = customLocation {
                                        onMapLongPress(customLocation)
                                    }
                                default:
                                    break
                                }
                            }
                    )
                    .highPriorityGesture(DragGesture(minimumDistance: 10))
                    .onTapGesture{ screenCoord in
                        if let pinLocation = reader.convert(screenCoord, from: .local) {
                            onMapTap(pinLocation)
                        }
                    }
                    .onDisappear {
                        task?.cancel()
                    }
                }
            }
        }
    }
}

private extension MapComponent {
    func convertTap(at point: CGPoint, for mapSize: CGSize) -> CLLocationCoordinate2D? {
        guard let rect = mapState.cameraPosition.rect else { return nil }
        let region = MKCoordinateRegion.init(rect)
        
        let lat = region.center.latitude
        let lng = region.center.longitude
        
        let mapCenter = CGPoint(x: mapSize.width / 2, y: mapSize.height / 2)
        
        // X
        let xValue = (point.x - mapCenter.x) / mapCenter.x
        let xSpan = xValue * region.span.longitudeDelta / 2
        
        // Y
        let yValue = (point.y - mapCenter.y) / mapCenter.y
        let ySpan = yValue * region.span.latitudeDelta / 2
        
        return CLLocationCoordinate2D(latitude: lat - ySpan, longitude: lng + xSpan)
    }
}

extension MKMapRect : Equatable { }
public func ==(lhs: MKMapRect, rhs: MKMapRect) -> Bool {
    return (lhs.size.width, lhs.size.height, lhs.origin.x, lhs.origin.y) == (rhs.size.width, rhs.size.height, rhs.origin.x, rhs.origin.y)
}
