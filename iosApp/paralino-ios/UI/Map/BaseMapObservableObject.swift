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
import paralino_apple_core

class BaseMapObservableObject : ObservableObject {
    
    @Published var markers: [MapAnnotatedItem] = []
    
    @Published var cameraPosition: MapCameraPosition = MapCameraPosition.automatic
    
    @Published var isUpdatingRegion: Bool = false
    @Published var isSatelliteMode: Bool = false
    
    @Published var bottomSafeArea: CGFloat = 80
    
    func onMarkerTap(markerData: FocusedMarkerData) {}
    
    func focusToMarker(data: FocusedMarkerData?, forceZoom: Bool = true) {
        guard let data = data else { return }
        let marker = markers.first { $0.id == data.markerId }
        guard let marker = marker else { return }
        
        if forceZoom {
            focusToMarkers([marker], animate: true)
        } else {
            // mapping from region to rect and vice-versa to get proper center and map size
            guard let rect = cameraPosition.rect else {
                focusToMarkers([marker], animate: true)
                return
            }
            let region = MKCoordinateRegion.init(rect)
            let newRegion = MKCoordinateRegion.init(
                center: marker.coordinate,
                span: region.span
            )
            let newRect = MKMapRect(origin: newRegion.rect.origin, size: rect.size)
            focusCamera(rect: newRect, animate: true)
        }
    }
    
    func focusToMarkers(_ markers: [MapAnnotatedItem], animate: Bool = true, canInteruptAnimation: Bool = false) {
        
        if markers.isEmpty {
            return
        }
        
        let rect = markers
            .map {
                let region = MKCoordinateRegion.init(
                    center: $0.coordinate,
                    latitudinalMeters: max(Double($0.data.accuracy) * 2, 250),
                    longitudinalMeters: max(Double($0.data.accuracy) * 2, 250)
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
        
        focusCamera(rect: mappedRect, animate: animate, canInteruptAnimation: canInteruptAnimation)
    }
    
    func focusCamera(rect: MKMapRect, animate: Bool = true, canInteruptAnimation: Bool = false) {
        
        if isUpdatingRegion && !canInteruptAnimation {
            Logr.v(tag: "marker_update", message: "Marker animation in progress, abort update")
            return
        }
        
        if animate {
            withAnimation {
                // region update flag is reset from map component
                isUpdatingRegion = true
                Logr.v(tag: "marker_update", message: "Start animation")
                cameraPosition = MapCameraPosition.rect(rect)
            }
        } else {
            cameraPosition = MapCameraPosition.rect(rect)
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
