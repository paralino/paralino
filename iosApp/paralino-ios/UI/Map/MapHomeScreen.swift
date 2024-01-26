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
    
    var body: some View {
        ZStack {
            if mapState.uiState is MapHomeScreenState.Ready {
                ComposeRepresentable {
                    ScreensIOS.shared.home.MapSheet(
                        vm: mapState.vm,
                        initialUiState: mapState.uiState,
                        trackMarker: { data in
                            mapState.vm.trackMarker(markerData: data)
                        },
                        onOpenPlacesScreen: {
                            if let groupId = mapState.uiState.asReady()?.groupId {
                                path.append(.home(.places(groupId: groupId)))
                            }
                        },
                        onOpenCreatePlaceScreen: {
                            if let readyState = mapState.uiState.asReady() {
                                path.append(.home(.placeCreate(groupId: readyState.groupId, placeId: nil, lat: nil, lng: nil)))
                            }
                        },
                        onSheetOffsetChange: { offset in },
                        onGroupPickerClick: {
                            groupPicker = BottomSheetsScreen.GroupPicker(groups: mapState.vm.getAllGroups())
                        },
                        onGroupDetailsClick: {
                            if let groupId = mapState.uiState.asReady()?.groupId {
                                path.append(.home(.groupDetail(groupId: groupId)))
                            }
                        },
                        onSharingStartClick: {
                            if let groupId = mapState.uiState.asReady()?.groupId {
                                path.append(.home(.sharingStart(groupId: groupId)))
                            }
                        },
                        onSettingsClick: {
                            path.append(.settings(.settings))
                        },
                        onPlaceClick: { place in
                            path.append(.home(.placeDetail(groupId: place.groupId, placeId: place.id)))
                        },
                        mapUIView: { () -> UIView in
                            SwiftUIInUIView(
                                content: VStack {
                                    MapComponent(
                                        mapState: mapState,
                                        onMapTouch: {
                                            mapState.vm.onMapTouch()
                                        }
                                    )
                                    .safeAreaPadding(.top, 80)
                                    .safeAreaPadding(.horizontal)
                                }
                            )
                        }
                    )
                }
                .ignoresSafeArea(.all, edges: .vertical)
            } else if mapState.uiState is MapHomeScreenState.Empty {
                EmptyView()
            } else {
                ProgressView().progressViewStyle(CircularProgressViewStyle())
            }
            
            //            MapHomeScreenElements(
            //                state: mapState.uiState,
            //                pickGroup: {
            //                    groupPicker = BottomSheetsScreen.GroupPicker(groups: mapState.vm.getAllGroups())
            //                },
            //                openProfile: {
            //                    path.append(.settings(.settings))
            //                },
            //                focusOnGroup: {
            //                    mapState.vm.toggleFocusOnMembers()
            //                    let firstUserMarker = mapState.markers.first {
            //                        $0.data.pinType == MapPinType.member
            //                    }
            //                    if let marker = firstUserMarker {
            //                        region = MapCameraPosition.region(
            //                            MKCoordinateRegion(
            //                                center: CLLocationCoordinate2D(
            //                                    latitude: marker.coordinate.latitude,
            //                                    longitude: marker.coordinate.longitude
            //                                ),
            //                                span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
            //                            )
            //                        )
            //                    }
            //                }
            //            )
            
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
