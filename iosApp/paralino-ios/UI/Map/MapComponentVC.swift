//
//  MapComponentVC.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-22.
//
import UIKit
import MapKit
import SwiftUI
import shared
import Combine

class MapComponentVC: UIViewController, UIGestureRecognizerDelegate {
    
    private var mapState: BaseMapObservableObject?
    
    private var cancellables = Set<AnyCancellable>()
    private let positionAnimator = ValueAnimator()
    
    var onMapTap: ((CLLocationCoordinate2D) -> Void)?
    var onMapLongPress: ((CLLocationCoordinate2D) -> Void)?
    var onMapPan: (() -> Void)?
    
    private let mapView: MKMapView = {
        let map = MKMapView()
        map.translatesAutoresizingMaskIntoConstraints = false
        map.showsCompass = true
        map.showsTraffic = true
        return map
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupMapView()
        setupGestures()
        bindToViewModel()
    }
    
    private func setupMapView() {
        view.addSubview(mapView)
        
        NSLayoutConstraint.activate([
            mapView.topAnchor.constraint(equalTo: view.topAnchor),
            mapView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            mapView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            mapView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -mapState!.bottomSafeArea)
        ])
        
        mapView.delegate = self
        
        // set map camera right away if available
        if let rect = mapState?.cameraPosition.cameraRect {
            mapView.setRegion(.init(rect), animated: false)
        }
    }
    
    func bindToViewModel() {
        mapState?.$markers
            .receive(on: RunLoop.main)
            .sink { [weak self] updatedMarkers in
                self?.updateMarkers(updatedMarkers)
            }
            .store(in: &cancellables)
        
        mapState?.$isSatelliteMode
            .sink { [weak self] isSatellite in
                self?.setMapStyle(satellite: isSatellite)
            }
            .store(in: &cancellables)
        
        mapState?.$bottomSafeArea
            .receive(on: RunLoop.main)
            .sink { [weak self] bottomSafeArea in
                self?.updateSafeArea(bottomOffset: bottomSafeArea)
            }
            .store(in: &cancellables)
        
        mapState?.$cameraPosition
            .receive(on: RunLoop.main)
            .sink { [weak self] cameraPosition in
                if let rect = cameraPosition.cameraRect {
                    self?.mapView.setRegion(.init(rect), animated: cameraPosition.animate)
                }
            }
            .store(in: &cancellables)
    }
    
    private func setupGestures() {
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        let longPressGesture = UILongPressGestureRecognizer(target: self, action: #selector(handleMapLongPress(_:)))
        let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handleMapPan(_:)))
        
        panGesture.delegate = self
        
        mapView.addGestureRecognizer(tapGesture)
        mapView.addGestureRecognizer(longPressGesture)
        mapView.addGestureRecognizer(panGesture)
    }
    
    func setMapStyle(satellite: Bool) {
        mapView.mapType = satellite ? .hybrid : .standard
    }
    
    func updateSafeArea(bottomOffset: CGFloat) {
        positionAnimator.animate(
            from: mapState!.previousBottomSafeArea,
            to: bottomOffset,
            duration: 0.3,
            timingFunction: .easeOut,
            onUpdate: { [weak self] value in
                self?.mapView.layoutMargins = UIEdgeInsets(top: 0, left: 0, bottom: value, right: 0)
            }
        )
        mapState!.previousBottomSafeArea = bottomOffset
    }
    
    private func updateMarkers(_ newMarkers: [MapAnnotatedItem]) {
        
        let existingOverlays = mapView.overlays
        let existingAnnotations = mapView.annotations
        
        for marker in newMarkers {
            if let currentMarker = existingAnnotations.first(where: { marker.id == ($0 as? MapAnnotatedItem)?.id }) as? MapAnnotatedItem {
                if currentMarker.shouldUpdateView(other: marker) {
                    mapView.removeAnnotation(currentMarker)
                    mapView.addAnnotation(marker)
                } else {
                    // Animations are disabled for now
//                    UIView.animate(withDuration: 0.5, delay: 0, options: .curveEaseInOut){
                        currentMarker.data = marker.data
//                    }
                }
            } else {
                let annotation = marker
                mapView.addAnnotation(annotation)
            }
        }
        
        for marker in newMarkers {
            if let currentOverlay = existingOverlays.first(where: { marker.id == ($0 as? MapAnnotatedItem)?.id }) {
                mapView.removeOverlay(currentOverlay)
                if marker.data.shouldShowApproximateCircle {
                    let circle = MKCircle(center: marker.coordinate, radius: Double(marker.data.accuracy))
                    mapView.addOverlay(circle)
                }
            } else {
                if marker.data.shouldShowApproximateCircle {
                    let circle = MKCircle(center: marker.coordinate, radius: Double(marker.data.accuracy))
                    mapView.addOverlay(circle)
                }
            }
        }
        
        let toRemove = existingAnnotations.filter { current in
            !newMarkers.contains(where: { $0.id == (current as? MapAnnotatedItem)?.id })
        }
        let toRemoveOverlays = existingOverlays.filter { current in
            !newMarkers.contains(where: { $0.id == (current as? MapAnnotatedItem)?.id })
        }
        mapView.removeAnnotations(toRemove)
        mapView.removeOverlays(toRemoveOverlays)
        
        // Animate changes
        UIView.animate(withDuration: 0.3) {
            self.mapView.layoutIfNeeded()
        }
    }
    
    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        let point = gesture.location(in: mapView)
        let coordinate = mapView.convert(point, toCoordinateFrom: mapView)
        onMapTap?(coordinate)
    }
    
    @objc private func handleMapLongPress(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .began else { return }
        let point = gesture.location(in: mapView)
        let coordinate = mapView.convert(point, toCoordinateFrom: mapView)
        onMapLongPress?(coordinate)
    }
    
    @objc private func handleMapPan(_ gesture: UIPanGestureRecognizer) {
        guard gesture.state == .ended else { return }
        mapState?.cameraPosition = MapCameraState(animate: false, cameraRect: mapView.visibleMapRect)
        onMapPan?()
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
       return true
    }
}

// MARK: - MKMapViewDelegate
extension MapComponentVC: MKMapViewDelegate {
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        guard let customAnnotation = annotation as? MapAnnotatedItem else {return nil}
        
        let identifier = customAnnotation.id
        var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? SwiftUIMapMarkerView
        
        if annotationView == nil {
            annotationView = SwiftUIMapMarkerView(annotation: customAnnotation, reuseIdentifier: identifier)
        }
        
        // Setup SwiftUI content
        annotationView?.setupSwiftUIView(data: customAnnotation)
        
        return annotationView
    }
    
    func mapView(_ mapView: MKMapView, didSelect annotation: MKAnnotation) {
        guard let customAnnotation = annotation as? MapAnnotatedItem else { return }
        mapState?.onMarkerTap(markerData: customAnnotation.data.asFocusedMarker())
    }
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if let circle = overlay as? MKCircle {
            let renderer = MKCircleRenderer(circle: circle)
            renderer.fillColor = UIColor.blue.withAlphaComponent(0.2)
            return renderer
        }
        return MKOverlayRenderer()
    }
    
    func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
        mapState?.isUpdatingRegion = false
    }
    
    func mapView(_ mapView: MKMapView, regionWillChangeAnimated animated: Bool) {
        mapState?.isUpdatingRegion = true
    }
}
