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
    @State private var path: [NavigationGraph] = []
    
    var body: some View {
        switch mapState.groupState {
        case nil: ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
        case true: NavigationStack(path: $path) {
            MapHomeScreen()
        }
        case false: NavigationStack(path: $path) {
            GroupInitScreen(path: $path)
                .navigationDestination(for: NavigationGraph.self) { route in
                    ZStack {
                        switch route {
                        case let .settings(settingsGraph):
                            switch settingsGraph {
                            case .account: AccountScreen(path: $path)
                            case .personalCodes: PersonalCodeScreen()
                            case .settings: SettingsScreen(path: $path)
                            case .changeEmail: ChangeEmailScreen()
                            case .changePassword: ChangePasswordScreen()
                            case .convertAccount: ConvertAccountScreen(path: $path)
                            case .blocklist: BlockListScreen(path: $path)
                            case .rawData(let type):
                                RawDataScreen(appState: RawDataScreen.createState(type: type))
                            case .editProfile: EditProfileScreen()
                            }
                        default:
                            GroupInitScreen(path: $path)
                        }
                    }.onAppear {
                        Logr.i(message: "Route: \(route)")
                    }
                }
        }
        case .some(_):
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
        }
    }
}

struct MapRootScreen_Previews: PreviewProvider {
    static var previews: some View {
        MapRootScreen()
    }
}
