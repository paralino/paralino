//
//  LocationSettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-12-02.
//

import SwiftUI
import shared

struct LocationSettingsScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.locationSettingsVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.LocationSettingsScreenVC(
                    vm: appState.vm,
                    openPaywall: {
                        path.append(.settings(.paywall(groupId: nil, type: .upgradeDefault)))
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.setting_location_title.localizedString())
        }
    }
}
