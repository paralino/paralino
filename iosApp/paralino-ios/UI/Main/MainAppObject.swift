//
//  MainAppState.swift
//  paralino-ios
//
//  Created by Zelimir on 13.04.23.
//

import Foundation
import shared
import SwiftUICore

class MainAppObject: ObservableObject {
    
    let vm: MainViewModel
    
    @Published var uiState: MainActivityState = MainActivityState.companion.doNewInstance() {
        didSet {
            accountState = ResourceSwift(uiState.userState)
            let isSessionExpired = uiState.isSessionExpiredShown
            sessionExpiredDialog = isSessionExpired ? AlertDialogData(coreData: MainAlertDialogSessionExpired.shared) : nil
        }
    }
    
    @Published private(set) var accountState: ResourceSwift<AppAccountState> = .notInitialized
    @Published var sessionExpiredDialog: AlertDialogData? = nil
    
    @Published var appStartGraph: [WelcomeGraph] = []
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
           let inviteDeeplink = deeplink as? DeeplinkType.ParalinoID {
            deeplinkTask?.cancel()
            DispatchQueue.main.async { [weak self] in
                self?.deeplinkTask = Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 500 * 1_000_000)
                    self?.userReadyGraph.append(.home(.memberInvite(groupId: nil, code: inviteDeeplink.paralinoID, isOnboarding: false)))
                    self?.vm.setDeeplink(deeplinkType: nil)
                }
            }
        }
    }
    
    deinit {
        deeplinkTask?.cancel()
        vm.cleanup()
    }
}
