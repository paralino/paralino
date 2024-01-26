//
//  MapMarker.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import MapKit
import shared

struct MapMarker: View {
    let pinData: MapPinData
    
    // make every pin size slightly larger on iOS
    private var pinSize: Double { CGFloat(pinData.getPinSize()) * 1.2 }
    
    var body: some View {
        let vii = ZStack(alignment: .top) {
            if pinData.markerStyle == MapMarkerStyle.pin {
                Image(resource: MR.shared.images.ic_pin_bubble)
                    .resizable()
                    .renderingMode(.template)
                    .aspectRatio(contentMode: .fill)
                    .frame(width: pinSize, height: pinSize)
                    .foregroundColor(pinData.active ? Color.accentDark : Color.gray)
            }
            
            let maxSize: CGFloat = pinData.markerStyle == MapMarkerStyle.pin ? 0.92 : 1.0
            let realSize = pinSize * maxSize
            
            ZStack(alignment: .center) {
                ImageModelImage(
                    imageModel: pinData.image ?? ImageModel.companion.placeholder(),
                    useBackgroundForEmoji: true,
                    imageScale: 0.75
                )
                .frame(width: realSize * 0.7, height: realSize * 0.7)
                .clipShape(Circle())
                .overlay(
                    Circle().stroke(Color.themeSecondary, lineWidth: 2)
                )
            }
            .frame(width: realSize, height: realSize)
        }
        
        if pinData.active {
            vii
        } else {
            vii
                .colorMultiply(.yellow)
                .saturation(0.2)
        }
    }
}

struct MapAnnotatedItem: Identifiable {
    let id: String
    var data: MapPinData
    var coordinate: CLLocationCoordinate2D
    
    init(id: String, data: MapPinData, coordinate: CLLocationCoordinate2D) {
        self.id = id
        self.data = data
        self.coordinate = coordinate
    }
    
    init(marker: MapPinData) {
        id = marker.markerId
        data = marker
        coordinate = CLLocationCoordinate2D(
            latitude: marker.lat,
            longitude: marker.lng
        )
    }
}

struct MapPin_Previews: PreviewProvider {
    static let data = MapPinData(
        groupId: "",
        markerId: "",
        pinType: MapPinType.member,
        markerStyle: MapMarkerStyle.pin,
        focused: true,
        active: false,
        obsolete: false,
        hidden: false,
        image: ImageModel.companion.emoji(emoji: Emojis.shared.robot),
        lat: 12.3, lng: 12.3,
        title: "Marker",
        timeInfo: "",
        accuracy: 0,
        shouldShowApproximateCircle: false
    )
    static var previews: some View {
        MapMarker(pinData: data)
    }
}
