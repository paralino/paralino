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
    @State var groupPicker: BottomSheetPickerTypeGroupPickerBottomSheet? = nil
    
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
                                path.append(.home(.places(groupId: groupId, isOnboarding: false)))
                            }
                        },
                        onOpenCreatePlaceScreen: {
                            if let readyState = mapState.uiState.asReady() {
                                path.append(.home(.placeCreate(groupId: readyState.groupId, placeId: nil, lat: nil, lng: nil)))
                            }
                        },
                        onSheetOffsetChange: { offset in
                            withAnimation(.linear(duration: 0.1)) {
                                mapState.bottomSafeArea = CGFloat(offset.floatValue) - 35
                            }
                        },
                        onGroupPickerClick: {
                            groupPicker = mapState.vm.getGroupPickerState()
                        },
                        onGroupDetailsClick: {
                            if let groupId = mapState.uiState.asReady()?.groupId {
                                path.append(.home(.groupDetail(groupId: groupId)))
                            }
                        },
                        onSharingStartClick: {
                            if let groupId = mapState.uiState.asReady()?.groupId {
                                path.append(.home(.sharingStart(groupId: groupId, isOnboarding: false)))
                            }
                        },
                        onSettingsClick: {
                            path.append(.settings(.settings))
                        },
                        onPlaceClick: { place in
                            path.append(.home(.placeDetail(groupId: place.groupId, placeId: place.id)))
                        },
                        onCreateGroup: {
                            path.append(.home(.groupCreate(groupId: nil)))
                        },
                        openPaywall: {
                            
                        },
                        mapUIView: { () -> UIView in
                            SwiftUIInUIView(
                                content: VStack {
                                    MapComponent(
                                        mapState: mapState,
                                        onMapTap: { point in },
                                        onMapLongPress: { point in
                                            if let readyState = mapState.uiState.asReady() {
                                                path.append(.home(.placeCreate(groupId: readyState.groupId, placeId: nil, lat: point.latitude, lng: point.longitude)))
                                            }
                                        },
                                        onMapTouch: {
                                            mapState.vm.onMapTouch()
                                        }
                                    )
                                    .safeAreaPadding(.top, 70)
                                }
                            )
                        }
                    )
                }
                .ignoresSafeArea(.all, edges: .vertical)
            } else if mapState.uiState is MapHomeScreenState.Empty {
                VStack {
                    Image("paralino_logo")
                        .resizable()
                        .scaledToFit()
                        .colorMultiply(.themePrimary)
                        .frame(width: 100, height: 100)
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                }
            } else {
                VStack {
                    Image("paralino_logo")
                        .resizable()
                        .scaledToFit()
                        .colorMultiply(.themePrimary)
                        .frame(width: 60, height: 60)
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                }
            }
        }
        .onAppear {
            mapState.vm.switchGroup(groupId: "")
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
                mapState.vm.tryCreateGroup()
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
