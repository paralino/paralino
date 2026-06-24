//
//  MapTapActionMarker.swift
//  paralino-ios
//
//  Created by Zelimir on 24.06.26.
//

import SwiftUI
import MapKit
import shared
import UIKit

final class MapTapActionMarkerData: NSObject, MKAnnotation {
    static let reuseIdentifier = "MapTapActionMarker"

    let showPrivacyZoneButton: Bool
    dynamic var coordinate: CLLocationCoordinate2D

    init(lat: Double, lng: Double, showPrivacyZoneButton: Bool) {
        coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lng)
        self.showPrivacyZoneButton = showPrivacyZoneButton
    }
}

private struct MapLongTapPopupShape: Shape {
    var cornerRadius: CGFloat
    var pointerHeight: CGFloat
    var pointerHalfWidth: CGFloat

    func path(in rect: CGRect) -> Path {
        let width = rect.width
        let height = rect.height
        let bodyBottom = height - pointerHeight
        let centerX = width / 2
        let radius = min(cornerRadius, min(width, bodyBottom) / 2)

        var path = Path()

        // Single continuous outline so stroke does not draw a small line above the pointer.
        path.move(to: CGPoint(x: centerX, y: height))
        path.addLine(to: CGPoint(x: centerX + pointerHalfWidth, y: bodyBottom))
        path.addLine(to: CGPoint(x: width - radius, y: bodyBottom))
        path.addArc(
            center: CGPoint(x: width - radius, y: bodyBottom - radius),
            radius: radius,
            startAngle: .degrees(90),
            endAngle: .degrees(0),
            clockwise: true
        )
        path.addLine(to: CGPoint(x: width, y: radius))
        path.addArc(
            center: CGPoint(x: width - radius, y: radius),
            radius: radius,
            startAngle: .degrees(0),
            endAngle: .degrees(-90),
            clockwise: true
        )
        path.addLine(to: CGPoint(x: radius, y: 0))
        path.addArc(
            center: CGPoint(x: radius, y: radius),
            radius: radius,
            startAngle: .degrees(-90),
            endAngle: .degrees(-180),
            clockwise: true
        )
        path.addLine(to: CGPoint(x: 0, y: bodyBottom - radius))
        path.addArc(
            center: CGPoint(x: radius, y: bodyBottom - radius),
            radius: radius,
            startAngle: .degrees(180),
            endAngle: .degrees(90),
            clockwise: true
        )
        path.addLine(to: CGPoint(x: centerX - pointerHalfWidth, y: bodyBottom))
        path.closeSubpath()

        return path
    }
}

private struct MapActionBubbleButton: View {
    let text: String
    let action: () -> Void
    @ViewBuilder let icon: () -> Image

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                icon()
                    .renderingMode(.template)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 14, height: 14)

                Text(text)
                    .font(.caption)
                    .fontWeight(.regular)
                    .fixedSize(horizontal: true, vertical: false)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            .background(Color.themePrimary)
            .foregroundColor(Color.themeOnPrimary)
            .clipShape(Capsule())
            .fixedSize(horizontal: true, vertical: false)
        }
        .buttonStyle(.plain)
    }
}

struct MapActionBubbleMarkerView: View {
    let showPrivacyZoneButton: Bool
    let onCreatePlace: () -> Void
    let onCreatePrivacyZone: () -> Void

    private let pointerHeight: CGFloat = 10
    private let cornerRadius: CGFloat = 12
    private let pointerHalfWidth: CGFloat = 8
    private let borderWidth: CGFloat = 2

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            MapActionBubbleButton(
                text: MR.shared.strings.map_long_tap_action_new_place.localizedString(),
                action: onCreatePlace
            ) {
                Image(resource: MR.shared.images.ic_home_work)
            }

            if showPrivacyZoneButton {
                MapActionBubbleButton(
                    text: MR.shared.strings.map_long_tap_action_new_privacy_zone.localizedString(),
                    action: onCreatePrivacyZone
                ) {
                    Image(resource: MR.shared.images.ic_privacy_zone)
                }
            }
        }
        .fixedSize(horizontal: true, vertical: true)
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        .padding(.bottom, pointerHeight)
        .background {
            MapLongTapPopupShape(
                cornerRadius: cornerRadius,
                pointerHeight: pointerHeight,
                pointerHalfWidth: pointerHalfWidth
            )
            .fill(Color.themebackground)
            .overlay(
                MapLongTapPopupShape(
                    cornerRadius: cornerRadius,
                    pointerHeight: pointerHeight,
                    pointerHalfWidth: pointerHalfWidth
                )
                .fill(Color.accentDark.opacity(0.2))
            )
            .overlay(
                MapLongTapPopupShape(
                    cornerRadius: cornerRadius,
                    pointerHeight: pointerHeight,
                    pointerHalfWidth: pointerHalfWidth
                )
                .stroke(Color.themebackground, lineWidth: borderWidth)
            )
        }
    }
}

final class SwiftUIMapTapActionMarkerView: MKAnnotationView {
    private var hostingController: UIHostingController<MapActionBubbleMarkerView>?

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        canShowCallout = false
        isEnabled = true
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) not implemented")
    }

    func setupSwiftUIView(
        data: MapTapActionMarkerData,
        onCreatePlace: @escaping () -> Void,
        onCreatePrivacyZone: @escaping () -> Void
    ) {
        let markerView = MapActionBubbleMarkerView(
            showPrivacyZoneButton: data.showPrivacyZoneButton,
            onCreatePlace: onCreatePlace,
            onCreatePrivacyZone: onCreatePrivacyZone
        )

        hostingController?.view.removeFromSuperview()
        hostingController = nil

        let hosting = UIHostingController(rootView: markerView)
        hostingController = hosting
        hosting.view.backgroundColor = .clear
        hosting.view.sizeToFit()

        addSubview(hosting.view)
        frame = hosting.view.frame
        anchorPoint = CGPoint(x: 0.5, y: 1.0)
    }
}
