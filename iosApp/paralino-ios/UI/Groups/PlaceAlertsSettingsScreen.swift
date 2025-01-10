//
//  PlaceDetailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-01-21.
//

import SwiftUI
import shared

struct PlaceAlertsSettingsScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: PlaceDetailState
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PlaceAlertsSettingsScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    )
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationBarBackButtonHidden(false)
        }
    }
}
