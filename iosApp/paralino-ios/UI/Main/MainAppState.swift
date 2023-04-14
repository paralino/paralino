//
//  MainAppState.swift
//  paralino-ios
//
//  Created by Zelimir on 13.04.23.
//

import Foundation
import shared

class MainAppState: ObservableObject {
    
    let vm: MainViewModel
    @Published
    var uiState: ResourceSwift<AppAccountState> = .notInitialized
    
    init() {
        vm = KoinIOS.ViewModels.shared.mainVM
        vm.observe(flow: vm.userState) { [weak self] state in
            self?.uiState = ResourceSwift(data: state)
        }
    }
    
    deinit {
        vm.cleanup()
    }
    
}
