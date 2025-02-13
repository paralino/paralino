//
//  MainApp.swift
//  paralino-ios
//
//  Created by Zelimir on 23.03.23.
//

import SwiftUI
import shared
import paralino_apple_core

@main
struct MainApp: App {
    
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject var appState = MainAppObject()
    
    @Environment(\.scenePhase) var scenePhase
    @Environment(\.requestReview) private var requestReview
    
    @State var isOrWasActive = false
    
    init() {
        AppDelegate.sharedInstance = appDelegate
    }
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                if isOrWasActive {
                    switch appState.accountState {
                    case .notInitialized: VStack {
                        ProgressView().progressViewStyle(CircularProgressViewStyle())
                    }
                    case .ready(let data): AppReadyScreen(userState: data)
                    }
                }
            }
            .onChange(of: scenePhase, initial: true) { oldScene, newScene in
                Logr.d(tag:"delegate", message: "App scene switch (old: \(oldScene)) - (new: \(newScene))")
                
                switch newScene {
                case .active:
                    isOrWasActive = true
                    appDelegate.appInForeground()
                case .inactive:
                    appDelegate.appInactive()
                case .background:
                    appDelegate.appInBackground()
                @unknown default:
                    Logr.i(tag:"delegate", message: "App scene unknown")
                    notifyScene(message: "\(oldScene) -> Unknown")
                }
            }
            .environmentObject(appDelegate)
            .environmentObject(appDelegate.locationManager)
            .environmentObject(appState)
            .onOpenURL { url in
                if let deeplink = DeeplinkType.companion.parse(deeplink: url.absoluteString) {
                    appState.vm.setDeeplink(deeplinkType: deeplink)
                } else {
                    Logr.w(tag:"delegate", message: "Unknown url launched")
                }
            }
            .mainAlert($appState.sessionExpiredDialog) { action in
                appState.vm.cancelSessionExpired()
            }
            .loadingOverlay(isLoading: appState.uiState.dataRefreshBlockingLoadingShown)
            .onChange(of: appDelegate.appRatingPresenter.appRatingRequested, initial: false) { oldState, newState in
                if newState == true {
                    Task {
                        try await Task.sleep(for: .seconds(2))
                        Logr.v(tag: "app_rating", message: "Trigger iOS app rating dialog")
                        requestReview()
                    }
                }
            }
            
        }
    }
    
    private func notifyScene(id: String = "scene", message: String) {
        
        guard AppBuildConfig.Companion.shared.instance.isDevMode else { return }
        
        let content = UNMutableNotificationContent()
        content.title = "Paralino scene switch"
        content.body = message
    
        let request = UNNotificationRequest(identifier: id, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { (error : Error?) in }
    }
}

struct AppReadyScreen: View {
    let userState: AppAccountState
    var body: some View {
        if userState is AppAccountState.ACCOUNT_RESET {
            RecoverKeysScreen()
        } else if userState is AppAccountState.EMAIL_VERIFICATION {
            NavigationStack {
                VerifyEmailScreen()
            }
        } else if userState is AppAccountState.NO_PROFILE {
            ProfileSetupScreen()
        } else if userState is AppAccountState.NO_USER {
            NavigationStack {
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
