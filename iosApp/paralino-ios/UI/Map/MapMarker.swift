//
//  MapMarker.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import MapKit
import shared
import UIKit

private struct PinShape: Shape {
    func path(in rect: CGRect) -> Path {
        let totalSize = min(rect.width, rect.height)
        let pointerHeight = totalSize * 0.1
        let circleRadius = (totalSize / 2.0) * 0.9
        let circleCenterY = circleRadius
        let circleBottomY = circleCenterY + circleRadius
        let centerX = totalSize / 2.0

        var path = Path()

        path.addEllipse(in: CGRect(
            x: centerX - circleRadius,
            y: circleCenterY - circleRadius,
            width: circleRadius * 2,
            height: circleRadius * 2
        ))

        let triangleHalfWidth = circleRadius * 0.45
        let triangleOverlap = pointerHeight * 0.8
        path.move(to: CGPoint(x: centerX, y: circleBottomY + pointerHeight))
        path.addLine(to: CGPoint(x: centerX - triangleHalfWidth, y: circleBottomY - triangleOverlap))
        path.addLine(to: CGPoint(x: centerX + triangleHalfWidth, y: circleBottomY - triangleOverlap))
        path.closeSubpath()

        return path
    }
}

struct MapMarker: View {
    @ObservedObject var markerItem: MapAnnotatedItem
    
    // make every pin size slightly larger on iOS
    private var pinSize: Double { CGFloat(markerItem.data.getPinSize()) * 1.2 }
    
    var body: some View {
        let vii = ZStack(alignment: .top) {
            if markerItem.data.markerStyle == MapMarkerStyle.pin {
                PinShape()
                    .fill(markerItem.data.active ? Color.accentDark : Color.themeSecondary)
                    .frame(width: pinSize, height: pinSize)
            }
            
            let maxSize: CGFloat = markerItem.data.markerStyle == MapMarkerStyle.pin ? 0.9 : 1.0
            let realSize = pinSize * maxSize
            
            ZStack(alignment: .center) {
                ImageModelImage(
                    imageModel: markerItem.data.image ?? ImageModel.companion.placeholder(),
                    useBackgroundForEmoji: true,
                    imageScale: 0.75
                )
                .frame(width: realSize * 0.85, height: realSize * 0.85)
                .clipShape(Circle())
                .overlay(
                    Circle().stroke(Color.themeSurface, lineWidth: 2)
                )
            }
            .frame(width: realSize, height: realSize)
            .overlay(alignment: .bottomTrailing) {
                if markerItem.data.pinType == MapPinType.privacyZone {
                    Image(
                        resource: MR.shared.images.ic_privacy_zone
                    )
                        .resizable()
                        .renderingMode(.template)
                        .aspectRatio(contentMode: .fill)
                        .frame(width: realSize * 0.35, height: realSize * 0.35)
                        .foregroundStyle(Color.accentDark)
                        .padding(pinSize * 0.03)
                        .background(Color.accentDark.opacity(0.2))
                        .clipShape(Circle())
                        .background(Color.themebackground)
                        .clipShape(Circle())
                }
            }
        }
        
        if markerItem.data.active {
            vii
        } else {
            vii
                .colorMultiply(.yellow)
                .saturation(0.2)
        }
    }
}

class MapAnnotatedItem: MKPointAnnotation, ObservableObject, Identifiable {
    
    var id: String {
        data.markerId
    }
    
    @Published dynamic var data: MapPinData {
        didSet {
            title = data.title
            subtitle = data.title
            coordinate = CLLocationCoordinate2D(
                latitude: data.lat,
                longitude: data.lng
            )
            radius = Double(data.accuracy)
        }
    }
    
    @Published dynamic var radius: Double
    
    init(marker: MapPinData) {
        data = marker
        radius = Double(marker.accuracy)
        super.init()
        title = marker.title
        subtitle = marker.title
        coordinate = CLLocationCoordinate2D(
            latitude: marker.lat,
            longitude: marker.lng
        )
    }
    
    func shouldUpdateView(other: MapAnnotatedItem) -> Bool {
        return data.image != other.data.image || data.active != other.data.active || data.focused != other.data.focused || data.markerStyle != other.data.markerStyle || data.pinType != other.data.pinType || data.getPinSize() != other.data.getPinSize()
    }
}

