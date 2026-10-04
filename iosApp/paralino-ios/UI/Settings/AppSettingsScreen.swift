//
//  SubscriptionsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import SwiftUI
import shared

struct AppSettingsScreen: View {
    @Binding var path: [NavigationGraph]
    
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.appSettingsVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.AppSettingsScreenVC(
                    vm: appState.vm,
                    openAppThemeScreen: {
                        path.append(.settings(.appThemeSettings))
                    },
                    openMeasurementSystemScreen: {
                        path.append(.settings(.measurementSystemSettings))
                    },
                    openNotificationGroupingScreen: {
                        path.append(.settings(.notificationGroupingSettings))
                    },
                    openMapInteractionsScreen: {
                        path.append(.settings(.mapInteractionsSettings))
                    },
                    openNavigationAppScreen: {
                        path.append(.settings(.navigationAppSettings))
                    },
                    openPlaceSearchSettings: {
                        path.append(.settings(.placeSearchSettings))
                    },
                    openPersonalSpaceSettings: {
                        path.append(.settings(.personalSpaceSettings))
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.setting_appSettings_title.localizedString())
        }
    }
}
