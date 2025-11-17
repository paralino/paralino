//
//  AppStatusDebugScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-05-08.
//

import SwiftUI
import shared

struct AppStatusDebugScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.appStatusDebugVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.AppStatusDebugScreenVC(
                    vm: appState.vm
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle("App status")
        }
    }
}
