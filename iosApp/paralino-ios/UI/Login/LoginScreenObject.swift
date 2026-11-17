//
//  LoginScreenObject.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import Foundation
import shared

@MainActor
class LoginScreenObject: ObservableObject {
    // Read by the nonisolated deinit to clean up the view model.
    nonisolated(unsafe) let vm: LoginViewModel
    @Published
    var uiState: ResourceSwift<LoginScreenState> = .notInitialized
    
    init() {
        vm = KoinIOS.ViewModels.shared.loginVM(screenUiType: .signup)
        vm.observe(flow: vm.uiState) { [weak self] state in
            self?.uiState = ResourceSwift(data: state)
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
