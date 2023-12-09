//
//  MapPin.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import MapKit
import shared

struct MapPin: View {
    var body: some View {
        Text(/*@START_MENU_TOKEN@*/"Hello, World!"/*@END_MENU_TOKEN@*/)
    }
}

struct MapAnnotatedItem: Identifiable {
    let id: String
    var data: MapPinData
    var coordinate: CLLocationCoordinate2D
}

struct MapPin_Previews: PreviewProvider {
    static var previews: some View {
        MapPin()
    }
}
