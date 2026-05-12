//
//  HistoryStartStopMapMarker.swift
//  paralino-ios
//
//  Created by Zelimir on 12.05.26.
//

import MapKit
import SwiftUI
import UIKit

struct HistoryStartStopMapMarker: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.system(size: 10, weight: .bold))
            .foregroundColor(.white)
            .frame(width: 18, height: 18)
            .background {
                Circle()
                    .fill(Color.redMain)
            }
            .overlay {
                Circle()
                    .stroke(Color.white, lineWidth: 1)
            }
    }
}

final class HistoryStartStopMapMarkerData: MKPointAnnotation, ObservableObject, Identifiable {
    let id: String
    let pointId: String
    let label: String

    init(pointId: String, label: String, coordinate: CLLocationCoordinate2D) {
        self.pointId = pointId
        self.label = label
        self.id = "history-start-stop-\(pointId)-\(label)"
        super.init()
        self.coordinate = coordinate
    }
}

final class SwiftUIHistoryStartStopMapMarkerView: MKAnnotationView {
    private var hostingController: UIHostingController<HistoryStartStopMapMarker>?

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }

    func setupSwiftUIView(data: HistoryStartStopMapMarkerData) {
        let markerView = HistoryStartStopMapMarker(label: data.label)

        hostingController?.view.removeFromSuperview()
        hostingController = nil

        let hosting = UIHostingController(rootView: markerView)
        hostingController = hosting

        hosting.view.backgroundColor = .clear
        hosting.view.sizeToFit()

        addSubview(hosting.view)

        frame = hosting.view.frame
        anchorPoint = CGPoint(x: 0.5, y: 0.5)
        clusteringIdentifier = nil
    }
}
