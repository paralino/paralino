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
                            case .changeEmail: ChangeEmailScreen()
                            case .changePassword: ChangePasswordScreen()
                            case .convertAccount: ConvertAccountScreen(path: $mapState.path)
                            case .blocklist: BlockListScreen(path: $mapState.path)
                            case .rawData(let type):
                                RawDataScreen(appState: RawDataScreen.createState(type: type))
                            case .editProfile: EditProfileScreen()
                            }
                        case let .home(homeGraph):
                            switch homeGraph {
                            case .groupCreate(let groupId): GroupCreateScreen(path: $mapState.path, appState: GroupCreateScreen.createState(groupId: groupId))
                            case .groupDetail(let groupId): GroupDetailScreen(path: $mapState.path, appState: GroupDetailScreen.createState(groupId: groupId))
                            case .sharingStart(let groupId): SharingStartScreen(path: $mapState.path, appState: SharingStartScreen.createState(groupId: groupId))
                            case .map: MapHomeScreen(path: $mapState.path, mapState: mapState)
                            case .groupInit: GroupInitScreen(path: $mapState.path)
                            case .memberInvite(let groupId, let code): MemberInviteScreen(path: $mapState.path, appState: MemberInviteScreen.createState(groupId: groupId, code: code))
                            }
                        }
                    }.onAppear {
                        Logr.i(message: "Route: \(route)")
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
