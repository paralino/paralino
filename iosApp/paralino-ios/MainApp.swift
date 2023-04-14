//
//  MainApp.swift
//  paralino-ios
//
//  Created by Zelimir on 23.03.23.
//

import SwiftUI
import shared

@main
struct MainApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject var appState = MainAppState()
    
    var body: some Scene {
        WindowGroup {
            switch appState.uiState {
            case .notInitialized: ContentView(state: "not ready")
            case .ready(let state): ContentView(state: state.debugDescription)
            }
        }
    }
}