struct MapPin_Previews: PreviewProvider {
    static let data = MapPinData(
        groupOrSpaceId: GroupOrSpaceIdHelper.shared.wrap(id: ""),
        markerId: "",
        pinType: MapPinType.member,
        markerStyle: MapMarkerStyle.pin,
        focused: true,
        active: false,
        obsolete: false,
        hidden: false,
        image: ImageModel.companion.emoji(emoji: Emojis.shared.robot),
        lat: 12.3,
        lng: 12.3,
        isApproximate: false,
        title: "Marker",
        timeInfo: "",
        accuracy: 0,
        shouldShowApproximateCircle: false
    )
    static var previews: some View {
        MapMarker(markerItem: MapAnnotatedItem(marker: data))
    }
}

class SwiftUIMapMarkerView: MKAnnotationView {
    private var hostingController: UIHostingController<MapMarker>?
    
    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    func setupSwiftUIView(data: MapAnnotatedItem) {
        let markerView = MapMarker(markerItem: data)
        
        // Remove old view
        hostingController?.view.removeFromSuperview()
        hostingController = nil
        
        let hosting = UIHostingController(rootView: markerView)
        hostingController = hosting
        
        // Size the hosted view to fit its content
        hosting.view.backgroundColor = .clear
        hosting.view.sizeToFit()
        
        // Add hosted view to annotation view
        addSubview(hosting.view)
        
        // Update frame to match hosted content
        frame = hosting.view.frame
        
        anchorPoint = CGPoint(x: 0.5, y: data.data.markerStyle == MapMarkerStyle.pin ? 1.0 : 0.5)
    }
}

//class CircleAnimationManager {
//    private var mapView: MKMapView
//    private var currentCircles: [MKCircle] = []
//    private var currentMarkerData: [MapAnnotatedItem] = []
//    
//    init(mapView: MKMapView) {
//        self.mapView = mapView
//    }
//    
//    func updateMarkers(with newData: [MapAnnotatedItem], duration: TimeInterval = 0.3) {
//        // Create new circles for the animation
//        let newCircles = newData.map { MKCircle(center: $0.coordinate, radius: $0.radius) }
//        
//        // If this is the first update, simply add the circles
//        guard !currentCircles.isEmpty else {
//            currentCircles = newCircles
//            newCircles.forEach { mapView.addOverlay($0) }
//            currentMarkerData = newData
//            return
//        }
//        
//        // Create intermediate circles for animation
//        let steps = 60 // Number of animation steps (60fps * duration)
//        let stepDuration = duration / Double(steps)
//        
//        var currentStep = 0
//        
//        // Timer for animation
//        Timer.scheduledTimer(withTimeInterval: stepDuration, repeats: true) { [weak self] timer in
//            guard let self = self else {
//                timer.invalidate()
//                return
//            }
//            
//            currentStep += 1
//            let progress = Double(currentStep) / Double(steps)
//            
//            // Remove current circles
//            
//            mapView.removeOverlays(mapView.overlays)
//            
//            // Create interpolated circles
//            let interpolatedCircles = zip(self.currentMarkerData, newData).map { (old, new) -> MKCircle in
//                // Interpolate coordinates
//                let latDiff = new.coordinate.latitude - old.coordinate.latitude
//                let longDiff = new.coordinate.longitude - old.coordinate.longitude
//                let radiusDiff = new.radius - old.radius
//                
//                let interpolatedLat = old.coordinate.latitude + (latDiff * progress)
//                let interpolatedLong = old.coordinate.longitude + (longDiff * progress)
//                let interpolatedRadius = old.radius + (radiusDiff * progress)
//                
//                return MKCircle(
//                    center: CLLocationCoordinate2D(
//                        latitude: interpolatedLat,
//                        longitude: interpolatedLong
//                    ),
//                    radius: interpolatedRadius
//                )
//            }
//            
//            // Add interpolated circles
//            interpolatedCircles.forEach { self.mapView.addOverlay($0) }
//            self.currentCircles = interpolatedCircles
//            
//            // Check if animation is complete
//            if currentStep >= steps {
//                timer.invalidate()
//                self.currentMarkerData = newData
//                self.currentCircles = newCircles
//            }
//        }
//    }
//}
