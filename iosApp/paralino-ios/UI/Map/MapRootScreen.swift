//
//  MapHomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared
import paralino_apple_core

struct MapRootScreen: View {
    
    @EnvironmentObject var mainAppState: MainAppObject
    @StateObject var mapState = MapHomeState()
    
    var body: some View {
        NavigationStack(path: $mainAppState.userReadyGraph) {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                .navigationDestination(for: NavigationGraph.self) { route in
                    ZStack {
                        switch route {
                        case let .settings(settingsGraph):
                            switch settingsGraph {
                            case .account: AccountScreen(path: $mainAppState.userReadyGraph)
                            case .personalCodes: PersonalCodeScreen()
                            case .settings: SettingsScreen(path: $mainAppState.userReadyGraph)
                            case .changeEmail: ChangeEmailScreen(path: $mainAppState.userReadyGraph)
                            case .changePassword: ChangePasswordScreen(path: $mainAppState.userReadyGraph)
                            case .convertAccount: ConvertAccountScreen(path: $mainAppState.userReadyGraph)
                            case .blocklist: BlockListScreen(path: $mainAppState.userReadyGraph)
                            case .rawData(let type):
                                RawDataScreen(appState: RawDataScreen.createState(type: type))
                            case .editProfile: EditProfileScreen(path: $mainAppState.userReadyGraph)
                            case .appStatusDebug: AppStatusDebugScreen(path: $mainAppState.userReadyGraph)
                            case .licences: LicencesScreen(path: $mainAppState.userReadyGraph)
                            case .subscriptions: SubscriptionsScreen(path: $mainAppState.userReadyGraph)
                            case .paywall(let groupId, let type): PaywallScreen(path: $mainAppState.userReadyGraph, state: PaywallScreenState(groupId: groupId, type: type))
                            }
                        case let .home(homeGraph):
                            switch homeGraph {
                            case .groupCreate(let groupId): GroupCreateScreen(path: $mainAppState.userReadyGraph, appState: GroupCreateScreen.createState(groupId: groupId))
                            case .groupDetail(let groupId): GroupDetailScreen(path: $mainAppState.userReadyGraph, appState: GroupDetailScreen.createState(groupId: groupId))
                            case .groupSettings(let groupId): GroupSettingsScreen(path: $mainAppState.userReadyGraph, appState: GroupSettingsScreen.createState(groupId: groupId))
                            case .sharingStart(let groupId, let isOnboarding):
                                SharingStartScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: SharingStartScreen.createState(groupId: groupId, isOnboarding: isOnboarding)
                                )
                            case .map: MapHomeScreen(path: $mainAppState.userReadyGraph, mapState: mapState)
                            case .groupInit: GroupInitScreen(path: $mainAppState.userReadyGraph)
                            case .memberInvite(let groupId, let code, let isOnboarding):
                                MemberInviteScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: MemberInviteScreen.createState(groupId: groupId, code: code, isOnboarding: isOnboarding)
                                )
                            case .places(let groupId, let isOnboarding):
                                PlacesScreen(
                                    path: $mainAppState.userReadyGraph,
                                    appState: PlacesScreen.createState(groupId: groupId, isOnboarding: isOnboarding)
                                )
                            case .placeDetail(groupId: let groupId, placeId: let placeId):
                                PlaceDetailScreen(path: $mainAppState.userReadyGraph, appState: PlaceDetailScreen.createState(groupId: groupId, placeId: placeId))
                            case .placeCreate(groupId: let groupId, placeId: let placeId, lat: let lat, lng: let lng):
                                PlaceCreateScreen(path: $mainAppState.userReadyGraph, appState: PlaceCreateScreen.createState(groupId: groupId, placeId: placeId, lat: lat, lng: lng))
                            case .freePlanInfo: FreePlanInfoScreen(path: $mainAppState.userReadyGraph)
                            }
                        }
                    }.onAppear {
                        Logr.d(message: "Route: \(route)")
                    }
                }
        }
        .onChange(of: mapState.groupState, initial: true) { oldValue, newValue in
            if newValue == true {
                if mainAppState.userReadyGraph.isEmpty == true {
                    mainAppState.userReadyGraph = [.home(.map)]
                } else {
                    mainAppState.userReadyGraph[0] = .home(.map)
                }
            } else if newValue == false {
                if mainAppState.userReadyGraph.isEmpty == true {
                    mainAppState.userReadyGraph = [.home(.groupInit)]
                } else {
                    mainAppState.userReadyGraph[0] = .home(.groupInit)
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
