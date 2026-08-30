//
//  MainApp.swift
//  paralino-ios
//
//  Created by Zelimir on 23.03.23.
//

import Sentry
import StoreKit
import SwiftUI
import paralino_apple_core
import shared

@main
struct MainApp: App {
    
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject var appState = MainAppObject()
    
    @Environment(\.scenePhase) var scenePhase
    
    @State var isOrWasActive = false
    
    init() {
        AppDelegate.sharedInstance = appDelegate
        
        SentrySDK.start { options in
            options.dsn = Bundle.main.object(forInfoDictionaryKey: "glitchTipDSNiOS") as? String
            options.enableUserInteractionTracing = false
            options.attachScreenshot = false
            options.attachViewHierarchy = false
            options.enableAppHangTracking = false
            options.debug = false
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                if isOrWasActive {
                    switch appState.accountState {
                    case .notInitialized:
                        VStack {
                            ProgressView().progressViewStyle(
                                CircularProgressViewStyle()
                            )
                        }
                    case .ready(let data): AppReadyScreen(userState: data)
                    }
                }
            }
            .onChange(of: scenePhase, initial: true) { oldScene, newScene in
                Logr.d(
                    tag: "delegate",
                    message: "App scene switch (old: \(oldScene)) - (new: \(newScene))"
                )
                
                switch newScene {
                case .active:
                    isOrWasActive = true
                    appDelegate.appInForeground()
                case .inactive:
                    appDelegate.appInactive()
                case .background:
                    appDelegate.appInBackground()
                @unknown default:
                    Logr.i(tag: "delegate", message: "App scene unknown")
                    notifyScene(message: "\(oldScene) -> Unknown")
                }
            }
            .environmentObject(appDelegate)
            .environmentObject(appDelegate.locationManager)
            .environmentObject(appState)
            .onOpenURL { url in
                if let deeplink = DeeplinkType.companion.parse(
                    deeplink: url.absoluteString
                ) {
                    appState.vm.setDeeplink(deeplinkType: deeplink)
                } else {
                    Logr.w(tag: "delegate", message: "Unknown url launched")
                }
            }
            .mainAlert($appState.sessionExpiredDialog) { action in
                if let dialog = appState.uiState.sessionExpiredDialog {
                    appState.vm.onEvent(event: MainActivityUiEventDialogAction(action: AlertDialogAction(dialog: dialog, action: action)))
                }
            }
            .mainAlert($appState.appUpdateDialog) { action in
                if let dialog = appState.uiState.appUpdateDialog {
                    appState.vm.onEvent(event: MainActivityUiEventDialogAction(action: AlertDialogAction(dialog: dialog, action: action)))
                }
            }
            .loadingOverlay(
                isLoading: appState.uiState.dataRefreshBlockingLoadingShown
            )
        }
        .onChange(
            of: appDelegate.appRatingPresenter.appRatingRequested,
            initial: true
        ) { oldState, newState in
            if newState == true {
                Logr.i(
                    tag: "app_rating",
                    message: "Trigger iOS app rating dialog"
                )
                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
#if os(iOS)
                    if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                        SKStoreReviewController.requestReview(in: scene)
                    }
#else
                    SKStoreReviewController.requestReview()
#endif
                }
            }
        }
    }
    
    private func notifyScene(id: String = "scene", message: String) {
        
        guard AppBuildConfig.Companion.shared.instance.isDevMode else { return }
        
        let content = UNMutableNotificationContent()
        content.title = "Paralino scene switch"
        content.body = message
        
        let request = UNNotificationRequest(
            identifier: id,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request) { (error: Error?) in }
    }
}

struct AppReadyScreen: View {
    let userState: AppAccountState
    var body: some View {
        if userState is AppAccountState.ACCOUNT_RESET {
            RecoverKeysScreen()
        } else if userState is AppAccountState.EMAIL_VERIFICATION {
            VerifyEmailScreen()
        } else if userState is AppAccountState.NO_PROFILE {
            ProfileSetupScreen()
        } else if userState is AppAccountState.NO_USER {
            WelcomeScreen()
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
