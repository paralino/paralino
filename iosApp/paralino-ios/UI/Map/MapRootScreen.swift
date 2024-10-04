//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct MapRootScreen: View {
    
    @StateObject var mapState = MapHomeState()
    
    var body: some View {
        NavigationStack(path: $mapState.path) {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                .navigationDestination(for: NavigationGraph.self) { route in
                    ZStack {
                        switch route {
                        case let .settings(settingsGraph):
                            switch settingsGraph {
                            case .account: AccountScreen(path: $mapState.path)
                            case .personalCodes: PersonalCodeScreen()
                            case .settings: SettingsScreen(path: $mapState.path)
                            case .changeEmail: ChangeEmailScreen(path: $mapState.path)
                            case .changePassword: ChangePasswordScreen(path: $mapState.path)
                            case .convertAccount: ConvertAccountScreen(path: $mapState.path)
                            case .blocklist: BlockListScreen(path: $mapState.path)
                            case .rawData(let type):
                                RawDataScreen(appState: RawDataScreen.createState(type: type))
                            case .editProfile: EditProfileScreen(path: $mapState.path)
                            case .appStatusDebug: AppStatusDebugScreen(path: $mapState.path)
                            case .subscriptions: SubscriptionsScreen(path: $mapState.path)
                            case .paywall(type: let type): SubscriptionsScreen(path: $mapState.path)
                            }
                        case let .home(homeGraph):
                            switch homeGraph {
                            case .groupCreate(let groupId): GroupCreateScreen(path: $mapState.path, appState: GroupCreateScreen.createState(groupId: groupId))
                            case .groupDetail(let groupId): GroupDetailScreen(path: $mapState.path, appState: GroupDetailScreen.createState(groupId: groupId))
                            case .groupSettings(let groupId): GroupSettingsScreen(path: $mapState.path, appState: GroupSettingsScreen.createState(groupId: groupId))
                            case .sharingStart(let groupId, let isOnboarding):
                                SharingStartScreen(
                                    path: $mapState.path,
                                    appState: SharingStartScreen.createState(groupId: groupId, isOnboarding: isOnboarding)
                                )
                            case .map: MapHomeScreen(path: $mapState.path, mapState: mapState)
                            case .groupInit: GroupInitScreen(path: $mapState.path)
                            case .memberInvite(let groupId, let code, let isOnboarding):
                                MemberInviteScreen(
                                    path: $mapState.path,
                                    appState: MemberInviteScreen.createState(groupId: groupId, code: code, isOnboarding: isOnboarding)
                                )
                            case .places(let groupId, let isOnboarding):
                                PlacesScreen(
                                    path: $mapState.path,
                                    appState: PlacesScreen.createState(groupId: groupId, isOnboarding: isOnboarding)
                                )
                            case .placeDetail(groupId: let groupId, placeId: let placeId):
                                PlaceDetailScreen(path: $mapState.path, appState: PlaceDetailScreen.createState(groupId: groupId, placeId: placeId))
                            case .placeCreate(groupId: let groupId, placeId: let placeId, lat: let lat, lng: let lng):
                                PlaceCreateScreen(path: $mapState.path, appState: PlaceCreateScreen.createState(groupId: groupId, placeId: placeId, lat: lat, lng: lng))
                            case .freePlanInfo: FreePlanInfoScreen(path: $mapState.path)
                            }
                        }
                    }.onAppear {
                        Logr.d(message: "Route: \(route)")
                    }
                }
        }
    }
}

struct MapRootScreen_Previews: PreviewProvider {
    static var previews: some View {
        MapRootScreen()
    }
}
