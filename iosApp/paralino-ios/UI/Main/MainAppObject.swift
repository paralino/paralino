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
    @Published
    var uiState: ResourceSwift<AppAccountState> = .notInitialized
    
    @Published var appStartGraph: [WelcomeGraph] = []
    @Published var userReadyGraph: [NavigationGraph] = []
    
    @State private var deeplinkTask: Task<Void, Never>?
    
    init() {
        vm = KoinIOS.ViewModels.shared.mainVM
        vm.observe(flow: vm.uiState) { [weak self] state in
            let uiState = state as? MainActivityState
            self?.uiState = ResourceSwift(data: uiState?.userState)
            self?.processDeeplink(deeplink: uiState?.pendingDeeplinkType)
        }
    }
    
    private func processDeeplink(deeplink: DeeplinkType?) {
        if self.uiState.data is AppAccountState.USER_READY,
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
