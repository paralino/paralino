//
//  MainAppState.swift
//  paralino-ios
//
//  Created by Zelimir on 13.04.23.
//

import Foundation
import shared

class MainAppObject: ObservableObject {
    
    let vm: MainViewModel
    @Published
    var uiState: ResourceSwift<AppAccountState> = .notInitialized
    
    init() {
        vm = KoinIOS.ViewModels.shared.mainVM
        vm.observe(flow: vm.uiState) { [weak self] state in
            let uiState = state as? MainActivityState
            self?.uiState = ResourceSwift(data: uiState?.userState)
        }
    }
    
    deinit {
        vm.cleanup()
    }
    
}
