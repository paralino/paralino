//
//  UserDevicesScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 25.09.26.
//

import SwiftUI
import shared

struct UserDevicesScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.userDevicesVM)

    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.UserDevicesScreenVC(
                    vm: appState.vm
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_account_devices_screen_title.localizedString())
        }
    }
}
