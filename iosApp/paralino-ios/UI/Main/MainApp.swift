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
    @StateObject var appState = MainAppObject()
    @Environment(\.scenePhase) var scenePhase
    
    init() {
        AppDelegate.shared = appDelegate
    }
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                switch appState.uiState {
                case .notInitialized: VStack {
                    ProgressView().progressViewStyle(CircularProgressViewStyle())
                }
                case .ready(let data): AppReadyScreen(userState: data)
                }
            }
            .onChange(of: scenePhase, initial: true) { old, newScenePhase in
                switch newScenePhase {
                case .active:
                    appDelegate.appInForeground()
                case .inactive: break
                case .background:
                    appDelegate.appInBackground()
                @unknown default:
                    Logr.i(tag:"delegate", message: "App scene unknown")
                }
            }
            .environmentObject(appDelegate)
            .environmentObject(appDelegate.locationManager)
        }
    }
}

struct AppReadyScreen: View {
    let userState: AppAccountState
    var body: some View {
        if userState is AppAccountState.ACCOUNT_RESET {
            RecoverKeysScreen()
        } else if userState is AppAccountState.NO_PROFILE {
            ProfileSetupScreen()
        } else if userState is AppAccountState.NO_USER {
            NavigationView {
                WelcomeScreen()
            }
        } else if userState is AppAccountState.NO_USER_DATA || userState is AppAccountState.DATA_REFRESH {
            VStack {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
            }
        } else if userState is AppAccountState.USER_READY {
            MapRootScreen()
        }
    }
}
