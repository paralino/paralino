//
//  HistoryMapMarker.swift
//  paralino-ios
//
//  Created by Zelimir on 10.07.25.
//

import SwiftUI
import MapKit
import shared
import UIKit

struct HistoryMapMarker: View {
    let isDarkMode: Bool
    
    var body: some View {
        Image(resource: MR.shared.images.ic_location_pin_filled)
            .resizable()
            .renderingMode(.template)
            .aspectRatio(contentMode: .fill)
            .frame(width: 16, height: 16)
            .foregroundColor(isDarkMode ? Color.highlightGold : Color.redMain)
    }
}

class HistoryMapMarkerData: MKPointAnnotation, ObservableObject, Identifiable {
    
    var id: String
    
    @Published dynamic var data: LocationHistoryPointState {
        didSet {
            coordinate = CLLocationCoordinate2D(
                latitude: data.lat!.doubleValue,
                longitude: data.lng!.doubleValue
            )
        }
    }
    
    init(id: String, point: LocationHistoryPointState) {
        data = point
        self.id = id
        super.init()
        coordinate = CLLocationCoordinate2D(
            latitude: data.lat!.doubleValue,
            longitude: data.lng!.doubleValue
        )
    }
}

class SwiftUIHistoryMapMarkerView: MKAnnotationView {
    private var hostingController: UIHostingController<HistoryMapMarker>?
    
    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }
    
    func setupSwiftUIView(data: HistoryMapMarkerData, isDarkMode: Bool) {
        let markerView = HistoryMapMarker(isDarkMode: isDarkMode)
        
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
        
        anchorPoint = CGPoint(x: 0.5, y: 1.0)
    }
}
