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
    let isAccurate: Bool
    let isClustered: Bool
    
    var body: some View {
        if isAccurate {
            Image(
    //            systemName: isAccurate ? "mappin" : "mappin.and.ellipse"
                resource: isClustered ? MR.shared.images.ic_target_circles : isAccurate ? MR.shared.images.ic_circle_with_dot : MR.shared.images.ic_not_listed_location_filled
            )
                .resizable()
                .renderingMode(.template)
                .aspectRatio(contentMode: .fill)
                .frame(width: markerSize, height: markerSize)
                .foregroundColor(markerTintColor)
                .background {
                    if let backgroundColor = markerBackgroundColor {
                        Circle()
                            .fill(backgroundColor)
                            .frame(width: markerSize, height: markerSize)
                    }
                }
        } else {
            Image(
                resource: MR.shared.images.ic_not_listed_location_filled
            )
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: markerSize, height: markerSize)
        }
    }

    private var markerSize: CGFloat {
        if isClustered {
            24
        } else if isAccurate {
            16
        } else {
            28
        }
    }

    private var markerBackgroundColor: Color? {
        guard isClustered || isAccurate else { return nil }
        if isDarkMode {
            if isClustered {
                return Color.accentLight
            } else if isAccurate {
                return Color.lightBackground
            }
        } else {
            if isClustered {
                return Color.accentDark
            } else if isAccurate {
                return Color.redMain
            }
        }
        return nil
    }

    private var markerTintColor: Color? {
        if isDarkMode {
            if isClustered {
                return Color.darkThemePrimary
            } else if isAccurate {
                return Color.redMain
            }
        } else {
            if isClustered || isAccurate {
                return Color.lightBackground
            }
        }
        return nil
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
    
    var isInaccurate: Bool {
        NumberExtKt.isLocationInaccurate(data.accuracy)
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
        let isClustered: Bool = data.data is LocationHistoryPointStateCluster
        let markerView = HistoryMapMarker(isDarkMode: isDarkMode, isAccurate: !data.isInaccurate, isClustered: isClustered)
        
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
        
        if !data.isInaccurate || isClustered {
            anchorPoint = CGPoint(x: 0.5, y: 0.5)
        } else {
            anchorPoint = CGPoint(x: 0.5, y: 1.0)
        }
    }
}
