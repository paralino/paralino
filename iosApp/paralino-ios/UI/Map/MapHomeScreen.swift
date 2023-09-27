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
    @State private var region: MKCoordinateRegion = MKCoordinateRegion(
        center: CLLocationCoordinate2D(
            latitude: 0,
            longitude: 0
        ),
        span: MKCoordinateSpan(latitudeDelta: 60, longitudeDelta: 60)
    )
    
    var body: some View {
        ZStack {
            if mapState.uiState is MapHomeScreenState.Ready {
                Map(coordinateRegion: $region, showsUserLocation: true, annotationItems: mapState.markers) { marker in
                    MapMarker(coordinate: marker.coordinate, tint: marker.type == MapPinType.member ? Color.themePrimary : Color.redMain)
                }
                    .ignoresSafeArea(.all, edges: .vertical)
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
                        $0.type == MapPinType.member
                    }
                    if let marker = firstUserMarker {
                        region = MKCoordinateRegion(
                            center: CLLocationCoordinate2D(
                                latitude: marker.coordinate.latitude,
                                longitude: marker.coordinate.longitude
                            ),
                            span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
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
