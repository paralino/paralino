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
    
    var mapState: BaseMapObservableObject?
    
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
    
    var showsTraffic: Bool = false {
        didSet {
            mapView.showsTraffic = showsTraffic
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupMapView()
        setupGestures()
        bindToViewModel()
        
        // dark/light mode changes
        registerForTraitChanges([UITraitUserInterfaceStyle.self], handler: { (self: Self, previousTraitCollection: UITraitCollection) in
            if self.traitCollection.userInterfaceStyle != previousTraitCollection.userInterfaceStyle {
                self.refreshAnnotations()
            }
        })
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
        
        mapState?.$historyPinMarkers
            .removeDuplicates(by: { oldArray, newArray in
                guard oldArray.count == newArray.count else { return false }
                for (old, new) in zip(oldArray, newArray) {
                    if old.id != new.id {
                        return false
                    }
                }
                return true
            })
            .receive(on: RunLoop.main)
            .sink { [weak self] updatedMarkers in
                self?.updateHistoryPoints(updatedMarkers)
            }
            .store(in: &cancellables)
        
        mapState?.$clusterHistoryPins
            .removeDuplicates()
            .sink { [weak self] _ in
                self?.updateHistoryPoints(self?.mapState?.historyPinMarkers ?? [])
            }
            .store(in: &cancellables)
        
        mapState?.$polylinePoints
            .receive(on: RunLoop.main)
            .sink { [weak self] updatedPolyline in
                self?.updatePolyline(updatedPolyline)
            }
            .store(in: &cancellables)
        
        mapState?.$isSatelliteMode
            .removeDuplicates()
            .sink { [weak self] isSatellite in
                self?.setMapStyle(satellite: isSatellite)
            }
            .store(in: &cancellables)
        
        mapState?.$bottomSafeArea
            .removeDuplicates()
            .receive(on: RunLoop.main)
            .sink { [weak self] bottomSafeArea in
                self?.updateSafeArea(bottomOffset: bottomSafeArea)
            }
            .store(in: &cancellables)
        
        
        mapState?.$pendingFocusedMarker
            .debounce(for: .milliseconds(30), scheduler: RunLoop.main)
            .receive(on: RunLoop.main)
            .sink { [weak self] marker in
                if let marker = marker {
                    self?.mapState?.focusToMarker(
                        data: marker.marker,
                        forceZoom: marker.forceZoom,
                        animate: marker.animate,
                        canInteruptAnimation: true
                    )
                }
            }
            .store(in: &cancellables)
        
        mapState?.$cameraPosition
            .receive(on: RunLoop.main)
            .sink { [weak self] cameraPosition in
                if let rect = cameraPosition.cameraRect {
                    if cameraPosition.animate {
                        self?.mapView.setRegion(.init(rect), animated: true)
                    } else {
                        self?.mapView.setRegion(.init(rect), animated: false)
                    }
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
    
    private func refreshAnnotations() {
        // Get current annotations
        let currentAnnotations = mapView.annotations.filter { $0 is HistoryMapMarkerData }
        let currentOverlays = mapView.overlays.filter { $0 is MKPolyline }
        
        print("Refreshing annotations: \(currentAnnotations.count)")
        
        // For clustering to work properly, we need to completely recreate the annotations
        // because MapKit doesn't update clustering behavior of existing annotations
        mapView.removeAnnotations(currentAnnotations)
        mapView.removeOverlays(currentOverlays)
        
        // Add a small delay to ensure removal is complete
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.mapView.addAnnotations(currentAnnotations)
            self.mapView.addOverlays(currentOverlays)
        }
    }
    
    func updateSafeArea(bottomOffset: CGFloat) {
        //        positionAnimator.animate(
        //            from: mapState!.previousBottomSafeArea,
        //            to: bottomOffset,
        //            duration: 0.3,
        //            timingFunction: .easeOut,
        //            onUpdate: { [weak self] value in
        //                self?.mapView.layoutMargins = UIEdgeInsets(top: 0, left: 0, bottom: value, right: 0)
        //            }
        //        )
        mapView.layoutMargins = UIEdgeInsets(top: 0, left: 0, bottom: bottomOffset, right: 0)
        mapState!.previousBottomSafeArea = bottomOffset
    }
    
    private func updatePolyline(_ polylinePoints: [CLLocationCoordinate2D]) {
        
        let existingOverlays = mapView.overlays.filter { $0 is MKPolyline }
        let polyline = MKPolyline(coordinates: polylinePoints, count: polylinePoints.count)
        
        mapView.removeOverlays(existingOverlays)
        mapView.addOverlay(polyline)
        
        // Animate changes
        UIView.animate(withDuration: 0.3) {
            self.mapView.layoutIfNeeded()
        }
    }
    
    private func updateMarkers(_ newMarkers: [MapAnnotatedItem]) {
        
        let existingOverlays = mapView.overlays.filter { $0 is MKCircle }
        let existingAnnotations = mapView.annotations.filter { $0 is MapAnnotatedItem }
        
        for marker in newMarkers {
            if let currentMarker = existingAnnotations.first(where: { marker.id == ($0 as? MapAnnotatedItem)?.id }) as? MapAnnotatedItem {
                if currentMarker.shouldUpdateView(other: marker) {
                    mapView.removeAnnotation(currentMarker)
                    mapView.addAnnotation(marker)
                } else {
                    UIView.animate(withDuration: 0.2, delay: 0, options: .curveEaseInOut){
                        currentMarker.data = marker.data
                    }
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
    
    private func updateHistoryPoints(_ newMarkers: [HistoryMapMarkerData]) {
        
        let existingAnnotations = mapView.annotations.filter { $0 is HistoryMapMarkerData }
        
        mapView.removeAnnotations(existingAnnotations)
        mapView.addAnnotations(newMarkers)
        
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
        // Handle cluster annotations for history markers
        if let clusterAnnotation = annotation as? MKClusterAnnotation {
            let identifier = "HistoryClusterAnnotation"
            var clusterView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? SwiftUIHistoryClusterAnnotationView
            
            if clusterView == nil {
                clusterView = SwiftUIHistoryClusterAnnotationView(annotation: clusterAnnotation, reuseIdentifier: identifier)
            }
            
            // Update with member count
            clusterView?.annotation = clusterAnnotation
            clusterView?.updateCluster(count: clusterAnnotation.memberAnnotations.count)
            
            return clusterView
        }
        
        // Handle history map markers
        if let historyAnnotation = annotation as? HistoryMapMarkerData {
            let identifier = historyAnnotation.id
            var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? SwiftUIHistoryMapMarkerView
            
            if annotationView == nil {
                annotationView = SwiftUIHistoryMapMarkerView(annotation: historyAnnotation, reuseIdentifier: identifier)
            }
            
            // Set clustering identifier based on current state (always check current state)
            if mapState?.clusterHistoryPins == true {
                annotationView?.clusteringIdentifier = "HistoryMapMarkerData"
            } else {
                annotationView?.clusteringIdentifier = nil
            }
            
            // Setup SwiftUI content for history marker
            annotationView?.setupSwiftUIView(data: historyAnnotation, isDarkMode: mapView.mapType == .hybrid || traitCollection.userInterfaceStyle == .dark)
            
            return annotationView
        }
        
        // Handle regular map annotations
        if let customAnnotation = annotation as? MapAnnotatedItem {
            let identifier = customAnnotation.id
            var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? SwiftUIMapMarkerView
            
            if annotationView == nil {
                annotationView = SwiftUIMapMarkerView(annotation: customAnnotation, reuseIdentifier: identifier)
            }
            
            // Setup SwiftUI content
            annotationView?.setupSwiftUIView(data: customAnnotation)
            
            return annotationView
        }
        
        return nil
    }
    
    func mapView(_ mapView: MKMapView, didSelect annotation: MKAnnotation) {
        // Handle cluster selection for history markers
        if let clusterAnnotation = annotation as? MKClusterAnnotation {
            // Update visual state
            if let clusterView = mapView.view(for: clusterAnnotation) as? SwiftUIHistoryClusterAnnotationView {
                clusterView.updateCluster(count: clusterAnnotation.memberAnnotations.count, isSelected: true)
            }
            
            // Get the first history marker in the cluster to trigger the event
            if let firstHistoryMarker = clusterAnnotation.memberAnnotations.first as? HistoryMapMarkerData {
                mapState?.onHistoryMarkerTap(markerData: firstHistoryMarker)
            }
            
            // Zoom into cluster
            let memberAnnotations = clusterAnnotation.memberAnnotations
            let region = MKCoordinateRegion(coordinates: memberAnnotations.map { $0.coordinate })
            let adjustedRegion = mapView.regionThatFits(region)
            mapView.setRegion(adjustedRegion, animated: true)
            
            // Deselect after a delay to show the selection animation
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                mapView.deselectAnnotation(annotation, animated: true)
            }
            
            return
        }
        
        if let customAnnotation = annotation as? MapAnnotatedItem {
            mapState?.onMarkerTap(markerData: customAnnotation.data)
            return
        }
        
        if let historyAnnotation = annotation as? HistoryMapMarkerData {
            mapState?.onHistoryMarkerTap(markerData: historyAnnotation)
            return
        }
    }
    
    func mapView(_ mapView: MKMapView, didDeselect annotation: MKAnnotation) {
        // Handle cluster deselection for history markers
        if let clusterAnnotation = annotation as? MKClusterAnnotation {
            if let clusterView = mapView.view(for: clusterAnnotation) as? SwiftUIHistoryClusterAnnotationView {
                clusterView.updateCluster(count: clusterAnnotation.memberAnnotations.count, isSelected: false)
            }
        }
    }
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if let circle = overlay as? MKCircle {
            let renderer = MKCircleRenderer(circle: circle)
            renderer.fillColor = UIColor.blue.withAlphaComponent(0.2)
            return renderer
        } else if let polyline = overlay as? MKPolyline {
            let renderer = MKPolylineRenderer(polyline: polyline)
            
            let color = (mapView.mapType == .hybrid || traitCollection.userInterfaceStyle == .dark)
            ? UIColor(withAlphaRGB: AppColors.Raw.redMain)
            : UIColor(withAlphaRGB: AppColors.Raw.lightThemePrimary)
            
            renderer.strokeColor = color
            renderer.lineWidth = 5.0
            renderer.lineCap = .round
            renderer.lineJoin = .round
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

// MARK: - Extensions
extension MKCoordinateRegion {
    init(coordinates: [CLLocationCoordinate2D]) {
        var minLat = coordinates.first!.latitude
        var maxLat = coordinates.first!.latitude
        var minLon = coordinates.first!.longitude
        var maxLon = coordinates.first!.longitude
        
        for coordinate in coordinates {
            minLat = min(minLat, coordinate.latitude)
            maxLat = max(maxLat, coordinate.latitude)
            minLon = min(minLon, coordinate.longitude)
            maxLon = max(maxLon, coordinate.longitude)
        }
        
        let center = CLLocationCoordinate2D(
            latitude: (minLat + maxLat) / 2,
            longitude: (minLon + maxLon) / 2
        )
        
        let span = MKCoordinateSpan(
            latitudeDelta: max(maxLat - minLat, 0.01) * 1.2,
            longitudeDelta: max(maxLon - minLon, 0.01) * 1.2
        )
        
        self.init(center: center, span: span)
    }
}
