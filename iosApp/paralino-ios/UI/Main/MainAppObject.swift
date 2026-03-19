//
//  MainAppState.swift
//  paralino-ios
//
//  Created by Zelimir on 13.04.23.
//

import Foundation
import shared
import SwiftUI

class MainAppObject: ObservableObject {
    
    let vm: MainViewModel
    
    @Published var uiState: MainActivityState = MainActivityState.companion.doNewInstance() {
        didSet {
            accountState = ResourceSwift(data: uiState.userState)
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
                guard let self else { return }
                if let sessionExpiredDialog = self.uiState.sessionExpiredDialog {
                    self.sessionExpiredDialog = AlertDialogData(coreData: sessionExpiredDialog)
                }
                if let appUpdateDialog = self.uiState.appUpdateDialog {
                    self.appUpdateDialog = AlertDialogData(coreData: appUpdateDialog)
                }
            }
        }
    }
    
    @Published private(set) var accountState: ResourceSwift<AppAccountState> = .notInitialized
    @Published var sessionExpiredDialog: AlertDialogData? = nil
    @Published var appUpdateDialog: AlertDialogData? = nil
    
    @Published var appStartGraph: [WelcomeGraph] = []
    @Published var emailVerificationGraph: [EmailVerificationGraph] = []
    @Published var userReadyGraph: [NavigationGraph] = []
    
    @State private var deeplinkTask: Task<Void, Never>?
    
    init() {
        vm = KoinIOS.ViewModels.shared.mainVM
        vm.observe(flow: vm.uiState) { [weak self] state in
            if let uiState = state as? MainActivityState {
                self?.uiState = uiState
                self?.processDeeplink(deeplink: uiState.pendingDeeplinkType)
            }
        }
    }
    
    private func processDeeplink(deeplink: DeeplinkType?) {
        if accountState.data is AppAccountState.USER_READY,
            let actualDeeplink = deeplink as? DeeplinkType.ParalinoID {
            deeplinkTask?.cancel()
            DispatchQueue.main.async { [weak self] in
                self?.deeplinkTask = Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 500 * 1_000_000)
                    self?.userReadyGraph.append(.home(.memberInvite(groupId: nil, code: actualDeeplink.paralinoID, isOnboarding: false)))
                    self?.vm.setDeeplink(deeplinkType: nil)
                }
            }
        } else if accountState.data is AppAccountState.USER_READY,
                  let _ = deeplink as? DeeplinkType.PaywallScreen {
            deeplinkTask?.cancel()
            DispatchQueue.main.async { [weak self] in
                self?.deeplinkTask = Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 500 * 1_000_000)
                    self?.userReadyGraph.append(.settings(.paywall(groupId: nil, type: BillingScreenSourceType.normal)))
                    self?.vm.setDeeplink(deeplinkType: nil)
                }
            }
        } else if accountState.data is AppAccountState.NO_USER,
                  let actualDeeplink = deeplink as? DeeplinkType.PasswordReset {
            deeplinkTask?.cancel()
            DispatchQueue.main.async { [weak self] in
                self?.deeplinkTask = Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 500 * 1_000_000)
                    self?.appStartGraph = [.passwordReset(deeplink: actualDeeplink)]
                    self?.vm.setDeeplink(deeplinkType: nil)
                }
            }
        }
        
        // reset graph if state is out of graph
        if accountState.data is AppAccountState.USER_READY {
            appStartGraph = []
            emailVerificationGraph = []
        } else if accountState.data is AppAccountState.NO_USER {
            userReadyGraph = []
            emailVerificationGraph = []
        } else if accountState.data is AppAccountState.EMAIL_VERIFICATION {
            appStartGraph = []
            userReadyGraph = []
        }
    }
    
    deinit {
        deeplinkTask?.cancel()
        vm.cleanup()
    }
}
