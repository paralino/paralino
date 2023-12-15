//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared
import MapKit

struct MapHomeScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var mapState: MapHomeState
    @State var groupPicker: BottomSheetsScreen.GroupPicker? = nil
    
    @State private var selectedMarkerId: String?
    @State private var region: MapCameraPosition = MapCameraPosition.region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: 0,
                longitude: 0
            ),
            span: MKCoordinateSpan(latitudeDelta: 60, longitudeDelta: 60)
        )
    )
    
    var body: some View {
        ZStack {
            if mapState.uiState is MapHomeScreenState.Ready {
                MapReader { reader in
                    Map(position: $region, selection: $selectedMarkerId) {
                        ForEach(mapState.markers) { marker in
                            
                            MapCircle(center: marker.coordinate, radius: CLLocationDistance(Int32(marker.data.accuracy)))
                                .foregroundStyle(marker.data.pinType == MapPinType.member ? Color.themePrimary.opacity(0.6) : Color.blue.opacity(0.4))
                                .mapOverlayLevel(level: marker.data.pinType == MapPinType.member ? .aboveLabels : .aboveRoads)
                            
                            Annotation(
                                coordinate: marker.coordinate,
                                anchor: UnitPoint(x: 0.5, y: marker.data.markerStyle == MapMarkerStyle.pin ? 1.0 : 0.5),
                                content: {
                                    Button(
                                        action: { mapState.vm.trackMarker(markerData: FocusedMarkerData(markerId: marker.id, type: marker.data.pinType)) },
                                        label: { MapMarker(pinData: marker.data) }
                                    )
                                },
                                label: {
                                    Text(marker.data.title ?? "--")
                                }
                            )
                        }
                    }
                    .mapControls {
//                        MapUserLocationButton()
                        MapCompass()
                    }
                    .mapStyle(.hybrid)
                    .safeAreaPadding(.top, 80)
                    .safeAreaPadding(.horizontal)
                    .onChange(of: selectedMarkerId, initial: true) { old, new in
                        guard let selection = new else { return }
                        let marker = mapState.markers.first { $0.id == selection }
                        guard let marker = marker else { return }
                        let center = CLLocationCoordinate2D(
                            latitude: marker.coordinate.latitude,
                            longitude: marker.coordinate.longitude
                        )
                        
                        region = MapCameraPosition.camera(MapCamera(centerCoordinate: center, distance: 3000))
                    }
                }
                .onTapGesture {
                    // TODO fix: not working
                    mapState.vm.onMapTouch()
                }
            } else if mapState.uiState is MapHomeScreenState.Empty {
                EmptyView()
            } else {
                ProgressView().progressViewStyle(CircularProgressViewStyle())
            }
            
            MapHomeScreenElements(
                state: mapState.uiState,
                pickGroup: {
                    groupPicker = BottomSheetsScreen.GroupPicker(groups: mapState.vm.getAllGroups())
                },
                openProfile: {
                    path.append(.settings(.settings))
                },
                focusOnGroup: {
                    mapState.vm.toggleFocusOnMembers()
                    let firstUserMarker = mapState.markers.first {
                        $0.data.pinType == MapPinType.member
                    }
                    if let marker = firstUserMarker {
                        region = MapCameraPosition.region(
                            MKCoordinateRegion(
                                center: CLLocationCoordinate2D(
                                    latitude: marker.coordinate.latitude,
                                    longitude: marker.coordinate.longitude
                                ),
                                span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
                            )
                        )
                    }
                }
            )
        }
        .groupPickerSheet(
            $groupPicker,
            onSelectGroup: { state in
                groupPicker = nil
                mapState.vm.switchGroup(groupId: state.id)
            },
            onEditGroup: { state in
                groupPicker = nil
                path.append(.home(.groupDetail(groupId: state.id)))
            },
            onCreateGroup: {
                groupPicker = nil
                path.append(.home(.groupCreate(groupId: nil)))
            },
            onJoinGroup: {
                groupPicker = nil
                path.append(.settings(.personalCodes))
            }
        )
        .navigationBarBackButtonHidden(true)
    }
}

//struct MapHomeScreen_Previews: PreviewProvider {
//    @State static var path: [NavigationGraph] = [.home(.map)]
//    @StateObject var mapState = MapHomeState()
//    static var previews: some View {
//        MapHomeScreen(path: $path, mapState: mapState)
//    }
//}
