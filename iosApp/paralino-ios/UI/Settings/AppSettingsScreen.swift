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
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.AppSettingsScreenVC(
                    openMeasurementSystemScreen: {
                        path.append(.settings(.measurementSystemSettings))
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.title_settings.localizedString())
        }
    }
}
