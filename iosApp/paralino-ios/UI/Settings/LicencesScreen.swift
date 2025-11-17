//
//  LicencesScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-11-04.
//

import SwiftUI
import shared

struct LicencesScreen: View {
    @Binding var path: [NavigationGraph]
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.LicencesScreenVC()
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.setting_title_libraries.localizedString())
        }
    }
}
