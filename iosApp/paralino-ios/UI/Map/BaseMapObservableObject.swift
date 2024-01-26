//
//  BaseMapObservableObject.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-01-25.
//

import Foundation
import MapKit
import SwiftUI
import shared

class BaseMapObservableObject : ObservableObject {
    @Published var markers: [MapAnnotatedItem] = []
    
    @Published var region: MapCameraPosition = MapCameraPosition.automatic
    
    var isUpdatingRegion: Bool = false
    @Published var isSatelliteMode: Bool = false
    
    func onMarkerTap(markerData: FocusedMarkerData) {}
    
    func focusToMarker(data: FocusedMarkerData?) {
        guard let data = data else { return }
        let marker = markers.first { $0.id == data.markerId }
        guard let marker = marker else { return }
        focusToMarkers([marker], animate: true)
    }
    
    func focusToMarkers(_ markers: [MapAnnotatedItem], animate: Bool = true, canInteruptAnimation: Bool = false) {
        if isUpdatingRegion && !canInteruptAnimation {
            Logr.v(tag: "marker_update", message: "Marker animation in progress, abort update")
            return
        }
        
        if markers.isEmpty {
            return
        }
        
        let rect = markers
            .map {
                let region = MKCoordinateRegion.init(
                    center: $0.coordinate,
                    latitudinalMeters: Double($0.data.accuracy) * 2,
                    longitudinalMeters: Double($0.data.accuracy) * 2
                )
                return region.rect
            }
            .reduce(MKMapRect.null) { $0.union($1) }
        
        let mappedRect = MKMapRect(
            x: rect.origin.x,
            y: rect.origin.y,
            width: rect.size.width * 1.4,
            height: rect.size.height * 1.4
        )
            .offsetBy(dx: -rect.width * 0.2, dy: -rect.height * 0.2)
        
        if animate {
            withAnimation {
                // region update flag is reset from map component
                isUpdatingRegion = true
                Logr.v(tag: "marker_update", message: "Start animation")
                region = MapCameraPosition.rect(mappedRect)
            }
        } else {
            region = MapCameraPosition.rect(mappedRect)
        }
    }
    
    func refocusMap(animate: Bool = true, canInteruptAnimation: Bool = false) {
        focusToMarkers(markers, animate: animate, canInteruptAnimation: canInteruptAnimation)
    }
}

// credit: https://stackoverflow.com/a/73201022
extension MKCoordinateRegion {
    public var rect : MKMapRect {
        let topLeft = CLLocationCoordinate2D(latitude: self.center.latitude + (self.span.latitudeDelta/2), longitude: self.center.longitude - (self.span.longitudeDelta/2))
        let bottomRight = CLLocationCoordinate2D(latitude: self.center.latitude - (self.span.latitudeDelta/2), longitude: self.center.longitude + (self.span.longitudeDelta/2))

        let a = MKMapPoint(topLeft)
        let b = MKMapPoint(bottomRight)
        
        return MKMapRect(origin: MKMapPoint(x:min(a.x,b.x), y:min(a.y,b.y)), size: MKMapSize(width: abs(a.x-b.x), height: abs(a.y-b.y)))
    }
}
